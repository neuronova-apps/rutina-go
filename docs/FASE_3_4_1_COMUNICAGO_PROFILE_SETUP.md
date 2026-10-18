# Fase 3.4.1 — Configuración inicial de ComunicaGo por perfil

## Baseline y alcance

- Baseline local/remoto confirmado:
  `74275435d2fc5fa3ab7bfeac809ad6a212c562ba`.
- Origen estable: `feature/comunicago-message-builder`.
- Rama exclusiva: `feature/comunicago-profile-setup`, publicada en ese
  baseline antes de implementar.
- Sin merges ni cambios sobre ramas anteriores.
- `.idea/deploymentTargetSelector.xml` se conserva local y fuera del commit.

Esta fase registra dos respuestas contextuales explícitas por perfil,
permite consultarlas y editarlas, y no deduce comprensión, capacidad,
diagnóstico ni potencial de aprendizaje. No aplica adaptaciones automáticas.

## Modelo y respuestas

`CommunicationProfileSetup` contiene:

- `profileId`: identificador estable del perfil ya existente.
- `ageRange`: enum con seis rangos.
- `oralCommunication`: enum con cinco opciones.
- `setupCompleted`: marca de configuración completada.
- `schemaVersion`: versión 1.

| Edad visible | Valor estable |
| --- | --- |
| 3 a 5 años | YEARS_3_TO_5 |
| 6 a 8 años | YEARS_6_TO_8 |
| 9 a 12 años | YEARS_9_TO_12 |
| 13 a 17 años | YEARS_13_TO_17 |
| 18 años o más | ADULT |
| Prefiero no especificar | NOT_SPECIFIED |

| Comunicación oral visible | Valor estable |
| --- | --- |
| Habla utilizando palabras y frases. | WORDS_AND_PHRASES |
| Utiliza algunas palabras o vocalizaciones. | EMERGING_SPEECH |
| No utiliza habla oral actualmente. | NO_CURRENT_SPEECH |
| Su comunicación oral es variable. | VARIABLE_SPEECH |
| Prefiero no especificar. | NOT_SPECIFIED |

La primera configuración no preselecciona respuestas. «Guardar y continuar»
se habilita al elegir ambas opciones, incluidas las dos «Prefiero no
especificar». Estas últimas completan la configuración sin declarar los
datos. El rango 9–12 y EMERGING_SPEECH se usan como referencia de pruebas,
nunca como valores predeterminados.

## Arquitectura y persistencia

- `CommunicationProfileSetupRepository` es la interfaz de lectura/escritura.
- `CommunicationSetupPreferences` usa Preferences DataStore y un registro
  JSON versionado por clave `profile:<ID>`. El ID está en la clave; el payload
  contiene solo los otros cuatro campos, sin duplicar nombre ni datos de perfil.
- `CommunicationSetupStorage` mantiene una única instancia de DataStore
  para el archivo de configuración.
- `CommunicationProfileSetupManager` valida que el perfil exista y coordina
  guardado y limpieza mediante un mutex compartido dentro del proceso.
- `UserPreferencesRepository` adapta los perfiles existentes a esa interfaz.
  Su flujo de preferencias, archivo anterior, modos y operaciones previas
  permanecen funcionalmente iguales.

Archivo:
`context.noBackupFilesDir/communication_profile_setup.preferences_pb`.
Se reutilizan las dependencias e infraestructura Preferences DataStore ya
presentes. Se elige un archivo separado para mantener estas respuestas fuera
de las copias Android sin alterar las copias de las preferencias RutinaGo.
No se añade almacenamiento SQL, esquema, migración ni dependencia.

El guardado espera a que termine la escritura DataStore antes de continuar.
Las respuestas se recuperan en entradas posteriores y después de cerrar y
reabrir la app. Solo se persisten configuraciones completadas; los cambios
sin guardar permanecen temporales en la UI.

La lectura y la escritura exponen errores explícitos. Un registro corrupto
o incompatible no se sustituye silenciosamente por respuestas inventadas.
La interfaz ofrece reintento o regreso; si falla el guardado conserva las
selecciones y muestra un mensaje genérico, sin información personal en errores.

## Privacidad y copias de Android

Se inspeccionaron el manifiesto y ambas reglas de backup: `allowBackup=true`
y exclusión previa únicamente de `pictograms/`. Las preferencias antiguas
pueden participar en copias Android; no se modifica ese comportamiento.
El nuevo archivo está en `noBackupFilesDir`, excluido del mecanismo estándar
de Auto Backup de Android, incluso ante reglas de inclusión
([documentación Android](https://developer.android.com/identity/data/autobackup)).
No se añaden red, sincronización, analítica ni exportación de estas respuestas.
No se afirma protección absoluta frente a acceso privilegiado, copias ajenas
al mecanismo estándar o compromiso del dispositivo; tampoco se añade cifrado
propio. Tras desinstalar/borrar datos o reinstalar/restaurar perfiles sin este
archivo, habrá que completar de nuevo la configuración.

## Eliminación de perfiles

El baseline no tenía interfaz ni método de borrado de perfiles. Se incorpora
`UserPreferencesRepository.deleteProfile(profileId)` como punto de eliminación
con limpieza asociada, sin añadir una pantalla o botón de borrado.

El manager elimina primero el registro contextual y después el perfil
en las preferencias existentes, actualizando el perfil activo si corresponde.
Solo se elimina la clave del ID solicitado. Guardar y borrar usan el mismo
mutex para impedir que un guardado concurrente recree respuestas huérfanas.

Las dos escrituras pertenecen a archivos distintos y no se presentan como
una transacción conjunta. Si falla la limpieza, el perfil no se elimina.
Si se interrumpe después de limpiar y antes de borrar el perfil, este puede
permanecer sin configuración y solicitarla de nuevo; no deja contexto huérfano.
No se introduce borrado de rutinas, pasos, ejecuciones, historial o progreso.
Toda futura interfaz de eliminación debe usar este punto con limpieza.

## Entrada, edición y navegación

`CommunicationProfileEntryScreen` comprueba la configuración del ID activo:

- Sin configuración completada: selector → configuración inicial → guardar
  → ComunicaGo.
- Con configuración completada: selector → ComunicaGo directamente.
- Volver o Atrás desde configuración inicial: selector de módulos, sin guardar.
- «Personalizar ComunicaGo» en el comunicador: abre la edición con las
  selecciones actuales.
- Guardar edición: persiste los cambios y regresa al comunicador.
- Volver o Atrás desde edición: regresa al comunicador y descarta cambios
  sin guardar.

La edición no modifica el botón «Volver al selector» del comunicador.
Durante una escritura se deshabilitan acciones para evitar doble envío
o salida a mitad del guardado. No se cierra la Activity desde estas rutas.

La entrada tiene estado y claves por perfil. Un `SaveableStateHolder` conserva
el constructor mientras se edita; el holder del shell ya existente mantiene
la sesión al pasar por el selector/RutinaGo. Se conservan mensaje, orden,
repeticiones, categoría, Borrar último, Limpiar, Deshacer y restauración
temporal de Fase 3.4. El mensaje sigue siendo temporal, no persistente.

## Accesibilidad

Formulario vertical desplazable, ancho adaptable hasta 720 dp y texto sin
límite artificial de líneas. Cada pregunta usa un grupo de opciones con
rol radio, selección explícita, marcador visual y estado accesible.
Las opciones tienen al menos 56 dp y las acciones 48 dp.

Se reutilizan Día, Noche, Sistema, Alto contraste y los tres tamaños de
texto. No se usa solo color para expresar selección. Guardado/errores
tienen estados visibles y el error es una región viva discreta para TalkBack.
No se solicitan diagnósticos ni datos clínicos.

## Catálogo y RutinaGo preservados

El constructor, su codec temporal, las tarjetas, repositorio, JSON y fuente
canónica permanecen intactos. Las respuestas no se pasan al filtrado, layout,
selección inicial de categorías, acceso rápido ni reglas de mensajes.
«Personalizar ComunicaGo» es una entrada adicional de interfaz.

| Dato | Conservado |
| --- | ---: |
| Vocablos | 132 |
| Categorías | 18 |
| VALIDADO | 111 |
| PENDIENTE_ARASAAC | 21 |
| SOLO_TEXTO | 10 |
| PICTOGRAMA_PENDIENTE | 11 |

Sin TTS, pictogramas nuevos, integración ARASAAC nueva, IA, evaluaciones,
clasificaciones cognitivas, ocultación de palabras o cambios funcionales
en Hoy, Rutinas, Crear, Progreso, Configuración, Perfiles y modos RutinaGo.

## Pruebas

| Verificación ejecutada | Resultado |
| --- | --- |
| `gradlew.bat testDebugUnitTest` | 96 pruebas, 0 fallos y 0 errores; 15 nuevas de configuración |
| `gradlew.bat assembleDebug` | Correcto |
| `gradlew.bat lintDebug` | 0 errores, 6 avisos y 1 hint en archivos sin cambios |
| `gradlew.bat assembleDebugAndroidTest` | Correcto |
| `node tools/generate_communication_catalog.mjs --check` | Catálogo idéntico y reproducible, sin regenerarlo |

No hubo fallos técnicos o unitarios que requirieran reproducir regresiones
en el baseline. Los avisos lint siguen en `gradle/libs.versions.toml` y
`ArasaacSearchDialog.kt`, sin modificaciones respecto a Fase 3.4.
La JVM emitió un aviso de API `sun.misc.Unsafe` obsoleta desde protobuf de
DataStore 1.2.1 al ejecutar la prueba nueva de persistencia; no produjo
fallos y no se actualizó esa dependencia fuera del alcance.
La deuda instrumentada conocida no se declara resuelta ni se atribuye a
esta fase, porque no se ejecutó la suite en un dispositivo.

`CommunicationProfileSetupTest` usa un DataStore real en archivos temporales:
ausencia inicial de respuestas, persistencia tras cerrar/reabrir el store,
edición, perfiles con igual nombre, renombrado, opciones sin especificar,
las 30 combinaciones de respuestas, eliminación selectiva, prevención de
huérfanos, errores de escritura/borrado y rechazo de registros dañados.
También comprueba que las respuestas no entren en el código de catálogo,
constructor o presentación de vocabulario.

`CommunicationProfileSetupUiTest` prepara configuración inicial, edición
con cancelación/guardado, ambos recorridos Atrás, perfiles independientes,
renombrado, recreación de Activity, preservación del mensaje y acceso a
los 132 vocablos. Crea y limpia únicamente sus perfiles de prueba.

`CommunicationProfileSetupAccessibilityTest` prepara una matriz de cinco
apariencias (Día, Noche, Sistema claro/oscuro y Alto contraste), tres tamaños
de texto y pantallas 400×760 / 320×480 dp, con escala adicional 1.6 en la
pequeña. Comprueba geometría del texto, semántica, selección y acciones.

Las pruebas de navegación anteriores se preparan explícitamente eligiendo
«Prefiero no especificar» solo si aparece el nuevo formulario. Se mantienen
las comprobaciones del constructor y navegación; no se quitan asserts para
ocultar fallos. Las pruebas directas del componente de catálogo no necesitan
pasar por el flujo de entrada.

No se detectó dispositivo conectado ni AVD configurado. Las pruebas
instrumentadas quedan pendientes de ejecución; compilar el APK de tests
no equivale a validar interacción, TalkBack o geometría en dispositivo.

## Archivos

Nuevos:

- `domain/communication/CommunicationProfileSetup.kt`
- `data/communication/CommunicationSetupPreferences.kt`
- `data/communication/CommunicationSetupStorage.kt`
- `ui/screens/communication/CommunicationProfileEntryScreen.kt`
- `ui/screens/communication/CommunicationProfileSetupScreen.kt`
- `app/src/test/java/com/neuronovaapps/rutinago/CommunicationProfileSetupTest.kt`
- `app/src/androidTest/java/com/neuronovaapps/rutinago/CommunicationProfileSetupUiTest.kt`
- `app/src/androidTest/java/com/neuronovaapps/rutinago/CommunicationProfileSetupAccessibilityTest.kt`
- `docs/FASE_3_4_1_COMUNICAGO_PROFILE_SETUP.md`

Modificados:

- `data/preferences/UserPreferencesRepository.kt`
- `ui/navigation/RutinaGoApp.kt`
- `ui/screens/communication/CommunicationHomeScreen.kt`
- `app/src/androidTest/java/com/neuronovaapps/rutinago/ModuleShellNavigationTest.kt`
- `app/src/androidTest/java/com/neuronovaapps/rutinago/CommunicationMessageBuilderUiTest.kt`

Las rutas Kotlin de producción se indican relativas a
`app/src/main/java/com/neuronovaapps/rutinago/`.

APK: `G:\NeuroNovaApps\AndroidStudioProjects\RutinaGo\app\build\outputs\apk\debug\app-debug.apk`.
