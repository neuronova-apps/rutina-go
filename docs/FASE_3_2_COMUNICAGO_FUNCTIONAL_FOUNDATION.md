# Fase 3.2 — Base funcional de ComunicaGo

## Punto de partida y alcance

- Rama exclusiva: `feature/comunicago-functional-foundation`.
- Baseline local y remoto verificado: `9d21d0cb6c6abaf410283c26bb43719e13928247`.
- La rama se publicó en ese baseline antes de implementar. Fase 3.1 y las
  demás ramas estables conservan sus referencias e historial.

Esta fase sustituye el placeholder por una base real de presentación. El
nombre visible sigue siendo ComunicaGo; los paquetes, rutas y componentes
internos mantienen `Communication` / `communication`.

## Arquitectura y componentes

- `CommunicationHomeScreen`: recibe únicamente el nombre del perfil activo y
  la acción de regreso al selector. Reutiliza tema, tipografía, accesibilidad
  e infraestructura general; no recibe repositorios ni datos de RutinaGo.
- `CommunicationMessageBar`: superficie diferenciada con título «Mensaje»
  y estado inicial «Tu mensaje aparecerá aquí». Su altura mínima puede crecer
  con el contenido. Será el punto de presentación para futuras secuencias.
- `CommunicationVocabularyArea`: contenedor con «Palabras y apoyos visuales»,
  «Las palabras y apoyos visuales aparecerán aquí.» y una orientación sobre
  la selección de palabras en la siguiente etapa.

No se crea un modelo de estado vacío artificial: todavía no hay mensaje,
elementos seleccionados ni vocabulario. El futuro estado pertenecerá a
Communication y no dependerá del Modo Simple/Avanzado de RutinaGo.

No se muestran acciones Hablar, Borrar último, Limpiar, Repetir o Favoritos:
su implementación queda para fases posteriores y no se simulan resultados.

## Navegación y perfil

El selector conserva la ruta interna `communication`. Solo se sustituye la
llamada al placeholder por la pantalla principal. El botón visible «Volver
al selector» y Atrás de Android invocan el mismo callback existente. ComunicaGo
no tiene bottom bar. La navegación y bottom bar de RutinaGo no se modifican.

El nombre mostrado procede del perfil compartido actual. No hay perfiles,
UUID ni persistencia adicionales. Cambiar de módulo conserva la identidad.

## Diseño y accesibilidad

- Superficies Material 3 con esquinas de 12 dp y ancho limitado a 720 dp.
- Scroll vertical y áreas seguras del sistema; márgenes de la infraestructura
  común. Las áreas crecen para admitir texto grande y nombres largos.
- Regreso rectangular, con altura mínima de 48 dp y etiqueta textual.
- Día, Noche, Sistema y alto contraste usan los colores existentes. El borde
  se refuerza a 2 dp en alto contraste; el contenido no depende solo del color.
- Títulos semánticos en pantalla, mensaje y vocabulario. Ambas áreas son
  grupos de recorrido para TalkBack, sin descripciones que dupliquen el texto.

## Alcance excluido

No se importa ni hardcodea el banco maestro: no hay 132 vocablos, 18 categorías,
25 estructuras ni 35 frases rápidas. Tampoco se añaden constructor real de
mensajes, TTS, favoritos, historial de comunicación o selector Simple/Avanzado.

No hay tablas Room, migraciones, cambios de versión de base de datos ni uso
de tablas, pasos, historial o progreso de rutinas desde Communication. No se
modifica ARASAAC, su caché o downloader ni se crea una integración paralela.

## Pruebas

La integración verifica entrada desde el selector, título y perfil compartido,
área de mensaje, vocabulario vacío, ausencia de las cinco pestañas de RutinaGo,
ambas vías de regreso y recreación de actividad al cambiar de módulo.

`CommunicationFoundationTest` verifica 30 combinaciones: Día, Noche, Sistema
claro/oscuro y alto contraste; Normal/Grande/Muy grande; pantallas verticales
400 × 760 dp y 320 × 480 dp. En la pantalla pequeña se añade escala de fuente
de sistema 1,6. Comprueba texto sin recorte, scroll, títulos y grupos semánticos,
regreso accesible y ausencia de acciones ficticias. Guarda capturas de la
parte superior e inferior como evidencia local excluida de Git.

### Validación técnica

| Comando | Resultado |
| --- | --- |
| `gradlew.bat testDebugUnitTest` | 55 pruebas, 0 fallos y 0 errores |
| `gradlew.bat assembleDebug` | Correcto |
| `gradlew.bat lintDebug` | Sin errores; 6 advertencias de versiones y 1 sugerencia existentes |
| `gradlew.bat assembleDebugAndroidTest` | Correcto |

APK actualizado: `app/build/outputs/apk/debug/app-debug.apk`.

### Validación instrumentada

Emulador temporal Pixel 6, Android 15 / API 35, orientación vertical:

- La matriz nueva de ComunicaGo pasó sus 30 combinaciones; se revisaron
  capturas de Día, Noche, alto contraste y texto muy grande con scroll.
- La matriz existente del selector también pasó sus 30 combinaciones.
- Las 13 pruebas de Crear, ejecución, navegación entre módulos y bienvenida
  pasaron en una ejecución conjunta con datos de prueba limpios.
- La suite global inicial ejecutó 61 casos: 48 correctos y 13 fallidos. Dos
  fallos de arranque/estado entre pruebas quedaron superados en la ejecución
  limpia de las 13 pruebas de navegación. La repetición sin limpiar también
  detectó una colisión del ID fijo de una rutina de prueba entre perfiles.
- La suite global confirmó los casos de perfiles, preferencias y modos,
  WorkManager de pictogramas y 12 casos de persistencia de rutinas, incluidos
  referencias offline y snapshots históricos.
- Con la APK de Fase 3.2 restaurada tras el contraste, se verificaron con
  pulsaciones reales el perfil compartido, ambas áreas y el regreso tanto
  por el botón visible como por `KEYCODE_BACK`. Correcto, sin cerrar la app.

### Contraste de los fallos heredados

Se exportó el baseline exacto con `git archive` dentro de `build/`, se compilaron
sus APK y se ejecutó su suite en el mismo emulador con datos limpios. No se
cambió de rama ni se editó código histórico. Resultado: 60 casos, 49 correctos
y 11 fallidos. Esos mismos 11 casos fallan en Fase 3.2:

| Pruebas heredadas | Fallos | Evidencia |
| --- | ---: | --- |
| `MasterCatalogRoomTest` | 1 | Exige `database.isOpen` antes del primer acceso |
| `RoutineExecutionRoomTest` | 1 | Error de inicialización del runner; `@Before` no devuelve `Unit` |
| `RoutineInteractionAndSaveActionTest` | 5 | Aserciones y nodos de fixtures de creación/guardado |
| `RoutineSaveNavigationTest` | 2 | Nodos de plantilla y confirmación/edición |
| Clases de migración 1→2 y 2→3 | 2 | Los esquemas 1.json/2.json no están incluidos en assets de test |

Los únicos dos fallos adicionales de la ejecución global de Fase 3.2 son los
casos de Crear y completar una rutina que pasaron después con datos limpios.
La comparación no deja fallos nuevos sin resolver atribuibles a esta fase.
La suite global **no está completamente verde**: estos 11 fallos heredados
quedan fuera de alcance y no se cambia Room ni la implementación de RutinaGo
para corregir pruebas de fases anteriores.

Evidencias locales excluidas de Git:

- `app/build/phase32-instrumentation.log`: ejecución global de Fase 3.2.
- `app/build/phase32-navigation-clean.log`: 13 casos de navegación correctos.
- `app/build/phase32-baseline-instrumentation.log`: ejecución del baseline.
- `app/build/phase32-comparison.json`: comparación por clase y nombre de caso.
- `app/build/phase32-captures/`: 60 capturas de ComunicaGo, arriba/abajo.
- `app/build/phase32-manual/`: captura y resultado del recorrido real de Android.

## Estado final

Base de presentación implementada; banco aún no integrado. Validaciones
obligatorias correctas y navegación/comunicación verificadas. La suite global
heredada conserva 11 fallos reproducidos en el baseline; no es una validación
global completamente verde. Lista para validación de la base de Fase 3.2.
`.idea/deploymentTargetSelector.xml` se conserva intacto y fuera del commit.

RAMA_FASE_3_2_CREADA · COMUNICAGO_BASE_FUNCIONAL_IMPLEMENTADA ·
RUTINAGO_PRESERVADO · BANCO_AUN_NO_INTEGRADO · LISTO_PARA_VALIDACION_FASE_3_2.
