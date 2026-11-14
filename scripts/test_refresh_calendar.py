import datetime as dt
import unittest

from refresh_calendar import build_snapshot


def usage(used_today=1, used_month=1):
    return {
        "plan": "Free Developer",
        "access_scope": "schedule_json",
        "quota_basis": "successful responses",
        "daily_limit": 10,
        "monthly_limit": 100,
        "used_today": used_today,
        "remaining_today": 10 - used_today,
        "used_this_month": used_month,
        "remaining_this_month": 100 - used_month,
        "resets_at": "2026-11-15T00:00:00Z",
        "monthly_resets_at": "2026-12-01T00:00:00Z",
        "max_events_per_response": 20,
        "window": {
            "from": "2026-11-14",
            "to": "2026-11-20",
            "timezone": "UTC",
        },
        "upgrade_url": "https://www.financecalendar.com/api/",
        "resume_at_utc": "2026-11-15T00:00:00Z",
    }


class CalendarSnapshotTests(unittest.TestCase):
    def test_merges_events_and_preserves_attribution_source_rights_and_usage(self):
        now = dt.datetime(2026, 11, 14, 0, 0, tzinfo=dt.timezone.utc)
        attribution = {
            "source": "financecalendar.com",
            "url": "https://www.financecalendar.com/",
            "terms": "Use is governed by https://www.financecalendar.com/api/terms/",
        }
        first_event = {
            "id": "us-cpi",
            "name": "US CPI",
            "date": "2026-11-14",
            "category": "economic-indicators",
            "impact": "high",
            "source_name": "US Bureau of Labor Statistics",
            "source_url": "https://www.bls.gov/",
            "url": "https://www.financecalendar.com/event/us-cpi/",
            "source_license": {"attribution": "BLS", "notice": "Keep source credit"},
            "figures_available": False,
            "result_available": False,
        }
        second_event = {
            "id": "fomc",
            "name": "FOMC",
            "date": "2026-11-15",
            "category": "central-banks-monetary-policy",
            "impact": "high",
            "release_at_utc": "2026-11-15T19:00:00Z",
            "time_precision": "minute",
        }
        first = {
            "from": "2026-11-14",
            "to": "2026-11-20",
            "events": [first_event, second_event],
            "generated_at": "2026-11-14T00:00:00Z",
            "count": 2,
            "total": 2,
            "truncated": False,
            "more_on_paid_plans": 0,
            "attribution": attribution,
            "free_developer": usage(used_today=1, used_month=1),
        }
        second = {
            "from": "2026-11-14",
            "to": "2026-11-20",
            "events": [{"id": "fomc", "name": "FOMC revised", "date": "2026-11-15"}],
            "generated_at": "2026-11-14T00:10:00Z",
            "count": 1,
            "total": 1,
            "truncated": True,
            "more_on_paid_plans": 2,
            "attribution": attribution,
            "free_developer": usage(used_today=2, used_month=2),
        }

        result = build_snapshot([first, second], now)

        self.assertEqual(2, len(result["events"]))
        self.assertEqual("FOMC revised", result["events"][1]["name"])
        self.assertEqual(first_event["source_license"], result["events"][0]["source_license"])
        self.assertEqual(first_event["source_url"], result["events"][0]["source_url"])
        self.assertTrue(result["truncated"])
        self.assertEqual(2, result["more_on_paid_plans"])
        self.assertEqual(attribution, result["attribution"])
        self.assertEqual(2, result["free_developer"]["used_today"])
        self.assertEqual(2, result["free_developer"]["used_this_month"])
        self.assertEqual("2026-11-14T00:00:00Z", result["fetched_at"])
        self.assertEqual("2026-11-14", result["from"])
        self.assertEqual("2026-11-20", result["to"])

    def test_rejects_missing_attribution(self):
        now = dt.datetime(2026, 11, 14, 0, 0, tzinfo=dt.timezone.utc)
        with self.assertRaises(RuntimeError):
            build_snapshot([{"events": [], "attribution": None}], now)

    def test_rejects_attribution_mismatch_between_groups(self):
        now = dt.datetime(2026, 11, 14, 0, 0, tzinfo=dt.timezone.utc)
        a = {"source": "Finance Calendar", "terms": "terms", "url": "https://www.financecalendar.com/"}
        b = {"source": "Different provider", "terms": "terms", "url": "https://www.financecalendar.com/"}
        responses = [
            {"events": [], "truncated": False, "attribution": a, "free_developer": usage()},
            {"events": [], "truncated": False, "attribution": b, "free_developer": usage(2, 2)},
        ]
        with self.assertRaises(RuntimeError):
            build_snapshot(responses, now)


if __name__ == "__main__":
    unittest.main()
