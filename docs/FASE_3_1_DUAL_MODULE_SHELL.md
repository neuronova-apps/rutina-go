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

## Verificación inicial de Fase 3.1

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

En esa verificación inicial no había un dispositivo Android conectado y las
pruebas instrumentadas quedaron pendientes de ejecución.

## Ajuste de usabilidad de Fase 3.1 (2026-10-17)

El selector limita el contenido a 480 dp y mantiene el título y el perfil en
la cabecera. Dos espacios flexibles equilibran el bloque de opciones debajo
de la cabecera; al faltar altura, el contenido crece y se puede desplazar.
Los márgenes laterales son de 24 dp y se respetan las áreas seguras del sistema.

Ambos módulos usan el mismo botón rectangular con esquinas de 12 dp, altura
mínima de 72 dp, texto centrado y borde de 1 dp (2 dp en alto contraste).
Las descripciones aparecen debajo de cada botón. Los botones conservan la
semántica de acción, sin estado de selección permanente. Selector y placeholder
aplican explícitamente fondo y texto del tema para Día, Noche y alto contraste.

En Hoy, «Volver al selector» aparece en una fila superior independiente del
título y el perfil. Configuración conserva «Cambiar módulo». Atrás de Android
desde las cinco pestañas principales vuelve al selector y conserva el destino
para la siguiente entrada. Las pantallas internas mantienen su regreso previo;
los controles de Crear y los diálogos de salida del editor siguen teniendo
prioridad. La ejecución conserva su navegación y sus confirmaciones existentes.
No se añade una salida que omita la confirmación de cambios en el editor.

En Comunicación, «Volver al selector» se ubica al principio de la pantalla,
antes del título. Este botón y Atrás de Android ejecutan el mismo retorno
directo al selector. No se introduce contenido de Fase 3.2.

La cobertura nueva incluye el regreso visible desde Hoy, Atrás desde las cinco
pestañas y el regreso desde perfiles antes de abandonar el módulo. La matriz
de accesibilidad cubre Día, Noche, Sistema claro/oscuro y alto contraste, cada
uno con Normal/Grande/Muy grande: 30 combinaciones entre 400 × 760 dp y
320 × 480 dp; en la pantalla pequeña se suma una escala de fuente de sistema
de 1,6. Se comprueban texto sin recorte, botones alcanzables mediante scroll,
roles accesibles y ausencia de selección permanente.

### Verificación del ajuste

- `gradlew.bat testDebugUnitTest`: correcto, 55 pruebas sin fallos ni errores.
- `gradlew.bat assembleDebug`: correcto.
- `gradlew.bat lintDebug`: correcto, sin errores; conserva 6 advertencias de
  versiones y 1 sugerencia de estado primitivo en código ARASAAC sin modificar.
- `gradlew.bat assembleDebugAndroidTest`: correcto.
- Emulador temporal Android 15 / API 35, Pixel 6: 14 casos instrumentados
  verificados en grupos, incluidos los 30 escenarios de accesibilidad. Los
  casos fallidos durante el ajuste de las pruebas se corrigieron y repitieron
  hasta pasar. Se actualizaron referencias antiguas a «Hoy · Lunes», esperas
  de navegación, selección de una rutina concreta y scroll de la fila de texto.
- Comprobación con pulsaciones reales y `KEYCODE_BACK`: abrir selector,
  entrar en RutinaGo y volver tanto con el botón visible como con Atrás;
  entrar en Comunicación y volver por ambas vías. Correcto, sin salir de la app.
- Capturas revisadas en Día, Noche, alto contraste y texto muy grande;
  en pantalla pequeña el contenido continúa mediante scroll. Roles y etiquetas
  accesibles comprobados; no se realizó escucha de la voz de TalkBack.
- Evidencias locales (excluidas de Git): `app/build/qa-manual/`,
  `app/build/qa-captures-final/`, `app/build/qa-final-instrumentation.log`,
  `app/build/qa-regression-final.log` y `app/build/qa-save-final.log`.
- APK actualizado: `app/build/outputs/apk/debug/app-debug.apk`.

No se cambió de rama ni se hizo merge. `.idea/deploymentTargetSelector.xml`
se conservó intacto y se retiró del área de preparación para excluirlo del
commit. No se inició Fase 3.2 ni se añadieron tablas, bancos, pictogramas o TTS.
