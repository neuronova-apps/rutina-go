# Fase 2.7C: pictogramas ARASAAC seleccionados disponibles sin conexión

## Punto de partida y alcance

- Rama: `feature/rutinago-master-bank-arasaac`.
- HEAD inicial: `65f28364c45a795700ab491c3b1362bb8661a01d`.
- Se conserva Room **v3**: la referencia versionada de la Fase 2.7B continúa en `user_routine_steps.future_visual_reference` y en el snapshot `execution_steps.visual_reference`. La ruta del archivo se reconstruye a partir del ID, así que no se añade columna ni migración.

RutinaGo descarga únicamente pictogramas ARASAAC que han sido seleccionados y guardados por el usuario. No se descarga el banco completo.

La búsqueda de Fase 2.7A puede seguir mostrando miniaturas remotas transitorias de candidatos; esas previsualizaciones no se guardan en `filesDir/pictograms/`.

## Archivo privado y descarga

`PictogramFileStore` guarda un PNG por ID en `filesDir/pictograms/arasaac/{id}.png`. No usa `cacheDir`, base64, blobs de Room, almacenamiento público ni permisos de almacenamiento. La resolución offline única es **500 px** y la URL se construye mediante `ArasaacImageUrls`. El mismo ID se comparte físicamente entre pasos, rutinas, perfiles e historial.

Tras el éxito transaccional de **Guardar rutina** o **Guardar cambios**, la app programa trabajo único `arasaac-pictogram-{id}` con [WorkManager 2.12.0](https://developer.android.com/jetpack/androidx/releases/work), política `KEEP`, requisito `NetworkType.CONNECTED` y backoff exponencial desde 30 segundos. El guardado y su confirmación no esperan Internet ni la imagen. Una selección descartada no programa descarga. La aplicación también reconcilia al arrancar solo los IDs realmente persistidos en pasos personales y snapshots: así recupera referencias de instalaciones de Fase 2.7B sin reselección. Los trabajos pendientes sobreviven al cierre y a la falta temporal de red. El worker vuelve a comprobar que el ID siga referenciado globalmente antes de descargar.

OkHttp solicita solo la ruta oficial del ID. Se exige HTTP 200, cuerpo no vacío, tipo PNG si se declara, máximo 5 MB y encabezado/final PNG coherentes. Se escribe primero un archivo `.part` privado; solo tras completar y validar la descarga se renombra al PNG definitivo dentro del mismo directorio. Un fallo elimina el temporal y nunca publica un archivo parcial. HTTP 404 y respuestas inválidas terminan sin reintentos automáticos; 429, 5xx, timeout o conexión fallida se reintentan con backoff. Un problema de escritura termina el trabajo; Room conserva siempre la referencia. Al guardar de nuevo una rutina se puede volver a programar un trabajo terminado con fallo.

## Lectura, historial y conservación

`PictogramImageResolver` da prioridad al archivo privado válido, luego a la URL remota para Coil. Si ambos fallan, aparece **Pictograma no disponible**; el nombre, la instrucción y las acciones de ejecución siguen operativos. El editor y la ejecución observan el estado del trabajo para pasar al archivo local cuando termina. Coil no se usa como garantía offline.

`PictogramReferenceRepository` consulta los pasos personales, incluidos los archivados, y todos los `execution_steps` entre perfiles. Permite comprobar `isPictogramReferenced(id)` antes de cualquier futura limpieza. Esta fase **no borra automáticamente PNGs** al cambiar, quitar o eliminar rutinas: una copia puede seguir sirviendo a otra rutina o a un historial. La política conservadora evita romper snapshots. `PictogramFileStore.stats()` informa cantidad de PNGs válidos y bytes ocupados para una futura gestión de espacio.

Los PNG son reconstruibles desde ARASAAC. Se excluye `pictograms/` de Auto Backup en `backup_rules.xml` y de copia en nube y transferencia de dispositivo en `data_extraction_rules.xml`, siguiendo las [reglas de backup de Android](https://developer.android.com/identity/data/autobackup). La base Room y los datos del usuario conservan sus reglas previas. Después de restaurar datos, la reconciliación vuelve a programar solo las referencias persistidas que carecen de archivo.

## Privacidad, licencia y accesibilidad

La solicitud a `static.arasaac.org` contiene solo ID y resolución: no envía perfil, UUID, rutina, usuario, historial ni progreso. La actualización editorial de la política web de privacidad sigue pendiente antes de la publicación final; no se modificó desde este repositorio. Se conserva la atribución a Sergio Palao, ARASAAC, Gobierno de Aragón y Creative Commons BY-NC-SA en búsqueda/detalle y **Acerca de RutinaGo**. El PNG se conserva sin cambios.

La imagen mantiene descripción contextual para TalkBack y no es el único indicador del paso. El texto y los controles siguen disponibles en Día, Noche, Sistema, alto contraste y los tamaños Normal, Grande y Muy grande. No se invierten ni recolorean pictogramas.

## Verificación y límites

- Pruebas JVM con MockWebServer: ruta por ID, PNG válido, temporal y publicación final, deduplicación, reinicio del store, prioridad local, tamaño y conteo, archivo vacío/incompleto/malformado/grande, 404, 429, 5xx y timeout. No dependen de Internet real.
- Pruebas instrumentadas compiladas: consultas de referencias entre perfiles y snapshots, configuración de red del trabajo, ID único y worker con entorno `work-testing`.
- Comprobación real acotada: `10384_500.png` respondió HTTP 200, `image/png`, 13 385 bytes y firma PNG correcta. El archivo temporal de verificación se leyó y eliminó; no hubo descarga masiva.
- `testDebugUnitTest`, `assembleDebug`, `lintDebug`, `assembleDebugAndroidTest`, validador del banco maestro y cinco pruebas Python.
- No había dispositivo ni AVD disponible para ejecutar pruebas instrumentadas o la prueba manual de desconexión. **Compiladas, no ejecutadas.** El APK de depuración queda preparado para comprobar creación, descarga, ejecución offline, recuperación de red y conservación histórica.

No hay recolección automática de archivos sin referencias en esta fase. Si ARASAAC deja de servir un ID de forma permanente, la referencia se mantiene y la pantalla conserva su fallback de texto.
