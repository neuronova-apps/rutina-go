# RutinaGo — Fase 2.2: contrato de datos y preparación del banco maestro

Fecha de la instantánea: 24 de septiembre de 2026
Contrato: 1.0.0
Catálogo: 1.0.0

## Alcance y fuente de verdad

La fuente editorial continúa siendo el [Google Sheet autorizado](https://docs.google.com/spreadsheets/d/1is0zFNvvA69Rb_C0V0qYG4a-AtEh0oxdSiJTFEFlaYo/edit). El repositorio conserva una instantánea CSV revisable y genera JSON versionado para que el banco no dependa de listas Kotlin gigantes ni de una conexión de red durante la compilación.

La comprobación directa de metadatos confirmó 17 pestañas. Para esta fase se sincronizaron únicamente las pestañas de datos contractuales:

| Pestaña | sheetId | Filas de datos |
| --- | ---: | ---: |
| Banco_actividades | 1097218425 | 203 |
| Banco_rutinas | 1664810288 | 52 |
| Banco_logros_retos | 1162591216 | 60 |
| Banco_mensajes_refuerzo | 1394843334 | 54 |
| Catalogos | 1728542767 | 17 categorías |

Los 369 registros maestros son 203 actividades + 52 rutinas + 40 logros + 20 retos + 54 mensajes. Las categorías, los pasos desnormalizados y las referencias visuales son estructuras auxiliares y no incrementan ese total.

No se consultó la API de ARASAAC, no se descargaron pictogramas y no se añadió ningún identificador visual supuesto.

## Organización del repositorio

    data/master_bank/
    ├── source/                  # Instantánea CSV exacta y metadatos de origen
    ├── schema/
    │   └── contract-v1.json    # Contrato versionado, vocabularios y políticas
    └── generated/v1/           # JSON determinista y manifest.json

    tools/master_bank/
    ├── prepare_source_snapshot.py
    ├── export_master_bank.py
    ├── validate_master_bank.py
    └── tests/test_master_bank.py

Cada artefacto JSON usa el mismo sobre con catalogVersion, contractVersion e items. manifest.json registra versión de catálogo y contrato, fecha UTC real de la instantánea, URL e identificador de la hoja, número de pestañas, conteos, SHA-256 de las cinco entradas y SHA-256 de cada salida.

La fecha no está fijada dentro del exportador. prepare_source_snapshot.py la obtiene del reloj UTC cada vez que se prepara una nueva instantánea. El exportador reutiliza esa fecha registrada; por eso dos ejecuciones sobre la misma instantánea y versiones producen exactamente los mismos bytes.

## Contrato y preservación de campos

### Categorías

categories.json mantiene el nombre y el orden editorial de las 17 categorías de Catalogos. No se inventan IDs para una entidad que no los tiene en la fuente.

### Actividades

activities.json conserva ACT-### y todos los campos fuente:

| Google Sheet | JSON |
| --- | --- |
| ID | id |
| Categoria_principal | category |
| Actividad | name |
| Instruccion_simple | simpleInstruction |
| Termino_busqueda_ARASAAC | arasaacSearchTerm |
| Grupo_edad | ageGroup |
| Nivel_autonomia | autonomyLevel |
| Supervision | supervision |
| Modo_simple | simpleMode (booleano) |
| Modo_avanzado | advancedMode (booleano) |
| Recurso_visual | visualResource |
| Estado | status |
| Observaciones | observations (admite null) |
| Contextos_uso | usageContexts (lista ordenada) |

arasaacSearchTerm es solo el término editorial de búsqueda; no equivale a un identificador ARASAAC.

### Rutinas y pasos

routines.json conserva RUT-###, nombre, área, contexto, edad, objetivo, momento sugerido, conteos declarados, personalización, supervisión, estado y observaciones.

Las tres columnas de secuencia se trasladan a routine_steps.json. Cada objeto tiene routineId, sequenceType, position y activityId. sequenceType solo admite SIMPLE, COMPLETE u OPTIONAL.

Las secuencias simple y completa se separan usando el delimitador editorial >; los opcionales usan |. No se concatenan variantes ni se pierde su orden. El exportador produjo 490 objetos de paso. El validador compara los conteos declarados de simple/completa con los objetos generados y comprueba todas las referencias ACT.

### Logros y retos

La pestaña común se divide sin cambiar identificadores:

- achievements.json: 40 registros LOG-### con type igual a Logro.
- challenges.json: 20 registros RET-### con type igual a Reto.

Ambos conservan categoría, nivel, edad, tipo y lista de referencias, condición, cantidad objetivo, ventana, repetición, recompensa, política de penalización, activación, estado y observaciones. Las referencias ACT y RUT deben resolver dentro del mismo catálogo.

### Mensajes de refuerzo

reinforcement_messages.json conserva los 54 MSG-### y los 17 campos editoriales: evento, categoría, tono, edad, requisito y nivel de gamificación, texto, canal, longitud, repetición, origen, regla de seguridad, estado, observaciones, frecuencia y configuración TTS.

### Referencias visuales

visual_references.json desacopla la actividad del recurso visual mediante:

    activityId, visualSource, arasaacId, variant, localFile,
    alternativeText, reviewStatus, attribution, license

En esta fase existe exactamente una fila por actividad con:

- visualSource igual a PENDING.
- reviewStatus igual a PENDING_SELECTION.
- arasaacId, variant, localFile, attribution y license en null.
- alternativeText derivado del nombre editorial de la actividad.

Esto deja el hueco contractual preparado sin presentar como seleccionado ningún pictograma. Para un recurso futuro con visualSource igual a ARASAAC, el validador exige un arasaacId entero positivo y metadatos no vacíos de archivo local, atribución y licencia.

## Vocabularios y tipos

El contrato canónico está en data/master_bank/schema/contract-v1.json. Entre otros, limita:

- IDs a ACT-###, RUT-###, LOG-###, RET-### y MSG-###.
- edades a General, Escolar+ o Adolescente.
- pasos a SIMPLE, COMPLETE u OPTIONAL.
- referencias de gamificación a ACT o RUT.
- fuentes visuales a PENDING, ARASAAC u OWN_PHOTO.
- estados de revisión visual a PENDING_SELECTION, SELECTED, REVIEWED o APPROVED.

Los valores binarios de la hoja (Sí/No) se exportan como booleanos. Los conteos y ventanas se exportan como enteros. Las listas editoriales se conservan en orden.

## Flujo reproducible

Para una actualización futura:

1. Revisar las 17 pestañas y exportar como CSV las cinco pestañas contractuales con los nombres existentes en data/master_bank/source.
2. Preparar una instantánea fechada:

       python -B tools/master_bank/prepare_source_snapshot.py <directorio-csv-revisado>

   El comando valida primero la presencia y cabeceras de las cinco entradas; si fallan, no reemplaza la instantánea vigente.

3. Generar el catálogo:

       python -B tools/master_bank/export_master_bank.py

4. Validarlo:

       python -B tools/master_bank/validate_master_bank.py

5. Ejecutar las pruebas:

       python -B -m unittest discover -s tools/master_bank/tests -v

El exportador no corrige ni completa la fuente. Ante cabeceras o valores incompatibles falla con un error explícito. El validador es de solo lectura.

## Cobertura del validador

El validador comprueba:

- conteos 203/52/40/20/54 y total 369;
- IDs únicos y formatos por entidad;
- presencia exacta de campos obligatorios;
- tipos, vocabularios y categorías;
- integridad de referencias entre actividades, rutinas y gamificación;
- orden contiguo y variantes de pasos;
- coherencia entre conteos declarados y pasos;
- obligatoriedad de campos de mensajes y gamificación;
- una referencia visual por actividad;
- ausencia de metadatos ARASAAC simulados en pendientes;
- inventario, conteos y SHA-256 del manifiesto.

Las pruebas usan los JSON reales de data/master_bank/generated/v1, no mocks. Además exportan dos veces a directorios temporales y comparan todos los archivos byte a byte.

## Proyección futura a Room

Esta fase no añade Room. La proyección recomendada mantiene el contrato y separa datos maestros de datos del usuario:

| Tabla futura | Clave primaria | Relaciones principales |
| --- | --- | --- |
| categories | nombre o clave estable acordada en una migración futura | 1:N con actividades y rutinas |
| activities | activity_id (ACT-###) | categoría; 1:1/N con referencias visuales |
| routines | routine_id (RUT-###) | categoría/área |
| routine_steps | (routine_id, sequence_type, position) | FK a rutinas y actividades |
| achievements | achievement_id (LOG-###) | referencias mediante tabla puente |
| challenges | challenge_id (RET-###) | referencias mediante tabla puente |
| gamification_references | (gam_id, reference_type, reference_id, position) | ACT o RUT validado en importación |
| reinforcement_messages | message_id (MSG-###) | eventos y configuración |
| visual_references | activity_id | FK a actividad; metadatos ARASAAC/propios |
| catalog_metadata | catalog_version | versión, fecha, contrato y checksums |

El importador futuro deberá ejecutarse en transacción, validar primero el manifiesto y cargar por versión. No debe reinterpretar IDs ni corregir referencias. Los perfiles, preferencias y opciones del usuario permanecen en DataStore: esta fase no cambia su modelo ni realiza migraciones.

## Límites de la fase

No se modificaron pantallas, navegación, gamificación en ejecución, modelos de perfiles, DataStore, Room ni dependencias Android. Tampoco se consumieron APIs externas ni se descargaron recursos visuales. El resultado es exclusivamente la base de datos contractual y reproducible para fases posteriores.
