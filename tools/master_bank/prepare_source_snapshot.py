#!/usr/bin/env python3
"""Prepare a dated source snapshot from five reviewed Google Sheets CSV exports."""

from __future__ import annotations

import argparse
import json
import shutil
from datetime import datetime, timezone
from pathlib import Path

from export_master_bank import SOURCE_FILES, read_rows


SPREADSHEET_ID = "1is0zFNvvA69Rb_C0V0qYG4a-AtEh0oxdSiJTFEFlaYo"
SPREADSHEET_URL = (
    "https://docs.google.com/spreadsheets/d/"
    "1is0zFNvvA69Rb_C0V0qYG4a-AtEh0oxdSiJTFEFlaYo/edit"
)
SHEET_IDS = {
    "activities": ("Banco_actividades", 1097218425),
    "routines": ("Banco_rutinas", 1664810288),
    "gamification": ("Banco_logros_retos", 1162591216),
    "messages": ("Banco_mensajes_refuerzo", 1394843334),
    "catalogs": ("Catalogos", 1728542767),
}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "reviewed_csv_dir",
        type=Path,
        help="Directorio con los cinco CSV revisados y sus nombres contractuales.",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path("data/master_bank/source"),
    )
    args = parser.parse_args()

    missing = [
        filename
        for filename in SOURCE_FILES.values()
        if not (args.reviewed_csv_dir / filename).is_file()
    ]
    if missing:
        parser.error(f"faltan archivos CSV: {', '.join(sorted(missing))}")

    rows = {
        key: len(read_rows(args.reviewed_csv_dir, key))
        for key in SOURCE_FILES
    }
    args.output_dir.mkdir(parents=True, exist_ok=True)
    for filename in SOURCE_FILES.values():
        shutil.copyfile(args.reviewed_csv_dir / filename, args.output_dir / filename)
    exported_at = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace(
        "+00:00", "Z"
    )
    snapshot = {
        "snapshotVersion": "1.0.0",
        "exportedAt": exported_at,
        "source": {
            "spreadsheetId": SPREADSHEET_ID,
            "spreadsheetUrl": SPREADSHEET_URL,
            "sheetCount": 17,
        },
        "sheets": [
            {
                "title": SHEET_IDS[key][0],
                "sheetId": SHEET_IDS[key][1],
                "file": SOURCE_FILES[key],
                "dataRowCount": rows[key],
            }
            for key in SOURCE_FILES
        ],
    }
    (args.output_dir / "source-snapshot.json").write_text(
        json.dumps(snapshot, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    print(f"Instantánea preparada en {args.output_dir} ({exported_at}).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
