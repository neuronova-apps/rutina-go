# Fase 2.7B: asociación persistente de pictogramas ARASAAC

## Punto de partida y decisión de datos

- Rama: `feature/rutinago-master-bank-arasaac`.
- HEAD inicial: `b2251220dbecf28ad281bb852e5d76526872ad55`.
- Room permanece en **v3**. `user_routine_steps.future_visual_reference` y `execution_steps.visual_reference` ya son columnas `TEXT` anulables. Se reutilizan sin alterar el esquema ni migrar. Una base v3 existente conserva sus datos; las referencias anteriores nulas siguen siendo válidas.
- Cada paso personal guarda un JSON pequeño y versionado con `schema`, `source`, `id` y `label`, por ejemplo `{"schema":"rutinago.visual.v1","source":"ARASAAC","id":10384,"label":"lavar"}`. El modelo de dominio valida fuente, ID positivo y etiqueta. No se guarda URL, respuesta HTTP ni información personal. La URL se reconstruye mediante `ArasaacImageUrls`.

La clave del paso sigue siendo la de `user_routine_steps`: ID de rutina personal y posición. Dos rutinas pueden usar la misma actividad maestra y tener pictogramas distintos. La selección no cambia `catalog_activities`, `catalog_visual_references`, el contrato del banco maestro ni los artefactos `generated/v1`.

## Editor y guardado

**Buscar pictograma** abre la búsqueda manual. El usuario inspecciona un resultado y pulsa **Usar este pictograma**; solo entonces el editor recibe el candidato con ID real. El pictograma aparece en el paso, con nombre de actividad, etiqueta y fuente. **Cambiar pictograma** vuelve a abrir la búsqueda; **Quitar pictograma** restaura el placeholder. Ambos cambios se hacen únicamente en el borrador y activan la confirmación de salida sin guardar. Volver o descartar deja intacta la fila de Room.

**Guardar rutina** o **Guardar cambios** envía las referencias del borrador en la misma transacción que nombre y secuencia. Se reutiliza la confirmación de guardado de la Fase 2.6.2. Al editar, el editor lee la referencia persistida y reconstruye la miniatura. Un paso sin pictograma se puede guardar. Al eliminar un paso de la secuencia se elimina su selección del borrador. La propiedad de la rutina se comprueba por perfil antes de cualquier edición.

## Ejecución e historial

Al iniciar una ejecución, la consulta existente copia `future_visual_reference` a `execution_steps.visual_reference`. La ejecución lee el snapshot, sin volver a consultar el paso personal. Cambiar, quitar, archivar o borrar después la rutina no modifica ejecuciones históricas. La FK con `ON DELETE CASCADE` elimina los pasos de la definición borrada; el historial terminado conserva sus snapshots independientes.

La ejecución muestra una miniatura ARASAAC de 300 px si tiene referencia válida. Sin referencia muestra el placeholder. Coil carga imágenes remotas y, si falla, muestra **Pictograma no disponible**. La actividad, la instrucción y las acciones para completar u omitir siguen visibles y operativas. El editor también utiliza 300 px; la búsqueda conserva la previsualización de 500 px.

Fase 2.7B persiste la referencia ARASAAC, pero todavía no garantiza el archivo de imagen para uso offline.

## Accesibilidad, atribución y privacidad

La imagen tiene descripción contextual con el nombre de la actividad. La información y el avance no dependen de la imagen ni del color. Los controles usan Material 3 y los temas Día, Noche, Sistema, alto contraste y las escalas de texto existentes; el borde se refuerza en alto contraste sin invertir el pictograma. La atribución completa permanece en búsqueda/detalle y en **Acerca de RutinaGo**.

La búsqueda envía a ARASAAC el término necesario y la carga de imagen solicita únicamente la URL de ID y resolución de `static.arasaac.org`; no añade perfil, UUID, rutina, historial ni progreso. La actualización editorial de la política web de privacidad sigue pendiente antes de la publicación final y queda fuera de este repositorio.

## Verificación y límites

- Pruebas unitarias del códec: identidad versionada, URL reconstruida y rechazo de IDs inválidos.
- Pruebas Room instrumentadas: persistencia tras reapertura de base, dos pasos y rutinas con la misma actividad, cambio y eliminación localizada, aislamiento por perfil, archivo, borrado y snapshot histórico.
- Pruebas Compose instrumentadas: selección manual al borrador, guardado, descarte de una eliminación y ejecución con texto y acción de avance aun con imagen remota.
- `testDebugUnitTest`, `assembleDebug`, `lintDebug` y `assembleDebugAndroidTest`: completados. El validador de banco maestro y sus cinco pruebas Python pasan.
- No había dispositivo ni AVD disponible: las pruebas instrumentadas se compilaron, pero no se ejecutaron. Queda pendiente la revisión manual del APK en un dispositivo, en especial red desconectada y los temas/tamaños de texto.

No se ha iniciado la descarga persistente de imágenes de la Fase 2.7C ni una asignación automática o masiva de ARASAAC.
