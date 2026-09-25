# RutinaGo — Fase 2.4: integración del catálogo maestro en la interfaz

Fecha: 25 de septiembre de 2026

## Resultado

Las pantallas **Rutinas**, **Detalle de rutina** y **Crear** consumen el catálogo maestro 1.0.0 importado en la base Room `rutinago_catalog.db`. La interfaz ya no usa `DemoContent` como fuente de rutinas o actividades en esos recorridos y presenta:

| Contenido | Registros |
| --- | ---: |
| Categorías funcionales | 17 |
| Actividades | 203 |
| Rutinas maestras | 52 |
| Pasos normalizados | 490 |

`DemoContent` se conserva únicamente en las áreas heredadas que quedan fuera de esta fase: **Hoy**, **Ahora / Después**, ejecución y progreso demostrativo. Ninguna ejecución demo se asocia a un ID `RUT-###` real.

## Fuente de datos y arquitectura

La fuente editorial continúa siendo el Google Sheet autorizado descrito en las fases 2.1 y 2.2. Los JSON validados y empaquetados en `app/src/main/assets/master_catalog/v1` se importan mediante `MasterCatalogImporter` antes de habilitar el contenido.

La integración reutiliza, sin duplicarla:

- la única instancia de `RutinaGoDatabase`;
- `CatalogReadDao` y `RoomCatalogRepository`;
- los IDs estables `ACT-###` y `RUT-###`;
- `CatalogImportStateRepository` para conocer el resultado real de la importación.

Se añadieron consultas `Flow` globales, ordenadas editorialmente, para categorías, actividades, rutinas y pasos. `CatalogUiState` combina esas consultas con el estado de importación y entrega una instantánea de solo lectura a Compose.

No existen listas Kotlin que reproduzcan los 203 o 52 registros.

## Estados Loading, Ready y Error

- **Loading**: se presenta mientras todavía no hay metadatos de importación o se está instalando una versión empaquetada más reciente.
- **Ready**: solo se entrega cuando el intento registrado es exitoso y la lectura contiene exactamente 17 categorías, 203 actividades, 52 rutinas y 490 pasos.
- **Error**: se muestra ante una importación fallida, un contrato incompatible, una lectura Room fallida o conteos incompletos.

Una falla no se sustituye silenciosamente por contenido demo ni se representa como catálogo vacío.

## Pantalla Rutinas

Rutinas muestra las 52 plantillas maestras. Cada tarjeta conserva internamente su ID real y presenta nombre, categoría funcional, objetivo, momento sugerido y cantidad de pasos correspondiente al modo activo. Los identificadores técnicos no se usan como título visible.

En modo Avanzado están disponibles la búsqueda por nombre, objetivo, contexto o actividad, los filtros por las 17 categorías reales y la acción para usar una rutina como plantilla. Esos filtros no reutilizan los momentos mañana/tarde/noche.

En modo Simple se presenta el mismo catálogo completo con menos controles y el conteo de la secuencia `SIMPLE`.

## Detalle de rutina

La navegación guarda y entrega el ID `RUT-###` seleccionado. El detalle resuelve ese ID dentro de la instantánea Room y muestra:

- nombre y categoría;
- objetivo funcional;
- contexto y supervisión;
- momento sugerido;
- actividades con nombre, instrucción y orden reales.

Simple consulta exclusivamente la variante `SIMPLE`. Avanzado consulta `COMPLETE` y representa `OPTIONAL` en una sección independiente; las variantes no se concatenan.

Cada elemento conserva su referencia `ACT-###` en el modelo. El ID no se expone como texto principal.

## Recursos visuales

Las 203 actividades continúan sin un pictograma ARASAAC validado. La interfaz usa un placeholder accesible rotulado como visual pendiente. No realiza búsquedas, no descarga archivos, no selecciona resultados y no presenta un recurso provisional como aprobado.

## Crear desde una plantilla

El recorrido es:

    Crear → Elegir origen → Usar una plantilla → Seleccionar plantilla → Ver pasos

La lista usa las 52 rutinas Room. En Avanzado admite búsqueda y categoría; en Simple mantiene la selección directa. Al abrir una plantilla se conserva su ID real, se presenta la secuencia correspondiente al modo y, en Avanzado, se separan los opcionales.

La acción **Preparar borrador con esta plantilla** solo mantiene una previsualización en memoria. La interfaz informa que todavía no existe persistencia de rutinas personales.

Desde el detalle de Rutinas también se puede llegar al mismo flujo conservando el ID seleccionado.

## Crear desde cero

El recorrido conserva el formulario de nombre, la selección de actividades, la confirmación al abandonar cambios y los botones Atrás de sistema e interfaz. La selección consulta las 203 actividades Room mediante una lista perezosa.

Avanzado incorpora búsqueda, categorías funcionales y una previsualización del orden seleccionado. Simple reduce controles sin crear otro banco ni ocultar registros arbitrariamente.

**Preparar borrador** mantiene nombre y referencias ACT en memoria. No se muestra una confirmación de guardado permanente ni se ofrecen programación o recordatorios que aparenten estar persistidos.

## Navegación

Se conservaron los recorridos:

- Rutinas → rutina real → detalle → Atrás → Rutinas.
- Crear → Elegir origen → plantilla → detalle de plantilla.
- Crear → Elegir origen → Crear desde cero.

El botón Atrás del sistema recorre las páginas internas de Crear. Un borrador modificado solicita confirmación antes de descartarse. La navegación inferior inicia un flujo nuevo de Crear; una entrada desde una plantilla entrega de forma explícita el ID seleccionado.

## Modos y accesibilidad

El cambio entre Simple y Avanzado selecciona una variante de pasos sobre la misma instantánea; no inserta ni duplica registros.

La implementación conserva los temas Día, Noche y Sistema, alto contraste y las tres escalas de texto. Las listas son desplazables, los encabezados conservan semántica, las acciones tienen texto completo y los placeholders anuncian que el visual está pendiente. No se aplica ningún filtro gráfico a futuros pictogramas.

Perfiles y preferencias continúan en DataStore con sus identificadores existentes. Las plantillas maestras no se registran como rutinas personales.

## Pruebas

Cobertura añadida o ampliada:

- consultas Room globales con 17/203/52/490;
- orden de `SIMPLE` y `COMPLETE`;
- separación de `OPTIONAL`;
- resolución exclusiva de referencias ACT existentes;
- búsqueda por contenido maestro;
- separación entre categoría funcional y momento sugerido;
- selección de variante por modo sin duplicar registros;
- estado de error explícito ante importación fallida;
- estado Ready únicamente con los conteos autorizados;
- conservación de las pruebas de DataStore, perfiles y navegación existentes.

Comandos de validación de la fase:

    .\gradlew.bat testDebugUnitTest
    .\gradlew.bat assembleDebug
    .\gradlew.bat lintDebug
    .\gradlew.bat assembleDebugAndroidTest
    python -B tools/master_bank/validate_master_bank.py
    python -B -m unittest discover -s tools/master_bank/tests -v

No se detectó un dispositivo Android ni un AVD configurado en la estación, por lo que la ejecución instrumentada queda pendiente; el APK de pruebas sí se compila.

## Limitaciones y próxima implementación

- No se descargan ni asignan pictogramas ARASAAC.
- No se guardan rutinas personalizadas en Room.
- No se programan recordatorios ni notificaciones.
- Gamificación, progreso y ejecución continúan fuera de la migración al catálogo real.
- La acción de iniciar una plantilla maestra queda deshabilitada de forma funcional y documentada en el detalle para evitar resultados ficticios.

La siguiente fase recomendada es modelar rutinas personales separadas del catálogo maestro y migrar la ejecución para consumir una única secuencia real. Después podrá incorporarse el flujo supervisado de selección, revisión, atribución y almacenamiento offline de pictogramas ARASAAC.
