#!/usr/bin/env python3
"""Validate generated RutinaGo master-bank artifacts without modifying them."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any


EXPECTED_COUNTS = {
    "activities": 203,
    "routines": 52,
    "achievements": 40,
    "challenges": 20,
    "reinforcementMessages": 54,
    "masterRecords": 369,
    "categories": 17,
    "visualReferences": 203,
}
ARTIFACT_KEYS = {
    "activities.json": "activities",
    "routines.json": "routines",
    "achievements.json": "achievements",
    "challenges.json": "challenges",
    "reinforcement_messages.json": "reinforcementMessages",
    "categories.json": "categories",
    "routine_steps.json": "routineSteps",
    "visual_references.json": "visualReferences",
}
ID_PATTERNS = {
    "activities.json": re.compile(r"^ACT-\d{3}$"),
    "routines.json": re.compile(r"^RUT-\d{3}$"),
    "achievements.json": re.compile(r"^LOG-\d{3}$"),
    "challenges.json": re.compile(r"^RET-\d{3}$"),
    "reinforcement_messages.json": re.compile(r"^MSG-\d{3}$"),
}
REQUIRED_FIELDS = {
    "activities.json": {
        "id", "category", "name", "simpleInstruction", "arasaacSearchTerm",
        "ageGroup", "autonomyLevel", "supervision", "simpleMode",
        "advancedMode", "visualResource", "status", "observations",
        "usageContexts",
    },
    "routines.json": {
        "id", "name", "primaryArea", "context", "ageGroup",
        "functionalObjective", "suggestedMoment", "declaredSimpleStepCount",
        "declaredCompleteStepCount", "personalizable", "keySupervision",
        "status", "observations",
    },
    "routine_steps.json": {
        "routineId", "sequenceType", "position", "activityId",
    },
    "achievements.json": {
        "id", "type", "name", "category", "gamificationLevel", "ageGroup",
        "referenceType", "references", "unlockCondition", "targetQuantity",
        "windowDays", "repeatable", "visualReward", "penalizesIfIncomplete",
        "defaultActivation", "status", "observations",
    },
    "challenges.json": {
        "id", "type", "name", "category", "gamificationLevel", "ageGroup",
        "referenceType", "references", "unlockCondition", "targetQuantity",
        "windowDays", "repeatable", "visualReward", "penalizesIfIncomplete",
        "defaultActivation", "status", "observations",
    },
    "reinforcement_messages.json": {
        "id", "eventType", "category", "tone", "ageGroup",
        "gamificationRequired", "gamificationLevel", "message",
        "usageChannel", "length", "allowsRepetition", "eventOrigin",
        "safetyRule", "status", "observations", "recommendedFrequency",
        "ttsActivation",
    },
    "categories.json": {"name", "order"},
    "visual_references.json": {
        "activityId", "visualSource", "arasaacId", "variant", "localFile",
        "alternativeText", "reviewStatus", "attribution", "license",
    },
}
AGE_GROUPS = {"General", "Escolar+", "Adolescente"}
AUTONOMY_LEVELS = {"Con apoyo / autónomo", "Apoyo parcial / autónomo"}
SUPERVISION_LEVELS = {
    "Según necesidad",
    "Supervisión recomendada",
    "Privacidad y apoyo según necesidad",
}
ACTIVITY_STATUSES = {"Revisado", "Validado con pictograma"}
ROUTINE_CONTEXTS = {
    "Casa",
    "Casa / comunidad",
    "Casa / escuela",
    "Casa / escuela / comunidad",
    "Casa / salud",
    "Comunidad",
    "Escuela",
    "Salud / comunidad",
}
SEQUENCE_TYPES = {"SIMPLE", "COMPLETE", "OPTIONAL"}
GAMIFICATION_LEVELS = {"Ligera", "Intermedia", "Completa"}
DEFAULT_ACTIVATIONS = {
    "No; activación voluntaria",
    "Sí al habilitar el nivel correspondiente",
}
MESSAGE_LEVELS = {"No aplica", "Ligera/Intermedia", "Intermedia", "Completa"}
MESSAGE_EVENTS = {
    "AYUDA_SOLICITADA",
    "CAMBIO_ANTICIPADO",
    "ELECCION_EXPRESADA",
    "LOGRO_DESBLOQUEADO",
    "PASO_COMPLETADO",
    "PASO_OMITIDO",
    "PAUSA_SOLICITADA",
    "PROGRESO_LOGRO",
    "PROGRESO_RETO",
    "RETO_ABANDONADO",
    "RETO_COMPLETADO",
    "RETO_FINALIZADO_SIN_COMPLETAR",
    "RETO_INICIADO",
    "RUTINA_COMPLETADA",
    "RUTINA_FINALIZADA_PARCIAL",
    "RUTINA_INICIADA",
    "RUTINA_PAUSADA",
    "RUTINA_REANUDADA",
}
MESSAGE_CATEGORIES = {
    "Anticipación",
    "Autodeterminación",
    "Autorregulación",
    "Comunicación funcional",
    "Continuidad",
    "Logro",
    "Progreso funcional",
    "Reto",
    "Rutina",
}
MESSAGE_TONES = {"Cálido", "Motivador sobrio", "Neutral"}
MESSAGE_ORIGINS = {"ACT/Sistema", "LOG", "RET", "RUT", "RUT/Sistema", "Sistema/ACT"}
VISUAL_SOURCES = {"PENDING", "ARASAAC", "OWN_PHOTO"}
REVIEW_STATUSES = {"PENDING_SELECTION", "SELECTED", "REVIEWED", "APPROVED"}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(65536), b""):
            digest.update(chunk)
    return digest.hexdigest()


def load_json(path: Path, errors: list[str]) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        errors.append(f"{path}: no se pudo leer JSON: {exc}")
        return None


def validate_envelope(
    filename: str,
    payload: Any,
    manifest: dict[str, Any],
    errors: list[str],
) -> list[dict[str, Any]]:
    if not isinstance(payload, dict):
        errors.append(f"{filename}: la raíz debe ser un objeto.")
        return []
    for key in ("catalogVersion", "contractVersion", "items"):
        if key not in payload:
            errors.append(f"{filename}: falta la propiedad raíz {key!r}.")
    if payload.get("catalogVersion") != manifest.get("catalogVersion"):
        errors.append(f"{filename}: catalogVersion no coincide con el manifiesto.")
    if payload.get("contractVersion") != manifest.get("contractVersion"):
        errors.append(f"{filename}: contractVersion no coincide con el manifiesto.")
    items = payload.get("items")
    if not isinstance(items, list):
        errors.append(f"{filename}: items debe ser una lista.")
        return []
    return items


def require_fields(
    filename: str,
    items: list[dict[str, Any]],
    errors: list[str],
) -> None:
    expected = REQUIRED_FIELDS[filename]
    for index, item in enumerate(items, start=1):
        if not isinstance(item, dict):
            errors.append(f"{filename}[{index}]: cada elemento debe ser un objeto.")
            continue
        missing = expected - item.keys()
        extra = item.keys() - expected
        if missing:
            errors.append(f"{filename}[{index}]: faltan campos {sorted(missing)}.")
        if extra:
            errors.append(
                f"{filename}[{index}]: campos fuera de contrato {sorted(extra)}."
            )


def validate_unique_ids(
    filename: str,
    items: list[dict[str, Any]],
    errors: list[str],
) -> set[str]:
    ids = [item.get("id") for item in items if isinstance(item, dict)]
    duplicates = sorted(value for value, count in Counter(ids).items() if count > 1)
    if duplicates:
        errors.append(f"{filename}: IDs duplicados {duplicates}.")
    pattern = ID_PATTERNS[filename]
    invalid = sorted(
        str(value)
        for value in ids
        if not isinstance(value, str) or not pattern.fullmatch(value)
    )
    if invalid:
        errors.append(f"{filename}: IDs con formato inválido {invalid}.")
    return {value for value in ids if isinstance(value, str)}


def validate_catalog(directory: Path) -> list[str]:
    errors: list[str] = []
    manifest = load_json(directory / "manifest.json", errors)
    if not isinstance(manifest, dict):
        return errors
    for key in (
        "catalogVersion", "contractVersion", "generatedAt",
        "source", "counts", "artifacts",
    ):
        if key not in manifest:
            errors.append(f"manifest.json: falta {key!r}.")

    counts = manifest.get("counts", {})
    for key, expected in EXPECTED_COUNTS.items():
        if counts.get(key) != expected:
            errors.append(
                f"manifest.json: counts.{key}={counts.get(key)!r}; "
                f"se esperaba {expected}."
            )

    payloads: dict[str, list[dict[str, Any]]] = {}
    for filename, count_key in ARTIFACT_KEYS.items():
        path = directory / filename
        payload = load_json(path, errors)
        if payload is None:
            continue
        items = validate_envelope(filename, payload, manifest, errors)
        payloads[filename] = items
        require_fields(filename, items, errors)
        if counts.get(count_key) != len(items):
            errors.append(
                f"{filename}: {len(items)} elementos; el manifiesto declara "
                f"{counts.get(count_key)!r}."
            )
        artifact = manifest.get("artifacts", {}).get(filename)
        if not isinstance(artifact, dict):
            errors.append(f"manifest.json: falta artifacts.{filename}.")
        else:
            if artifact.get("itemCount") != len(items):
                errors.append(f"manifest.json: itemCount incorrecto para {filename}.")
            if artifact.get("sha256") != sha256(path):
                errors.append(f"manifest.json: SHA-256 incorrecto para {filename}.")

    if set(manifest.get("artifacts", {})) != set(ARTIFACT_KEYS):
        errors.append("manifest.json: inventario de artefactos incorrecto.")
    source = manifest.get("source", {})
    if source.get("sheetCount") != 17:
        errors.append("manifest.json: la fuente debe declarar 17 pestañas.")
    input_files = source.get("inputFiles", {})
    if len(input_files) != 5:
        errors.append("manifest.json: deben registrarse las cinco entradas CSV.")
    for filename, metadata in input_files.items():
        if not re.fullmatch(r"[0-9a-f]{64}", str(metadata.get("sha256", ""))):
            errors.append(f"manifest.json: checksum de entrada inválido para {filename}.")
            continue
        source_path = directory.parent.parent / "source" / filename
        if not source_path.is_file():
            errors.append(f"manifest.json: no existe la entrada declarada {source_path}.")
        elif metadata["sha256"] != sha256(source_path):
            errors.append(f"manifest.json: SHA-256 de entrada incorrecto para {filename}.")

    if not set(ARTIFACT_KEYS).issubset(payloads):
        return errors

    activity_ids = validate_unique_ids(
        "activities.json", payloads["activities.json"], errors
    )
    routine_ids = validate_unique_ids(
        "routines.json", payloads["routines.json"], errors
    )
    validate_unique_ids(
        "achievements.json", payloads["achievements.json"], errors
    )
    validate_unique_ids("challenges.json", payloads["challenges.json"], errors)
    validate_unique_ids(
        "reinforcement_messages.json",
        payloads["reinforcement_messages.json"],
        errors,
    )

    categories = payloads["categories.json"]
    category_names = [item.get("name") for item in categories]
    if len(set(category_names)) != len(category_names):
        errors.append("categories.json: existen categorías duplicadas.")
    if [item.get("order") for item in categories] != list(
        range(1, len(categories) + 1)
    ):
        errors.append("categories.json: order debe comenzar en 1 y ser contiguo.")
    category_set = set(category_names)

    for item in payloads["activities.json"]:
        item_id = item.get("id", "?")
        if item.get("category") not in category_set:
            errors.append(f"{item_id}: categoría de actividad no catalogada.")
        if item.get("ageGroup") not in AGE_GROUPS:
            errors.append(f"{item_id}: ageGroup inválido.")
        if item.get("autonomyLevel") not in AUTONOMY_LEVELS:
            errors.append(f"{item_id}: autonomyLevel inválido.")
        if item.get("supervision") not in SUPERVISION_LEVELS:
            errors.append(f"{item_id}: supervision inválida.")
        if item.get("status") not in ACTIVITY_STATUSES:
            errors.append(f"{item_id}: status de actividad inválido.")
        if not isinstance(item.get("simpleMode"), bool) or not isinstance(
            item.get("advancedMode"), bool
        ):
            errors.append(f"{item_id}: los modos deben ser booleanos.")
        if not isinstance(item.get("usageContexts"), list) or not item.get(
            "usageContexts"
        ):
            errors.append(f"{item_id}: usageContexts debe ser una lista no vacía.")
        for field in (
            "category", "name", "simpleInstruction", "arasaacSearchTerm",
            "visualResource",
        ):
            if not isinstance(item.get(field), str) or not item[field].strip():
                errors.append(f"{item_id}: {field} debe ser texto no vacío.")

    routines_by_id = {
        item["id"]: item for item in payloads["routines.json"] if "id" in item
    }
    for item in payloads["routines.json"]:
        item_id = item.get("id", "?")
        if item.get("primaryArea") not in category_set:
            errors.append(f"{item_id}: área principal no catalogada.")
        if item.get("ageGroup") not in AGE_GROUPS:
            errors.append(f"{item_id}: ageGroup inválido.")
        if item.get("context") not in ROUTINE_CONTEXTS:
            errors.append(f"{item_id}: context inválido.")
        if item.get("status") != "Validado banco v1.0":
            errors.append(f"{item_id}: status de rutina inválido.")
        if not isinstance(item.get("personalizable"), bool):
            errors.append(f"{item_id}: personalizable debe ser booleano.")
        for field in ("declaredSimpleStepCount", "declaredCompleteStepCount"):
            if not isinstance(item.get(field), int) or item[field] < 1:
                errors.append(f"{item_id}: {field} debe ser entero positivo.")

    grouped_steps: dict[tuple[str, str], list[dict[str, Any]]] = defaultdict(list)
    for step in payloads["routine_steps.json"]:
        routine_id = step.get("routineId")
        activity_id = step.get("activityId")
        sequence_type = step.get("sequenceType")
        if routine_id not in routine_ids:
            errors.append(f"Paso: rutina inexistente {routine_id!r}.")
        if activity_id not in activity_ids:
            errors.append(f"Paso: actividad inexistente {activity_id!r}.")
        if sequence_type not in SEQUENCE_TYPES:
            errors.append(f"Paso: sequenceType inválido {sequence_type!r}.")
        if not isinstance(step.get("position"), int) or step["position"] < 1:
            errors.append(f"Paso: position inválida en {routine_id}/{sequence_type}.")
        grouped_steps[(routine_id, sequence_type)].append(step)

    for routine_id, routine in routines_by_id.items():
        for sequence_type, count_field in (
            ("SIMPLE", "declaredSimpleStepCount"),
            ("COMPLETE", "declaredCompleteStepCount"),
        ):
            steps = grouped_steps.get((routine_id, sequence_type), [])
            if len(steps) != routine[count_field]:
                errors.append(
                    f"{routine_id}/{sequence_type}: {len(steps)} pasos; "
                    f"se declararon {routine[count_field]}."
                )
        for sequence_type in SEQUENCE_TYPES:
            positions = [
                step["position"]
                for step in grouped_steps.get((routine_id, sequence_type), [])
            ]
            if positions != list(range(1, len(positions) + 1)):
                errors.append(
                    f"{routine_id}/{sequence_type}: posiciones no contiguas."
                )

    for filename, expected_type in (
        ("achievements.json", "Logro"),
        ("challenges.json", "Reto"),
    ):
        for item in payloads[filename]:
            item_id = item.get("id", "?")
            if item.get("type") != expected_type:
                errors.append(f"{item_id}: type debe ser {expected_type!r}.")
            if item.get("gamificationLevel") not in GAMIFICATION_LEVELS:
                errors.append(f"{item_id}: nivel de gamificación inválido.")
            if item.get("ageGroup") not in AGE_GROUPS:
                errors.append(f"{item_id}: ageGroup inválido.")
            if item.get("category") not in category_set:
                errors.append(f"{item_id}: categoría de gamificación no catalogada.")
            if item.get("defaultActivation") not in DEFAULT_ACTIVATIONS:
                errors.append(f"{item_id}: defaultActivation inválido.")
            if item.get("status") != "Validado banco v1.0":
                errors.append(f"{item_id}: status de gamificación inválido.")
            reference_type = item.get("referenceType")
            references = item.get("references")
            if reference_type not in {"ACT", "RUT"}:
                errors.append(f"{item_id}: referenceType inválido.")
            if not isinstance(references, list) or not references:
                errors.append(f"{item_id}: references debe ser lista no vacía.")
                continue
            valid_targets = activity_ids if reference_type == "ACT" else routine_ids
            invalid = [target for target in references if target not in valid_targets]
            if invalid:
                errors.append(f"{item_id}: referencias inexistentes {invalid}.")
            for field in ("targetQuantity", "windowDays"):
                if not isinstance(item.get(field), int) or item[field] < 0:
                    errors.append(f"{item_id}: {field} debe ser entero no negativo.")
            for field in ("repeatable", "penalizesIfIncomplete"):
                if not isinstance(item.get(field), bool):
                    errors.append(f"{item_id}: {field} debe ser booleano.")

    for item in payloads["reinforcement_messages.json"]:
        item_id = item.get("id", "?")
        if item.get("ageGroup") not in AGE_GROUPS:
            errors.append(f"{item_id}: ageGroup inválido.")
        if item.get("gamificationLevel") not in MESSAGE_LEVELS:
            errors.append(f"{item_id}: gamificationLevel inválido.")
        if item.get("eventType") not in MESSAGE_EVENTS:
            errors.append(f"{item_id}: eventType inválido.")
        if item.get("category") not in MESSAGE_CATEGORIES:
            errors.append(f"{item_id}: categoría de mensaje inválida.")
        if item.get("tone") not in MESSAGE_TONES:
            errors.append(f"{item_id}: tone inválido.")
        if item.get("usageChannel") != "Pantalla/TTS":
            errors.append(f"{item_id}: usageChannel inválido.")
        if item.get("length") not in {"Corta", "Media"}:
            errors.append(f"{item_id}: length inválida.")
        if item.get("allowsRepetition") != "Sí con control":
            errors.append(f"{item_id}: allowsRepetition inválido.")
        if item.get("eventOrigin") not in MESSAGE_ORIGINS:
            errors.append(f"{item_id}: eventOrigin inválido.")
        if item.get("status") != "Validado banco v1.0":
            errors.append(f"{item_id}: status de mensaje inválido.")
        if item.get("ttsActivation") != "Según configuración del usuario":
            errors.append(f"{item_id}: ttsActivation inválido.")
        if not isinstance(item.get("gamificationRequired"), bool):
            errors.append(f"{item_id}: gamificationRequired debe ser booleano.")
        for field in REQUIRED_FIELDS["reinforcement_messages.json"] - {"id"}:
            if item.get(field) in (None, ""):
                errors.append(f"{item_id}: {field} es obligatorio.")

    visual_activity_ids: list[str] = []
    for item in payloads["visual_references.json"]:
        activity_id = item.get("activityId")
        visual_activity_ids.append(activity_id)
        if activity_id not in activity_ids:
            errors.append(f"Referencia visual: actividad inexistente {activity_id!r}.")
        if item.get("visualSource") not in VISUAL_SOURCES:
            errors.append(f"{activity_id}: visualSource inválido.")
        if item.get("reviewStatus") not in REVIEW_STATUSES:
            errors.append(f"{activity_id}: reviewStatus inválido.")
        if item.get("visualSource") == "PENDING":
            forbidden = (
                "arasaacId", "variant", "localFile", "attribution", "license",
            )
            if any(item.get(field) is not None for field in forbidden):
                errors.append(
                    f"{activity_id}: pendiente con metadatos visuales simulados."
                )
        elif item.get("visualSource") == "ARASAAC":
            if not isinstance(item.get("arasaacId"), int) or item["arasaacId"] <= 0:
                errors.append(f"{activity_id}: arasaacId debe ser entero positivo.")
            for field in ("localFile", "attribution", "license"):
                if not item.get(field):
                    errors.append(f"{activity_id}: {field} obligatorio para ARASAAC.")
    if set(visual_activity_ids) != activity_ids or len(visual_activity_ids) != len(
        activity_ids
    ):
        errors.append(
            "visual_references.json: debe existir una referencia por actividad."
        )

    computed_master_records = sum(
        len(payloads[filename])
        for filename in (
            "activities.json", "routines.json", "achievements.json",
            "challenges.json", "reinforcement_messages.json",
        )
    )
    if counts.get("masterRecords") != computed_master_records:
        errors.append("manifest.json: masterRecords no coincide con los artefactos.")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "directory",
        nargs="?",
        type=Path,
        default=Path("data/master_bank/generated/v1"),
    )
    args = parser.parse_args()
    errors = validate_catalog(args.directory)
    if errors:
        print(f"VALIDACIÓN FALLIDA ({len(errors)} error(es)):")
        for error in errors:
            print(f"- {error}")
        return 1
    print(f"VALIDACIÓN CORRECTA: {args.directory}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
