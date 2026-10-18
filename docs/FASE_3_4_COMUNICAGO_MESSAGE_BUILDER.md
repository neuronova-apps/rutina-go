# Fase 3.4 — Constructor flexible y vocabulario esencial de ComunicaGo

## Baseline y rama

- Baseline autorizado y verificado local/remoto:
  `fc57fc9c3c6f9af00d82fd0cdae0bd303278071e`.
- Rama de origen: `feature/comunicago-vocabulary-catalog`.
- Rama exclusiva: `feature/comunicago-message-builder`, creada y publicada
  en el baseline antes de desarrollar.
- No se hacen merges ni se modifican las ramas estables.
- `.idea/deploymentTargetSelector.xml` permanece local, sin seguimiento
  y fuera de los commits.

## Vocabulario esencial

Se comprobaron las etiquetas y referencias exactas en
`app/src/main/assets/communication/communication_catalog_v1.json`:

| Posición de acceso rápido | Texto exacto | vocab_id | categoria_id |
| ---: | --- | --- | --- |
| 1 | Yo | VOC_001 | CAT_001 |
| 2 | Quiero | VOC_006 | CAT_002 |
| 3 | Necesito | VOC_007 | CAT_002 |
| 4 | Sí | VOC_063 | CAT_018 |
| 5 | No | VOC_053 | CAT_013 |
| 6 | Ayuda | VOC_014 | CAT_004 |

La selección está definida únicamente mediante IDs estables. El constructor
resuelve esas referencias al mismo objeto de vocabulario que entrega el
repositorio: no crea registros ni categorías nuevos. Falla explícitamente
si falta algún ID esencial.

La sección «Acceso rápido» mantiene las seis posiciones, también al cambiar
de categoría. Está antes de las categorías en la cuadrícula desplazable
común. En pantalla pequeña se accede mediante scroll; no se fija una
cabecera de gran altura que impida navegar con texto Muy grande.
No es necesario regresar a «Todos» para utilizarla.

## Arquitectura

`domain/communication/CommunicationMessageBuilder.kt` contiene:

- `CommunicationMessageItem`: `selectionId` de ocurrencia, `vocabularyId`
  estable y texto editorial exacto.
- `CommunicationUiState`: categoría seleccionada, secuencia del mensaje,
  copia temporal para deshacer limpieza y próximo ID de ocurrencia.
- `CommunicationMessageBuilder`: operaciones puras de añadir, borrar último,
  limpiar, deshacer, cambiar categoría, filtrar y validar la restauración.

Las listas se copian defensivamente y son inmutables incluso ante casts a
listas mutables. No se utiliza la posición de la cuadrícula ni el texto
como identidad. Cada incorporación tiene un ID de ocurrencia distinto;
dos pulsaciones sobre el mismo `vocab_id` producen dos fichas independientes.
Los IDs retirados no se reutilizan durante la sesión.

La lógica depende únicamente de `CommunicationCatalogRepository`. No recibe
repositorios RutinaGo, modos, almacenamiento, TTS ni servicios de red.
El filtro sigue usando la categoría del banco y su orden editorial.

`data/communication/CommunicationStateCodec.kt` convierte el estado a un
JSON versionado compatible con Bundle, únicamente para `rememberSaveable`.
No lo escribe en disco ni en preferencias. Conserva texto, IDs, orden,
categoría y opción de deshacer. Rechaza IDs desconocidos, etiquetas
alteradas, ocurrencias duplicadas o secuencias incompatibles.

`CommunicationHomeScreen` continúa cargando el mismo asset local en IO.
Cuando está disponible, `CommunicationBuilderScreen` crea el controlador
y mantiene el estado con un Saver. La UI emite acciones y representa el
estado; no contiene reglas gramaticales ni operaciones de persistencia.

## Construcción y acciones

- Una tarjeta, tanto de acceso rápido como del catálogo, añade exactamente
  ese registro al final.
- Se permiten una palabra, frases incompletas, orden libre, repeticiones y
  combinaciones entre categorías. No se agregan artículos, corrigen
  mayúsculas, reordenan vocablos ni completan frases.
- Las fichas se muestran en «Mi mensaje», con rectángulos de esquinas
  moderadas, texto multilínea y separación.
- «Borrar último» retira solo la última ocurrencia. En vacío no hace nada
  y el botón está deshabilitado.
- «Limpiar» retira todo el mensaje y muestra «Tu mensaje aparecerá aquí».
  También está deshabilitado en vacío.
- «Deshacer limpieza» recupera íntegramente el último mensaje limpiado,
  sea corto o largo. No caduca ni abre diálogos. Cambiar de categoría no
  cancela esa opción; añadir una nueva palabra sí la cancela, para evitar
  sobrescribir un mensaje nuevo con el anterior.

No existe historial general de mensajes ni deshacer de todas las acciones.
Solo se conserva una copia temporal para revertir la última limpieza.

## Estado temporal y navegación

El estado sobrevive a recomposición y se guarda mediante Saver ante
recreación de Activity/orientación. El shell incorpora exclusivamente un
`rememberSaveableStateHolder` alrededor de ComunicaGo, con clave del perfil
activo existente. Eso conserva el mensaje al regresar al selector, entrar
en RutinaGo y volver a ComunicaGo durante la sesión.

El estado temporal queda aislado por perfil dentro de ese holder. No se
crean perfiles, tablas ni vocabulario específico por perfil. Al cambiar a
otro perfil no se muestra el mensaje temporal del anterior.

Tanto «Volver al selector» como Atrás de Android regresan al selector sin
borrar el mensaje. Las rutas, bottom bar y operaciones RutinaGo no cambian.
ComunicaGo continúa sin bottom bar de RutinaGo.

El mensaje se pierde al finalizar la sesión/tarea sin estado Android
restaurable, forzar cierre o descartar los datos temporales. Android puede
restaurar el Bundle después de recrear el proceso, pero **no se promete
persistencia tras cerrar la aplicación**. Un snapshot incompatible se
descarta y comienza vacío; nunca se inventan vocablos para restaurarlo.
Los límites de tamaño del estado guardado de Android siguen siendo aplicables.

## Accesibilidad y disposición

Se mantiene una sola cuadrícula vertical adaptativa para cabeceras, mensaje,
acceso rápido y catálogo. El ancho mínimo de tarjetas crece con la escala
tipográfica y del sistema; no hay un número rígido de columnas.
Cambiar categoría conserva el mensaje y no fuerza un regreso al inicio.

La zona de fichas del mensaje puede crecer hasta 180 dp antes de utilizar
su propio scroll, para que las acciones no se desplacen indefinidamente en
mensajes largos. La ficha más reciente se lleva al final de esa zona sin
animación. El resto de la pantalla también permite scroll.

Las acciones se distribuyen con `FlowRow` y pueden pasar a otra fila.
No se limitan las líneas del texto. Los objetivos táctiles tienen al menos
48 dp; las tarjetas conservan un mínimo de 72 dp.
TalkBack recibe el texto de la tarjeta, rol de botón, estado de selección
de categorías, nombre de acciones y posición de cada ocurrencia del mensaje.
La actualización del mensaje usa región viva discreta.

La selección de categoría conserva marca ✓, borde y semántica, además del
color. Se reutilizan Día, Noche, Sistema y Alto contraste, sin introducir
ajustes ni modos nuevos.

## Banco preservado

No se reimportó el banco ni se leyó o modificó Google Sheets en esta fase.
El asset, snapshot fuente, generador, modelos y repositorio del catálogo
de Fase 3.3 permanecen intactos:

| Dato | Conteo |
| --- | ---: |
| Vocablos | 132 |
| Categorías | 18 |
| VALIDADO | 111 |
| PENDIENTE_ARASAAC | 21 |
| SOLO_TEXTO | 10 |
| PICTOGRAMA_PENDIENTE | 11 |

Los campos ARASAAC, curación, estrategias visuales, Simple/Avanzado y orden
permanecen sin cambios. Todos los vocablos continúan disponibles como texto.

## Pruebas

| Verificación | Resultado |
| --- | --- |
| `gradlew.bat testDebugUnitTest` | 81 pruebas, 0 fallos y 0 errores; 17 nuevas del constructor |
| `gradlew.bat assembleDebug` | Correcto |
| `gradlew.bat lintDebug` | Correcto: 0 errores, 6 avisos y 1 hint preexistentes |
| `gradlew.bat assembleDebugAndroidTest` | Correcto; ejecución instrumentada pendiente |
| `node tools/generate_communication_catalog.mjs --check` | Reproducción exacta; sin regenerar ni modificar el banco |

El primer análisis de lint falló internamente al inspeccionar el test nuevo
`CommunicationMessageBuilderUiTest.kt`, con
`findFirCompiledSymbol only works on compiled declarations`. Esa primera
ejecución coincidió con la incorporación de los fuentes de prueba. Una
repetición completa con los archivos terminados pasó sin reproducirlo.
No se deshabilitó lint, no se añadieron supresiones ni se cambiaron versiones.
Se documenta como incidencia transitoria de herramientas, sin afirmar una
causa definitiva ni clasificarla como fallo heredado del baseline.

Los avisos finales pertenecen a `gradle/libs.versions.toml` y el hint a
`ArasaacSearchDialog.kt`, archivos idénticos al baseline Fase 3.3.
No se detectaron fallos unitarios o regresiones reproducibles. La suite
instrumentada global y su deuda conocida no se ejecutaron al no disponer
de dispositivo; no se declara resuelta ni se atribuye a esta fase.

`CommunicationMessageBuilderTest` añade pruebas para:

- Estado vacío, una palabra, múltiples palabras y orden libre.
- Repeticiones y sus identidades de ocurrencia.
- Borrar último, incluso en vacío, y no reutilizar IDs.
- Limpieza y recuperación de mensajes largos, cancelación de deshacer
  al iniciar un mensaje nuevo.
- Cambio de categoría sin perder mensaje ni deshacer.
- IDs esenciales exactos y referencias a los mismos objetos del catálogo.
- Inmutabilidad y aislamiento entre sesiones sin dependencias RutinaGo.
- Restauración de categoría, secuencia, siguiente ocurrencia y deshacer.
- Rechazo de snapshots incoherentes o vocablos inexistentes.
- Banco intacto y ausencia de recargas durante las acciones.
- Límite arquitectónico sin entradas de red, base de datos o síntesis de voz.

`CommunicationMessageBuilderUiTest` prueba el flujo desde el selector:
Quiero → Comida y bebida → Agua, borrar, limpiar/deshacer, repeticiones,
cambio de categorías, perfil compartido, ausencia de bottom bar/TTS,
regreso a través de RutinaGo y recreación de Activity incluyendo deshacer.

`CommunicationFoundationTest` conserva la exploración de los 132 vocablos
y las 18 categorías, y adapta las expectativas de tarjetas a su nueva
acción. Amplía la matriz de accesibilidad con construcción, limpieza,
deshacer y borrado. Incluye Día, Noche, Sistema claro/oscuro y Alto contraste;
texto Normal/Grande/Muy grande; 400×760 y 320×480 dp (este último también con
escala del sistema 1.6). Guarda capturas en `phase34-communication-qa`.
Los tests de navegación existentes solo se adaptan a la espera de carga.
No se eliminan comprobaciones para ocultar fallos heredados.

ADB no detectó dispositivos conectados y `emulator -list-avds` no devolvió
emuladores configurados. **Las pruebas instrumentadas no se ejecutaron**.
La compilación del APK de tests no valida visualmente accesibilidad,
TalkBack, recreación real ni interacción en celular.

## Exclusiones y siguientes fases

Sin TTS ni botón Hablar; sin imágenes o descargas nuevas; sin clientes
ARASAAC paralelos ni llamadas a su API; sin Room nuevo, migraciones,
persistencia de mensajes, favoritos o historial; sin modos nuevos ni
reglas gramaticales. No se modifican cliente, caché o descargas existentes.
El `vocabularyId` de cada ficha permitirá resolver pictogramas reales en
una fase posterior, sin introducir imágenes ficticias.

## Archivos de la fase

Nuevos:

- `app/src/main/java/com/neuronovaapps/rutinago/domain/communication/CommunicationMessageBuilder.kt`
- `app/src/main/java/com/neuronovaapps/rutinago/data/communication/CommunicationStateCodec.kt`
- `app/src/test/java/com/neuronovaapps/rutinago/CommunicationMessageBuilderTest.kt`
- `app/src/androidTest/java/com/neuronovaapps/rutinago/CommunicationMessageBuilderUiTest.kt`
- `docs/FASE_3_4_COMUNICAGO_MESSAGE_BUILDER.md`

Modificados:

- `app/src/main/java/com/neuronovaapps/rutinago/ui/screens/communication/CommunicationHomeScreen.kt`
- `app/src/main/java/com/neuronovaapps/rutinago/ui/screens/communication/components/CommunicationComponents.kt`
- `app/src/main/java/com/neuronovaapps/rutinago/ui/navigation/RutinaGoApp.kt`
- `app/src/androidTest/java/com/neuronovaapps/rutinago/CommunicationFoundationTest.kt`
- `app/src/androidTest/java/com/neuronovaapps/rutinago/ModuleShellNavigationTest.kt`
- `app/src/androidTest/java/com/neuronovaapps/rutinago/ModuleSelectorAccessibilityTest.kt`

APK generada: `G:\NeuroNovaApps\AndroidStudioProjects\RutinaGo\app\build\outputs\apk\debug\app-debug.apk`.
