# RutinaGo — Fase 2.1: auditoría del banco maestro y arquitectura ARASAAC

**Fecha de auditoría:** 24 de septiembre de 2026  
**Rama:** `feature/rutinago-master-bank-arasaac`  
**Commit base:** `b52354e551d2dcf010bd44b1f6517c938555f3df`  
**Repositorio:** `https://github.com/neuronova-apps/rutina-go`  
**Fuente autorizada:** [Banco maestro de actividades - App de rutinas](https://docs.google.com/spreadsheets/d/1is0zFNvvA69Rb_C0V0qYG4a-AtEh0oxdSiJTFEFlaYo/edit)  
**Alcance:** auditoría y planificación. No se importaron registros, no se implementó la API, no se guardaron pictogramas y no se modificó código funcional.

## 1. Dictamen ejecutivo

El banco maestro está **listo como fuente de contenido estructurado**, pero **todavía no está listo para una integración visual ARASAAC de producción**.

La fuente contiene exactamente **369 registros maestros**:

| Banco | Registros verificados | Dictamen |
| --- | ---: | --- |
| Actividades | 203 | Integridad estructural apta |
| Rutinas | 52 | Relaciones ACT aptas |
| Logros y retos | 60 | Referencias aptas |
| Mensajes de refuerzo | 54 | Cobertura e integridad aptas |
| **Total** | **369** | **0 IDs duplicados y 0 referencias inválidas verificadas** |

Respuestas directas:

- **¿El banco maestro está listo para integrarse?** Sí para modelado, normalización e importación controlada; no para asignar imágenes definitivas sin una fase previa de curación visual.
- **¿Todas las actividades tienen identificadores y relaciones consistentes?** Sí. Hay 203 IDs ACT únicos; los saltos de numeración son históricos y no son duplicados. Las 52 rutinas referencian las 203 actividades, sin referencias inexistentes ni desajustes entre conteos y secuencias.
- **¿Cuántas actividades disponen de pictogramas o referencias visuales?** Las 203 declaran la política genérica `Pictograma ARASAAC o foto propia` y tienen término de búsqueda. Solo 76 están marcadas `Validado con pictograma`; 127 permanecen `Revisado`. Ninguna fila contiene un ID ARASAAC definitivo, URI o archivo de imagen.
- **¿Cuáles requieren selección de pictograma?** Las 127 actividades con estado `Revisado`, enumeradas en el anexo A.
- **¿Cómo se integrará ARASAAC?** Mediante selección humana asistida: búsqueda en español, previsualización, elección explícita de un `_id`, persistencia de ese ID y descarga únicamente del recurso seleccionado.
- **¿Cómo se utilizarán imágenes sin internet?** Cada visual seleccionado debe copiarse a almacenamiento interno administrado y registrarse en Room. La caché HTTP/Coil será secundaria y no la única garantía offline.
- **¿Cómo se guardarán rutinas personalizadas?** En Room, separadas del catálogo, con pasos ordenados que referencien actividades reutilizables.
- **¿Cómo se conservarán los perfiles actuales?** DataStore sigue siendo la fuente de verdad inicial. Room guardará el `profileId` existente como referencia opaca; no se cambia ni migra DataStore en la primera implantación.
- **¿Qué debe implementarse primero?** Contrato de datos y curación de IDs ARASAAC; después Room e importador versionado; luego almacenamiento visual offline; finalmente conectar gradualmente las pantallas existentes.

## 2. Verificación del repositorio

La comprobación inicial confirmó:

- Rama base: `feature/rutinago-android-foundation`.
- HEAD base: `b52354e551d2dcf010bd44b1f6517c938555f3df`.
- Remoto `origin`: `https://github.com/neuronova-apps/rutina-go.git`.
- La nueva rama se creó desde el commit exacto solicitado.
- `.idea/deploymentTargetSelector.xml` ya estaba staged antes de la auditoría y se mantuvo intacto y fuera del trabajo de documentación.
- La validación de cierre `gradlew testDebugUnitTest` finalizó correctamente (`BUILD SUCCESSFUL`).

## 3. Inventario de las 17 hojas

Los tamaños indicados son filas de contenido efectivamente pobladas, no la capacidad máxima de la cuadrícula de Google Sheets.

| # | Hoja | Contenido efectivo | Finalidad y estructura | Dependencias |
| ---: | --- | ---: | --- | --- |
| 1 | `Banco_actividades` | 203 actividades | 14 columnas: `ID`, categoría, actividad, instrucción, término ARASAAC, edad, autonomía, supervisión, modos, recurso visual, estado, observaciones y contextos | Fuente maestra ACT |
| 2 | `Cobertura` | 17 categorías + total | Conteos de actividades por `Contextos_uso` y estado de cobertura | Derivada de actividades |
| 3 | `Criterios` | 67 criterios/notas | Reglas de normalización, edades, ARASAAC, cierre de bancos y decisiones históricas | Describe los cuatro bancos y las hojas de diseño |
| 4 | `Catalogos` | 17 filas pobladas | Valores controlados de edad, autonomía, supervisión, modos, recurso/estado y catálogo de 17 categorías | Valida actividades y rutinas |
| 5 | `Banco_rutinas` | 52 rutinas | 16 columnas; secuencia simple, completa y opcionales se guardan como cadenas de IDs ACT | Depende de actividades |
| 6 | `Cobertura_rutinas` | 17 áreas + total | Rutinas y ACT distintos por área | Derivada de rutinas |
| 7 | `Banco_logros_retos` | 60 elementos | 17 columnas; tipo, nivel, edad, referencias ACT/RUT, condición, ventana, repetibilidad y seguridad | Depende de actividades y rutinas |
| 8 | `Cobertura_gamificacion` | 17 categorías + total | Cobertura por logros/retos y exclusiones deliberadas | Derivada de logros/retos |
| 9 | `Banco_mensajes_refuerzo` | 54 mensajes | 17 columnas; evento, categoría, tono, edad, nivel, mensaje, canal, repetición, seguridad, frecuencia y TTS | Depende de eventos ACT/RUT/LOG/RET |
| 10 | `Cobertura_mensajes` | 18 eventos + total | Tres variantes de tono/edad por evento | Derivada de mensajes |
| 11 | `Auditoria_integral_bancos` | 31 filas pobladas | Resumen de los cuatro bancos, controles cruzados, hallazgos y dictamen | Cruza los cuatro bancos |
| 12 | `Especificacion_funcional_V1` | 39 requisitos | Comportamientos V1, prioridad, modo, dependencia y aceptación | Consume los cuatro bancos |
| 13 | `Arquitectura_informacion_V1` | 25 pantallas + 14 rutas | Jerarquía, acceso, visibilidad Simple/Avanzado, navegación y salvaguardas | Depende de requisitos |
| 14 | `Wireframes_funcionales_V1` | 25 pantallas | Zonas, acciones, estados, roles, accesibilidad y aceptación | Depende de arquitectura de información |
| 15 | `Sistema_visual_UI_V1` | 48 tokens/reglas | Temas, color, tipografía, espaciado, visuales, estados y accesibilidad | Base visual |
| 16 | `Componentes_UI_V1` | 40 componentes | Anatomía, estados, modos, accesibilidad, seguridad y uso | Depende del sistema visual |
| 17 | `Especificacion_visual_25_V1` | 25 pantallas | Composición concreta, componentes, medidas, jerarquía y estados | Cruza wireframes, tokens y componentes |

### Tipos y obligatoriedad

- Los identificadores son cadenas estables con prefijos: `ACT-`, `RUT-`, `LOG-`, `RET-`, `MSG-`.
- Conteos, orden y ventanas son enteros representados como celdas numéricas o texto numérico.
- Banderas y selecciones usan vocabularios controlados en español (`Sí`/`No`, estados y niveles).
- Las secuencias y relaciones múltiples se codifican hoy como texto delimitado (`>`, `|`). Deben normalizarse a filas de relación durante la importación a Room.
- En actividades, `Observaciones` es opcional (113 vacías); las otras 13 columnas están completas.
- En rutinas, `Opcionales_ACT` y `Observaciones` son opcionales (42 vacías cada una); las otras 14 columnas están completas.
- Los bancos de logros/retos y mensajes no tienen campos vacíos.
- Las filas separadoras de hojas documentales no son registros incompletos.

## 4. Auditoría de actividades

### Integridad

- 203 registros; 203 IDs únicos; 203 nombres únicos.
- IDs entre `ACT-001` y `ACT-211`. Faltan por depuración histórica `ACT-042`, `ACT-064`, `ACT-124`, `ACT-125`, `ACT-126`, `ACT-174`, `ACT-177` y `ACT-178`. No deben renumerarse: ya existen referencias estables.
- 0 campos obligatorios vacíos.
- 1 instrucción repetida (`Pide ayuda si la necesitas.`) en dos actividades distintas; no es un ID duplicado y puede ser una coincidencia funcional válida.
- 11 términos de búsqueda se reutilizan: `esperar` (4), `descansar` (3), y nueve grupos de dos: `botella agua`, `cerrar puerta`, `comprar`, `elegir`, `emociones`, `hacer deberes`, `leer`, `pedir ayuda` y `respirar`. Esto refuerza que el término no identifica por sí solo un pictograma.
- Todos los registros son visibles en Simple y Avanzado. La diferencia entre modos debe estar en la interfaz y las opciones, no en duplicar el catálogo.

### Distribución principal

| Categoría principal | Actividades |
| --- | ---: |
| Higiene personal | 18 |
| Alimentación | 15 |
| Autonomía adolescente | 14 |
| Hogar y responsabilidades | 13 |
| Ocio y tiempo libre | 13 |
| Estudio y tareas | 12 |
| Interacción social | 12 |
| Preparación para estudiar | 12 |
| Salidas y comunidad | 12 |
| Vestido y presentación | 12 |
| Autorregulación y emociones | 11 |
| Cambios y situaciones especiales | 11 |
| Rutina de tarde y noche | 11 |
| Inicio del día | 10 |
| Salud y bienestar | 10 |
| Baño e intimidad | 9 |
| Actividad física y movimiento | 8 |

`Contextos_uso` suma 216 apariciones porque una actividad puede pertenecer a varios contextos. Los 17 conteos de `Cobertura` coinciden con el recálculo directo.

### Estado visual real

| Evidencia visual | Cantidad | Interpretación |
| --- | ---: | --- |
| Política genérica `Pictograma ARASAAC o foto propia` | 203 | Declara fuentes permitidas, no un recurso concreto |
| Término de búsqueda ARASAAC | 203 | Punto de partida para curación |
| Estado `Validado con pictograma` | 76 | Correspondencia documentada, pero sin ID persistido |
| Estado `Revisado` | 127 | Requiere selección o decisión contextual |
| ID ARASAAC explícito | 0 | Bloquea una importación visual reproducible |
| URI/archivo de imagen propia | 0 | No hay fotografía asignada en la fuente |
| Registro explícito “sin visual” | 0 | El fallback todavía no está modelado como dato |

Conclusión: no se puede reconstruir de manera determinista qué pictograma fue “validado” para las 76 actividades. Antes de importar imágenes debe añadirse una tabla de mapeo versionada, sin necesidad de modificar las filas maestras originales si se prefiere una hoja o archivo de curación separado.

## 5. Auditoría de rutinas

### Integridad y relaciones

- 52 rutinas; 52 IDs y nombres únicos.
- 29 rutinas `General`, 19 `Escolar+` y 4 `Adolescente`.
- Las 17 áreas tienen al menos 3 rutinas; `Cambios y situaciones especiales` tiene 4.
- Las 52 son personalizables y están en estado `Validado banco v1.0`.
- 490 apariciones de referencias ACT entre secuencias simples, completas y opcionales.
- 203/203 actividades aparecen al menos una vez.
- 0 referencias ACT inexistentes.
- 0 discrepancias entre `N_pasos_*` y el número de IDs de sus secuencias.
- 0 IDs repetidos dentro de una misma secuencia.
- 0 incompatibilidades de edad en las secuencias simples.

Las secuencias completas contienen 14 apariciones de actividades con perfil etario superior y los opcionales contienen 8. Esto está documentado como comportamiento deliberado: la secuencia simple define el mínimo, mientras la completa es una plantilla máxima que debe filtrarse al personalizar. La futura implementación debe convertir esa regla en validación ejecutable, no depender de que el configurador la recuerde.

La reutilización es real: por ejemplo, `ACT-044` aparece 9 veces, `ACT-034`, `ACT-127` y `ACT-138` aparecen 8 veces, y `ACT-011` y `ACT-160` aparecen 7 veces. No se deben copiar actividades dentro de cada rutina.

### Limitaciones del esquema actual

- Las relaciones y el orden viven en cadenas delimitadas. Son correctas en la fuente, pero no permiten claves foráneas ni consultas eficientes en Android.
- No existe una duración estructurada de rutina o paso; `Momento_sugerido` es texto descriptivo.
- La relación con perfiles es solo una etiqueta `Grupo_edad`; no hay IDs de perfil ni asignaciones por usuario.
- Simple y completa son variantes alternativas. Nunca deben concatenarse automáticamente.

## 6. Auditoría de logros, retos y mensajes

### Logros y retos

- 60 registros: 40 logros y 20 retos.
- 24 de nivel Ligera, 16 Intermedia y 20 Completa.
- 52 registros referencian RUT y 8 referencian ACT; todas las referencias existen.
- 0 IDs o nombres duplicados y 0 campos vacíos.
- Los 60 tienen `Penaliza_si_no = No`; 46 no son repetibles y 14 sí.
- Alimentación, baño/intimidad y salud/bienestar quedan excluidos deliberadamente de gamificación directa.

### Mensajes

- 54 mensajes; 54 IDs y textos únicos; 0 campos vacíos.
- 18 eventos con tres variantes cada uno.
- 18 mensajes Neutral/General, 18 Cálido/Escolar+ y 18 Motivador sobrio/Adolescente.
- 33 funcionan sin gamificación y 21 la requieren.
- 41 son cortos y 13 de longitud media.
- Los 54 respetan TTS `Según configuración del usuario`.

## 7. Auditoría de ARASAAC

### API observada el 24-09-2026

El repositorio público oficial es [Arasaac/public-api](https://github.com/Arasaac/public-api). Los siguientes endpoints `v1` respondieron correctamente durante la auditoría:

| Necesidad | Endpoint |
| --- | --- |
| Búsqueda amplia | `GET https://api.arasaac.org/v1/pictograms/es/search/{texto}` |
| Mejor coincidencia | `GET https://api.arasaac.org/v1/pictograms/es/bestsearch/{texto}` |
| Metadatos por ID e idioma | `GET https://api.arasaac.org/v1/pictograms/es/{id}` |
| Imagen seleccionada | `GET https://static.arasaac.org/pictograms/{id}/{id}_{resolucion}.png` |

Comprobaciones técnicas:

- Idioma: `es`.
- Búsqueda y detalle respondieron JSON sin credenciales durante la auditoría.
- Las imágenes `300`, `500` y `2500` respondieron como `image/png`.
- El JSON devuelve `_id`, palabras clave, categorías, etiquetas, variantes de piel/cabello y otros metadatos.
- Existen variantes de nombre de archivo como `nocolor`, plural, tiempo verbal y rasgos físicos. Deben guardarse como una elección explícita, no generarse de forma implícita.
- No se encontró en la documentación pública revisada un SLA ni una cuota/rate limit contractual. La integración debe usar debounce, cancelación de búsquedas, reintento limitado, caché y manejo de `429`/errores 5xx sin asumir disponibilidad ilimitada.

Una búsqueda de varias palabras puede devolver coincidencias parciales. Por eso, el término del banco es una consulta, no una asignación. La decisión debe mostrar miniatura, palabra clave y contexto; el configurador confirma el ID.

### Licencia y atribución

Las [condiciones oficiales](https://aulaabierta.arasaac.org/condiciones-de-uso) y la [guía oficial de atribución](https://aulaabierta.arasaac.org/senalizacion-de-espacios-publicos-y-servicios-con-pictogramas-de-arasaac) indican licencia **Creative Commons BY-NC-SA**, atribución a Sergio Palao/ARASAAC y propiedad del Gobierno de Aragón, con restricción no comercial y obligación de compartir igual en las obras derivadas alcanzadas por la licencia.

Texto de atribución propuesto para la app y las exportaciones que contengan pictogramas:

> Pictogramas: Sergio Palao para ARASAAC (https://arasaac.org). Propiedad: Gobierno de Aragón. Licencia: CC BY-NC-SA.

**Decisión del proyecto registrada en la Fase 2.1.1:** RutinaGo se distribuirá de forma completamente gratuita y sin fines de lucro, sin publicidad, compras integradas, suscripciones ni funciones premium. No se desarrollará como producto comercial de NeuroNova Apps. Esta decisión corresponde exclusivamente a RutinaGo y cierra el riesgo hipotético asociado a una posible monetización.

La decisión no elimina las obligaciones de licencia: se debe reconocer la autoría, identificar la fuente, respetar la restricción no comercial, cumplir las condiciones de compartir igual cuando correspondan y revisar las condiciones oficiales antes de distribuir pictogramas. No se ha solicitado ni recibido una autorización comercial individual de ARASAAC. Esta auditoría técnica no sustituye asesoría jurídica.

## 8. Arquitectura de imágenes y uso offline

### Modelo de fuentes

`VisualAsset` debe representar cuatro fuentes sin acoplar la UI a ARASAAC:

1. `ARASAAC`: ID, variante, resolución, URL original, archivo local, licencia y atribución.
2. `USER_PHOTO`: URI/origen, copia local privada, MIME, orientación y texto alternativo.
3. `LOCAL_BUNDLED`: recurso incorporado y autorizado, si se decide distribuir un conjunto mínimo.
4. `PLACEHOLDER`: recurso vectorial/local y etiqueta textual cuando no exista imagen.

La selección debe guardarse aparte de la actividad para permitir preferencias por perfil. Prioridad recomendada por defecto: elección explícita del usuario (foto o pictograma) > visual predeterminado del catálogo > placeholder. No se debe imponer siempre “foto sobre pictograma”; la preferencia es un dato configurable.

### Flujo ARASAAC

1. Buscar por `Termino_busqueda_ARASAAC` en `es`, con texto editable.
2. Mostrar candidatos y metadatos; nunca seleccionar solo por posición o score.
3. Confirmar manualmente `_id`, variante y texto alternativo.
4. Persistir primero la selección.
5. Descargar únicamente el PNG elegido a un archivo temporal.
6. Verificar MIME/tamaño y mover atómicamente a almacenamiento interno.
7. Registrar hash, ruta, fecha y estado `AVAILABLE`.
8. Si falla, conservar metadatos y usar placeholder; reintentar solo con red disponible.

### Garantía offline

- Room almacena metadatos; los bytes viven en `filesDir/visuals/...`, no dentro de la base.
- La caché de Coil puede acelerar, pero es evictable y no garantiza una rutina preparada sin conexión.
- Al guardar o programar una rutina, un verificador comprueba que todos sus visuales estén disponibles localmente.
- WorkManager puede completar descargas seleccionadas con restricción de red, sin descargar el catálogo completo.
- Las fotos del usuario deben copiarse al almacenamiento privado de la app o mantener permiso persistente SAF; copiar es más robusto frente a pérdida del permiso/origen.
- Borrar o reemplazar un visual debe usar conteo de referencias para no eliminar un archivo compartido por otras actividades/rutinas.
- Copias de seguridad deben excluir o tratar cuidadosamente fotos personales y respetar las reglas Android existentes.

## 9. Auditoría de la arquitectura Android existente

### Estado real

| Área | Estado actual |
| --- | --- |
| Modelos | `ActivityStep`, `Routine`, `ProgressRecord` y enums mínimos, orientados al prototipo |
| Navegación | Enum y estado `rememberSaveable`; no usa Navigation Compose ni ViewModel |
| Perfiles | Persistencia real en Preferences DataStore: crear, renombrar, seleccionar y recuperar perfil activo |
| Preferencias | Persistencia real de Día/Noche/Sistema, escala de texto, alto contraste y Simple/Avanzado |
| Hoy | `DemoContent`; el perfil activo solo se muestra como contexto |
| Rutinas | `DemoContent`; búsqueda/filtros en memoria |
| Crear | Estado temporal `rememberSaveable`; “preparar” no guarda una rutina |
| Ejecución | Estado temporal por composición; no crea ejecución persistida |
| Progreso | Historial demostrativo fijo |
| Visuales | Placeholders de texto; no existe modelo de fuente visual ni carga de imágenes |
| Datos estructurados | Sin Room, DAOs, repositorios de catálogo, importador ni migraciones |
| Red | Sin cliente HTTP y sin implementación ARASAAC |

El perfil activo no filtra ni posee rutinas. Todas las pantallas ven el mismo `DemoContent`; únicamente reciben el nombre del perfil en Hoy/Progreso/configuración.

### Qué conservar

- `UserPreferencesRepository`, el nombre del DataStore y sus claves, para no perder datos de Fase 1.
- Los IDs UUID ya generados para perfiles.
- `ThemeMode`, `TextScaleMode`, `AppMode` y la infraestructura de tema.
- Las pantallas y componentes Compose como capa de presentación, sustituyendo gradualmente `DemoContent` por flujos de repositorio.
- `RoutineOutcome` y `filterFor` como conceptos, adaptados a modelos de lectura reales.
- La navegación actual durante la introducción del data layer; migrarla no es requisito para importar el banco.

### Qué debe evolucionar

- `RoutineCategory` actual describe momentos (mañana/tarde/noche), no las 17 áreas maestras. Debe separarse `CatalogCategory` de programación temporal.
- `visualLabel` debe convertirse en un modelo visual tipado.
- `Routine.steps` embebido debe mapearse desde relaciones ordenadas.
- Crear/Ejecución/Progreso necesitan ViewModels y repositorios para sobrevivir al proceso y persistir resultados.
- El contenido demo debe quedar detrás de una implementación de prueba o eliminarse cuando la migración de cada pantalla esté validada.

## 10. Propuesta de persistencia Room

Separar **catálogo versionado** de **datos del usuario** evita que una actualización del banco sobrescriba personalizaciones.

### Entidades de catálogo

- `CatalogCategoryEntity(id, name, sortOrder)`.
- `CatalogActivityEntity(id, categoryId, name, instruction, arasaacSearchTerm, ageGroup, autonomy, supervision, simpleEnabled, advancedEnabled, status, notes, catalogVersion)`.
- `ActivityContextCrossRef(activityId, categoryId)`.
- `CatalogRoutineEntity(id, name, areaId, context, ageGroup, objective, suggestedMoment, supervision, status, notes, catalogVersion)`.
- `CatalogRoutineStepEntity(routineId, variant, position, activityId)`, donde `variant` es `SIMPLE`, `COMPLETE` u `OPTIONAL`.
- Entidades equivalentes para logros/retos y mensajes cuando entren en alcance.

### Entidades del usuario

- `UserRoutineEntity(id UUID, profileId, sourceCatalogRoutineId?, name, schedule, createdAt, updatedAt, archived)`.
- `UserRoutineStepEntity(userRoutineId, position, catalogActivityId?, customActivityId?, instructionOverride?, visualSelectionId?)`.
- `CustomActivityEntity(id UUID, profileId, name, instruction, ...)` para contenido creado por el usuario sin contaminar el catálogo.
- `ProfileRoutineAssignmentEntity(profileId, userRoutineId, enabled, schedule...)` si se necesita compartir una rutina entre perfiles.
- `RoutineExecutionEntity(id, profileId, userRoutineId, startedAt, endedAt, outcome)`.
- `ExecutionStepEntity(executionId, position, activitySnapshotId/name, outcome, completedAt)`; el snapshot mínimo conserva el significado histórico aunque cambie la rutina.
- `VisualAssetEntity` y `ActivityVisualSelectionEntity` para fuente, archivo y preferencia por catálogo/perfil.

### Catálogo e importación

- Distribuir un manifiesto versionado (JSON/CSV validado o base preempaquetada) generado desde la fuente autorizada.
- Importar en una transacción idempotente con versión y checksum.
- Validar IDs, referencias, vocabularios, posiciones, conteos y cobertura antes de publicar el artefacto.
- Actualizar filas de catálogo por ID sin tocar `UserRoutine*`, ejecuciones ni selecciones del usuario.
- No usar los 369 registros en código Kotlin generado ni duplicarlos por modo.

### Conservación de DataStore

Primera implantación recomendada:

- DataStore continúa almacenando perfiles y preferencias.
- Room guarda `profileId` como cadena estable sin clave foránea física entre almacenes.
- Un repositorio de sesión expone perfil activo y consulta Room por ese ID.
- Al eliminar perfiles en una fase futura se define antes una política de reasignación/archivo.

Si más adelante los perfiles requieren relaciones complejas, realizar una migración idempotente: leer DataStore, insertar conservando exactamente los UUID, verificar conteos/activo y solo entonces cambiar la fuente de verdad. Mantener lectura de compatibilidad durante al menos una versión. No borrar claves antiguas en el mismo lanzamiento.

## 11. Simple, Avanzado y perfiles

Ambos modos deben consultar las mismas tablas y repositorios.

- **Simple:** secuencia `SIMPLE`, visual principal grande, búsqueda opcional mínima, valores predeterminados y menos controles simultáneos.
- **Avanzado:** permite elegir `COMPLETE`/opcionales, filtrar las 17 áreas, buscar las 203 actividades, configurar duración/recordatorio/ayudas y seleccionar visual.
- Cambiar de modo no crea copias ni elimina configuración avanzada guardada.
- `ageGroup` es un nivel mínimo de contenido, no capacidad o diagnóstico.
- El perfil actual solo tiene ID y nombre. Añadir edad/contenido, autonomía y supervisión requiere campos nuevos con valor nulo/predeterminado y confirmación del configurador; no se deben inferir a partir del nombre.

## 12. Accesibilidad

- Mantener siempre nombre e instrucción textual junto al visual; la imagen nunca es el único canal.
- Cada visual debe tener descripción semántica contextual, no solo la palabra “pictograma”. Para imágenes decorativas, excluirlas de TalkBack y dejar que el texto de la tarjeta describa la acción.
- En alto contraste, conservar los colores originales del pictograma sobre una superficie neutra con borde/espaciado de alto contraste. No invertir, recolorear ni aplicar filtros automáticos que alteren significado.
- Una variante ARASAAC `nocolor` solo se usa si fue seleccionada y validada como recurso, no como transformación automática del tema.
- Mantener objetivos táctiles de al menos 48 dp, foco lógico, regiones vivas solo para cambios relevantes y soporte para texto Normal/Grande/Muy grande.
- Probar Día, Noche, Sistema, alto contraste, 1.00/1.12/1.24, TalkBack y ausencia de imagen/red.

## 13. Riesgos

| Prioridad | Riesgo | Mitigación |
| --- | --- | --- |
| Alta | 76 validaciones sin ID ARASAAC reproducible | Crear mapeo `ACT -> arasaacId/variante` revisado por persona |
| Alta | Omitir atribución, fuente, restricción NC o compartir igual al distribuir recursos ARASAAC | Modelo no comercial documentado, atribución visible y revisión previa a cada distribución |
| Alta | Confiar solo en caché evictable rompe el uso offline | Almacenamiento interno administrado + verificador de disponibilidad |
| Alta | Sobrescribir personalizaciones al actualizar catálogo | Tablas separadas y actualización por versión/ID |
| Media | Resultado textual semánticamente incorrecto | Selección humana con contexto y previsualización |
| Media | Perder perfiles al moverlos prematuramente a Room | Mantener DataStore; migración posterior idempotente |
| Media | Cadenas de pasos difíciles de consultar | Normalizar a `RoutineStep` con posición y FK |
| Media | Fotos privadas incluidas en backup o compartidas | Almacenamiento privado, política de backup y permisos SAF |
| Media | API sin SLA/cuotas públicas claras | Caché, debounce, backoff y funcionamiento local-first |
| Baja | Saltos en numeración ACT interpretados como error | Tratar IDs como cadenas opacas; nunca renumerar |

## 14. Decisiones pendientes

**Decisión cerrada en la Fase 2.1.1:** el modelo de distribución de RutinaGo es gratuito y sin fines de lucro, sin publicidad, compras integradas, suscripciones ni funciones premium. Las demás aplicaciones de NeuroNova Apps quedan fuera de esta decisión.

1. Quién realiza y aprueba la curación de los 127 registros pendientes y la captura de IDs para los 76 ya “validados”.
2. Resolución local estándar (recomendación inicial: 500 px; 300 px puede bastar tras medición real de densidad/tamaño).
3. Si la selección visual es global por actividad, por perfil o ambas; se recomienda valor global con override por perfil.
4. Política de copias de seguridad y eliminación de fotografías.
5. Formato del artefacto de importación y proceso para regenerarlo desde Sheets.
6. Campos avanzados exactos de programación y finalización antes de fijar el esquema Room.

## 15. Orden recomendado de implementación

1. **Contrato y validador del banco:** exportación reproducible, esquema, vocabularios, relaciones y checksum; todavía sin importar a la app.
2. **Curación visual:** tabla separada con `activityId`, `arasaacId`, variante, descripción accesible, estado de revisión y atribución.
3. **Room del catálogo:** categorías, actividades, rutinas y pasos normalizados; importador idempotente y pruebas de los conteos 203/52.
4. **Room del usuario:** rutinas personalizadas, asignación por `profileId`, ejecución y progreso.
5. **Abstracción visual:** `VisualAsset`, fotos, placeholder y selección por perfil.
6. **Cliente ARASAAC:** búsqueda manual, detalle, descarga individual, errores y licencia; no sincronización masiva.
7. **Offline:** almacenamiento administrado, WorkManager, verificador de rutinas y limpieza segura por referencias.
8. **Integración UI incremental:** Rutinas/Detalle/Crear, luego Hoy/Ejecución/Progreso; conservar componentes Compose.
9. **Pruebas:** migración DataStore intacta, integridad Room, offline, accesibilidad y licencias/atribución.
10. **Solo después:** retirar `DemoContent` de producción y considerar la Fase 2.2.

## Anexo A. Actividades que requieren selección o decisión visual (127)

Estas son exactamente las filas con `Estado = Revisado`. “Requiere selección” puede terminar en pictograma, fotografía o fallback deliberado; no implica que siempre exista un pictograma único adecuado.

- **Inicio del día (8):** ACT-002 Apagar la alarma; ACT-003 Sentarse en la cama; ACT-004 Levantarse de la cama; ACT-005 Abrir las cortinas; ACT-007 Guardar el pijama; ACT-008 Ir al baño; ACT-009 Revisar la agenda del día; ACT-010 Elegir la ropa.
- **Higiene personal (12):** ACT-012 Mojarse las manos; ACT-013 Usar jabón; ACT-014 Frotarse las manos; ACT-015 Enjuagarse las manos; ACT-016 Secarse las manos; ACT-018 Poner pasta dental; ACT-019 Enjuagarse la boca; ACT-020 Lavarse la cara; ACT-021 Secarse la cara; ACT-023 Usar desodorante; ACT-026 Secarse el cuerpo; ACT-027 Cortarse las uñas con ayuda.
- **Vestido y presentación (8):** ACT-029 Ponerse la ropa interior; ACT-031 Ponerse el pantalón; ACT-032 Ponerse el vestido; ACT-033 Ponerse las medias; ACT-034 Ponerse los zapatos; ACT-038 Ponerse una casaca; ACT-039 Ponerse gorra o sombrero; ACT-040 Revisar que la ropa esté bien puesta.
- **Alimentación (8):** ACT-041 Sentarse a la mesa; ACT-043 Servir agua; ACT-051 Usar servilleta; ACT-052 Masticar despacio; ACT-053 Esperar antes de levantarse; ACT-054 Llevar el plato; ACT-055 Guardar los alimentos; ACT-056 Limpiar la mesa.
- **Baño e intimidad (4):** ACT-057 Entrar al baño; ACT-058 Cerrar la puerta; ACT-065 Pedir ayuda si la necesita; ACT-211 Dejar el baño ordenado.
- **Preparación para estudiar (11):** ACT-067 Guardar los cuadernos; ACT-068 Guardar los útiles; ACT-069 Revisar el horario escolar; ACT-070 Llevar la lonchera; ACT-071 Llevar una botella de agua; ACT-072 Salir de casa; ACT-073 Esperar el transporte; ACT-074 Entrar al aula; ACT-075 Guardar la mochila; ACT-076 Sacar el material; ACT-077 Sentarse en el lugar.
- **Estudio y tareas (10):** ACT-078 Revisar las tareas; ACT-079 Elegir una tarea; ACT-080 Preparar el espacio de estudio; ACT-081 Guardar el celular; ACT-083 Resolver una actividad; ACT-085 Tomar un descanso breve; ACT-086 Retomar la tarea; ACT-087 Revisar el trabajo; ACT-088 Guardar la tarea terminada; ACT-089 Ordenar el espacio de estudio.
- **Hogar y responsabilidades (9):** ACT-091 Ordenar la habitación; ACT-092 Guardar la ropa; ACT-093 Poner la ropa sucia en el cesto; ACT-094 Doblar ropa; ACT-096 Recoger la mesa; ACT-098 Limpiar una superficie; ACT-099 Sacar la basura con supervisión; ACT-101 Dar de comer a la mascota; ACT-102 Guardar objetos en su lugar.
- **Ocio y tiempo libre (9):** ACT-103 Elegir una actividad de ocio; ACT-106 Colorear; ACT-109 Ver televisión; ACT-110 Usar la tablet; ACT-111 Usar el celular; ACT-112 Jugar un videojuego; ACT-113 Hacer una manualidad; ACT-114 Salir al parque; ACT-115 Guardar el material de ocio.
- **Actividad física y movimiento (4):** ACT-116 Ponerse ropa deportiva; ACT-117 Preparar agua; ACT-122 Hacer estiramientos; ACT-123 Practicar un deporte.
- **Salidas y comunidad (9):** ACT-127 Prepararse para salir; ACT-128 Revisar lo que debe llevar; ACT-129 Cerrar la puerta de casa con un adulto; ACT-130 Caminar por la vereda; ACT-131 Esperar para cruzar; ACT-133 Subir al transporte; ACT-134 Sentarse en el transporte; ACT-135 Bajar del transporte; ACT-138 Volver a casa.
- **Salud y bienestar (6):** ACT-141 Descansar; ACT-142 Tomar la temperatura con un adulto; ACT-143 Tomar medicación indicada con supervisión; ACT-144 Ir a una cita de salud; ACT-145 Esperar en la sala; ACT-146 Seguir una indicación del profesional.
- **Autorregulación y emociones (3):** ACT-152 Contar lentamente; ACT-153 Ir a un lugar tranquilo; ACT-157 Volver a la actividad.
- **Interacción social (3):** ACT-167 Pedir prestado; ACT-168 Devolver un objeto; ACT-170 Pedir espacio.
- **Rutina de tarde y noche (6):** ACT-172 Llegar a casa; ACT-173 Cambiarse de ropa; ACT-176 Preparar la ropa del día siguiente; ACT-180 Ir al dormitorio; ACT-181 Elegir un cuento o lectura; ACT-182 Apagar pantallas.
- **Cambios y situaciones especiales (8):** ACT-186 Mirar un cambio en la agenda; ACT-187 Aceptar una actividad diferente; ACT-191 Prepararse para una visita; ACT-192 Prepararse para un viaje; ACT-193 Hacer una maleta con apoyo; ACT-194 Esperar en un lugar nuevo; ACT-195 Pedir información; ACT-196 Usar una actividad de transición.
- **Autonomía adolescente (9):** ACT-197 Revisar el calendario; ACT-198 Preparar documentos personales; ACT-199 Cargar el celular; ACT-200 Revisar el saldo o pasaje; ACT-201 Preparar dinero para una compra; ACT-205 Guardar el comprobante; ACT-208 Organizar una cita o actividad; ACT-209 Enviar un mensaje acordado; ACT-210 Revisar la hora de salida.

## Anexo B. Limitaciones de la auditoría

- Se auditó la fuente viva el 24-09-2026; cambios posteriores en Sheets pueden alterar los resultados.
- No se hizo evaluación clínica/terapéutica de la idoneidad de cada pictograma.
- No se asignaron ni descargaron archivos de pictogramas al proyecto.
- La API pública se verificó por llamadas de lectura y repositorio oficial, pero no se hizo prueba de carga ni existe SLA público identificado.
- El análisis de licencia es técnico y documental, no asesoría jurídica.
