import datetime as dt
import unittest

from refresh_calendar import build_snapshot


class CalendarSnapshotTests(unittest.TestCase):
    def test_merges_events_and_preserves_attribution_and_source_licence(self):
        now = dt.datetime(2026, 11, 14, 0, 0, tzinfo=dt.timezone.utc)
        attribution = {
            "source": "financecalendar.com",
            "terms": "Use is governed by https://www.financecalendar.com/api/terms/",
            "docs": "https://www.financecalendar.com/api/",
        }
        first_event = {
            "id": "us-cpi",
            "series_code": "us-cpi",
            "name": "US CPI",
            "country": "US",
            "category": "economic-indicators",
            "impact": "high",
            "date": "2026-11-14",
            "release_at_utc": None,
            "time_precision": "date",
            "all_day": False,
            "source_name": "US Bureau of Labor Statistics",
            "source_url": "https://www.bls.gov/",
            "url": "https://www.financecalendar.com/event/us-cpi/",
            "source_license": {"attribution": "BLS", "notice": "Keep source credit"},
            "figures_available": False,
            "result_available": False,
        }
        second_event = {
            "id": "fomc",
            "series_code": "fomc",
            "name": "FOMC",
            "country": "US",
            "category": "central-banks-monetary-policy",
            "impact": "high",
            "date": "2026-11-15",
            "release_at_utc": "2026-11-15T19:00:00Z",
            "time_precision": "minute",
            "all_day": False,
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
        }
        second = {
            "from": "2026-11-14",
            "to": "2026-11-20",
            "events": [{"id": "fomc", "series_code": "fomc", "name": "FOMC revised", "date": "2026-11-15"}],
            "generated_at": "2026-11-14T00:10:00Z",
            "count": 1,
            "total": 1,
            "truncated": True,
            "more_on_paid_plans": 2,
            "attribution": attribution,
        }

        result = build_snapshot([first, second], now)

        self.assertEqual(2, len(result["events"]))
        self.assertEqual("FOMC revised", result["events"][1]["name"])
        self.assertEqual(first_event["source_license"], result["events"][0]["source_license"])
        self.assertEqual(first_event["source_url"], result["events"][0]["source_url"])
        self.assertTrue(result["truncated"])
        self.assertEqual(2, result["more_on_paid_plans"])
        self.assertEqual(attribution, result["attribution"])
        self.assertEqual("2026-11-14T00:00:00Z", result["fetched_at"])
        self.assertEqual("2026-11-14", result["from"])
        self.assertEqual("2026-11-20", result["to"])

    def test_rejects_missing_attribution(self):
        now = dt.datetime(2026, 11, 14, 0, 0, tzinfo=dt.timezone.utc)
        with self.assertRaises(RuntimeError):
            build_snapshot([{"events": [], "attribution": None}], now)

    def test_rejects_attribution_mismatch_between_groups(self):
        now = dt.datetime(2026, 11, 14, 0, 0, tzinfo=dt.timezone.utc)
        a = {"source": "Finance Calendar", "terms": "terms", "docs": "https://www.financecalendar.com/api/"}
        b = {"source": "Different provider", "terms": "terms", "docs": "https://www.financecalendar.com/api/"}
        responses = [
            {"events": [], "truncated": False, "attribution": a},
            {"events": [], "truncated": False, "attribution": b},
        ]
        with self.assertRaises(RuntimeError):
            build_snapshot(responses, now)


if __name__ == "__main__":
    unittest.main()
