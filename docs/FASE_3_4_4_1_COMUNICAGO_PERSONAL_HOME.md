# Fase 3.4.4.1 — Inicio personalizado de ComunicaGo

## Objetivo y punto de partida

Inicio es el espacio habitual para comunicarse con las palabras elegidas por cada perfil. Categorías permite explorar, usar palabras y asignarlas a Inicio. Mi vocabulario administra la disponibilidad y ofrece la organización de Inicio. Se mantienen cuatro pestañas y las preferencias existentes de Configuración.

- Rama exclusiva: `feature/comunicago-personal-home`, existente y publicada al comenzar.
- Origen: `feature/comunicago-bottom-navigation`.
- Baseline confirmado con `git fetch origin`: `bb71b09ecc31d8fee2a593c46d71a11330d1eb7a`.
- HEAD y ambas referencias remotas coincidían inicialmente. No se recreó ninguna rama.
- `.idea/deploymentTargetSelector.xml` se conserva fuera del commit.

## Dos selecciones independientes

| Conjunto | Función | Persistencia |
| --- | --- | --- |
| Vocabulario disponible | Palabras habilitadas para el perfil y accesibles en Categorías. | Clave existente `vocabulary_visible_v1:<profileId>`. |
| Palabras asignadas a Inicio | Accesos elegidos entre las palabras disponibles. | Nueva clave `home_vocabulary_v1:<profileId>` en el mismo DataStore de ComunicaGo. |

La proyección visible de Inicio es la intersección entre asignaciones, vocabulario disponible y registros reales del catálogo. Se guardan únicamente IDs estables, sin copiar objetos, crear perfiles, instanciar repositorios alternativos, agregar Room o enviar información a servidores.

`CommunicationHomeVocabulary` resuelve la propuesta inicial y la proyección visible. `CommunicationHomeVocabularyRepository` agrega observación y cambios atómicos de pertenencia a la implementación existente de `CommunicationProfileSetupManager`; `UserPreferencesRepository` delega estas dos operaciones. El bloqueo de mutaciones existente protege guardados y eliminación de perfiles. Cada cambio también usa una transacción `DataStore.edit`, evitando sobrescribir otras asignaciones con una selección capturada antes.

Las referencias desconocidas no se muestran. Las escrituras validan el vocablo contra el catálogo y conservan únicamente IDs conocidos. Agregar una palabra exige que esté disponible; quitar una asignación también es posible si esa palabra está temporalmente oculta.

## Compatibilidad con perfiles anteriores

Se distinguen tres estados:

1. Clave ausente: propuesta inicial de las seis palabras esenciales que estén disponibles: Yo `VOC_001`, Quiero `VOC_006`, Necesito `VOC_007`, Sí `VOC_063`, No `VOC_053` y Ayuda `VOC_014`.
2. Conjunto explícitamente vacío: Inicio permanece vacío, sin restaurar valores predeterminados.
3. Conjunto personalizado: se respeta la selección guardada y se filtra únicamente su representación visible.

Leer la propuesta inicial no crea registros ni migra destructivamente datos anteriores. El primer cambio materializa la propuesta disponible más la modificación solicitada. Las palabras que ya estaban ocultas no se incorporan a esa primera selección. No hay un asistente obligatorio nuevo.

La propuesta inicial mantiene el orden histórico de las seis palabras. Las selecciones personalizadas se presentan en el orden editorial del catálogo. Esta fase no incluye un editor de orden. No existen cantidades obligatorias ni reglas por edad o comunicación oral; ambas vistas muestran la misma selección. El banco no se modifica al ordenar la presentación.

Edad, comunicación oral, vista guiada/completa, vocabulario disponible y preferencias globales conservan sus registros. La eliminación de un perfil retira también su clave de Inicio, preservando las de otros perfiles.

## Inicio: uso del comunicador

Inicio conserva título ComunicaGo, perfil, mensaje completo, Borrar último, Limpiar y Deshacer limpieza. Muestra únicamente sus palabras elegidas, sin introducir el catálogo completo ni las categorías en la pantalla inicial.

Cada tarjeta tiene una sola acción: **usar la palabra para añadirla al mensaje**. No contiene controles de edición pequeños. El botón **Organizar palabras de Inicio** abre la administración correspondiente.

Si no hay palabras visibles se ofrece **Agregar palabras a Inicio**. Una selección vacía abre la lista de disponibles para facilitar nuevas asignaciones. Si no hay vocabulario disponible, la organización ofrece recuperar palabras en Mi vocabulario. El comunicador y sus cuatro pestañas permanecen accesibles.

## Categorías: usar y asignar

Se mantienen las 18 categorías originales y Todos, el filtro de disponibilidad y el mensaje compacto compartido. La explicación contextual es:

> Toca una palabra para comunicarla. Usa Añadir a Inicio para tenerla siempre a mano.

Cada resultado contiene dos superficies independientes:

- Tarjeta principal: añade una ocurrencia al mensaje.
- Botón **Añadir a Inicio**: guarda la asignación, sin modificar el mensaje ni cambiar de pestaña.

Una palabra asignada muestra **En Inicio** y no ofrece una segunda incorporación. La operación del repositorio también es idempotente. Los avisos de guardado y error se muestran fuera de la lista desplazable; el error permite reintentar sin aplicar una selección optimista que no esté guardada.

## Mi vocabulario y organización de Inicio

Mi vocabulario mantiene búsqueda, categorías, filtros, agregar/ocultar/recuperar y restablecer disponibilidad, incluidas las confirmaciones de palabras importantes. Se agrega la sección diferenciada **Palabras de Inicio**, con recuento y acceso a **Organizar palabras de Inicio**. El texto permite participar al propio usuario con el apoyo que necesite.

La organización ofrece búsqueda por palabra, categoría o ID; filtros **Asignadas a Inicio** y **Todas las disponibles**; cantidades visibles y temporalmente ocultas; y botones **Añadir a Inicio** / **Quitar de Inicio**. No recibe ningún callback del constructor: una pulsación de administración no puede añadir palabras al mensaje.

Quitar de Inicio retira solo la asignación. La palabra sigue disponible en Categorías y cualquier mensaje previo permanece intacto. Ocultar en Mi vocabulario modifica la disponibilidad, una operación diferente. La organización explica esa diferencia y permite consultar también las asignaciones temporalmente ocultas.

## Política de ocultación y recuperación

Se adopta la política preferida de la solicitud:

- Ocultar una palabra disponible la retira de Inicio y Categorías.
- Su ID asignado a Inicio permanece guardado.
- Recuperar disponibilidad hace que vuelva a Inicio si conserva esa asignación.
- Quitar expresamente la asignación mientras está oculta evita que reaparezca en Inicio al recuperarla.
- Restablecer el vocabulario disponible no borra las asignaciones de Inicio; su intersección visible cambia según las palabras recuperadas.
- Ninguna de estas acciones modifica mensajes ya construidos o el banco maestro.

## Estado del mensaje y navegación

El constructor y `CommunicationStateCodec` no se reescriben. Se conserva una única instancia del constructor y un solo estado temporal, compartido entre Inicio y Categorías y situado por encima de las pantallas de administración. Continúan el orden libre, las repeticiones, las categorías, borrar, limpiar, deshacer y restaurar temporalmente.

`CommunicationProfileEntryScreen` combina ahora la observación de Inicio con los flujos existentes por ID de perfil. Se conserva el `SaveableStateHolder` exterior por perfil y el de contenidos internos. No se garantiza persistencia permanente del mensaje después del cierre de la app.

`CommunicationNavigationState` agrega el origen de la organización; no introduce una quinta pestaña. Desde Inicio, Atrás o el botón de regreso de la organización vuelve a Inicio con el mensaje intacto. Si se abrió desde Mi vocabulario, regresa allí. La barra se oculta en esta pantalla secundaria, siguiendo el criterio de personalización existente. El saver conserva el origen al recrear la Activity y acepta también snapshots anteriores con solo dos campos.

Desde cualquiera de las cuatro pestañas principales, Atrás sigue saliendo al selector de módulos. Añadir a Inicio desde Categorías no navega. Configuración, el editor de perfil y la barra de RutinaGo conservan su funcionamiento.

## Accesibilidad

Se reutilizan tema Día/Noche/Sistema, alto contraste y texto Normal/Grande/Muy grande. Guiada conserva tarjetas amplias y mayor espaciado; Completa utiliza mayor densidad sin cambiar la selección.

Las acciones de uso y asignación tienen nombres semánticos diferentes, incluyendo el vocablo: por ejemplo, **Usar palabra: Agua**, **Añadir a Inicio: Agua** y **Quitar de Inicio: Agua**. Los controles de administración tienen etiquetas visibles y al menos 48 dp, sin superficies de clic anidadas. El estado En Inicio se expresa por texto, no solo color.

Se conservan tarjetas adaptativas, listas desplazables, safe drawing padding, tratamiento de teclado e insets y la barra responsiva anterior. El organizador utiliza la misma búsqueda existente. Las tarjetas siguen referenciando los objetos y metadatos del banco, preparados para incorporar apoyos visuales posteriormente; no se descargan imágenes en esta fase.

## Pruebas y comprobaciones

Ejecutar con el JBR existente:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest --console=plain
```

Resultado unitario: **154 pruebas, 0 fallos y 0 errores**: 132 anteriores y 22 nuevas en `CommunicationHomeVocabularyTest`.

Las nuevas pruebas cubren propuesta inicial y palabras ocultas; añadir/quitar; duplicados; persistencia y reapertura del DataStore; selección explícitamente vacía; aislamiento entre perfiles; ocultación/recuperación conservando IDs; retirada durante ocultación; disponibilidad en Categorías; rechazo de palabras no disponibles o desconocidas; compatibilidad de datos previos; cambios de edad/habla/vista; independencia del mensaje y sus repeticiones/deshacer; eliminación de perfiles; concurrencia; referencias corruptas; selección sin límite artificial; banco de 132/18 intacto; y navegación de regreso.

La prueba existente `CommunicationMessageBuilderTest.communicationHasNoNetworkSpeechOrDatabaseEntryPoints` inspecciona también las nuevas fuentes de ComunicaGo y sigue verificando la ausencia de clientes de red, voz y Room. `CommunicationCatalogTest` conserva sus verificaciones de fuente, IDs, estados, orden, metadatos y recuentos.

Se preparan y compilan cuatro casos nuevos en `CommunicationPersonalHomeUiTest`: ciclo usar/asignar/quitar sin mensajes accidentales; ocultar/recuperar; selección vacía y recreación; dos perfiles, ambas vistas, navegación y regresión de RutinaGo. `CommunicationPersonalHomeAccessibilityTest` agrega una matriz de cinco apariencias × tres tamaños de texto × dos tamaños de pantalla × ambas vistas, incluyendo texto de sistema 1.6 y 320×480 dp. Comprueba controles distintos, etiquetas, áreas táctiles, textos sin desbordamiento y mensaje preservado durante administración.

En la publicación inicial `c59a87f`, las pruebas instrumentadas solo se habían compilado: no había dispositivos conectados ni AVD configurados. El diagnóstico posterior crea un emulador temporal con la imagen Android ya instalada y ejecuta las pruebas de la app real descritas al final de este documento. La validación en el celular del usuario sigue pendiente.

Lint conserva **0 errores, 6 advertencias y 1 sugerencia**: versiones de AGP, core-ktx, Compose BOM, Kotlin Compose y las dos dependencias Coil en `gradle/libs.versions.toml`; y `mutableLongStateOf` en `ArasaacSearchDialog.kt:81`. Esos archivos no se modifican. El JBR también informa el aviso heredado de `sun.misc.Unsafe` usado por protobuf de DataStore.

## Archivos y límites

Producción bajo `app/src/main/java/com/neuronovaapps/rutinago/`:

- Nuevo `domain/communication/CommunicationHomeVocabulary.kt`.
- Nuevo `ui/screens/communication/CommunicationHomeVocabularyScreen.kt`.
- `data/communication/CommunicationSetupPreferences.kt`.
- `data/preferences/UserPreferencesRepository.kt` (delegación mínima).
- `domain/communication/CommunicationDestination.kt`.
- `ui/screens/communication/CommunicationHomeScreen.kt`.
- `ui/screens/communication/CommunicationNavigationSaver.kt`.
- `ui/screens/communication/CommunicationProfileEntryScreen.kt`.
- `ui/screens/communication/CommunicationVocabularyScreen.kt`.
- `ui/screens/communication/components/CommunicationComponents.kt`.

Pruebas nuevas bajo `app/src/`: `test/java/com/neuronovaapps/rutinago/CommunicationHomeVocabularyTest.kt`, `androidTest/java/com/neuronovaapps/rutinago/CommunicationPersonalHomeUiTest.kt` y `androidTest/java/com/neuronovaapps/rutinago/CommunicationPersonalHomeAccessibilityTest.kt`. Documentación: este archivo.

El banco offline y su fuente permanecen intactos: **132 vocablos, 18 categorías, 111 VALIDADO, 21 PENDIENTE_ARASAAC, 10 SOLO_TEXTO y 11 PICTOGRAMA_PENDIENTE**. No se altera Google Sheets. No se incorporan TTS, voz, pictogramas o descargas ARASAAC, categorías, Room ni sincronización.

RutinaGo no recibe cambios funcionales; la única ampliación en su repositorio compartido son las delegaciones explícitas de Inicio de ComunicaGo y la limpieza correspondiente al eliminar un perfil. El constructor, selector de módulos, barra RutinaGo, rutinas, ejecución, historial, progreso y preferencias globales no se modifican.

Límites: no hay reordenación manual de Inicio ni historial nuevo de selecciones; las asignaciones ocultas se recuperan mediante la disponibilidad existente; el mensaje sigue siendo temporal; la validación visual en el celular del usuario sigue pendiente.

## APK y publicación

APK: `G:\NeuroNovaApps\AndroidStudioProjects\RutinaGo\app\build\outputs\apk\debug\app-debug.apk`.

Commit: `feat: add personalized ComunicaGo home vocabulary`. Publicar exclusivamente `feature/comunicago-personal-home` y comprobar que HEAD coincide con su referencia remota tras `git fetch origin`. La entrega informa el hash final. No se realizan merge ni PR ni se modifican ramas anteriores.

## Diagnóstico y corrección controlada de visibilidad

Punto de partida: rama `feature/comunicago-personal-home`, HEAD y remoto `c59a87f8ac4e691254d2aef4f9c4900644642721`. El único archivo ajeno al cambio era `.idea/deploymentTargetSelector.xml`, conservado sin seguimiento. El usuario confirmó que su celular mostraba el título **Palabras de Inicio**, propio de esta fase.

### Evidencia anterior a la corrección

No se confirmó una APK local desactualizada. El archivo entregado contenía las clases de Inicio personalizado y los parámetros de integración en su DEX. Sus datos eran:

- `applicationId`: `com.neuronovaapps.rutinago`.
- `versionCode`: `1`; `versionName`: `1.0`.
- Última generación: 18/10/2026, 11:40:37 (Lima). La fecha de creación original del archivo reutilizado no identifica la compilación.
- SHA-256: `EF777ED9175EF2227CA576630BF65F3FFB6E8D35F92E8885AD35FBF6EE464939`.
- La APK instalada y extraída del emulador produjo exactamente ese mismo SHA-256. No se pudo extraer ni comparar la instalada en el celular del usuario.

Se creó un AVD temporal dentro de `app/build/diagnostic-avd` utilizando la imagen Android 35 x86_64 ya instalada; no se descargaron imágenes ni se alteraron datos del celular. Las instalaciones se realizaron con `adb -s emulator-5582 install -r`, sin desinstalar ni limpiar datos.

La prueba `CommunicationHomeExposureUiTest` utiliza **MainActivity real**, el selector de módulos, `RutinaGoApp`, `CommunicationProfileEntryScreen` y el DataStore existente. Prepara un perfil con la misma forma de registros anterior a la fase: configuración y vocabulario disponible, sin clave de palabras de Inicio. Es una simulación de compatibilidad de un perfil anterior, no una lectura del perfil personal del usuario.

En una pantalla de **320 × 480 dp**, la aserción de que **Organizar palabras de Inicio** estaba visible al entrar falló. Tras desplazar la lista, el mismo control apareció. Las capturas `app/build/reports/baseline-home-exposure/entry.png` y `entry-after-scroll.png`, junto con sus árboles semánticos, documentan la diferencia.

El recorrido funcional completo de once pasos **pasó contra la APK original**, incluyendo añadir Agua, verla en Inicio, usarla en el mensaje, quitarla, encontrarla en Categorías, volver a asignarla y recrear la interfaz conservando selección y mensaje. Esto verifica que `onSetHomeWord` y `onOrganizeHome` sí llegaban a la UI real y que la administración era alcanzable; la condición de callback nulo no fue la causa reproducida.

La causa confirmada es de visibilidad y descubrimiento: el acceso estaba después del área del mensaje y quedaba fuera de la vista inicial en pantalla pequeña. Además, el organizador abría por defecto solo las palabras ya asignadas y situaba búsqueda y filtros después de explicaciones largas. Que un perfil sin personalización siga mostrando seis palabras es el comportamiento de compatibilidad previsto; necesita un acceso evidente para elegir otras.

### Intervención mínima

| Archivo de producción | Corrección |
| --- | --- |
| `CommunicationHomeScreen.kt` | Mueve Organizar antes del área del mensaje, conservando su callback y la navegación existente. |
| `CommunicationVocabularyScreen.kt` | Prioriza el botón de Palabras de Inicio antes del recuento y las explicaciones; usa el encabezado breve Perfil activo. |
| `CommunicationHomeVocabularyScreen.kt` | Abre inicialmente Todas las disponibles y adelanta búsqueda y filtros. Conserva Asignadas a Inicio y el estado guardado del filtro durante la sesión. |

No cambian repositorios, claves DataStore, constructor, filtros por perfil, catálogo, preferencias, barra inferior ni código de RutinaGo.

En pruebas, se corrigió la firma de limpieza JUnit para que devuelva `Unit` no nullable y Android pueda ejecutarla. Se ajustaron también las secuencias de dos pruebas anteriores para cerrar el teclado, desplazar hasta el botón, esperar la pantalla de organización y sincronizar antes de enviar Atrás. La primera tanda completa detectó estos dos fallos de automatización; no se contaron como pruebas aprobadas.

Otros intentos detectaron el teclado abierto en la prueba integrada y un timeout de PixelCopy al capturar evidencia. Se cerró el teclado antes de accionar la asignación y se sustituyó la captura por `UiAutomation.takeScreenshot()`, sin relajar las aserciones. Al reanudar el emulador hubo además un intento interrumpido por timeout de arranque del proceso antes de ejecutar casos; se conservó el registro y se repitió con Android completamente iniciado.

### Validación y evidencias

- Los cuatro objetivos Gradle exigidos finalizaron correctamente: `testDebugUnitTest`, `assembleDebug`, `lintDebug`, `assembleDebugAndroidTest`.
- Pruebas unitarias: **154, sin fallos ni errores**.
- Lint: **0 errores, 6 advertencias y 1 sugerencia heredadas**, ya detalladas arriba.
- `CommunicationHomeExposureUiTest` agrega cuatro pruebas integradas: entrada sin scroll en Vista guiada, entrada sin scroll en Vista completa, recorrido completo con registros de perfil anterior y accesibilidad de Inicio/Mi vocabulario con Texto Muy grande y Alto contraste. Comprueba también que el filtro inicial es Todas las disponibles sin seleccionarlo mediante la prueba.
- La tanda final incluye esas cuatro pruebas, las cuatro de `CommunicationPersonalHomeUiTest` y las cuatro de `ModuleShellNavigationTest`, ejecutadas en Android 35 a 320 × 480 dp: **OK (12 tests), 0 fallos, 56.789 segundos**. El resultado real se registra en `app/build/reports/phase3441-exposure-final-instrumentation.txt`. No quedan fallos pendientes en esta tanda.
- Capturas y árboles semánticos posteriores: `app/build/reports/final-home-exposure/`, incluyendo Inicio en ambas vistas, Mi vocabulario, Muy grande/Alto contraste y ciclo de Agua completado.

La simulación de perfil anterior, la navegación real, la presencia de callbacks y la visibilidad son evidencias distintas de la compilación. El celular del usuario no estuvo conectado; su instalación y validación final siguen pendientes.

Se revisaron los cinco archivos previamente modificados: tres pantallas de ComunicaGo, `CommunicationPersonalHomeUiTest.kt` y este documento. Se incorpora además `CommunicationHomeExposureUiTest.kt` como archivo nuevo de integración. `.idea/deploymentTargetSelector.xml` se mantiene sin seguimiento y con SHA-256 `40F6E1076D4C7A498494E680339CDE6B4C9D52033EF4E45F6D1E5C62D6E71644`. No se modifican el banco maestro, TTS, ARASAAC ni funcionalidades ajenas.

### APK identificable e instalación conservando datos

Se conserva la salida estándar `app/build/outputs/apk/debug/app-debug.apk` y se entrega una copia con nombre único:

`G:\NeuroNovaApps\AndroidStudioProjects\RutinaGo\app\build\outputs\apk\debug\RutinaGo-ComunicaGo-personal-home-fix-20261018.apk`

- Generada el **18/10/2026 a las 12:05:50 (Lima)**.
- SHA-256: `208BEC39424156C45484F4BC155799FF2AF5F663761BC98DD58C882129383569`.
- Mismo `applicationId`, `versionCode 1` y `versionName 1.0`; estos números no distinguen las dos entregas.
- Firma de depuración, certificado SHA-256: `ff36e3c58c80a5d345625ee32a1c02910e27aed95e3503256733475c61131846`.
- La APK extraída del emulador después de actualizar coincide exactamente con el SHA-256 de esta entrega; se verifica también su firma con `apksigner`.

Para instalarla, abrir esta APK nueva y actualizar sobre la aplicación existente, sin desinstalar ni borrar datos. Con ADB, identificar primero el dispositivo y paquete:

```powershell
& 'G:\Android\Sdk\platform-tools\adb.exe' devices -l
& 'G:\Android\Sdk\platform-tools\adb.exe' -s SERIAL_DEL_CELULAR shell pm list packages com.neuronovaapps.rutinago
& 'G:\Android\Sdk\platform-tools\adb.exe' -s SERIAL_DEL_CELULAR install -r 'G:\NeuroNovaApps\AndroidStudioProjects\RutinaGo\app\build\outputs\apk\debug\RutinaGo-ComunicaGo-personal-home-fix-20261018.apk'
```

Tras actualizar: ComunicaGo → Inicio → Organizar palabras de Inicio → buscar Agua → Añadir a Inicio → Volver a Inicio. El botón debe verse antes del mensaje y la búsqueda debe permitir encontrar palabras disponibles sin cambiar primero de filtro.

Publicación exclusivamente en `feature/comunicago-personal-home`, con commit `fix: expose ComunicaGo home vocabulary controls`. Sin merge ni PR. La entrega informa el hash final local y remoto. No se declara validación física del celular a partir de la compilación ni de las pruebas del emulador.
