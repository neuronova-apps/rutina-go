# Fase 3.4.4 — ComunicaGo: navegación inferior

## Objetivo y baseline

Reorganizar la comunicación existente en cuatro pestañas para reducir el recorrido de Inicio, sin ampliar las funciones de comunicación.

- Repositorio: `https://github.com/neuronova-apps/rutina-go`.
- Origen: `feature/comunicago-personal-vocabulary`.
- Baseline remoto confirmado mediante `git fetch origin`: `2550a23e34048b58d19c0ef311d72a1ccbc0f071`.
- Rama de trabajo: `feature/comunicago-bottom-navigation`, ya existente al iniciar. HEAD y ambas referencias remotas coincidían inicialmente con el baseline. No se recreó la rama.
- Se preservó `.idea/deploymentTargetSelector.xml`, sin agregarlo al índice.
- No se modifica ninguna rama anterior; no se abre PR ni se realiza merge.

## Arquitectura y destinos

`RutinaGoApp` conserva intacta su bifurcación de módulos y su `SaveableStateHolder` con clave `communication:<profileId>`. `CommunicationProfileEntryScreen` conserva la carga combinada de los repositorios existentes de configuración, presentación y vocabulario por ID estable de perfil. No se crean perfiles ni repositorios alternativos.

`CommunicationDestination` define identificadores propios, independientes de `AppDestination`. `CommunicationNavigationState` contiene la pestaña actual y si está abierta la personalización. Un `listSaver` restaura ambos valores tras recrear la Activity. La política `back()` devuelve la pestaña de origen desde el editor y devuelve `null` desde los destinos principales para salir al selector.

`CommunicationHomeScreen` compone el Scaffold del módulo, su barra Material 3 y el contenido seleccionado. El único `CommunicationMessageBuilder` y su único `CommunicationUiState` viven por encima del contenido de las pestañas. El catálogo cargado en ese contenedor se comparte con la administración del vocabulario. La configuración inicial de vocabulario mantiene su pantalla de carga original.

| Identificador | Etiqueta | Contenido |
| --- | --- | --- |
| HOME | Inicio | ComunicaGo, perfil activo, mensaje completo, Borrar último, Limpiar, Deshacer limpieza y acceso rápido a Yo, Quiero, Necesito, Sí, No, Ayuda, respetando palabras ocultas. Sin catálogo completo ni selector de categorías. |
| CATEGORIES | Categorías | Resumen fijo del mensaje, acceso a Inicio, categoría activa, selector Todos + 18 categorías originales, recuento y palabras visibles filtradas. |
| MY_VOCABULARY | Mi vocabulario | Administración existente: perfil, cantidades visibles/ocultas, búsqueda, filtros, categorías, agregar/ocultar/recuperar y restablecer selección, con sus confirmaciones. |
| SETTINGS | Configuración | Nombre del perfil, edad, comunicación oral, acceso al editor existente, Vista guiada/completa, información de accesibilidad compartida y Volver al selector de módulos. |

La barra permanece en las cuatro secciones. Se oculta uniformemente en el asistente inicial y en la personalización secundaria. Los diálogos de administración conservan el comportamiento modal existente.

## Mensaje compartido y categorías

Inicio y Categorías leen exactamente la misma lista de ocurrencias del constructor. Una pulsación agrega inmediatamente el vocablo, mantiene orden y repeticiones y actualiza el resumen. No hay confirmación intermedia. Borrar, limpiar y deshacer reutilizan las operaciones existentes.

El resumen en Categorías ocupa una línea con elipsis visual, semántica de texto completo y anuncio cortés de cambios. El botón **Ver en Inicio** permite consultar la representación completa. El resumen permanece por encima del área desplazable del catálogo para que no desaparezca al buscar palabras.

Cambiar de categoría conserva mensaje y deshacer. Se aplica primero el filtro de categoría del catálogo y después la selección de palabras visibles del perfil. Todos conserva la selección personal y muestra todas sus palabras visibles, sin alterar registros del banco. Las 18 categorías siguen disponibles incluso si alguna no contiene palabras visibles. Un estado vacío enlaza con Mi vocabulario.

## Estado por perfil y preferencias

- Se reutiliza `CommunicationStateCodec` en `rememberSaveable` para categoría, mensaje, repeticiones, identidades de ocurrencia, siguiente identidad y deshacer.
- El `SaveableStateHolder` de las pestañas conserva scroll, búsqueda, filtros de administración, selector de categorías y estados de confirmación que correspondan.
- El contenedor existente del constructor conserva el mensaje al abrir y cerrar personalización.
- La navegación y los borradores del editor sobreviven a recreación de Activity mediante los savers existentes y el nuevo saver de navegación.
- Al salir a módulos se selecciona Inicio para el siguiente ingreso. El mensaje y las demás selecciones de la sesión permanecen asociados al ID del perfil.
- El `SaveableStateHolder` exterior, ya existente, aísla las sesiones de cada perfil. No se usa el nombre para identificar datos.
- La selección visible y la presentación continúan en el DataStore existente por perfil. Editar edad o comunicación oral no reemplaza una preferencia explícita de presentación.
- Cambiar de vista solo cambia tamaños y espaciado de tarjetas: no restringe vocabulario ni modifica mensaje, selección personal o categoría.
- Día/Noche/Sistema, alto contraste y texto Normal/Grande/Muy grande continúan heredados de `RutinaGoTheme`. Configuración explica dónde se editan; no crea controles desconectados ni preferencias paralelas.
- El mensaje es temporal. No se garantiza conservación permanente tras cerrar la app, ni se agrega almacenamiento de mensajes.

## Primer ingreso y Atrás

Se conserva el flujo Selector → ComunicaGo → personalización inicial si falta → selección inicial de vocabulario si falta → Inicio. Los asistentes no se repiten si ya hay datos y no se reescriben registros al leerlos.

Desde cualquiera de las cuatro pestañas, Atrás Android vuelve directamente al selector de módulos. Desde la personalización secundaria, Atrás o el botón de regreso vuelve a la pestaña de origen; guardar también regresa allí. Los diálogos de ocultación y restablecimiento conservan su cancelación y confirmaciones. La opción explícita de salir se encuentra en Configuración.

## Accesibilidad

- Barra Material 3, con iconos vectoriales locales de casa, cuadrícula, libro y engranaje, etiquetas siempre visibles y estado semántico seleccionado mediante `NavigationBarItem`.
- El indicador de selección aporta una forma visible y no depende únicamente de un cambio de color.
- Se mide el texto con la tipografía y escala activas. Cuando cuatro columnas no permiten las etiquetas, la barra usa dos filas de dos destinos. La altura se calcula a partir del texto medido, sin reducir el tamaño configurado.
- Superficies táctiles de al menos 48 dp; borde adicional en alto contraste.
- El Scaffold reserva el espacio de la barra y consume los insets; la barra respeta el área de navegación del sistema y el contenido conserva safe drawing padding.
- El contenedor aplica `imePadding` y cada cambio de pestaña libera el foco de búsqueda. Mi vocabulario conserva su área desplazable y su tratamiento del teclado.
- Tarjetas adaptativas y contenido desplazable reutilizan el diseño guiado/completo existente.

## Verificaciones

Ejecutadas con el JBR de Android Studio:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest --console=plain
```

Los cuatro objetivos Gradle finalizaron correctamente. Resultado final unitario: **132 pruebas, 0 fallos y 0 errores** (122 existentes + 10 nuevas). La suite nueva es `CommunicationBottomNavigationTest`.

| Requisito de prueba unitaria | Cobertura |
| --- | --- |
| Cuatro destinos, Inicio inicial y cambio entre pestañas | CommunicationBottomNavigationTest: etiquetas exactas y todas las transiciones entre destinos. |
| Mensaje, orden y repeticiones conservados | CommunicationBottomNavigationTest y CommunicationMessageBuilderTest: snapshots y ocurrencias. |
| Categoría activa y filtro personal conservados | CommunicationBottomNavigationTest, CommunicationMessageBuilderTest y CommunicationPersonalVocabularyTest. |
| Acceso rápido respeta ocultas | CommunicationBottomNavigationTest y CommunicationPersonalVocabularyTest. |
| Mi vocabulario mantiene selección | CommunicationPersonalVocabularyTest: DataStore, reapertura y edición aislada. |
| Configuración recupera datos del perfil | CommunicationProfileSetupTest: persistencia, lectura, edición y aislamiento por ID. |
| Vista conserva mensaje | CommunicationPresentationTest: ambas vistas mantienen mensaje, categoría y deshacer. |
| Cambio de perfil aislado | CommunicationBottomNavigationTest, CommunicationPersonalVocabularyTest y CommunicationProfileSetupTest. |
| Atrás principal y secundario | CommunicationBottomNavigationTest: salida en las cuatro pestañas y retorno al origen del editor. |
| Banco maestro intacto | CommunicationCatalogTest y MasterBankArtifactsTest: fuente, registros, metadatos, orden y recuentos. |
| Sin llamadas de red adicionales | CommunicationMessageBuilderTest: frontera de fuentes sin clientes de red/voz/Room y operaciones que usan exclusivamente el catálogo local. |

También se prepararon y compilaron estas pruebas Compose nuevas:

- `CommunicationBottomNavigationUiTest` (4 casos): entrada al módulo, cuatro destinos, Atrás desde cada pestaña, retorno a Inicio al reingresar, independencia de la barra RutinaGo y navegación por sus cinco destinos; mensaje compacto/completo, repeticiones, categoría y recreación; ocultar/recuperar y búsqueda conservada; presentación; editor, borrador recreado y retorno a Configuración; segundo perfil con vocabulario, mensaje y presentación independientes.
- `CommunicationBottomNavigationAccessibilityTest` (1 caso con matriz): Día, Noche, Sistema claro/oscuro y alto contraste × tres tamaños de texto × 320×480/400×760 dp, con escala adicional de sistema 1.6 en pantalla pequeña. Comprueba etiquetas, límites, áreas táctiles, rol Tab, estado seleccionado, separación de contenido/barra y mensajes largos accesibles.
- Se adaptaron las pruebas de fases anteriores para navegar a los controles en sus nuevas pestañas, conservando sus verificaciones del constructor, preferencias, banco completo y configuración inicial.

**Instrumentación no ejecutada**: `adb devices -l` no enumeró dispositivos y `emulator -list-avds` no enumeró AVD. Compilar `assembleDebugAndroidTest` no equivale a ejecutar pruebas. Quedan pendientes su ejecución en Android y la validación manual de TalkBack, navegación por gestos y teclado real.

Para ejecutar después, conectar un dispositivo y usar `gradlew.bat connectedDebugAndroidTest`. Las clases específicas pueden seleccionarse con el argumento `android.testInstrumentationRunnerArguments.class`.

Lint: **0 errores, 6 advertencias y 1 sugerencia**. Las seis advertencias corresponden a versiones en `gradle/libs.versions.toml` (AGP, core-ktx, Compose BOM, Kotlin Compose, Coil Compose y Coil OkHttp); la sugerencia es `mutableLongStateOf` en `ArasaacSearchDialog.kt:81`. Ambos archivos permanecen intactos respecto del baseline. También aparece la advertencia del JBR sobre `sun.misc.Unsafe` en el protobuf de DataStore. No se cambiaron dependencias para resolver avisos fuera de alcance.

## Banco, exclusiones y límites

El catálogo y el snapshot de origen no se modifican: **132 vocablos, 18 categorías, 111 VALIDADO, 21 PENDIENTE_ARASAAC, 10 SOLO_TEXTO, 11 PICTOGRAMA_PENDIENTE**. Se mantienen IDs, textos, órdenes, categorías, estados y metadatos. No se accede a Google Sheets.

El código funcional de RutinaGo, incluida su barra, gestión de perfiles, rutinas, ejecución, progreso, historial, ARASAAC y modo Simple/Avanzado permanece intacto. Sus pruebas de integración solo cambian los pasos correspondientes a ComunicaGo.

No se incorpora TTS, Hablar, repetición de voz, pictogramas o descargas ARASAAC, favoritos, frases nuevas, Room nuevo, sincronización, estadísticas, categorías ni criterios clínicos.

Limitaciones conocidas: la barra puede ocupar dos filas con texto grande; el mensaje compacto usa elipsis; el mensaje no es persistente de forma permanente; las verificaciones de interfaz están compiladas y pendientes de ejecución en dispositivo.

## Archivos del cambio

Producción (rutas bajo `app/src/main/java/com/neuronovaapps/rutinago/`):

- `domain/communication/CommunicationDestination.kt` (nuevo: destinos y estado de navegación).
- `ui/screens/communication/CommunicationNavigationSaver.kt` (nuevo).
- `ui/screens/communication/CommunicationBottomBar.kt` (nuevo).
- `ui/screens/communication/CommunicationSettingsScreen.kt` (nuevo).
- `ui/screens/communication/CommunicationHomeScreen.kt` (contenedor y división Inicio/Categorías).
- `ui/screens/communication/CommunicationProfileEntryScreen.kt` (integración de navegación, configuración y guardado reutilizado).
- `ui/screens/communication/CommunicationVocabularyScreen.kt` (parámetro para omitir el botón Atrás al integrarse como pestaña).

Pruebas nuevas (rutas bajo `app/src/`):

- `test/java/com/neuronovaapps/rutinago/CommunicationBottomNavigationTest.kt`.
- `androidTest/java/com/neuronovaapps/rutinago/CommunicationBottomNavigationUiTest.kt`.
- `androidTest/java/com/neuronovaapps/rutinago/CommunicationBottomNavigationAccessibilityTest.kt`.

Pruebas adaptadas en `app/src/androidTest/java/com/neuronovaapps/rutinago/`:

- `CommunicationFoundationTest.kt`.
- `CommunicationMessageBuilderUiTest.kt`.
- `CommunicationPersonalVocabularyUiTest.kt`.
- `CommunicationPresentationUiTest.kt`.
- `CommunicationProfileSetupUiTest.kt`.
- `ModuleSelectorAccessibilityTest.kt`.
- `ModuleShellNavigationTest.kt`.

Documentación: este archivo, `docs/FASE_3_4_4_COMUNICAGO_BOTTOM_NAVIGATION.md`.

## APK y publicación

APK de validación:

`G:\NeuroNovaApps\AndroidStudioProjects\RutinaGo\app\build\outputs\apk\debug\app-debug.apk`

Commit previsto: `feat: add ComunicaGo bottom navigation`. Publicar únicamente en `origin/feature/comunicago-bottom-navigation`, comprobar coincidencia de HEAD y referencia remota tras `git fetch origin`, y reportar el hash final en la entrega. Sin merge ni PR.
