# Fase 3.3 — Catálogo base de ComunicaGo

## Baseline y aislamiento

- Rama: `feature/comunicago-vocabulary-catalog`.
- Baseline exacto local/remoto de Fase 3.2:
  `b5fc8a884ab46bcb8870843af644fcb33e3ed34a`.
- Al comenzar, la rama exclusiva ya existía publicada en ese mismo commit.
  Se verificaron las referencias con `git fetch origin`; no se desarrolló sobre
  `feature/comunicago-functional-foundation`.
- Único archivo ajeno presente: `.idea/deploymentTargetSelector.xml`, sin
  seguimiento; se mantiene fuera del commit.

## Fuente y reproducción

Fuente exclusiva: [Banco maestro de comunicación pictográfica - RutinaGo](https://docs.google.com/spreadsheets/d/1yoTSsM0V9Hrej1ttkE4xzdUXpXdVswY9JOM-s8ywQ04/edit).

Lectura directa con el conector Google Drive/Sheets el **17 de octubre de 2026**,
con `UNFORMATTED_VALUE`. Se verificaron metadatos, encabezados y las hojas:

| Hoja | sheetId | Rango leído |
| --- | --- | --- |
| Vocabulario | 729896269 | A1:Z1000 |
| Categorías | 422539881 | A1:L200 |
| Contrato integración | 1965086801 | A1:H100 |
| README | 483884013 | A1:H100 |

El snapshot verificable `docs/communication/source_v1.json` conserva filas,
tipos, encabezados, rangos y contrato. No contiene datos reconstruidos
manualmente. La huella SHA-256 del snapshot UTF-8 con saltos LF es
`4f49c92c6d1c569c8d50e9e0108ce5de1c1e7560b5105a3a91750286e5ed9df4`;
un test fija esa huella para detectar alteraciones.

Artefacto empaquetado:
`app/src/main/assets/communication/communication_catalog_v1.json`.
Versión local **1**, contrato **1.0**, con fuente, fecha y conteos explícitos.
La hoja README describe la integración técnica v1.3; ese texto se conserva
en el snapshot sin sustituir la versión del contrato.

Reproducción desde la raíz:

```powershell
node tools/generate_communication_catalog.mjs
node tools/generate_communication_catalog.mjs --check
```

El generador exige los conteos aprobados e integridad referencial antes de
escribir. Solo completa con cadena vacía las celdas finales omitidas por
Sheets en una fila; no deriva contenido editorial, IDs ni estrategias.
El test compara todas las celdas originales con el JSON empaquetado,
incluidos booleanos y IDs ARASAAC cuyo tipo original puede ser texto o número.
La comparación de saltos de línea tolera checkout LF/CRLF.

## Conteos verificados

| Dato | Total |
| --- | ---: |
| Categorías | 18 |
| Vocablos | 132 |
| VALIDADO | 111 |
| PENDIENTE_ARASAAC | 21 |
| SOLO_TEXTO | 10 |
| PICTOGRAMA_PENDIENTE | 11 |

Los 21 pendientes se importan y aparecen como texto. No se ocultan por sus
campos de Modo Simple/Avanzado. Las 18 categorías están activas en la fuente.
`Todos` es únicamente el filtro de interfaz identificado por `null`.
No modifica el banco ni sus conteos.

## Modelos y repositorio

`domain/communication/CommunicationCatalog.kt` define:

- `CommunicationCategory`: ID, nombre, descripción, orden, modos y activo
  (los siete campos reales de la hoja).
- `CommunicationVocabularyItem`: los veinte campos de Vocabulario, incluidos
  términos alternativos, observaciones, curación, fuente, criterios,
  estrategia visual y tratamiento Simple.
- `CommunicationCatalogRepository`: acceso común de solo lectura y filtro
  por `categoryId`, sin dependencia de un perfil.

`LocalCommunicationCatalogRepository` recibe un lector de texto y valida
el archivo completo antes de exponer resultados. Comprueba versión, esquema,
booleanos estrictos, órdenes enteros positivos, IDs únicos con patrones
`VOC_###`/`CAT_###`, etiquetas requeridas, referencias de categorías y
todos los conteos. Los validados requieren un ID ARASAAC positivo.
Si falla, no publica un catálogo parcial ni reemplaza datos por ejemplos.

Las listas y los resultados filtrados son inmutables también ante intentos
de modificación desde Java/Kotlin mediante cast. Se ordenan por el campo
editorial `orden` y, en empate, por ID estable, conforme al contrato.
No se ordena por texto ni se renumeran registros.

Los campos vacíos de estrategia visual/tratamiento Simple permanecen vacíos.
Aunque el contrato permite derivaciones para fases de presentación futuras,
esta fase no las necesita: todas las tarjetas muestran su texto exacto.

## Carga offline y pantalla

ComunicaGo lee exclusivamente el asset local en `Dispatchers.IO`. Expone
un estado de carga y un error explícito si el catálogo falla. No accede a
Sheets en ejecución, no descarga datos ni crea almacenamiento específico.

La pantalla mantiene el nombre del perfil activo, «Tu mensaje aparecerá aquí»
y ambas salidas al selector. Tiene una única `LazyVerticalGrid` con claves
estables; cabeceras a ancho completo y tarjetas con columnas adaptativas.
El ancho mínimo de tarjeta aumenta con la tipografía y la escala del sistema.
Las alturas pueden crecer y el texto no tiene límite artificial de líneas.

Las categorías usan una fila horizontal desplazable, conservando el orden
del banco; cada opción tiene al menos 48 dp, semántica de radio, estado
seleccionado y descripción accesible. La selección incorpora una marca ✓
y borde más grueso además del color, también en alto contraste.
La marca es decorativa para TalkBack; el nombre y estado se anuncian.

El filtro por `categoria_id` conserva el orden global y muestra el conteo.
Al cambiar de categoría la cuadrícula vuelve al inicio; el filtro se
conserva durante recreación mediante `rememberSaveable`. Las tarjetas tienen
al menos 72 dp y texto accesible, sin acción falsa de mensaje o voz.

## Verificación

Comandos ejecutados con el JBR local de Android Studio:

| Verificación | Resultado |
| --- | --- |
| `node tools/generate_communication_catalog.mjs --check` | Correcto; reproducción exacta |
| `gradlew.bat testDebugUnitTest` | 64 pruebas, 0 fallos, 0 errores (9 pruebas nuevas de catálogo) |
| `gradlew.bat assembleDebug` | Correcto |
| `gradlew.bat lintDebug` | 0 errores, 6 avisos y 1 hint en archivos preexistentes sin cambios |
| `gradlew.bat assembleDebugAndroidTest` | Correcto |

Los avisos de lint corresponden a versiones de dependencias en
`gradle/libs.versions.toml` y el hint a `ArasaacSearchDialog.kt`; ninguno
de esos archivos se modifica. No hubo fallos de pruebas unitarias ni de
compilación que exigieran comparación contra baseline.

`CommunicationCatalogTest` cubre conteos, identidades, integridad referencial,
orden, los 18 filtros y Todos, conservación de pendientes, inmutabilidad,
fidelidad celda por celda y rechazo de archivos dañados: filas ausentes,
duplicados, referencias rotas, campos requeridos vacíos, órdenes inválidos,
booleanos incorrectos, estructura incompatible y metadatos ARASAAC inválidos.

`CommunicationFoundationTest` se actualiza desde el placeholder de Fase 3.2
para comprobar los 132 vocablos, las 18 categorías, cambio de filtro,
ausencia de acciones ficticias y legibilidad. La matriz incluye Día, Noche,
Sistema claro/oscuro y Alto contraste; texto Normal, Grande y Muy grande;
400×760 dp y 320×480 dp, este último además con escala del sistema 1.6.
Verifica geometría de texto, semántica y objetivos táctiles; guarda capturas
en `phase33-communication-qa` cuando se ejecute.

Las pruebas de navegación del selector se adaptan al catálogo lazy y mantienen
las comprobaciones de perfil, regreso visible/Atrás, ausencia de bottom bar
y preservación del módulo RutinaGo.

No se detectó ningún dispositivo con `adb devices -l` ni AVD configurado con
`emulator -list-avds`. Las pruebas instrumentadas y capturas quedan
**pendientes de ejecución en dispositivo**; compilar el APK de tests no
constituye validación visual ni de TalkBack.

## Exclusiones y límites

- Sin TTS, botón Hablar, construcción de mensajes, borrar/limpiar/repetir,
  favoritos, historial ni personalización por perfil.
- Sin Room nuevo, migraciones, base paralela, Supabase ni persistencia propia.
- Sin cliente ARASAAC nuevo, búsquedas, llamadas API, descargas o imágenes
  remotas. Los IDs y criterios son únicamente metadatos.
- Sin pictogramas inventados ni iconos sustitutivos de vocabulario.
- Sin selector Simple/Avanzado de ComunicaGo ni conexión a los modos RutinaGo.
- Sin importar Estructuras o Frases rápidas: pertenecen a fases posteriores.
- Sin cambios funcionales en Hoy, Rutinas, Crear, Progreso, Configuración,
  Perfiles, historial, ARASAAC existente ni navegación de RutinaGo.

La deuda conocida de la suite instrumentada global de Fase 3.2 no se declara
resuelta ni se atribuye a esta fase: no se ejecutó al no haber dispositivo.
Cualquier fallo futuro requiere reproducirlo y compararlo con el baseline
antes de clasificarlo como nuevo o heredado.

## Archivos de la fase

Nuevos:

- `app/src/main/assets/communication/communication_catalog_v1.json`
- `app/src/main/java/com/neuronovaapps/rutinago/domain/communication/CommunicationCatalog.kt`
- `app/src/main/java/com/neuronovaapps/rutinago/data/communication/LocalCommunicationCatalogRepository.kt`
- `app/src/test/java/com/neuronovaapps/rutinago/CommunicationCatalogTest.kt`
- `docs/communication/source_v1.json`
- `tools/generate_communication_catalog.mjs`
- `docs/FASE_3_3_COMUNICAGO_VOCABULARY_CATALOG.md`

Modificados:

- `app/src/main/java/com/neuronovaapps/rutinago/ui/screens/communication/CommunicationHomeScreen.kt`
- `app/src/main/java/com/neuronovaapps/rutinago/ui/screens/communication/components/CommunicationComponents.kt`
- `app/src/androidTest/java/com/neuronovaapps/rutinago/CommunicationFoundationTest.kt`
- `app/src/androidTest/java/com/neuronovaapps/rutinago/ModuleShellNavigationTest.kt`
- `app/src/androidTest/java/com/neuronovaapps/rutinago/ModuleSelectorAccessibilityTest.kt`

APK: `G:\NeuroNovaApps\AndroidStudioProjects\RutinaGo\app\build\outputs\apk\debug\app-debug.apk`.
