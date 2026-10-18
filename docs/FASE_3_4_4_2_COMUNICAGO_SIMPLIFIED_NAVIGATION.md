# Fase 3.4.4.2 — Navegación simplificada de ComunicaGo

## Base y alcance

Rama exclusiva: `feature/comunicago-simplified-navigation`, previamente creada y publicada desde `feature/comunicago-personal-home`. Antes de editar se verificaron estado, rama, HEAD, `git fetch origin` y referencia remota: todos los commits de partida coinciden con `5a2411bdec29b88a801c0a7995f86de66c818ae9`. El único archivo local ajeno era `.idea/deploymentTargetSelector.xml`, que se conserva intacto y sin seguimiento.

La duplicación entre Categorías y Mi vocabulario mezclaba comunicación con administración. Se reutilizan las pantallas, tarjetas, filtros, controles y repositorios actuales para separar esas tareas.

## Tres destinos principales

- **Inicio**: perfil activo, mensaje, Borrar último, Limpiar, Deshacer limpieza y selección personal de palabras. Dos acciones independientes y prioritarias: Buscar más palabras y Organizar palabras de Inicio.
- **Mi vocabulario**: administración de disponibilidad y asignación a Inicio. Entrada inmediata al organizador existente, sección Todas las palabras y filtros Todas, Visibles y Ocultas. Búsqueda y las 18 categorías internas permiten localizar palabras y añadirlas/quitarles la asignación de Inicio, ocultarlas o recuperarlas. Restablecer conserva su confirmación y alcance por perfil.
- **Configuración**: preferencias existentes de edad, comunicación oral, presentación y salida al selector.

La barra Material 3 conserva iconos, etiquetas siempre visibles, selección, semántica de pestaña, objetivos táctiles y zona de gestos. Su cálculo de anchura usa tres destinos; conserva dos filas cuando el tamaño del texto lo necesita.

## Explorador de comunicación

Buscar más palabras abre una pantalla secundaria de Inicio, sin una cuarta pestaña. Reutiliza la cuadrícula y categorías actuales, incluye Todos y búsqueda de palabra/categoría, muestra solo el vocabulario disponible y mantiene un mensaje compacto con retorno visible a Inicio. Se pueden elegir varias palabras, incluidas repeticiones, sin abandonar el explorador.

Tocar tarjetas agrega palabras al mensaje. En el explorador no aparecen asignaciones a Inicio ni controles de ocultación. Esas acciones están en Mi vocabulario y en el organizador reutilizado. Quitar de Inicio mantiene la disponibilidad; ocultar conserva la asignación registrada y permite recuperarla. Una palabra oculta no se puede añadir a Inicio hasta recuperarla; una asignación oculta sí se puede quitar.

## Estado, persistencia y Atrás

Se mantiene un único `CommunicationMessageBuilder` y estado temporal por perfil sobre los contenidos guardados. El explorador tiene su propia clave del `SaveableStateHolder`; categoría, búsqueda y desplazamiento se restauran sin crear mensajes separados. Orden, repeticiones, limpieza/deshacer y mensaje sobreviven a navegación y recreación.

La selección explícitamente vacía de Inicio sigue vacía. No hay cambios en repositorios, preferencias globales, claves DataStore, esquema, Room ni IDs. Se mantienen los asistentes iniciales cuando corresponden y se omiten para perfiles configurados. El guardado temporal de la antigua pestaña `CATEGORIES` se interpreta como el explorador secundario de Inicio.

| Origen / pantalla | Atrás |
| --- | --- |
| Inicio → Buscar más palabras | Inicio |
| Inicio → Organizar palabras | Inicio |
| Mi vocabulario → Organizar palabras | Mi vocabulario |
| Configuración → Personalizar perfil | Configuración |
| Cualquiera de las tres pestañas, sin secundaria | Selector de módulos |

Cambiar de pestaña cierra la pantalla secundaria. El retorno visible del explorador y el botón Android no borran el mensaje.

## Accesibilidad y regresiones

Se conservan Vista guiada y Vista completa, tema Día/Noche/Sistema, alto contraste y los tres tamaños de texto. Las matrices instrumentadas comprueban etiquetas sin desbordamiento, controles de al menos 48 dp, semántica, mensaje compacto y separación entre comunicación y administración, incluyendo 320 × 480 dp y escala de sistema 1.6. Las pruebas de la app real verifican los accesos al entrar con texto Muy grande y alto contraste.

El banco y sus metadatos permanecen intactos: 132 vocablos, 18 categorías, 111 VALIDADO, 21 PENDIENTE_ARASAAC, 10 SOLO_TEXTO y 11 PICTOGRAMA_PENDIENTE. No se modifica Google Sheets ni la barra o funcionalidades de RutinaGo. No se incorporan TTS, Hablar, pictogramas, descargas ARASAAC ni llamadas de red.

## Validación

Los cuatro objetivos Gradle finalizaron correctamente: `testDebugUnitTest`, `assembleDebug`, `lintDebug` y `assembleDebugAndroidTest`. Resultado final unitario: **157 pruebas, 0 fallos, 0 errores**. Lint: **0 errores, 6 advertencias y 1 sugerencia heredadas**, relativas a versiones en `gradle/libs.versions.toml` y `mutableLongStateOf` en `ArasaacSearchDialog.kt`, sin cambios en esos archivos. Registro final: `app/build/reports/phase3442-build-accessibility-fix.log`. Las compilaciones no se cuentan como ejecución instrumentada.

Comparación de fallos: una aserción unitaria antigua seguía esperando cuatro destinos; se actualizó a tres sin retirar las comprobaciones de retorno. La primera ejecución Android de los 18 recorridos reales pasó (`OK (18 tests)`, 150.959 s). Las dos matrices detectaron `hasVisualOverflow` en etiquetas antes de alcanzar los recorridos nuevos; ambos fallos se reprodujeron contra la APK exacta del baseline, SHA-256 `208BEC39424156C45484F4BC155799FF2AF5F663761BC98DD58C882129383569`, conservando datos mediante `install -r`.

Se añadieron medidas a los mensajes de fallo, sin debilitar aserciones: Inicio informaba tamaño 71×34 frente a párrafo de 300×34, y Organizar palabras de Inicio 421×50 frente a párrafo de 720×50. El ajuste local hace que las etiquetas de navegación, accesos de Inicio y controles de asignación ocupen la anchura disponible y se centren, sin reducir tamaños de texto ni cambiar el tema compartido. Registros de comparación: `app/build/reports/phase3442-baseline-accessibility.txt` y `phase3442-accessibility-diagnostics.txt`.

Las matrices finales pasaron: **OK (2 tests)**, 252.833 s. Se completaron las 30 combinaciones de barra inferior y las 60 de palabras/asignación: cinco apariencias × tres tamaños de texto × dos tamaños de contenido, y ambas vistas en la segunda matriz. El viewport del emulador de 480×853 dp contiene los escenarios de 400×760 y 320×480 dp sin recortar artificialmente el escenario grande. Registro: `app/build/reports/phase3442-accessibility-final.txt`. Se conservaron las comprobaciones `hasVisualOverflow`, altura/anchura táctil, selección, semántica, mensaje y separación entre áreas.

Se repitieron los 18 recorridos funcionales sobre la APK final a resolución física equivalente a **320×480 dp**: **OK (18 tests)**, 74.925 s, registro `app/build/reports/phase3442-navigation-final.txt`. Incluyen entrada de perfil configurado, los tres destinos, exploración de las 18 categorías y Todos, búsqueda, repeticiones, recreación, retorno visible y Android, disponibilidad/asignación independientes, añadir/quitar Agua, ocultar/recuperar, selección vacía, aislamiento entre perfiles, ambas vistas, edición del perfil y regresión de navegación de RutinaGo. Total final: **20 pruebas instrumentadas aprobadas**, sin fallos pendientes en estas tandas. Capturas y árboles semánticos: `app/build/reports/phase3442-final-ui-evidence/`.

Durante la revisión visual se detectó una captura adelantada al render, aunque la aserción posterior pasaba. Se sincronizó la captura con el hilo UI y la presentación del siguiente frame, y se comprobó el control visible antes de capturar el caso de alto contraste. Ese caso se repitió adicionalmente: **OK (1 test)**, 9.283 s, `app/build/reports/phase3442-evidence-contrast-final.txt`. La imagen sincronizada fue inspeccionada visualmente; no se cambió la APK de producción durante este ajuste de evidencia.

Las pruebas unitarias de navegación cubren tres destinos, explorador secundario, retornos y compatibilidad de estados temporales anteriores. Las pruebas existentes de mensajes, vocabulario y selección de Inicio conservan cobertura de persistencia, aislamiento, selección vacía, banco y ausencia de red. Se actualizan los recorridos de las pruebas UI para administrar desde Mi vocabulario y comunicar desde el explorador, manteniendo sus aserciones de mensaje y persistencia.

Limitaciones: la ejecución automatizada Android no sustituye la validación manual con TalkBack y gestos físicos en el celular del usuario. No se desinstala ni se limpia la aplicación; las instalaciones de validación se realizan con `adb -s emulator-5582 install -r`, solo en el emulador temporal.

## Entrega

Archivos de esta fase:

- Producción: `domain/communication/CommunicationDestination.kt`; `ui/screens/communication/CommunicationBottomBar.kt`, `CommunicationHomeScreen.kt`, `CommunicationHomeVocabularyScreen.kt`, `CommunicationNavigationSaver.kt`, `CommunicationProfileEntryScreen.kt` y `CommunicationVocabularyScreen.kt`.
- Unitarias: `CommunicationBottomNavigationTest.kt` y `CommunicationHomeVocabularyTest.kt`.
- Android: `CommunicationBottomNavigationAccessibilityTest.kt`, `CommunicationBottomNavigationUiTest.kt`, `CommunicationHomeExposureUiTest.kt`, `CommunicationPersonalHomeAccessibilityTest.kt`, `CommunicationPersonalHomeUiTest.kt` y `ModuleShellNavigationTest.kt` (adaptación del helper compartido al explorador).
- Documentación: este archivo nuevo. Todas las rutas Kotlin parten de `app/src/{main,test,androidTest}/java/com/neuronovaapps/rutinago/`, según corresponda.

Commit recomendado: `refactor: simplify ComunicaGo navigation and vocabulary flow`. Publicación únicamente en `feature/comunicago-simplified-navigation`, sin merge ni PR y comprobando igualdad entre HEAD local y remoto.

APK estándar: `G:\NeuroNovaApps\AndroidStudioProjects\RutinaGo\app\build\outputs\apk\debug\app-debug.apk`.

Copia identificable: `G:\NeuroNovaApps\AndroidStudioProjects\RutinaGo\app\build\outputs\apk\debug\RutinaGo-ComunicaGo-simplified-navigation.apk`.

Se conserva `applicationId` y certificado de depuración para actualizar sobre la instalación existente sin borrar perfiles ni preferencias.

Instalación en el celular: abrir la copia identificable y elegir actualizar sobre la app existente, sin desinstalar ni borrar datos. La instalación y comprobación manual en el celular siguen pendientes.

- `applicationId`: `com.neuronovaapps.rutinago`, `versionCode 1`, `versionName 1.0`.
- Generación: 18/10/2026, 17:44:39 (Lima).
- SHA-256: `FAB244B54F5F23D469ADD41860B9D6A252EF6E39EDAD1E5A4375CE7E30A3B512`.
- Certificado SHA-256: `ff36e3c58c80a5d345625ee32a1c02910e27aed95e3503256733475c61131846`, igual al baseline y verificado con `apksigner`.
- La APK extraída del emulador coincide exactamente con la copia de entrega.
