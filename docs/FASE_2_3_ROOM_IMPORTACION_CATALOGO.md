# RutinaGo — Fase 2.3: Room e importación del catálogo maestro

Fecha: 24 de septiembre de 2026

## Resultado

RutinaGo incorpora su primera base persistente Room y empaqueta dentro del APK el catálogo maestro 1.0.0 generado en la Fase 2.2. La aplicación importa y verifica localmente:

| Entidad | Registros |
| --- | ---: |
| Categorías | 17 |
| Actividades | 203 |
| Rutinas | 52 |
| Pasos normalizados | 490 |
| Logros | 40 |
| Retos | 20 |
| Mensajes de refuerzo | 54 |
| Registros maestros | 369 |

Las pantallas siguen usando DemoContent. La incorporación de Room no cambia navegación, aspecto, ejecución, progreso, perfiles ni preferencias.

## Versiones

- Room: 2.8.5.
- KSP: 2.3.6.
- Esquema Room: 1.
- Contrato del catálogo: 1.0.0.
- Catálogo empaquetado: 1.0.0.
- minSdk: 24, compatible con Room 2.8.

Room 2.8.5 es la versión estable documentada en las [notas oficiales de AndroidX Room](https://developer.android.com/jetpack/androidx/releases/room). KSP se usa en lugar de KAPT y mantiene el soporte de Kotlin integrado de AGP 9 descrito en la [guía oficial](https://developer.android.com/build/migrate-to-built-in-kotlin).

No se actualizaron AGP, Kotlin, Compose ni otras dependencias no relacionadas.

## Componentes principales

La implementación está separada en:

- data/catalog/local/entity: entidades y vocabularios persistentes.
- data/catalog/local/dao: consultas, escrituras y metadatos.
- data/catalog/local/RutinaGoDatabase.kt: base Room y singleton.
- data/catalog/importer: assets, parser, validación e importación atómica.
- data/catalog/repository: repositorio Room y estado de importación.
- domain/catalog: modelos e interfaces independientes de Room.
- app/src/main/assets/master_catalog/v1: catálogo real y manifiesto.

El esquema exportado se conserva en app/schemas para permitir migraciones verificables en fases posteriores.

## Entidades y relaciones

### Catálogo maestro

- CatalogCategoryEntity: nombre original como clave primaria, orden editorial y versión.
- CatalogActivityEntity: ID ACT original, categoría, nombre, instrucción, término ARASAAC, edad, autonomía, supervisión, modos, recurso visual, estado y observaciones.
- ActivityContextCrossRef: relación múltiple actividad-contexto.
- CatalogRoutineEntity: ID RUT original y todos los campos no secuenciales.
- CatalogRoutineStepEntity: relación ordenada entre rutina y actividad con variante SIMPLE, COMPLETE u OPTIONAL.
- CatalogAchievementEntity: ID LOG y campos completos del logro.
- CatalogChallengeEntity: ID RET y campos completos del reto.
- CatalogGamificationReferenceEntity: referencias ordenadas ACT/RUT para logros y retos.
- CatalogReinforcementMessageEntity: ID MSG y sus 17 campos contractuales.
- CatalogVisualReferenceEntity: estructura separada de selección visual.

### Metadatos

CatalogImportMetadataEntity registra un historial de intentos. Cada entrada contiene:

- ID único del intento.
- versión del catálogo y contrato;
- SHA-256 del manifiesto;
- fecha real del intento en epoch milliseconds;
- estado SUCCESS, FAILED o INCOMPATIBLE;
- conteos verificados;
- mensaje de error cuando corresponde.

No existe un booleano isImported. CatalogImportStateRepository distingue catálogo ausente, listo, con actualización disponible, fallido o incompatible.

## Diagrama

    catalog_categories
        ├──< catalog_activities ──< catalog_activity_contexts
        │          └──< catalog_visual_references
        └──< catalog_routines ──< catalog_routine_steps >── catalog_activities

    catalog_achievements ─┐
                          ├──< catalog_gamification_references >── ACT / RUT
    catalog_challenges ───┘

    catalog_reinforcement_messages
    catalog_import_metadata

Los IDs ACT, RUT, LOG, RET y MSG son claves estables. No se renumeran ni se reemplazan por IDs autogenerados.

## DAOs

CatalogReadDao ofrece Flow para:

- todas las categorías;
- actividades por categoría;
- actividad por ID;
- rutinas por categoría;
- rutina por ID;
- pasos ordenados de una rutina;
- pasos filtrados por variante;
- logros;
- retos;
- mensajes.

También incluye consultas suspendidas por ID y de pasos para comprobaciones directas.

CatalogWriteDao contiene upserts por lotes, conteos y limpieza selectiva de filas pertenecientes a versiones anteriores. CatalogImportMetadataDao conserva el historial y permite consultar el intento más reciente o el último éxito.

RoomCatalogRepository transforma entidades persistentes en modelos del dominio. Ninguna pantalla lo consume todavía.

## Catálogo dentro del APK

Los nueve archivos de data/master_bank/generated/v1 se copian sin cambiar bytes a:

    app/src/main/assets/master_catalog/v1/

Se incluye manifest.json y los ocho artefactos:

- categories.json
- activities.json
- routines.json
- routine_steps.json
- achievements.json
- challenges.json
- reinforcement_messages.json
- visual_references.json

tools/master_bank/package_android_assets.py valida primero el banco de la Fase 2.2, copia los archivos y vuelve a comprobar sus SHA-256. Una prueba JVM confirma que los assets y los artefactos originales son idénticos byte a byte.

## Importador versionado

MasterCatalogImporter sigue este orden:

1. Lee manifest.json.
2. exige contrato 1.0.0;
3. valida fecha, fuente y las 17 pestañas declaradas;
4. comprueba el inventario exacto de artefactos;
5. calcula y compara cada SHA-256;
6. comprueba versiones y conteos de los sobres JSON;
7. transforma todos los registros a entidades;
8. valida IDs, categorías, contextos, pasos, variantes, posiciones y referencias;
9. comprueba que no existan metadatos ARASAAC simulados;
10. inicia una transacción Room;
11. ejecuta upserts por claves estables;
12. retira únicamente filas maestras selladas con versiones anteriores;
13. vuelve a contar entidades y relaciones;
14. registra SUCCESS dentro de la misma transacción;
15. confirma la transacción.

Un error antes o durante la transacción impide registrar éxito. Room revierte las escrituras parciales y el importador registra posteriormente un intento FAILED o INCOMPATIBLE. El error no se autocorrige ni modifica el asset.

## Idempotencia

Antes de escribir, el importador compara:

- versión del catálogo;
- SHA-256 del manifiesto;
- conteos de todas las tablas maestras;
- conteos de contextos y referencias de gamificación.

Si coinciden con el último éxito, devuelve ALREADY_CURRENT y no crea otro intento ni ejecuta upserts. Una segunda importación mantiene exactamente 203 actividades, 52 rutinas, 490 pasos, 40 logros, 20 retos, 54 mensajes y 17 categorías.

Un Mutex evita importaciones concurrentes dentro del mismo proceso.

## Actualizaciones

Las actualizaciones se hacen por IDs estables con Upsert. Cada fila maestra y de relación conserva catalogVersion como metadato de procedencia; después de insertar la nueva versión se eliminan selectivamente las filas que no fueron renovadas.

No se vacían tablas completas. La operación ocurre en una transacción y no toca tablas ajenas al catálogo.

Las futuras tablas de rutinas personalizadas, visuales propios, historial, progreso y perfiles deberán vivir separadas de estas tablas maestras. Sus migraciones deberán conservar referencias o aplicar una política contractual de desactivación antes de retirar IDs maestros. No se habilitaron migraciones destructivas.

## DataStore

DataStore continúa usando appearance_preferences y conserva sin cambios:

- tema;
- tamaño de texto;
- alto contraste;
- modo Simple/Avanzado;
- perfiles;
- perfil activo.

Room no lee, borra ni reinicializa estas preferencias. Las pruebas existentes de DataStore se mantienen y una prueba de integración verifica que importar Room no modifica alto contraste.

## Preparación ARASAAC

CatalogActivityEntity conserva arasaacSearchTerm como texto de búsqueda. No se interpreta como ID.

Las 203 filas visuales siguen con visualSource PENDING y arasaacId nulo. El validador rechaza un pendiente con variante, archivo, atribución o licencia simulados. Para un registro ARASAAC futuro exigirá ID positivo, archivo local, atribución y licencia.

No se añadió cliente HTTP, no se realizaron búsquedas y no se descargaron imágenes.

## Inicialización

RutinaGoApplication inicia la importación en Dispatchers.IO al arrancar. El fallo se registra en metadatos y en Logcat, pero no bloquea la interfaz. CatalogContainer expone la base, el importador, el repositorio de lectura y el estado de importación para futuras fases.

Las pantallas no consultan todavía CatalogContainer.

## Pruebas

### JVM ejecutadas

- validación del banco maestro real;
- conteos e IDs;
- orden y referencias de pasos;
- ausencia de IDs ARASAAC inventados;
- SHA-256 de artefactos;
- exportación reproducible;
- igualdad byte a byte entre generated/v1 y assets.

### Instrumentadas compiladas

MasterCatalogRoomTest usa una base Room real en memoria y cubre:

- apertura;
- importación inicial;
- conteos;
- consulta por categoría e ID;
- pasos y orden;
- logros, retos y mensajes;
- segunda importación idempotente;
- actualización a una versión derivada del asset real;
- checksum incorrecto;
- referencia ACT inexistente;
- rollback forzado por SQLite;
- conservación de DataStore.

assembleDebugAndroidTest compila correctamente el APK de pruebas. En esta estación no se detectó un dispositivo conectado ni un AVD configurado, por lo que no se declara una ejecución instrumentada.

## Limitaciones

- DemoContent sigue siendo la fuente de las pantallas.
- No hay migración desde un esquema Room anterior porque este es el esquema 1.
- No hay entidades de usuario, historial o personalización en Room.
- No se activa gamificación.
- No se conecta ARASAAC.
- No se implementa una política de retiro de IDs que ya estén referenciados por futuros datos de usuario; deberá aprobarse en el contrato antes de esa evolución.
