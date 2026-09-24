from __future__ import annotations

import json
import sys
import tempfile
import unittest
from collections import defaultdict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
TOOLS = ROOT / "tools" / "master_bank"
SOURCE = ROOT / "data" / "master_bank" / "source"
GENERATED = ROOT / "data" / "master_bank" / "generated" / "v1"
sys.path.insert(0, str(TOOLS))

from export_master_bank import export_catalog  # noqa: E402
from validate_master_bank import validate_catalog  # noqa: E402


def load_items(filename: str) -> list[dict]:
    return json.loads((GENERATED / filename).read_text(encoding="utf-8"))["items"]


class MasterBankArtifactsTest(unittest.TestCase):
    def test_generated_catalog_passes_full_validator(self) -> None:
        self.assertEqual([], validate_catalog(GENERATED))

    def test_expected_counts_and_unique_source_ids(self) -> None:
        manifest = json.loads((GENERATED / "manifest.json").read_text(encoding="utf-8"))
        self.assertEqual(
            {
                "activities": 203,
                "routines": 52,
                "achievements": 40,
                "challenges": 20,
                "reinforcementMessages": 54,
                "categories": 17,
                "routineSteps": 490,
                "visualReferences": 203,
                "masterRecords": 369,
            },
            manifest["counts"],
        )
        for filename in (
            "activities.json",
            "routines.json",
            "achievements.json",
            "challenges.json",
            "reinforcement_messages.json",
        ):
            ids = [item["id"] for item in load_items(filename)]
            self.assertEqual(len(ids), len(set(ids)), filename)

    def test_routine_step_references_and_order_are_valid(self) -> None:
        activity_ids = {item["id"] for item in load_items("activities.json")}
        routine_ids = {item["id"] for item in load_items("routines.json")}
        grouped: dict[tuple[str, str], list[int]] = defaultdict(list)
        for step in load_items("routine_steps.json"):
            self.assertIn(step["activityId"], activity_ids)
            self.assertIn(step["routineId"], routine_ids)
            grouped[(step["routineId"], step["sequenceType"])].append(step["position"])
        for positions in grouped.values():
            self.assertEqual(list(range(1, len(positions) + 1)), positions)

    def test_pending_visuals_do_not_contain_invented_arasaac_ids(self) -> None:
        visuals = load_items("visual_references.json")
        self.assertEqual(203, len(visuals))
        for visual in visuals:
            self.assertEqual("PENDING", visual["visualSource"])
            self.assertIsNone(visual["arasaacId"])
            self.assertIsNone(visual["localFile"])
            self.assertIsNone(visual["attribution"])
            self.assertIsNone(visual["license"])

    def test_export_is_byte_reproducible_for_same_snapshot_and_version(self) -> None:
        with tempfile.TemporaryDirectory() as first, tempfile.TemporaryDirectory() as second:
            first_path = Path(first)
            second_path = Path(second)
            export_catalog(SOURCE, first_path)
            export_catalog(SOURCE, second_path)
            first_files = sorted(path.name for path in first_path.iterdir())
            second_files = sorted(path.name for path in second_path.iterdir())
            self.assertEqual(first_files, second_files)
            for filename in first_files:
                self.assertEqual(
                    (first_path / filename).read_bytes(),
                    (second_path / filename).read_bytes(),
                    filename,
                )


if __name__ == "__main__":
    unittest.main()
