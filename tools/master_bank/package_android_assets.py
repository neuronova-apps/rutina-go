#!/usr/bin/env python3
"""Copy the validated master catalog into Android assets without changing bytes."""

from __future__ import annotations

import argparse
import json
import shutil
from pathlib import Path

from validate_master_bank import ARTIFACT_KEYS, sha256, validate_catalog


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--generated-dir",
        type=Path,
        default=Path("data/master_bank/generated/v1"),
    )
    parser.add_argument(
        "--assets-dir",
        type=Path,
        default=Path("app/src/main/assets/master_catalog/v1"),
    )
    args = parser.parse_args()

    errors = validate_catalog(args.generated_dir)
    if errors:
        for error in errors:
            print(f"- {error}")
        return 1

    manifest = json.loads(
        (args.generated_dir / "manifest.json").read_text(encoding="utf-8")
    )
    filenames = ["manifest.json", *sorted(ARTIFACT_KEYS)]
    args.assets_dir.mkdir(parents=True, exist_ok=True)
    for filename in filenames:
        shutil.copyfile(args.generated_dir / filename, args.assets_dir / filename)

    for filename, metadata in manifest["artifacts"].items():
        actual = sha256(args.assets_dir / filename)
        if actual != metadata["sha256"]:
            raise RuntimeError(f"Checksum distinto después de copiar {filename}")
    print(f"Catálogo empaquetado sin cambios en {args.assets_dir}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
