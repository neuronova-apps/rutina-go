# Fase 3.4.3 — Mi vocabulario por perfil

## Base y alcance

El 18 de octubre de 2026 se comprobó `git status`, la rama, HEAD, `git fetch origin`
y la referencia remota. Se reutilizó la rama existente
`feature/comunicago-personal-vocabulary`. HEAD y origin coincidían en
`77cf54c7957f1f681fc46738143586ce16992b62` antes de modificar.

El único archivo local pendiente era `.idea/deploymentTargetSelector.xml`, preservado
sin modificaciones, con SHA-256
`40F6E1076D4C7A498494E680339CDE6B4C9D52033EF4E45F6D1E5C62D6E71644`.
No se crean ramas, merges o PR. Los cambios quedan locales para revisión.

## Funcionalidad y primera configuración

Después de las características iniciales de ComunicaGo, un perfil sin selección
recibe una configuración breve de Mi vocabulario. Puede utilizar las 132 palabras
o revisar una selección funcional sugerida. Las dos opciones permiten agregar y
ocultar palabras del catálogo completo antes de confirmar. El borrador es temporal,
se conserva ante recreación de Activity y solo se persiste al confirmar.

La sugerencia reutiliza los seis IDs del acceso rápido y referencias verificadas
para personas, necesidades, actividades, lugares, emociones, molestias y rechazo.
No es una evaluación, una selección suficiente para todos ni una asignación
pedagógica validada. Edad y comunicación oral no intervienen en la selección.
La familia puede incorporar cualquiera de los 132 vocablos. Confirmar una selección
vacía también es válido. Una selección existente nunca se sustituye al ingresar,
cambiar características, renombrar el perfil o cambiar de vista.

El acceso visible Mi vocabulario abre una administración identificada para familiares
y apoyos, con el nombre del perfil, contadores, búsqueda por palabra/categoría/ID,
filtros Todas las palabras / Palabras visibles / Palabras ocultas y categorías reales
con Todos. La búsqueda ignora mayúsculas y acentos. Cada registro muestra texto,
categoría, estado escrito y acción explícita; no se convierte la tarjeta del
comunicador en un control de edición.

En selecciones ya confirmadas, agregar, ocultar y recuperar guardan inmediatamente.
Los contadores y el comunicador derivan de los IDs persistidos. Durante una escritura
se deshabilitan las mutaciones y el regreso desde administración. Un error conserva
la selección guardada y permite reintentar la acción; no se simula un guardado exitoso.

Ocultar Sí, No, Ayuda y otras referencias verificadas para necesidades básicas,
rechazo, dolor, molestias o malestar abre una advertencia breve, con Cancelar y
Ocultar. No hay bloqueo permanente. La recuperación desde Palabras ocultas no exige
confirmación. Restablecer vocabulario permite elegir el banco completo o la sugerencia;
explica el reemplazo y su cantidad, advierte sobre necesidades y pide confirmación.
Cancelar no modifica datos. No borra perfiles, rutinas, mensajes ni historial.

## Almacenamiento y compatibilidad

Se reutilizan `CommunicationSetupStorage`, `CommunicationSetupPreferences`,
`CommunicationProfileSetupManager` y `UserPreferencesRepository`.
No se modifica Room, el esquema de RutinaGo, backup, manifiesto ni dependencias.

- Archivo existente: `context.noBackupFilesDir/communication_profile_setup.preferences_pb`.
- Estrategia: whitelist de IDs visibles, en un `stringSetPreferencesKey`
  `vocabulary_visible_v1:<profileId>`.
- Clave ausente: selección nunca configurada (`null`).
- Clave presente con conjunto vacío: selección personalizada vacía.
- Clave presente con los 132 IDs: selección completa.
- Los conjuntos impiden duplicados y las escrituras validan pertenencia al banco
  maestro y existencia del perfil. La lectura devuelve una copia inmutable.
- No se almacenan copias de palabras, categorías ni metadatos. El nombre no es clave.
- Los registros anteriores `profile:<ID>` y `presentation:<ID>` conservan formato,
  versión y contenido. Leer una selección ausente no escribe valores por defecto.
- Referencias desconocidas en preferencias no generan vocablos ni provocan fallos:
  la presentación se obtiene por intersección con el banco. La lectura no reescribe
  preferencias; una edición posterior guarda únicamente referencias vigentes.
- Errores de lectura se muestran con reintento; no se confunden con ausencia.
- Guardado y borrado comparten el mutex existente. El borrado limpia configuración,
  presentación y vocabulario del ID en una edición del DataStore contextual,
  antes de borrar el perfil. Se rechazan guardados posteriores para IDs eliminados.
  Como en la fase anterior, el archivo de perfiles y el contextual no constituyen
  una transacción única.

No se envía información a servidores ni se introducen servicios de sincronización.

## Catálogo, vistas, mensajes y navegación

El banco maestro, su fuente, su generador y su repositorio permanecen intactos:
132 vocablos, 18 categorías, 111 VALIDADO, 21 PENDIENTE_ARASAAC, 10 SOLO_TEXTO,
11 PICTOGRAMA_PENDIENTE. No se modifican IDs, textos, orden ni metadatos. No se
accede a Google Sheets. El filtro devuelve los mismos objetos del catálogo,
conservando sus referencias para futuros pictogramas.

La entrada combina características, presentación y selección del mismo profileId
antes de mostrar el comunicador. Ambas vistas filtran vocabulario y acceso rápido
con la misma whitelist; Vista completa no recupera automáticamente palabras ocultas.
Las dimensiones y espaciado de las vistas se conservan. Todas las categorías siguen
presentes; una categoría sin palabras visibles ofrece un estado vacío y acceso
a Mi vocabulario.

`CommunicationMessageBuilder` y `CommunicationStateCodec` permanecen intactos y
dependen del catálogo íntegro. Solo se filtran los resultados visibles para nuevas
selecciones. Ocultar una palabra no cambia fichas, texto, orden, repeticiones,
categoría, selección temporal o Deshacer limpieza. La restauración sigue validando
contra el banco maestro, incluidas las palabras ahora ocultas.

Se conserva el holder de estado del constructor al abrir administración y editar
características. La navegación es ComunicaGo → Mi vocabulario → ComunicaGo;
el botón de regreso y Atrás respetan el flujo. En la primera configuración, ambos
regresan al selector de módulos. ComunicaGo conserva su regreso al selector y no
se modifica la navegación de RutinaGo.

## Accesibilidad

La administración usa el tema existente para Día, Noche, Sistema, Alto contraste
y los tres tamaños de texto. Lista vertical con claves estables y margen para el
teclado de búsqueda, texto sin límite
de líneas, controles de al menos 48 dp, estados escritos Visible/Oculta, nombres
de acción con la palabra, opciones con semántica de selección y contadores/errores
como regiones vivas discretas. Las advertencias permiten desplazamiento del texto.
La selección no depende exclusivamente del color.

La prueba instrumentada prepara 30 combinaciones de apariencia, tamaño y pantalla:
Día, Noche, Sistema claro/oscuro, Alto contraste × tres escalas × 400×760 y 320×480 dp,
con fontScale adicional 1.6 en la pantalla pequeña. Comprueba geometría del texto,
estados, selección, acciones de ocultar/recuperar y diálogo de restablecimiento.
La lectura real con TalkBack y la ausencia de recortes en dispositivo quedan
pendientes de ejecutar, no se deducen de una compilación correcta.

## Verificación

- 122 pruebas unitarias: cero fallos y cero errores, incluidas las 111 anteriores
  y 11 nuevas en `CommunicationPersonalVocabularyTest`.
- Las nuevas pruebas cubren ausencia/vacío/completo tras reabrir DataStore,
  independencia y renombrado, compatibilidad con características/presentación,
  validación de IDs, eliminación selectiva, carrera guardado/borrado, referencias
  desconocidas, orden e identidad de objetos, búsqueda, sugerencia y advertencias,
  filtrado del acceso rápido en ambas vistas y preservación de mensaje/Deshacer.
- `assembleDebug` y `assembleDebugAndroidTest`: APK de aplicación y de pruebas
  compiladas. El test funcional prepara revisión inicial, agregado antes de confirmar,
  advertencia/cancelación, ocultar, recuperar, restablecer/cancelar, mensaje y Deshacer,
  cambio de vista, recreación y aislamiento entre dos perfiles temporales.
- Los helpers previos eligen explícitamente el catálogo completo al encontrar
  la nueva configuración inicial; mantienen sus verificaciones del comunicador.
- `node tools/generate_communication_catalog.mjs --check`: conserva todos los conteos
  y verifica reproducibilidad del catálogo.
- `git diff --check`: sin errores de espacios.

La primera ejecución concurrente encontró una excepción interna de lint Kotlin
(`findFirCompiledSymbol`) mientras se incorporaba el nuevo archivo de pruebas;
se repitió la validación con las fuentes terminadas, sin deshabilitar análisis.
Las cuatro tareas finalizaron con `BUILD SUCCESSFUL`. Lint reporta cero errores,
seis avisos de dependencias y un hint de autoboxing preexistentes en archivos
que esta fase no modifica.

`adb devices -l` no encontró dispositivos y `emulator -list-avds` no encontró AVDs.
Las pruebas instrumentadas están compiladas, pendientes de ejecución. Compilarlas
no equivale a validar navegación o accesibilidad en un dispositivo.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

No se implementan TTS, Hablar, pictogramas nuevos, descargas ARASAAC, favoritos,
frases rápidas, categorías nuevas, generación con IA ni sincronización.
