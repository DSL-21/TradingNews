#!/usr/bin/env python3
"""Fetch a small, licensed economic-calendar snapshot for the Android client.

The API key is read only from FC_API_KEY in the GitHub Actions environment.
It is never written into the repository or the generated snapshot.
"""
from __future__ import annotations

import datetime as dt
import json
import os
import pathlib
import tempfile
import urllib.parse
import urllib.request

BASE_URL = "https://www.financecalendar.com/wp-json/fc/v1/calendar"
COUNTRY_GROUPS = ("US,GB,EU", "DE,JP,CH", "CA,AU,NZ")
OUTPUT = pathlib.Path("data/economic-calendar.json")
MAX_EVENTS_PER_RESPONSE = 20


def fetch_group(api_key: str, countries: str, start: dt.date, end: dt.date) -> dict:
    query = urllib.parse.urlencode({
        "from": start.isoformat(),
        "to": end.isoformat(),
        "country": countries,
        "limit": str(MAX_EVENTS_PER_RESPONSE),
    })
    request = urllib.request.Request(
        f"{BASE_URL}?{query}",
        headers={
            "Accept": "application/json",
            "X-API-Key": api_key,
            "User-Agent": "TradingNews-GitHubCalendarUpdater/1.0",
        },
        method="GET",
    )
    with urllib.request.urlopen(request, timeout=25) as response:
        if response.status != 200:
            raise RuntimeError(f"Finance Calendar returned HTTP {response.status}")
        payload = response.read(1_000_001)
    if len(payload) > 1_000_000:
        raise RuntimeError("Finance Calendar response exceeded 1 MB")
    parsed = json.loads(payload)
    if not isinstance(parsed.get("events"), list):
        raise RuntimeError("Finance Calendar response has no events array")
    if len(parsed["events"]) > MAX_EVENTS_PER_RESPONSE:
        raise RuntimeError("Finance Calendar exceeded the documented free response limit")
    attribution = parsed.get("attribution")
    if not isinstance(attribution, dict) or not attribution.get("source"):
        raise RuntimeError("Finance Calendar response is missing attribution")
    return parsed


def build_snapshot(responses: list[dict], fetched_at: dt.datetime) -> dict:
    events: dict[str, dict] = {}
    truncated = False
    generated_values: list[str] = []
    sources: list[dict] = []
    for response in responses:
        truncated = truncated or bool(response.get("truncated", False))
        generated = response.get("generated_at")
        if isinstance(generated, str) and generated:
            generated_values.append(generated)
        attribution = response.get("attribution")
        if isinstance(attribution, dict):
            sources.append(attribution)
        for event in response["events"]:
            if not isinstance(event, dict):
                continue
            event_id = event.get("id")
            if not isinstance(event_id, str) or not event_id.strip():
                continue
            events[event_id] = event

    attribution = next(
        (item for item in sources if item.get("url") == "https://www.financecalendar.com/"),
        sources[0] if sources else None,
    )
    if not attribution:
        raise RuntimeError("No provider attribution was returned")

    ordered = sorted(
        events.values(),
        key=lambda item: (
            item.get("date", "9999-12-31"),
            item.get("release_at_utc") or "9999-12-31T23:59:59Z",
            item.get("name", ""),
        ),
    )
    return {
        "schema_version": 1,
        "fetched_at": fetched_at.replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "generated_at": max(generated_values) if generated_values else None,
        "truncated": truncated,
        "country_groups": list(COUNTRY_GROUPS),
        "attribution": attribution,
        "events": ordered,
    }


def main() -> None:
    api_key = os.environ.get("FC_API_KEY", "").strip()
    if not api_key:
        raise SystemExit("FC_API_KEY is required; configure it as a GitHub Actions repository secret.")

    now = dt.datetime.now(dt.timezone.utc)
    start = now.date()
    end = start + dt.timedelta(days=6)
    responses = [fetch_group(api_key, group, start, end) for group in COUNTRY_GROUPS]
    snapshot = build_snapshot(responses, now)

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    encoded = json.dumps(snapshot, ensure_ascii=False, indent=2) + "\n"
    with tempfile.NamedTemporaryFile(
        "w", encoding="utf-8", dir=OUTPUT.parent, delete=False
    ) as temporary:
        temporary.write(encoded)
        temporary_path = pathlib.Path(temporary.name)
    temporary_path.replace(OUTPUT)
    print(
        f"Published {len(snapshot['events'])} events; "
        f"truncated={snapshot['truncated']}; fetched_at={snapshot['fetched_at']}"
    )


if __name__ == "__main__":
    main()
