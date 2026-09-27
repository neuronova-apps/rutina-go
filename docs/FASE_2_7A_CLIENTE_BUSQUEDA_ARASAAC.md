# Fase 2.7A: cliente y búsqueda manual de ARASAAC

## Punto de partida y alcance

- Rama: `feature/rutinago-master-bank-arasaac`.
- HEAD inicial: `c532795656eedfe9d76603b0bd150af4558b267d`.
- La acción **Buscar pictograma** aparece en cada actividad seleccionada del editor de Crear y de edición de rutinas personales. Abre una vista temporal; al cerrarla se descarta la selección provisional.
- Fase 2.7A NO persiste selecciones definitivas ni garantiza imágenes offline.
- El banco maestro, Room v3, los placeholders, las rutinas y el flujo de guardado mantienen su contrato previo. No hay consultas automáticas a las 203 actividades.

## Fuente oficial y comprobación controlada

La [API pública oficial](https://github.com/Arasaac/public-api) se consultó sin credenciales el 27 de septiembre de 2026. Se usan `GET https://api.arasaac.org/v1/pictograms/es/search/{texto}` y `GET https://api.arasaac.org/v1/pictograms/es/{id}`; las imágenes se sirven desde `https://static.arasaac.org/pictograms/{id}/{id}_{resolucion}.png`. No se usa `bestsearch`. La API devuelve el ID real en `_id`.

| Consulta real | Respuesta observada | Referencia de prueba |
| --- | --- | --- |
| `lavarse manos` | HTTP 200, 63 candidatos | Primer `_id`: 10384; miniatura de 300 px HTTP 200 `image/png` |
| `comer` | HTTP 200, 10 candidatos | Primer `_id`: 6456 |
| `vestirse` | HTTP 404, sin candidatos | Se presenta como estado vacío en la búsqueda |

La cantidad y el orden pueden cambiar en el servicio. Estos IDs solo verifican el contrato; no se asignaron a actividades. `GET /es/10384` confirmó el objeto de detalle con `keywords`, `desc` y `categories`. Un 404 en búsqueda equivale a ausencia de resultados; un 404 en detalle indica que el pictograma ya no está disponible.

## Arquitectura

- **Red:** OkHttp 5.5.0 con 8 s de conexión, 12 s de lectura y 20 s por llamada, más Gson 2.14.0. La llamada suspendida cancela el `Call` de OkHttp cuando cambia el término o se cierra la vista. El cliente solo envía el texto de búsqueda codificado en el segmento de URL; el detalle envía solo el ID real.
- **DTOs:** `ArasaacPictogramDto` y `ArasaacKeywordDto` interpretan `_id`, palabras, descripción y categorías. JSON inesperado produce un error tipado; no se inventan IDs.
- **Dominio:** `ArasaacPictogramCandidate`, `ArasaacRepository`, `ArasaacApiException` y `ArasaacImageUrls`. Las resoluciones de 300 y 500 px están centralizadas.
- **Estado de UI:** `Idle`, `Loading`, `Success`, `Empty` y `Error`. El término editorial `arasaacSearchTerm` inicia una búsqueda editable, sin interpretarse como correspondencia aprobada. El controlador aplica 400 ms de debounce y cancela búsquedas anteriores con `collectLatest`.
- **Reintento:** solo a petición del usuario. HTTP 429 espera el valor `Retry-After` entre 2 y 30 segundos, o 3 segundos por defecto. HTTP 5xx espera 1,5 segundos. No hay bucle de reintentos.
- **Imágenes:** Coil 3.3.0 carga miniaturas remotas de 300 px y una vista de 500 px. La caché en disco se desactiva en estas solicitudes; una imagen fallida muestra texto. La memoria temporal de Coil no constituye soporte offline.

## Interfaz, accesibilidad y atribución

La vista enseña el resultado en texto además de la imagen, ID y fuente, con semántica de candidato contextual para TalkBack. Respeta los temas y tamaños de texto de la app. El modo de alto contraste usa bordes de 2 dp y una superficie neutra; no filtra ni invierte el pictograma. La previsualización muestra la atribución: “Pictogramas: Sergio Palao para ARASAAC. Propiedad: Gobierno de Aragón. Licencia: Creative Commons BY-NC-SA.” Véanse las [condiciones oficiales](https://aulaabierta.arasaac.org/condiciones-de-uso).

**Seleccionar provisionalmente** solo actualiza el estado de la vista abierta. Se indica expresamente que no se ha guardado. El repositorio ARASAAC carece de API de escritura y no está conectado a Room ni al borrador de rutina.

## Errores y privacidad

La UI diferencia falta de conexión, timeout, HTTP 429, error temporal HTTP 5xx, 404 de detalle, JSON inválido, ausencia de resultados e imagen fallida. El registro de depuración contiene solo clase de error y estado HTTP, sin consulta ni datos personales. La búsqueda no envía perfil, UUID, nombre de rutina, historial ni progreso. La sección Privacidad de **Acerca de RutinaGo** revela la consulta externa.

La [política web enlazada desde la app](https://neuronova-apps.github.io/privacy/rutinago/) requiere una revisión editorial para describir la nueva consulta a ARASAAC. Queda fuera de este repositorio y no se modificó en esta fase.

## Verificación

- `testDebugUnitTest`: cliente con MockWebServer (URL, JSON realista, ID, vacíos, 404, 429, 5xx, timeout y conexión rechazada), controlador con tiempo virtual (término inicial, debounce, cancelación, estados y espera de reintento), y pruebas previas.
- `assembleDebug`, `lintDebug` y `assembleDebugAndroidTest` verifican código de app y prueba de interfaz.
- La prueba instrumentada manual usa un repositorio falso para comprobar consulta editable, detalle, atribución, selección provisional y ausencia de referencia visual en el borrador guardado. No depende de la API real.
- El validador del banco maestro y sus pruebas Python comprueban los 203 registros de actividades, 52 rutinas, 17 categorías, 40 logros, 20 retos y 54 mensajes.

La ejecución de la prueba instrumentada requiere un dispositivo Android disponible. La asignación definitiva de IDs, la curación del banco y la descarga offline pertenecen a una fase posterior.
