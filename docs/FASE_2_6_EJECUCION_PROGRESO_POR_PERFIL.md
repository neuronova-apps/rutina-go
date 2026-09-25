# RutinaGo — Fase 2.6: ejecución y progreso persistentes por perfil

Fecha: 25 de septiembre de 2026

Rama: `feature/rutinago-master-bank-arasaac`

HEAD inicial: `a7d56f760edd30367cce70173edf7c452492c461`

## Resultado

RutinaGo ejecuta exclusivamente rutinas personales del perfil activo. Cada inicio crea una ejecución Room y una instantánea ordenada de sus actividades; completar, omitir, finalizar parcialmente o abandonar modifica esa ejecución de forma transaccional. El historial y los indicadores de Progreso se calculan a partir de esos registros reales y permanecen disponibles tras cerrar la aplicación.

Las plantillas maestras continúan siendo contenido editorial. No se ejecutan directamente ni se modifican al usar una rutina personal derivada de ellas.

## Esquema Room v3

La versión previa comprobada era Room v2. La base evoluciona explícitamente mediante `MIGRATION_2_3`, registrada junto con `MIGRATION_1_2`; una instalación v1 puede recorrer ambas migraciones sin destrucción. No se usa `fallbackToDestructiveMigration`.

El esquema exportado está en:

    app/schemas/com.neuronovaapps.rutinago.data.catalog.local.RutinaGoDatabase/3.json

La migración solo crea las tablas e índices de ejecución. No altera las tablas del catálogo, `user_routines`, `user_routine_steps`, metadatos de importación ni DataStore.

### `routine_executions`

Conserva:

- ID UUID estable de la ejecución;
- UUID del perfil propietario;
- ID de la rutina personal de origen;
- nombre de la rutina al iniciarse;
- fechas de inicio y finalización;
- estado `IN_PROGRESS`, `COMPLETED`, `PARTIAL` o `ABANDONED`;
- total, completados, omitidos y posición pendiente actual.

Los índices por perfil/estado, perfil/fecha e ID de rutina personal permiten recuperar una ejecución pendiente y construir el historial sin mezclar perfiles.

### `execution_steps`

La clave primaria es `(execution_id, position)`. Cada fila conserva el ID ACT original, nombre, instrucción efectiva, referencia visual futura nullable, estado `PENDING`, `COMPLETED` o `SKIPPED` y fecha de realización. La FK a la ejecución usa borrado en cascada; no se enlaza la instantánea a la rutina personal para que una edición o archivado posterior no reescriba el historial.

## Repositorio y transacciones

`RoomRoutineExecutionRepository` concentra las reglas de propiedad y consistencia:

1. `start` valida perfil, propiedad, estado no archivado y secuencia no vacía.
2. Si la misma rutina ya está activa, devuelve esa ejecución; si existe otra activa en el perfil, exige reanudarla o salir.
3. Copia en una transacción el nombre de la rutina y las actividades ordenadas con sus instrucciones efectivas.
4. `completeStep` y `skipStep` solo cambian una fila `PENDING` que sea el paso actual.
5. Después de cada transición recalcula conteos desde las filas persistidas y selecciona el siguiente pendiente.
6. La última actividad produce `COMPLETED` si no hubo omisiones y `PARTIAL` si las hubo.
7. `finishPartial` conserva pendientes y registra el cierre explícito como `PARTIAL`.
8. `abandon` conserva lo realmente completado u omitido, registra `ABANDONED` y no lo cuenta como rutina completada.

Las actualizaciones filtran también por `profile_id`. Una pulsación repetida no vuelve a incrementar conteos ni crea una segunda finalización.

## Inicio, ejecución y recuperación

El recorrido real es:

    Rutinas → Mis rutinas → Iniciar rutina → ejecución persistente

La pantalla presenta nombre, posición, total, actividad, instrucción textual, placeholder visual pendiente y las acciones Completar, Omitir, Pausar, Finalizar por ahora y Salir. No consulta `DemoContent`.

Una ejecución `IN_PROGRESS` aparece en Hoy y Mis rutinas con la acción **Reanudar rutina**. La posición actual se obtiene de Room. El ID seleccionado se conserva con `rememberSaveable` y se recarga desde el repositorio tras recrear la Activity. Tras cerrar y abrir la aplicación, el perfil propietario puede reanudar desde el aviso; no se inicia automáticamente una segunda ejecución.

Atrás de Android y **Salir** abren la misma confirmación. Atrás sobre el diálogo lo cierra. Confirmar salida persiste `ABANDONED` y regresa al origen sin mostrar un resultado de éxito.

## Resultados, historial y progreso

La pantalla de resultado usa la ejecución guardada y muestra nombre, completados, omitidos, total y el mensaje respetuoso correspondiente:

- `COMPLETED`: **Rutina completada**.
- `PARTIAL`: **Rutina realizada parcialmente**.

`ABANDONED` queda en el historial, pero una salida normal no abre un panel de resultado.

Progreso observa únicamente el `profile_id` activo y calcula:

- rutinas completadas;
- rutinas parciales;
- actividades completadas reales;
- historial cronológico con fecha, resultado y conteos.

Si no hay ejecuciones se muestra un estado vacío. Los logros, retos y cifras demostrativas se retiraron de esta pantalla.

## Pantalla Hoy y cambio de perfil

Hoy ya no presenta rutinas u horarios ficticios. Informa que la programación definitiva todavía no existe, permite ir a Mis rutinas y muestra solo un resumen calculado cuando hay actividad real.

Mientras hay una ejecución activa, la interfaz de perfiles deshabilita el cambio y explica que primero debe reanudarse y finalizarse o abandonarse. La capa de actividad vuelve a consultar Room antes de cambiar el perfil; además, todas las mutaciones validan el `profileId` original, por lo que un cambio externo no puede atribuir pasos a otro perfil.

## Accesibilidad

Se mantienen Día, Noche, Sistema, alto contraste y las tres escalas de texto. La ejecución conserva desplazamiento vertical, objetivos táctiles de Material, encabezados semánticos, región viva para la pausa e instrucciones siempre visibles en texto. Los recursos sin validar se anuncian como **Visual pendiente** y no se simulan pictogramas ARASAAC.

## Pruebas

La cobertura añadida valida:

- migración v2→v3 y conservación de catálogo y rutinas personales;
- propiedad por perfil y rechazo de accesos cruzados;
- orden e instantánea de pasos;
- inicio idempotente y una sola ejecución pendiente;
- completar, omitir y prevención del doble conteo;
- cierre completo, parcial explícito y abandono;
- historial y progreso reales por perfil;
- reanudación desde un repositorio recreado;
- edición y archivado sin alterar el historial;
- confirmación de salida y Atrás del sistema en Compose.

Comandos de verificación de la fase:

    .\gradlew.bat testDebugUnitTest
    .\gradlew.bat assembleDebug
    .\gradlew.bat lintDebug
    .\gradlew.bat assembleDebugAndroidTest
    python -B tools/master_bank/validate_master_bank.py
    python -B -m unittest discover -s tools/master_bank/tests -v

`assembleDebugAndroidTest` compila el APK instrumentado. `adb devices -l` no detectó dispositivos y `emulator -list-avds` no devolvió AVD configurado en el SDK del proyecto (`G:\Android\Sdk`), por lo que las pruebas instrumentadas quedaron compiladas pero no ejecutadas.

## Limitaciones

- No se implementa programación ni recordatorios.
- No se incorpora cliente HTTP, búsqueda o descarga de ARASAAC.
- No se incorporan fotografías personalizadas.
- Logros, retos y gamificación permanecen inactivos.
- Los perfiles continúan en DataStore; no se migran a Room.
- Las rutinas archivadas conservan historial, pero todavía no tienen una pantalla de restauración.
