# Fase 3.1 — Dual module shell

- Rama: `feature/rutinago-dual-module-shell`.
- Baseline: `a57e4dcf4bccad28e9152d74c2d6dd1276d402cb`.

Después de la bienvenida y de contar con un perfil, se muestra el selector
superior de módulos. RutinaGo abre su navegación existente (Hoy, Rutinas,
Crear, Progreso y Configuración). En Configuración, «Cambiar módulo» vuelve
al selector. Al volver a RutinaGo se conserva el destino seleccionado.

Comunicación es una pantalla independiente con el nombre del perfil activo,
el mensaje «Próximamente» y «Volver al selector». El botón Atrás del sistema
también regresa al selector. No muestra la barra de navegación de RutinaGo.
Ambos módulos comparten el perfil actual y las preferencias existentes.

La selección del módulo se conserva con `rememberSaveable` durante la
recreación de la actividad. Un arranque nuevo mantiene la bienvenida y el
selector. No se persiste una preferencia de módulo ni se añaden tablas,
migraciones, banco de comunicación, TTS o integración ARASAAC de Comunicación.
El almacenamiento, catálogo, creación, guardado y ejecución de rutinas no se
modifican. Una ejecución pendiente continúa disponible al regresar a RutinaGo.

## Verificación

- `gradlew.bat testDebugUnitTest`: correcto, 55 pruebas, 0 fallos y 0 errores.
- `gradlew.bat assembleDebug`: correcto.
- `gradlew.bat assembleDebugAndroidTest`: correcto; pruebas compiladas, no ejecutadas.
- `gradlew.bat lintDebug`: correcto, 0 errores, 6 advertencias de versiones
  de dependencias y 1 sugerencia de estado primitivo en código ARASAAC sin modificar.
- APK: `app/build/outputs/apk/debug/app-debug.apk`.

Se adaptaron las pruebas existentes de bienvenida, creación, guardado y
ejecución para seleccionar RutinaGo. `ModuleShellNavigationTest` cubre el
placeholder independiente, el perfil compartido, ambos retornos al selector,
la conservación del destino, la recreación de actividad y las pestañas existentes.

No hay dispositivo Android conectado en este entorno. Las pruebas instrumentadas
deben ejecutarse en un dispositivo o emulador antes de confirmar la ausencia
de regresiones visuales y de interacción en ejecución real.
