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

**Las pruebas instrumentadas no se ejecutaron**: `adb devices -l` no enumeró dispositivos y `emulator -list-avds` no enumeró emuladores. Compilar la APK de pruebas no equivale a ejecutarlas. Quedan pendientes ejecución en Android y validación manual con TalkBack, gestos y teclado real. Con un dispositivo disponible puede utilizarse `gradlew.bat connectedDebugAndroidTest`, o seleccionar las dos clases nuevas mediante `android.testInstrumentationRunnerArguments.class`.

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

Límites: no hay reordenación manual de Inicio ni historial nuevo de selecciones; las asignaciones ocultas se recuperan mediante la disponibilidad existente; el mensaje sigue siendo temporal; la validación visual en dispositivo está pendiente.

## APK y publicación

APK: `G:\NeuroNovaApps\AndroidStudioProjects\RutinaGo\app\build\outputs\apk\debug\app-debug.apk`.

Commit: `feat: add personalized ComunicaGo home vocabulary`. Publicar exclusivamente `feature/comunicago-personal-home` y comprobar que HEAD coincide con su referencia remota tras `git fetch origin`. La entrega informa el hash final. No se realizan merge ni PR ni se modifican ramas anteriores.
