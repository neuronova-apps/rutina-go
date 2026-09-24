#!/usr/bin/env python3
"""Export the authorized RutinaGo master-bank snapshot to deterministic JSON."""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
from pathlib import Path
from typing import Any, Iterable


CONTRACT_VERSION = "1.0.0"
CATALOG_VERSION = "1.0.0"
SOURCE_FILES = {
    "activities": "banco_actividades.csv",
    "routines": "banco_rutinas.csv",
    "gamification": "banco_logros_retos.csv",
    "messages": "banco_mensajes_refuerzo.csv",
    "catalogs": "catalogos.csv",
}
OUTPUT_ORDER = (
    "categories.json",
    "activities.json",
    "routines.json",
    "routine_steps.json",
    "achievements.json",
    "challenges.json",
    "reinforcement_messages.json",
    "visual_references.json",
)
EXPECTED_HEADERS = {
    "activities": [
        "ID", "Categoria_principal", "Actividad", "Instruccion_simple",
        "Termino_busqueda_ARASAAC", "Grupo_edad", "Nivel_autonomia",
        "Supervision", "Modo_simple", "Modo_avanzado", "Recurso_visual",
        "Estado", "Observaciones", "Contextos_uso",
    ],
    "routines": [
        "Rutina_ID", "Nombre_rutina", "Area_principal", "Contexto",
        "Grupo_edad", "Objetivo_funcional", "Momento_sugerido",
        "Secuencia_simple_ACT", "N_pasos_simple", "Secuencia_completa_ACT",
        "N_pasos_completa", "Opcionales_ACT", "Personalizable",
        "Supervision_clave", "Estado", "Observaciones",
    ],
    "gamification": [
        "Gam_ID", "Tipo", "Nombre", "Categoria", "Nivel_gamificacion",
        "Grupo_edad", "Referencia_tipo", "Referencias",
        "Condicion_desbloqueo", "Objetivo_cantidad", "Ventana_dias",
        "Repetible", "Recompensa_visual", "Penaliza_si_no",
        "Activo_por_defecto", "Estado", "Observaciones",
    ],
    "messages": [
        "MSG_ID", "Tipo_evento", "Categoria", "Tono", "Grupo_edad",
        "Gamificacion_requerida", "Nivel_gamificacion", "Mensaje",
        "Canal_uso", "Longitud", "Permite_repeticion", "Origen_evento",
        "Regla_seguridad", "Estado", "Observaciones",
        "Frecuencia_recomendada", "TTS_activacion",
    ],
    "catalogs": ["Campo", "Valores sugeridos", "", "Categorias"],
}


def read_rows(source_dir: Path, key: str) -> list[dict[str, str]]:
    path = source_dir / SOURCE_FILES[key]
    with path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        if reader.fieldnames != EXPECTED_HEADERS[key]:
            raise ValueError(
                f"{path}: cabeceras inesperadas. "
                f"Esperadas={EXPECTED_HEADERS[key]!r}; recibidas={reader.fieldnames!r}"
            )
        return list(reader)


def nullable(value: str | None) -> str | None:
    value = (value or "").strip()
    return value or None


def yes_no(value: str, field: str) -> bool:
    normalized = value.strip()
    if normalized == "Sí":
        return True
    if normalized == "No":
        return False
    raise ValueError(f"{field}: se esperaba 'Sí' o 'No', se recibió {value!r}")


def split_pipe(value: str) -> list[str]:
    return [part.strip() for part in value.split("|") if part.strip()]


def split_sequence(value: str, separator: str) -> list[str]:
    return [part.strip() for part in value.split(separator) if part.strip()]


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(65536), b""):
            digest.update(chunk)
    return digest.hexdigest()


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    serialized = json.dumps(
        payload, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"
    path.write_text(serialized, encoding="utf-8", newline="\n")


def envelope(
    items: Iterable[dict[str, Any]],
    catalog_version: str,
    contract_version: str,
) -> dict[str, Any]:
    return {
        "catalogVersion": catalog_version,
        "contractVersion": contract_version,
        "items": list(items),
    }


def build_activity(row: dict[str, str]) -> dict[str, Any]:
    return {
        "id": row["ID"].strip(),
        "category": row["Categoria_principal"].strip(),
        "name": row["Actividad"].strip(),
        "simpleInstruction": row["Instruccion_simple"].strip(),
        "arasaacSearchTerm": row["Termino_busqueda_ARASAAC"].strip(),
        "ageGroup": row["Grupo_edad"].strip(),
        "autonomyLevel": row["Nivel_autonomia"].strip(),
        "supervision": row["Supervision"].strip(),
        "simpleMode": yes_no(row["Modo_simple"], "Modo_simple"),
        "advancedMode": yes_no(row["Modo_avanzado"], "Modo_avanzado"),
        "visualResource": row["Recurso_visual"].strip(),
        "status": row["Estado"].strip(),
        "observations": nullable(row["Observaciones"]),
        "usageContexts": split_pipe(row["Contextos_uso"]),
    }


def build_routine(row: dict[str, str]) -> dict[str, Any]:
    return {
        "id": row["Rutina_ID"].strip(),
        "name": row["Nombre_rutina"].strip(),
        "primaryArea": row["Area_principal"].strip(),
        "context": row["Contexto"].strip(),
        "ageGroup": row["Grupo_edad"].strip(),
        "functionalObjective": row["Objetivo_funcional"].strip(),
        "suggestedMoment": row["Momento_sugerido"].strip(),
        "declaredSimpleStepCount": int(row["N_pasos_simple"]),
        "declaredCompleteStepCount": int(row["N_pasos_completa"]),
        "personalizable": yes_no(row["Personalizable"], "Personalizable"),
        "keySupervision": row["Supervision_clave"].strip(),
        "status": row["Estado"].strip(),
        "observations": nullable(row["Observaciones"]),
    }


def build_steps(rows: list[dict[str, str]]) -> list[dict[str, Any]]:
    steps: list[dict[str, Any]] = []
    sequence_fields = (
        ("SIMPLE", "Secuencia_simple_ACT", ">"),
        ("COMPLETE", "Secuencia_completa_ACT", ">"),
        ("OPTIONAL", "Opcionales_ACT", "|"),
    )
    for row in rows:
        routine_id = row["Rutina_ID"].strip()
        for sequence_type, field, separator in sequence_fields:
            activity_ids = split_sequence(row[field], separator)
            steps.extend(
                {
                    "routineId": routine_id,
                    "sequenceType": sequence_type,
                    "position": position,
                    "activityId": activity_id,
                }
                for position, activity_id in enumerate(activity_ids, start=1)
            )
    return steps


def build_gamification(row: dict[str, str]) -> dict[str, Any]:
    return {
        "id": row["Gam_ID"].strip(),
        "type": row["Tipo"].strip(),
        "name": row["Nombre"].strip(),
        "category": row["Categoria"].strip(),
        "gamificationLevel": row["Nivel_gamificacion"].strip(),
        "ageGroup": row["Grupo_edad"].strip(),
        "referenceType": row["Referencia_tipo"].strip(),
        "references": split_pipe(row["Referencias"]),
        "unlockCondition": row["Condicion_desbloqueo"].strip(),
        "targetQuantity": int(row["Objetivo_cantidad"]),
        "windowDays": int(row["Ventana_dias"]),
        "repeatable": yes_no(row["Repetible"], "Repetible"),
        "visualReward": row["Recompensa_visual"].strip(),
        "penalizesIfIncomplete": yes_no(row["Penaliza_si_no"], "Penaliza_si_no"),
        "defaultActivation": row["Activo_por_defecto"].strip(),
        "status": row["Estado"].strip(),
        "observations": row["Observaciones"].strip(),
    }


def build_message(row: dict[str, str]) -> dict[str, Any]:
    return {
        "id": row["MSG_ID"].strip(),
        "eventType": row["Tipo_evento"].strip(),
        "category": row["Categoria"].strip(),
        "tone": row["Tono"].strip(),
        "ageGroup": row["Grupo_edad"].strip(),
        "gamificationRequired": yes_no(
            row["Gamificacion_requerida"], "Gamificacion_requerida"
        ),
        "gamificationLevel": row["Nivel_gamificacion"].strip(),
        "message": row["Mensaje"].strip(),
        "usageChannel": row["Canal_uso"].strip(),
        "length": row["Longitud"].strip(),
        "allowsRepetition": row["Permite_repeticion"].strip(),
        "eventOrigin": row["Origen_evento"].strip(),
        "safetyRule": row["Regla_seguridad"].strip(),
        "status": row["Estado"].strip(),
        "observations": row["Observaciones"].strip(),
        "recommendedFrequency": row["Frecuencia_recomendada"].strip(),
        "ttsActivation": row["TTS_activacion"].strip(),
    }


def export_catalog(
    source_dir: Path,
    output_dir: Path,
    catalog_version: str = CATALOG_VERSION,
    contract_version: str = CONTRACT_VERSION,
) -> dict[str, Any]:
    snapshot = json.loads(
        (source_dir / "source-snapshot.json").read_text(encoding="utf-8")
    )
    activity_rows = read_rows(source_dir, "activities")
    routine_rows = read_rows(source_dir, "routines")
    gamification_rows = read_rows(source_dir, "gamification")
    message_rows = read_rows(source_dir, "messages")
    catalog_rows = read_rows(source_dir, "catalogs")

    categories = [
        {"name": row["Categorias"].strip(), "order": order}
        for order, row in enumerate(catalog_rows, start=1)
        if row["Categorias"].strip()
    ]
    activities = [build_activity(row) for row in activity_rows]
    routines = [build_routine(row) for row in routine_rows]
    routine_steps = build_steps(routine_rows)
    gamification = [build_gamification(row) for row in gamification_rows]
    achievements = [item for item in gamification if item["type"] == "Logro"]
    challenges = [item for item in gamification if item["type"] == "Reto"]
    messages = [build_message(row) for row in message_rows]
    visual_references = [
        {
            "activityId": activity["id"],
            "visualSource": "PENDING",
            "arasaacId": None,
            "variant": None,
            "localFile": None,
            "alternativeText": activity["name"],
            "reviewStatus": "PENDING_SELECTION",
            "attribution": None,
            "license": None,
        }
        for activity in activities
    ]
    payloads = {
        "categories.json": envelope(categories, catalog_version, contract_version),
        "activities.json": envelope(activities, catalog_version, contract_version),
        "routines.json": envelope(routines, catalog_version, contract_version),
        "routine_steps.json": envelope(routine_steps, catalog_version, contract_version),
        "achievements.json": envelope(achievements, catalog_version, contract_version),
        "challenges.json": envelope(challenges, catalog_version, contract_version),
        "reinforcement_messages.json": envelope(messages, catalog_version, contract_version),
        "visual_references.json": envelope(visual_references, catalog_version, contract_version),
    }
    output_dir.mkdir(parents=True, exist_ok=True)
    for filename in OUTPUT_ORDER:
        write_json(output_dir / filename, payloads[filename])

    artifact_counts = {
        filename: len(payloads[filename]["items"]) for filename in OUTPUT_ORDER
    }
    manifest = {
        "catalogVersion": catalog_version,
        "contractVersion": contract_version,
        "generatedAt": snapshot["exportedAt"],
        "source": {
            "spreadsheetId": snapshot["source"]["spreadsheetId"],
            "spreadsheetUrl": snapshot["source"]["spreadsheetUrl"],
            "sheetCount": snapshot["source"]["sheetCount"],
            "snapshotVersion": snapshot["snapshotVersion"],
            "snapshotExportedAt": snapshot["exportedAt"],
            "inputFiles": {
                filename: {"sha256": sha256(source_dir / filename)}
                for filename in sorted(SOURCE_FILES.values())
            },
        },
        "counts": {
            "activities": len(activities),
            "routines": len(routines),
            "achievements": len(achievements),
            "challenges": len(challenges),
            "reinforcementMessages": len(messages),
            "categories": len(categories),
            "routineSteps": len(routine_steps),
            "visualReferences": len(visual_references),
            "masterRecords": (
                len(activities) + len(routines) + len(achievements)
                + len(challenges) + len(messages)
            ),
        },
        "artifacts": {
            filename: {
                "itemCount": artifact_counts[filename],
                "sha256": sha256(output_dir / filename),
            }
            for filename in OUTPUT_ORDER
        },
    }
    write_json(output_dir / "manifest.json", manifest)
    return manifest


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--source-dir", type=Path, default=Path("data/master_bank/source")
    )
    parser.add_argument(
        "--output-dir", type=Path, default=Path("data/master_bank/generated/v1")
    )
    parser.add_argument("--catalog-version", default=CATALOG_VERSION)
    parser.add_argument("--contract-version", default=CONTRACT_VERSION)
    args = parser.parse_args()
    manifest = export_catalog(
        args.source_dir, args.output_dir, args.catalog_version, args.contract_version
    )
    print(
        "Exportación completada: "
        f"{manifest['counts']['masterRecords']} registros maestros, "
        f"contrato {manifest['contractVersion']}, catálogo {manifest['catalogVersion']}."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
