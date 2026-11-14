import datetime as dt
import unittest

from refresh_calendar import build_snapshot


class CalendarSnapshotTests(unittest.TestCase):
    def test_merges_events_and_preserves_attribution_and_missing_values(self):
        now = dt.datetime(2026, 11, 14, 0, 0, tzinfo=dt.timezone.utc)
        attribution = {"source": "Finance Calendar", "url": "https://www.financecalendar.com/"}
        first = {
            "events": [
                {"id": "us-cpi", "name": "US CPI", "date": "2026-11-14", "actual": None},
                {"id": "fomc", "name": "FOMC", "date": "2026-11-15"},
            ],
            "generated_at": "2026-11-14T00:00:00Z",
            "truncated": False,
            "attribution": attribution,
        }
        second = {
            "events": [{"id": "fomc", "name": "FOMC revised", "date": "2026-11-15"}],
            "generated_at": "2026-11-14T00:10:00Z",
            "truncated": True,
            "attribution": attribution,
        }

        result = build_snapshot([first, second], now)

        self.assertEqual(2, len(result["events"]))
        self.assertEqual("FOMC revised", result["events"][1]["name"])
        self.assertIsNone(result["events"][0]["actual"])
        self.assertTrue(result["truncated"])
        self.assertEqual(attribution, result["attribution"])

    def test_rejects_empty_attribution(self):
        now = dt.datetime(2026, 11, 14, 0, 0, tzinfo=dt.timezone.utc)
        with self.assertRaises(RuntimeError):
            build_snapshot([{"events": [], "attribution": None}], now)


if __name__ == "__main__":
    unittest.main()
