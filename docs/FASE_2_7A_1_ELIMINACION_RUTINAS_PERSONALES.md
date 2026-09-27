# Fase 2.7A.1: eliminación segura de rutinas personales

## Punto de partida

- Rama: `feature/rutinago-master-bank-arasaac`.
- HEAD inicial: `8a641ac08c1d050998ca7b0f77e90fbef414b59c`.
- Problema: **Mis rutinas** permitía editar y archivar, pero no eliminar definitivamente una definición personal.

## Contrato de datos y decisión

`user_routine_steps.user_routine_id` tiene una FK a `user_routines` con `ON DELETE CASCADE`. Al borrar una rutina personal, Room elimina sus pasos en la misma transacción. Las tablas `routine_executions` y `execution_steps` guardan el nombre, las actividades, las instrucciones, las referencias visuales y los estados como snapshots. `routine_executions.user_routine_id` conserva el identificador de origen como dato histórico, sin una FK obligatoria hacia `user_routines`. La lectura de progreso e historial utiliza esos snapshots.

Por eso **no se requiere una migración de Room**: la versión sigue siendo 3. La eliminación no afecta ejecuciones terminadas, progreso histórico, otras rutinas ni el catálogo maestro.

`RoomUserRoutineRepository.delete(profileId, routineId)` ejecuta en `database.withTransaction` la comprobación de ejecución activa y el `DELETE` condicionado por ID y perfil. Si hay una ejecución `IN_PROGRESS` de esa rutina, lanza `UserRoutineDeletionBlockedException` antes de borrar. El inicio de ejecución y la eliminación usan transacciones de la misma base: cualquiera que se confirme primero determina si la otra operación puede continuar. Un ID ajeno o inexistente produce un error, sin éxito aparente. No hay borrados parciales ni `fallbackToDestructiveMigration`.

## Interfaz

Cada tarjeta personal conserva **Iniciar rutina** y **Editar**. **Más opciones** abre **Archivar** y **Eliminar**; las tarjetas de plantillas maestras carecen de esta acción. Archivar conserva su comportamiento previo. Eliminar abre un diálogo con el nombre de la rutina, la consecuencia permanente, la preservación del historial y los botones **Cancelar** y **Eliminar**. No hay borrado con un solo toque.

Si la ejecución de esa rutina está en curso, el diálogo explica el bloqueo y deshabilita Eliminar. El repositorio repite la comprobación dentro de la transacción para cubrir cambios concurrentes. Tras confirmar y recibir éxito de Room, el `Flow` de rutinas del perfil actual actualiza la lista y aparece **Rutina eliminada correctamente**. Ante una excepción, el diálogo permanece abierto, la rutina sigue visible, se muestra **No se pudo eliminar la rutina** y se puede reintentar.

La opción de menú anuncia **Eliminar rutina {nombre}** para TalkBack. El diálogo usa texto, título y botones accesibles; las superficies y tamaños de texto siguen el tema Día, Noche o Sistema, el alto contraste y las tres escalas de la app. La identificación de la acción no depende solo del color.

## Verificación

- Pruebas Room: borrado de rutina y pasos propios, Flow de lista, conservación de otra rutina y otro perfil, bloqueo de ejecución activa, conservación de snapshot terminado, rechazo de ID ajeno y protección de plantillas maestras.
- Pruebas de interfaz: Cancelar, confirmar, desaparición de tarjeta, mensaje posterior al éxito, error sin falso éxito, reintento, bloqueo visible de ejecución activa y archivado intacto.
- `testDebugUnitTest`, `assembleDebug`, `lintDebug`, `assembleDebugAndroidTest`, validador del banco maestro y pruebas Python existentes.
- El validador confirmó 203 actividades, 52 rutinas maestras, 17 categorías, 40 logros, 20 retos y 54 mensajes. Las 40 pruebas unitarias pasaron; lint terminó con 0 errores y avisos de versiones de dependencias existentes.
- La suite instrumentada necesita un dispositivo Android; compilarla no equivale a ejecutarla.

La búsqueda y la selección provisional de ARASAAC de la Fase 2.7A no cambian. No se asignan pictogramas ni se inicia la Fase 2.7B.
