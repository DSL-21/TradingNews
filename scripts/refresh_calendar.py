#!/usr/bin/env python3
"""Build the public, attributed Free Developer economic-calendar snapshot.

The API key is read from FC_API_KEY in GitHub Actions. It is sent only as an
X-API-Key header and is never written to the repository or output JSON.
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
# Three requests per daily run = about 90 successful requests per calendar month.
# The Free Developer allowance is 10/day and 100/month.
COUNTRY_GROUPS = ("US,GB,EU", "DE,JP,CH", "CA,AU,NZ")
OUTPUT = pathlib.Path("data/economic-calendar.json")
MAX_EVENTS_PER_RESPONSE = 20
MAX_RESPONSE_BYTES = 1_000_000


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
        payload = response.read(MAX_RESPONSE_BYTES + 1)
    if len(payload) > MAX_RESPONSE_BYTES:
        raise RuntimeError("Finance Calendar response exceeded 1 MB")

    parsed = json.loads(payload)
    events = parsed.get("events")
    attribution = parsed.get("attribution")
    if not isinstance(events, list) or len(events) > MAX_EVENTS_PER_RESPONSE:
        raise RuntimeError("Finance Calendar response has an invalid events array")
    if not isinstance(attribution, dict):
        raise RuntimeError("Finance Calendar response is missing its attribution object")
    for required in ("source", "terms", "docs"):
        if not isinstance(attribution.get(required), str) or not attribution[required].strip():
            raise RuntimeError(f"Finance Calendar attribution is missing {required}")
    if not isinstance(parsed.get("truncated"), bool):
        raise RuntimeError("Finance Calendar response is missing its truncation status")
    return parsed


def build_snapshot(responses: list[dict], fetched_at: dt.datetime) -> dict:
    events: dict[str, dict] = {}
    truncated = False
    more_on_paid_plans = 0
    generated_values: list[str] = []
    attribution: dict | None = None

    for response in responses:
        truncated = truncated or bool(response.get("truncated", False))
        more_on_paid_plans += int(response.get("more_on_paid_plans") or 0)
        generated = response.get("generated_at")
        if isinstance(generated, str) and generated:
            generated_values.append(generated)

        current_attribution = response.get("attribution")
        if not isinstance(current_attribution, dict):
            raise RuntimeError("Calendar response lost its attribution object")
        if attribution is None:
            attribution = current_attribution
        elif current_attribution != attribution:
            # The same source and legal notice must apply to all groups in one snapshot.
            for key in ("source", "terms", "docs"):
                if current_attribution.get(key) != attribution.get(key):
                    raise RuntimeError("Calendar attribution changed between country-group requests")

        for event in response["events"]:
            if not isinstance(event, dict):
                continue
            event_id = event.get("id")
            if not isinstance(event_id, str) or not event_id.strip():
                continue
            # Keep the provider's full schedule record, including publisher links,
            # source_license/source_rights objects, timing precision and null values.
            events[event_id] = event

    if not attribution:
        raise RuntimeError("No Finance Calendar attribution was returned")

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
        "from": min((r.get("from", "") for r in responses), default=""),
        "to": max((r.get("to", "") for r in responses), default=""),
        "truncated": truncated,
        "more_on_paid_plans": more_on_paid_plans,
        "attribution": {
            "source": attribution["source"],
            "terms": attribution["terms"],
            "docs": attribution["docs"],
        },
        "events": ordered,
    }


def main() -> None:
    api_key = os.environ.get("FC_API_KEY", "").strip()
    if not api_key:
        raise SystemExit("FC_API_KEY is required; add it as a GitHub Actions repository secret.")

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
        f"truncated={snapshot['truncated']}; "
        f"paid_more={snapshot['more_on_paid_plans']}; "
        f"fetched_at={snapshot['fetched_at']}"
    )


if __name__ == "__main__":
    main()
