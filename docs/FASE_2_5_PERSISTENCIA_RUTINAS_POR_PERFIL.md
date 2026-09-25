# RutinaGo — Fase 2.5: persistencia de rutinas personalizadas por perfil

Fecha: 25 de septiembre de 2026  
Rama: `feature/rutinago-master-bank-arasaac`

## Resultado

RutinaGo permite crear, guardar, recuperar, editar y archivar rutinas personales asociadas al UUID del perfil activo. Las rutinas se pueden iniciar desde una de las 52 plantillas maestras o desde una selección de las 203 actividades reales.

El catálogo maestro y los datos del usuario permanecen separados. Personalizar una plantilla crea una rutina nueva; no modifica `catalog_routines`, `catalog_routine_steps` ni `catalog_activities`.

## Esquema Room v2

`RutinaGoDatabase` evoluciona del esquema 1 al 2 y conserva las tablas de catálogo de las fases anteriores. El nuevo esquema exportado está en:

    app/schemas/com.neuronovaapps.rutinago.data.catalog.local.RutinaGoDatabase/2.json

Se añaden dos entidades:

### UserRoutineEntity

Tabla `user_routines`:

| Campo | Uso |
| --- | --- |
| `user_routine_id` | ID estable propio del borrador/rutina personal. |
| `profile_id` | UUID existente del perfil en DataStore. |
| `source_template_id` | RUT de origen, nullable para creaciones desde cero. |
| `name` | Nombre personalizado validado. |
| `description` | Descripción opcional preparada para evolución. |
| `created_at_epoch_millis` | Fecha de creación conservada al editar. |
| `modified_at_epoch_millis` | Fecha actualizada en guardado o archivado. |
| `archived` | Estado lógico; no elimina datos. |

Los índices por `(profile_id, archived)` y `source_template_id` permiten recuperar las rutinas activas del perfil y conservar la procedencia editorial.

### UserRoutineStepEntity

Tabla `user_routine_steps`:

| Campo | Uso |
| --- | --- |
| `user_routine_id` | FK a la rutina personal con borrado en cascada. |
| `position` | Orden 1..N, parte de la clave primaria. |
| `activity_id` | FK RESTRICT a `catalog_activities.activity_id`. |
| `custom_instruction` | Instrucción personal opcional. |
| `future_visual_reference` | Hueco nullable para una referencia visual futura. |

No se copian nombres de actividades como sustituto de la relación. Cada paso conserva el ACT maestro y Room impide referencias inexistentes o el borrado accidental de una actividad referenciada.

## Migración 1 → 2

`MIGRATION_1_2` crea únicamente las dos tablas de usuario y sus índices. No usa migración destructiva, no vacía tablas, no reinicializa la base y no modifica metadatos de importación.

La prueba de migración abre el esquema v1, inserta un marcador de catálogo, aplica la migración y valida tanto la conservación del marcador como la presencia y estructura de las tablas nuevas. Las pruebas de repositorio comprueban además que las operaciones personales mantienen los conteos maestros 17/203/52/490.

El singleton de la aplicación registra explícitamente `MIGRATION_1_2`. Una instalación existente conserva su catálogo y recibe las tablas vacías de usuario.

## DataStore y perfiles

DataStore conserva el nombre `appearance_preferences`, las claves existentes, los perfiles y sus UUID. No se migró ningún perfil a Room.

`profile_id` almacena el UUID estable, nunca el nombre visible. Renombrar un perfil no cambia la propiedad de sus rutinas. `activeRoutines(profileId)` filtra siempre por propietario y por `archived = false`; guardar o archivar exige el `profileId` activo.

Una comprobación de propietario impide reutilizar el ID de una rutina perteneciente a otro perfil.

## Repositorio y transacciones

`RoomUserRoutineRepository` implementa:

- `activeRoutines(profileId): Flow<List<UserRoutine>>`;
- `save(profileId, draft)`;
- `archive(profileId, routineId)`.

`save` valida nombre, secuencia, plantilla opcional, IDs ACT, duplicados y propiedad. Después ejecuta en una única transacción:

1. conserva la fecha de creación si la rutina ya existe;
2. actualiza la cabecera por ID estable;
3. reemplaza exclusivamente los pasos de esa rutina;
4. numera los pasos desde 1;
5. relee el resultado confirmado.

El UUID se crea al abrir el editor y se conserva mediante `rememberSaveable`. Si el guardado falla, el borrador y su ID permanecen; reintentar realiza un `upsert` sobre la misma rutina y no crea duplicados.

## Creación desde plantilla

El recorrido queda completo:

    Crear → Elegir origen → Usar una plantilla → Seleccionar → Personalizar → Guardar rutina

El borrador conserva `source_template_id`. Simple inicia con `SIMPLE`; Avanzado inicia con `COMPLETE` y presenta `OPTIONAL` como sugerencias separadas que la persona puede añadir. Las variantes no se concatenan automáticamente.

El usuario puede cambiar el nombre, añadir/quitar actividades y reorganizar cada paso antes de guardar. El guardado confirmado muestra una acción para abrir **Mis rutinas**.

## Creación desde cero

El editor consulta las 203 actividades Room y exige nombre no vacío y al menos una actividad. Avanzado conserva búsqueda y categorías funcionales; Simple reduce controles, pero utiliza la misma base.

Las acciones **Añadir**, **Quitar**, **Subir** y **Bajar** modifican la secuencia en memoria. Solo **Guardar rutina** inicia la transacción real.

## Edición

`Rutinas → Mis rutinas → rutina → Editar` abre el mismo editor con el ID personal, nombre, procedencia, pasos e instrucciones personalizadas existentes.

El guardado conserva `created_at_epoch_millis`, actualiza `modified_at_epoch_millis` y reemplaza únicamente la secuencia personal. La rutina o actividades maestras nunca se actualizan desde este flujo.

## Borradores y errores

Nombre, pasos, orden, filtros, UUID y mensajes se conservan con `rememberSaveable` durante recomposiciones y cambios normales de tema/configuración. No se declara recuperación del borrador tras cerrar el proceso.

Salir con cambios solicita confirmación tanto desde creación como desde edición. Un error de validación o transacción deja el borrador visible y permite reintentar. El éxito solo se muestra después de que Room devuelve la rutina releída dentro de la transacción.

## Archivado

**Mis rutinas** ofrece Archivar con confirmación. La operación actualiza `archived` y la fecha de modificación filtrando simultáneamente por `user_routine_id` y `profile_id`.

La rutina deja de aparecer en la lista activa, pero sus datos y pasos permanecen en Room. Esta fase no expone eliminación definitiva ni restauración de archivados.

## Accesibilidad

Se conservan Día/Noche/Sistema, alto contraste y las tres escalas de texto. La edición usa listas verticales desplazables, encabezados semánticos y texto visible además del color.

El orden no depende de arrastrar. Cada paso ofrece botones accesibles **Subir**, **Bajar** y **Quitar**, con descripciones semánticas que incluyen el nombre de la actividad. Los controles imposibles en los extremos aparecen deshabilitados.

Los recursos visuales continúan siendo placeholders accesibles pendientes de validación; no se consultó ni descargó contenido ARASAAC.

## Pruebas

Cobertura JVM y/o instrumentada añadida:

- migración Room v1→v2 y validación del esquema exportado;
- conservación de una fila del catálogo durante la migración;
- creación desde plantilla con RUT de origen y ACT reales;
- creación desde cero;
- recuperación tras cerrar y reabrir una base en disco;
- edición, fecha de modificación, instrucción personal y orden 1..N;
- eliminación de un paso;
- separación entre dos UUID de perfil;
- cambio de nombre de perfil sin perder propiedad;
- cambio Avanzado/Simple sin perder rutinas;
- confirmación al abandonar cambios;
- archivado lógico;
- reintento con el mismo ID sin duplicados;
- rechazo de nombres/secuencias inválidos y ACT inexistentes;
- rechazo de edición o archivado desde otro perfil;
- conservación de conteos del catálogo;
- conservación de DataStore;
- navegación después de guardar hacia Mis rutinas y edición.

Comandos de verificación:

    .\gradlew.bat testDebugUnitTest
    .\gradlew.bat assembleDebug
    .\gradlew.bat lintDebug
    .\gradlew.bat assembleDebugAndroidTest
    python -B tools/master_bank/validate_master_bank.py
    python -B -m unittest discover -s tools/master_bank/tests -v

En esta estación, `adb devices` no detectó dispositivos y el SDK no tiene un AVD configurado. El APK y todas las fuentes instrumentadas se compilaron correctamente, pero las pruebas instrumentadas quedaron pendientes de ejecución.

## Limitaciones

- No hay cliente ni descarga ARASAAC.
- No se admiten fotografías personalizadas.
- No existe historial persistente de ejecuciones.
- Gamificación y notificaciones no cambian.
- No se programa una rutina personal.
- Los borradores no se recuperan después de cerrar el proceso.
- Las rutinas archivadas no tienen todavía una pantalla de restauración.
- La ejecución visual continúa pendiente de migrarse desde el contenido demostrativo hacia las rutinas personales.
