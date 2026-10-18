# Fase 3.4.2 — Presentación personalizada de ComunicaGo

## Objetivo y control de alcance

Personalizar la presentación facilita localizar palabras; no restringe el catálogo,
las combinaciones ni la expresión. Edad y comunicación oral no determinan capacidad
intelectual, cognitiva, lectora o de aprendizaje.

- Rama existente reutilizada: `feature/comunicago-adaptive-presentation`.
- Baseline local y remoto confirmado antes de modificar:
  `104b2f88cb9f18acf1b3799ce1087aec8dd2799a` (Fase 3.4.1).
- Se comprobaron estado, rama, HEAD, fetch y referencia remota antes de implementar.
- Sin merge, PR, modificación de ramas anteriores ni creación de otra rama.
- `.idea/deploymentTargetSelector.xml` permanece local, intacto y fuera del commit.
  SHA-256: `40F6E1076D4C7A498494E680339CDE6B4C9D52033EF4E45F6D1E5C62D6E71644`.

## Arquitectura reutilizada

`CommunicationPresentation.kt` define modos estables `GUIDED` / `FULL`, preferencias,
política de sugerencia y una interfaz de repositorio. La selección explícita es
nullable: ausencia de elección significa sugerencia, no una clasificación guardada.

`CommunicationProfileEntryScreen` combina configuración y presentación del mismo
ID antes de mostrar el comunicador. Así, una preferencia Full recuperada no aparece
primero como Guided durante la carga. Conserva el formulario inicial y la edición
de Fase 3.4.1. Los errores de lectura del archivo siguen ofreciendo reintento y regreso.

`CommunicationHomeScreen` recibe solo el modo visual y el control de presentación;
no recibe edad o comunicación oral. Reutiliza constructor, codec, tarjetas,
categorías, acceso rápido y grilla adaptativa. El selector de categorías y el de
vista comparten el componente de opciones accesibles, sin una nueva arquitectura
de navegación, motor de filtrado o almacenamiento.

## Sugerencia inicial y perfiles sin configuración

- Comunicación oral emergente o sin habla actual: se sugiere Vista guiada y el
  texto introductorio explica el acceso rápido destacado.
- Otras respuestas, incluidas las no especificadas: se ofrece igualmente Vista
  guiada, con una explicación general modificable.
- Una configuración ausente produce una sugerencia Guided segura en la política.
  En el flujo real se mantiene primero la configuración inicial existente,
  sin preseleccionar edad ni comunicación oral; se puede elegir no especificarlas.
- La edad no interviene en la política. Tener mayor edad o usar palabras/frases
  nunca asigna automáticamente Vista completa.
- Una elección explícita siempre prevalece, aunque se editen las características.

En el primer uso se explica brevemente que ambas vistas incluyen todas las palabras
y que se puede cambiar. Al componer esa explicación se registra `introductionSeen`;
permanece en esa visita hasta pulsar Entendido o elegir vista. No vuelve en entradas
posteriores ni recreaciones una vez persistida. Ver la explicación no transforma
la sugerencia en una elección explícita. Pulsar la opción ya seleccionada también
permite guardarla expresamente.

## Diferencias reales entre vistas

| Aspecto | Vista guiada | Vista completa |
| --- | --- | --- |
| Ancho mínimo base de tarjeta | 184 dp | 132 dp |
| Altura mínima de vocabulario | 96 dp | 64 dp |
| Altura mínima de acceso rápido | 112 dp | 64 dp |
| Padding interior | 20 dp | 12 dp |
| Espaciado de grilla | 16 dp | 10 dp |
| Acceso rápido | Fondo destacado y mayor tamaño | Distribución compacta |
| Palabras / categorías disponibles | 132 / 18 y Todos | 132 / 18 y Todos |

Son mínimos: las tarjetas crecen con el texto; no se limita el número de líneas.
La grilla usa `GridCells.Adaptive`, con ancho ajustado por escala global y de sistema,
y un ancho máximo de contenido de 720 dp. No establece columnas rígidas. En pantalla
estrecha o texto muy grande ambas pueden mostrar una columna para conservar legibilidad.

Se mantiene el mensaje antes del acceso rápido, seguido por categorías y vocabulario.
El control Presentación está disponible dentro del comunicador, junto al mensaje.
Las categorías muestran siempre un resumen de la selección activa. Elegir categoría
abre un conjunto vertical adaptable con Todos y las 18 opciones originales, en su
orden de fuente; seleccionar una opción lo contrae. Puede abrirse de nuevo con el
mismo control. No se exige desplazamiento horizontal, ni se elimina una categoría
o palabra al contraer el selector. El filtro explícito por categoría sigue siendo
el de Fase 3.4; Todos permite regresar al catálogo íntegro.

El acceso rápido conserva registros, textos e IDs:
Yo `VOC_001`, Quiero `VOC_006`, Necesito `VOC_007`, Sí `VOC_063`, No `VOC_053`,
Ayuda `VOC_014`. No se duplican registros ni se reordena permanentemente el banco.

## Persistencia e independencia por perfil

Se reutiliza exactamente el DataStore de Fase 3.4.1:
`context.noBackupFilesDir/communication_profile_setup.preferences_pb`.

- Características: registro existente `profile:<ID>`, sin cambiar sus campos o versión.
- Presentación: registro independiente `presentation:<ID>` con
  `schemaVersion: 1`, `presentationMode: null | "GUIDED" | "FULL"` e
  `introductionSeen: boolean`.
- No se usa el nombre visible como clave; renombrar conserva la selección.
- Elegir vista solo modifica el registro de presentación; editar características
  solo modifica el registro de configuración.
- Cada escritura espera a DataStore. Mientras se guarda una elección, se deshabilita
  únicamente el selector de vista; se muestra progreso y un error genérico si falla.
  Se puede reintentar seleccionando otra vez. El modo aplicado se deriva del dato guardado.
- Registros de presentación ausentes, dañados o incompatibles recuperan un estado
  seguro sin elección explícita; leer ese fallback no reescribe el registro. Los
  errores de acceso al archivo no se confunden con ausencia de datos.

El manager existente valida que el perfil exista y comparte el mutex de guardado
y eliminación. `deleteProfile` elimina en una sola edición del archivo contextual
las claves de configuración y presentación del ID solicitado; después elimina el
perfil mediante el mecanismo existente. No toca los registros de otros IDs. Se
rechazan escrituras posteriores para IDs eliminados. La limpieza contextual y el
borrado del perfil están en archivos distintos: no constituyen una transacción
conjunta; se mantiene el orden de Fase 3.4.1 para evitar contexto huérfano.

La preferencia participa en la misma política local y fuera de las copias estándar
Android definida en Fase 3.4.1. No cambian manifiesto, reglas de backup, esquema SQL,
migraciones o dependencias; no se incorpora red, analítica, diagnóstico, rastreo,
sincronización o exportación. Borrar datos/desinstalar elimina estas preferencias.

## Mensaje y navegación

Cambiar modo solo cambia dimensiones, espaciado y énfasis. No crea otro constructor,
no usa el modo como clave de estado y no introduce ese modo en el codec temporal.
Permanecen intactos `CommunicationMessageBuilder` y `CommunicationStateCodec`.
Se mantienen orden, IDs de vocablos y ocurrencias, repeticiones, Borrar último,
Limpiar, Deshacer limpieza, categoría y estado vacío.

Se reutilizan los holders por ID del shell y de la entrada para conservar el
mensaje durante edición, regreso desde el selector y recreación de Activity.
El mensaje sigue siendo temporal; no se convierte en historial persistente.

- Sin configuración: selector → configuración inicial → ComunicaGo.
- Con configuración: selector → ComunicaGo con su preferencia.
- Volver/Atrás desde configuración inicial: selector.
- Volver/Atrás desde edición: ComunicaGo, con el mensaje temporal.
- Volver al selector/Atrás desde ComunicaGo: selector, sin cerrar la aplicación.

No se modifica el botón Volver al selector ni la navegación funcional de RutinaGo.

## Accesibilidad

Ambos modos reutilizan Día, Noche, Sistema, Alto contraste y los tres tamaños de
texto globales. No crean ajustes paralelos ni anulan los existentes. Los controles
tienen al menos 48 dp, texto adaptable, rol de botón o radio, grupos de selección,
estado seleccionado y descripción accesible. El check y el borde acompañan al
color de selección. Los errores son regiones vivas discretas para TalkBack.
Las categorías se exploran con desplazamiento vertical y la categoría activa
permanece identificable incluso con el selector contraído.

La matriz instrumentada preparada cubre 60 escenarios: dos vistas × cinco
apariencias (Día, Noche, Sistema claro/oscuro, Alto contraste) × tres tamaños ×
dos pantallas (400×760 / 320×480 dp; esta última con fontScale adicional 1.6).
Comprueba geometría del texto, semántica, selección, áreas táctiles, categorías,
palabras largas y acciones del mensaje. La lectura real de TalkBack y la geometría
en dispositivo requieren ejecución; no se consideran validadas por compilación.

## Catálogo e aislamiento

El asset, fuente canónica, generador y repositorio del catálogo permanecen intactos.
`node tools/generate_communication_catalog.mjs --check` verifica reproducibilidad:
18 categorías, 132 vocablos, 111 validados, 21 pendientes, 10 solo texto y 11 con
pictograma pendiente. No hay filtros, bloqueos o reglas gramaticales por edad/habla.

No se modifican Hoy, Rutinas, Crear, Progreso, Configuración, ejecución, historial,
modo Simple/Avanzado ni ARASAAC existente. El repositorio compartido solo delega
la nueva preferencia de ComunicaGo; la limpieza usa el punto de borrado ya existente.
No se añade la bottom bar de RutinaGo al comunicador.

Las tarjetas comunes mantienen área táctil, texto y mínimos ampliables para alojar
pictogramas más adelante. No se descargan imágenes, no hay imágenes ficticias ni
cambios de cliente/caché ARASAAC. El mensaje común permanece preparado para futuras
acciones Hablar/Repetir en ambas vistas, sin TTS ni botones que prometan esa función.

## Verificación

Ejecución del 17 de octubre de 2026, con el JBR de Android Studio. Las cuatro tareas
se solicitaron juntas en una invocación Gradle, que terminó `BUILD SUCCESSFUL`:

| Verificación ejecutada | Resultado |
| --- | --- |
| `gradlew.bat testDebugUnitTest` | 111 pruebas, 0 fallos, 0 errores, 0 omitidas |
| `gradlew.bat assembleDebug` | APK debug generada correctamente |
| `gradlew.bat lintDebug` | 0 errores, 6 avisos y 1 hint heredados |
| `gradlew.bat assembleDebugAndroidTest` | APK instrumentada compilada correctamente |
| `node tools/generate_communication_catalog.mjs --check` | Catálogo idéntico y reproducible; 132 / 18 |
| `git diff --check` | Sin errores de espacios |

El baseline documenta 96 pruebas unitarias sin fallos. Se conservan esas pruebas y
se añaden 15 (6 de política/presentación y 9 de persistencia); pasan todas las suites,
incluidas las de RutinaGo, perfiles, ARASAAC y codec existentes. No hubo fallos nuevos
que requirieran reproducir una regresión en el baseline. Los 6 avisos lint siguen en
`gradle/libs.versions.toml` y el hint en `ArasaacSearchDialog.kt`, archivos intactos.
Se mantiene el aviso JVM heredado `sun.misc.Unsafe` de protobuf de DataStore 1.2.1,
sin fallo de pruebas; no se actualizan dependencias fuera del alcance.

Las pruebas unitarias nuevas cubren política para las 30 combinaciones de respuestas,
preferencia explícita, ausencia de datos, catálogo y acceso rápido preservados,
estado/undo independiente del modo y mínimos de presentación. Las pruebas de
DataStore real verifican cerrar/reabrir, selección explícita, explicación vista,
concurrencia, nombres iguales, renombrado, independencia, edición de características,
registro dañado/incompatible y borrado selectivo. Se conservan las pruebas previas
que detectan dependencias de red, síntesis o persistencia de catálogo indebidas.

`CommunicationPresentationUiTest` prepara primer ingreso del perfil 9–12/emergente,
Guided → Full, acceso a 132 vocablos y 18 categorías, Yo/Quiero/Agua, repetición,
Borrar último, cambio de vista con mensaje y categoría, Limpiar/cambio/Deshacer,
edición, salida/reentrada, recreación, segundo perfil, renombrado, ambos regresos
y ausencia de bottom bar/voz. Crea y limpia solo sus dos perfiles de prueba.

Se adaptan los helpers previos al selector vertical conservando las comprobaciones
de selección, conteos y constructor. La matriz de catálogo ahora comprueba ambas
vistas. No se eliminan asserts ni se altera una prueba para ocultar un fallo.

No se detectó dispositivo conectado (`adb devices -l`) ni AVD configurado
(`emulator -list-avds`). Las pruebas instrumentadas quedan preparadas y pendientes
de ejecución; compilar su APK no equivale a ejecutarlas. La deuda instrumentada
heredada no se declara resuelta ni se atribuye a esta fase sin ejecución comparativa.

## Inventario de cambios

Nuevos:

- `app/src/main/java/com/neuronovaapps/rutinago/domain/communication/CommunicationPresentation.kt`
- `app/src/main/java/com/neuronovaapps/rutinago/ui/screens/communication/components/CommunicationPresentationSelector.kt`
- `app/src/test/java/com/neuronovaapps/rutinago/CommunicationPresentationTest.kt`
- `app/src/androidTest/java/com/neuronovaapps/rutinago/CommunicationPresentationUiTest.kt`
- Este documento.

Modificados:

- `app/src/main/java/com/neuronovaapps/rutinago/data/communication/CommunicationSetupPreferences.kt`
- `app/src/main/java/com/neuronovaapps/rutinago/data/preferences/UserPreferencesRepository.kt`
- `app/src/main/java/com/neuronovaapps/rutinago/ui/screens/communication/CommunicationProfileEntryScreen.kt`
- `app/src/main/java/com/neuronovaapps/rutinago/ui/screens/communication/CommunicationHomeScreen.kt`
- `app/src/main/java/com/neuronovaapps/rutinago/ui/screens/communication/components/CommunicationComponents.kt`
- `app/src/test/java/com/neuronovaapps/rutinago/CommunicationProfileSetupTest.kt`
- `app/src/androidTest/java/com/neuronovaapps/rutinago/CommunicationFoundationTest.kt`
- `app/src/androidTest/java/com/neuronovaapps/rutinago/CommunicationMessageBuilderUiTest.kt`
- `app/src/androidTest/java/com/neuronovaapps/rutinago/ModuleShellNavigationTest.kt`

APK de validación:
`G:\NeuroNovaApps\AndroidStudioProjects\RutinaGo\app\build\outputs\apk\debug\app-debug.apk`.
