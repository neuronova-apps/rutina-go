# Fase 3.5 — Voz de ComunicaGo

## Base, alcance y referencias protegidas

Rama exclusiva `feature/comunicago-tts`, ya creada y publicada. Antes de editar se verificaron `git status`, rama, HEAD, `git fetch origin` y referencia remota, todos en `afb7769151cee596357079af479552d9c985e7b6`. No había cambios funcionales pendientes; se preserva sin seguimiento `.idea/deploymentTargetSelector.xml` (SHA-256 `40F6E1076D4C7A498494E680339CDE6B4C9D52033EF4E45F6D1E5C62D6E71644`).

Se registraron las referencias protegidas: `main` en `684a31368b92b5c303b2480c02bd42761e62d4b2`; `feature/comunicago-simplified-navigation` y `backup/comunicago-pre-fase-3-5-2026-10-18` en el baseline; objeto de etiqueta `comunicago-v3.4.4.2-stable` en `55909968bbb09458328fe0af146da6c04869f241`. No se modifican esas referencias, ni se realiza merge o PR.

## Arquitectura y texto

`CommunicationSpeechController` es independiente de Android y recibe una interfaz de motor y un scope. Expone estado observable y admite un motor simulado en pruebas. `AndroidCommunicationSpeechEngine` encapsula `android.speech.tts.TextToSpeech`, usa contexto de aplicación y entrega los callbacks en el hilo principal. La UI no importa ni administra la API TTS.

Se reutiliza el constructor original y sus `CommunicationMessageItem`. El texto es la unión de sus etiquetas mediante un espacio, manteniendo orden, mayúsculas, repeticiones y combinaciones libres. Por ejemplo, las etiquetas Yo / Quiero / Agua producen `Yo Quiero Agua`. No se pronuncian sus IDs, no se corrige gramática ni se incorporan palabras. Hablar, Repetir y Detener no modifican mensaje, categoría, disponibilidad ni asignación a Inicio.

Se respeta `getMaxSpeechInputLength()` del motor. Un mensaje que supera ese límite se rechaza entero con explicación y conserva su composición; no se trunca silenciosamente.

## Solicitudes, confirmación y controles

- **Hablar**, con icono de altavoz y texto, está junto al mensaje y antes de las acciones de edición y el vocabulario. Se deshabilita con mensaje vacío o motor no disponible. No se reproduce automáticamente al tocar vocablos.
- **Detener** interrumpe la solicitud activa, invalida su ID y conserva mensaje y último texto confirmado. Se deshabilita sin solicitud activa.
- **Repetir** aparece cuando hay un último texto confirmado y permite reproducirlo aunque el mensaje actual esté vacío o sea distinto. No reemplaza la composición actual.

Aceptar `speak()` no confirma reproducción. Una solicitud se marca reproduciendo en `onStart`; el último texto repetible solo cambia con `onDone` del ID vigente, después de haber observado `onStart`. Error, interrupción, rechazo, ausencia de inicio o callback de una petición anterior conservan el último texto válido. Esta política confirma el ciclo del motor, no la audibilidad física del altavoz.

Se usa `QUEUE_FLUSH`, un ID nuevo por petición y una generación por instancia de motor. No se acumulan frases ni se reutilizan callbacks tras un reintento o cambio de propietario. La UI distingue preparación, solicitud enviada, reproducción, disponibilidad, detención y error sin diálogos por transición. Los estados usan una región semántica de anuncio cortés.

## Idioma y disponibilidad

Se consultan los servicios instalados y las voces en español. Se excluyen voces que el motor declara no instaladas. Se prefieren voces locales y, dentro de ellas, es-PE; después otras variantes de español. Si solo hay una voz española de red, se informa expresamente de su necesidad de conexión. No se selecciona inglés como sustituto. Un fallback de idioma comprueba compatibilidad y que la voz resultante siga siendo española.

Ausencia de motor, inicialización fallida, falta de español o datos incompletos se muestran con orientación hacia la configuración de síntesis de voz de Android. No se abren instalaciones ni descargas. Configuración de ComunicaGo permite reintentar tras un error.

Inicialización y aceptación sin inicio tienen un watchdog de 10 segundos. Tras el inicio se permite al menos 30 segundos, ampliados según la longitud (500 ms por carácter, máximo 30 minutos) antes de informar falta de respuesta e interrumpir. Se detecta volumen multimedia cero; una ruta Bluetooth, altavoz defectuoso u otros factores físicos pueden impedir audio aun con callbacks correctos.

## Sesión, ciclo de vida y perfiles

El propietario está en la entrada del perfil de ComunicaGo, por encima de sus pantallas. La recomposición no crea motores nuevos. Cambiar de pestaña, explorar vocabulario o editar preferencias conserva el mismo mensaje y controlador del perfil. No se condiciona la voz a edad, habla oral o vista guiada/completa.

Al pasar a segundo plano se detiene la solicitud (`ON_STOP`). Al abandonar ComunicaGo, cambiar de perfil o destruir/recrear su propietario se detiene y libera el motor con `shutdown()`. Al recrear se inicializa otro motor sin reanudar automáticamente audio. Generación e IDs descartan resultados tardíos.

Solo el último texto confirmado se restaura temporalmente con el estado guardado de la sesión del perfil, como el mensaje existente. No hay audio, grabaciones ni historial permanente. Un perfil nuevo no recibe el texto del anterior; cada clave de sesión queda dentro del `SaveableStateHolder` correspondiente al ID estable. La velocidad se recupera independientemente para el perfil entrante.

## Velocidad y DataStore

Configuración añade **Velocidad de voz**: Lenta (0.8), Normal (1.0) y Rápida (1.2). La elección es explícita y no se infiere de edad o forma de comunicación. Cada petición aplica la velocidad actual.

Se reutiliza el DataStore de ComunicaGo ya existente, fuera del backup, mediante `voice_rate_v1:<ID de perfil>`. Ausencia o valor desconocido devuelve Normal sin escribir ni migrar el registro. Guardado y eliminación usan el gestor existente y su mutex: solo se escribe la clave de velocidad y se elimina al borrar ese perfil. No se modifican edad, habla oral, presentación, vocabulario disponible, selección de Inicio ni preferencias globales de RutinaGo. No se agregan tablas ni dependencias.

## Android y operación sin internet

Se conservan minSdk 24, targetSdk/compileSdk 37, applicationId y firma. El manifest añade únicamente la consulta de servicios `android.intent.action.TTS_SERVICE`, necesaria para visibilidad de motores en Android moderno. INTERNET ya existía en el baseline por funciones anteriores; TTS no añade permisos de red, micrófono, almacenamiento ni otros permisos peligrosos.

La síntesis offline depende de una voz local instalada. No se promete audio sin conexión cuando el dispositivo carece de ella. El motor instalado puede variar entre dispositivos y puede procesar texto en red según sus características y configuración. No hay clientes, claves API, servicios propios ni transmisión a NeuroNova Apps; tampoco se registran mensajes o datos de perfil en logs/analítica de esta implementación. No se generan archivos de audio.

Referencias primarias: [Android TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech), [progreso de enunciados](https://developer.android.com/reference/android/speech/tts/UtteranceProgressListener) y [Voice](https://developer.android.com/reference/android/speech/tts/Voice).

## Validación y límites

Comprobaciones finales ejecutadas sobre las fuentes de entrega:

| Comprobación | Resultado real | Informe local |
| --- | --- | --- |
| `gradlew.bat testDebugUnitTest` | 189 pruebas, 0 fallos, 0 errores, 0 omitidas; incluye 26 de controlador y 6 de preferencias nuevas | `app/build/test-results/testDebugUnitTest/` |
| `gradlew.bat assembleDebug` | Aprobada | `app/build/reports/phase35-final-build.log` |
| `gradlew.bat lintDebug` | 0 errores; 6 advertencias de versiones y 1 sugerencia de autoboxing heredadas, en archivos sin cambios | `app/build/reports/lint-results-debug.xml` |
| `gradlew.bat assembleDebugAndroidTest` | Aprobada | `app/build/reports/phase35-final-build.log` |
| Tanda funcional Android | `OK (24 tests)`, 95.465 s | `app/build/reports/phase35-functional-final.txt` |
| Tanda final accesibilidad + motor Android | `OK (4 tests)`, 127.179 s, sin omitidas; la matriz exige los 60 escenarios completos | `app/build/reports/phase35-accessibility-native-final.txt` |

Son **27 casos instrumentados distintos aprobados**: un caso de descubrimiento del motor se repite en las dos tandas. Emulador Android 15/API 35, serie `emulator-5582`. La tanda funcional incluye las 5 pruebas UI nuevas de voz y regresiones de exposición de controles de Inicio (6), personalización de Inicio (4), navegación inferior (4) y separación/recreación de módulos y RutinaGo (4). Se ejecutó con viewport de 320 × 480 dp. La matriz usa contenedores de 400 × 760 y 320 × 480 dp; combina 5 apariencias (Día, Noche, Sistema claro, Sistema oscuro y Alto contraste), 3 tamaños de texto y las 2 vistas. El contenedor pequeño añade fuente del sistema a 1.6. Comprueba etiquetas, tamaños táctiles, ausencia de solapamiento entre Hablar y Detener, acceso por desplazamiento, Repetir, región semántica de estado y las tres pestañas. La otra prueba nueva comprueba segundo plano, regreso sin reproducción automática y liberación.

El adaptador real se inicializó y encontró una voz española **local es-US**, como fallback disponible; no se afirma que el emulador tenga es-PE. La prueba nativa de `Yo Quiero Agua` completó Hablar y Repetir mediante `onStart` y `onDone`, y comprobó Detener conservando la última confirmación. Informes: `app/build/reports/phase35-real-engine.txt` y `phase35-native-playback.txt`. Una consulta inicial había informado ausencia de español; las consultas finales y la reproducción nativa pasaron con la voz que el motor declaró disponible. No se instalaron motores ni paquetes de voz desde esta implementación.

Las capturas y árboles semánticos de mensaje, velocidad y texto Muy grande con Alto contraste están en `app/build/reports/phase35-ui-evidence/`; corresponden a UI con motor simulado, no a audibilidad real. En el tamaño extremo se usa desplazamiento para alcanzar cada control, sin superponerlos.

Las pruebas de controlador usan un motor simulado para comprobar confirmación, repetición, detención, sustitución, errores, tiempos de espera, callbacks tardíos y liberación. Las pruebas de preferencias usan DataStore real temporal para default, reapertura, aislamiento, conservación de otras claves y eliminación. Las pruebas UI utilizan entrada de perfil y DataStore reales con motor simulado; su aprobación no prueba audio audible. La prueba del adaptador Android registra por separado la disponibilidad del motor instalado.

La primera ejecución de la matriz de accesibilidad encontró `CalledFromWrongThreadException` durante una continuación del planificador no confinado de Compose Test y un error secundario de disposición. La misma matriz, aislada y sin cambios de producción, aprobó sus 60 escenarios. La prueba nueva fija explícitamente `StandardTestDispatcher`, conforme al soporte del framework, conserva todas las aserciones y exige 60 escenarios verificados. No se alteran dependencias ni se omiten casos para evitar el fallo. Se conservan los informes de ambos intentos en `app/build/reports/phase35-accessibility-attempt1.txt` y `phase35-accessibility-isolated.txt`.

La comprobación de límites de ComunicaGo conserva la prohibición de clientes de red y Room. La antigua prohibición absoluta de voz se actualiza únicamente para permitir Android TTS en `AndroidCommunicationSpeechEngine.kt`; se comprueba expresamente que el resto de la UI y dominio no importan esa API. Un import faltante en una prueba nueva y una aserción de texto que exigía coincidencia completa en lugar de parcial se corrigieron durante la preparación; no se cuentan sus intentos fallidos como pruebas aprobadas.

Audibilidad, calidad, volumen físico y latencia quedan pendientes de validación en un celular real. El emulador temporal se ejecuta sin salida de audio. TalkBack y gestos físicos requieren además revisión manual. Los estados y controles tienen cobertura semántica y de tamaño táctil automatizada.

El banco permanece intacto: 132 vocablos y 18 categorías, 111 VALIDADO, 21 PENDIENTE_ARASAAC, 10 SOLO_TEXTO y 11 PICTOGRAMA_PENDIENTE. No se modifica Google Sheets, ARASAAC ni pictogramas, y se conserva la navegación de tres pestañas. RutinaGo no recibe cambios funcionales.

## Entrega

Commit solicitado: `feat: add ComunicaGo Android text-to-speech`, exclusivamente en `feature/comunicago-tts`, tras aprobar las comprobaciones obligatorias. El reporte final informa el hash local/remoto y la verificación de las referencias protegidas. No quedan fallos automatizados pendientes; se mantienen las limitaciones de validación manual indicadas arriba.

APK estándar: `G:\NeuroNovaApps\AndroidStudioProjects\RutinaGo\app\build\outputs\apk\debug\app-debug.apk`.

Copia identificable: `G:\NeuroNovaApps\AndroidStudioProjects\RutinaGo\app\build\outputs\apk\debug\RutinaGo-ComunicaGo-TTS-fase-3-5.apk`.

Ambas APK tienen 16 264 435 bytes y SHA-256 `298384c124d903f783138f56bbdc1c8f4adf499a4a3620584c3743b6ec40937d`. La APK extraída del emulador después de las pruebas tiene el mismo hash. Se verificó la firma con `apksigner`: certificado debug SHA-256 `ff36e3c58c80a5d345625ee32a1c02910e27aed95e3503256733475c61131846`. ApplicationId `com.neuronovaapps.rutinago`, versionCode 1, versionName 1.0. El APK de producción debug se actualizó con `adb install -r` durante la validación, conservando los datos del emulador; no se tocó un celular.

Instalar como actualización sobre la app existente, sin desinstalar ni borrar datos. No se cambia versionCode porque la actualización de depuración con la misma firma admite el código actual.

## Archivos de esta fase

Nueve archivos creados:

- `app/src/main/java/com/neuronovaapps/rutinago/domain/communication/CommunicationSpeechController.kt`: interfaz, estados, política de voces y controlador.
- `app/src/main/java/com/neuronovaapps/rutinago/data/communication/AndroidCommunicationSpeechEngine.kt`: adaptador Android.
- `app/src/main/java/com/neuronovaapps/rutinago/ui/screens/communication/CommunicationSpeechControls.kt`: propietario, controles y ajustes accesibles.
- `app/src/test/java/com/neuronovaapps/rutinago/CommunicationSpeechControllerTest.kt`.
- `app/src/test/java/com/neuronovaapps/rutinago/CommunicationSpeechPreferencesTest.kt`.
- `app/src/androidTest/java/com/neuronovaapps/rutinago/CommunicationSpeechUiTest.kt`.
- `app/src/androidTest/java/com/neuronovaapps/rutinago/CommunicationSpeechAccessibilityTest.kt`.
- `app/src/androidTest/java/com/neuronovaapps/rutinago/CommunicationAndroidSpeechEngineTest.kt`.
- `docs/FASE_3_5_COMUNICAGO_TTS.md`.

Ocho archivos modificados:

- `app/src/main/AndroidManifest.xml`: visibilidad de servicios TTS, sin permisos nuevos.
- `app/src/main/java/com/neuronovaapps/rutinago/data/communication/CommunicationSetupPreferences.kt`: velocidad por perfil y limpieza al eliminarlo.
- `app/src/main/java/com/neuronovaapps/rutinago/data/preferences/UserPreferencesRepository.kt`: delegación a las preferencias existentes.
- `app/src/main/java/com/neuronovaapps/rutinago/ui/screens/communication/CommunicationProfileEntryScreen.kt`: propietario por perfil, lectura y guardado de velocidad.
- `app/src/main/java/com/neuronovaapps/rutinago/ui/screens/communication/CommunicationHomeScreen.kt`: puntos de composición para controles y ajustes.
- `app/src/main/java/com/neuronovaapps/rutinago/ui/screens/communication/CommunicationSettingsScreen.kt`: sección de voz.
- `app/src/main/java/com/neuronovaapps/rutinago/ui/screens/communication/components/CommunicationComponents.kt`: controles junto al mensaje antes de edición.
- `app/src/test/java/com/neuronovaapps/rutinago/CommunicationMessageBuilderTest.kt`: límite arquitectónico actualizado para permitir únicamente el adaptador TTS Android.
