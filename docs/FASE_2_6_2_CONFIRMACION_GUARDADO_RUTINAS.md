# Fase 2.6.2: confirmación de guardado de rutinas

Rama: `feature/rutinago-master-bank-arasaac`

## Problema

El guardado personal ya terminaba en una transacción Room, pero la confirmación quedaba al final del contenido desplazable del editor. Después de pulsar **Guardar rutina** era fácil no verla y pensar que la acción no había funcionado.

## Solución y estados

El editor usa los estados `IDLE`, `SAVING`, `SAVED` y `ERROR`. Al pulsar Guardar, `SAVING` deshabilita la acción y un guardia atómico impide solicitudes simultáneas. El botón muestra **Guardando…**. La respuesta de `onSaveRoutine` llega después de que `RoomUserRoutineRepository.save` termina su transacción `withTransaction`. Solo entonces el editor pasa a `SAVED` y emite una vez el resultado `UserRoutine` al nivel de navegación. La confirmación no se guarda como estado restaurable, así que recomponer, rotar o cambiar el tema no la vuelve a emitir.

Si Room lanza una excepción, el editor pasa a `ERROR`, presenta **No se pudo guardar la rutina** en un Snackbar y conserva el detalle cerca del encabezado. El nombre, la selección, el orden y el ID del borrador permanecen en el editor para reintentar. El error nunca navega ni presenta un mensaje de éxito.

## Flujos de creación y edición

- **Rutinas → Explorar plantillas → Usar como plantilla:** la copia conserva `sourceTemplateId`, se guarda para el perfil activo y muestra **Rutina guardada correctamente**.
- **Crear → Usar una plantilla:** utiliza el mismo editor, guardado y confirmación.
- **Crear → Crear desde cero:** utiliza el mismo editor, guardado y confirmación, sin plantilla de origen.
- **Editar rutina existente:** **Guardar cambios** envía el ID original a Room, actualiza el registro y muestra **Cambios guardados correctamente**.

Después de un éxito, la app abre **Mis rutinas** y muestra el Snackbar desde el `Scaffold` principal. El `SnackbarHost` queda encima de la barra de navegación inferior. La rutina devuelta por Room se presenta inmediatamente en la lista mientras llega la emisión actualizada del `Flow`; cuando llega el mismo objeto, se elimina esa presentación transitoria. La ruta de edición reemplaza visualmente la versión anterior por la guardada y mantiene su ID.

## Accesibilidad

Los mensajes describen la operación en texto y no dependen del color. Los `SnackbarHost` tienen una región viva cortés para el anuncio de TalkBack. Los colores del Snackbar proceden de Material 3 y del tema activo; el contenedor se adapta a día, noche, sistema y alto contraste. El tamaño del texto sigue los modos Normal, Grande y Muy grande de la app. El botón expone **Guardando rutina** y **Guardado en progreso** durante la operación. La semántica de la región viva está cubierta por una prueba Compose; la comprobación auditiva real con TalkBack y la inspección visual de cada variante requieren un dispositivo.

## Pruebas

Las pruebas Compose cubren guardado desde plantilla y desde cero, espera de la respuesta de persistencia, pulsaciones rápidas, error sin éxito falso, conservación del borrador, reintento, ID estable en edición, mensaje distinto para edición, navegación a **Mis rutinas**, región viva y aparición inmediata de la rutina guardada. Las pruebas Room existentes comprueban la transacción, actualización sin duplicados, orden de pasos y separación por perfil.

Validación ejecutada: `testDebugUnitTest`, `assembleDebug`, `lintDebug` y `assembleDebugAndroidTest` terminaron correctamente. Lint informó 0 errores y 4 avisos de versiones de dependencias preexistentes. `python -B tools/master_bank/validate_master_bank.py` confirmó el banco generado. `adb devices` no encontró dispositivos y `emulator -list-avds` no encontró AVD; las pruebas instrumentadas quedaron compiladas, sin ejecución en este entorno.
