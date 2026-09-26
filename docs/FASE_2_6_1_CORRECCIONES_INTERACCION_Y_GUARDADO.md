# RutinaGo — Fase 2.6.1: correcciones de interacción y guardado

Fecha: 25 de septiembre de 2026

Rama: `feature/rutinago-master-bank-arasaac`

HEAD inicial: `4b43669b89aed0d9bf572e326b574f973b461842`

## Resultado

La fase corrige dos puntos concretos sin cambiar el banco maestro, los perfiles ni el modelo de ejecución persistente:

- cada rutina personal conserva una interacción visual de inicio identificada por su ID real;
- el editor presenta una sola acción **Guardar rutina** fija sobre la navegación inferior, fuera del catálogo desplazable.

Se conservaron la navegación anterior, la confirmación de cambios sin guardar, las validaciones de la Fase 2.5 y el retorno posterior a un guardado correcto.

## Causa de la interacción compartida

La lista ya utilizaba `UserRoutine::id` como clave estable y no compartía explícitamente un `MutableInteractionSource`. La causa identificada estaba en el estado de operación: `startingId` era específico, pero se transformaba en la condición global `startingId == null` al calcular `startEnabled`. Al comenzar una rutina, todos los botones cambiaban simultáneamente de estado habilitado, lo que producía una respuesta visual colectiva.

La corrección mantiene las claves por ID y realiza estos ajustes:

1. cada botón recuerda su propio `MutableInteractionSource` con `remember(routine.id)`;
2. únicamente la tarjeta cuyo ID coincide con `startingId` cambia a **Iniciando…**, muestra progreso y se deshabilita;
3. las demás tarjetas no cambian visualmente;
4. el callback comprueba de forma síncrona que no exista otra solicitud de inicio en curso antes de lanzar la corrutina;
5. los identificadores de prueba y la semántica también incorporan el ID, por lo que dos rutinas con el mismo nombre continúan siendo independientes.

Room conserva además su idempotencia transaccional: iniciar de nuevo la misma rutina activa devuelve la ejecución existente y no crea otra. No se modificaron `COMPLETED`, `PARTIAL`, `ABANDONED`, los pasos, los resultados ni **Salir**.

## Organización final del editor

`RoutineDraftEditor` usa un `Scaffold` interno con dos zonas:

- un `LazyColumn` para nombre, secuencia, opcionales, buscador, categorías, catálogo, validaciones y confirmación de guardado;
- una barra inferior fija con la única acción principal **Guardar rutina**.

La barra queda dentro del área de contenido que el `Scaffold` principal de RutinaGo ya sitúa por encima de la navegación inferior. No usa coordenadas absolutas ni medidas específicas de un dispositivo. El botón anterior fue retirado del `LazyColumn`; la nueva ubicación invoca la misma función de validación y persistencia.

El padding entregado por el `Scaffold` interno se aplica y consume en la lista, con espacio inferior adicional. Por ello el último paso, la última actividad, los controles **Subir**, **Bajar** y **Quitar**, y los mensajes de resultado pueden desplazarse completamente por encima de la barra.

## Guardado, errores y teclado

La acción solo se habilita cuando existe un nombre no vacío y al menos una actividad. El repositorio continúa validando nombre, actividades y referencias antes de ejecutar la transacción Room.

Una guarda síncrona sobre `saving` ignora pulsaciones repetidas mientras la operación está en curso. El éxito solo se muestra después de que `onSaveRoutine` devuelve la rutina persistida. Ante un error se conserva el borrador, se presenta el mensaje previo y el botón vuelve a permitir el reintento.

El editor aplica `imePadding()` al `Scaffold` interno. Al abrir el teclado, la barra se desplaza con el área disponible y el contenido puede seguir desplazándose; la acción no queda permanentemente detrás del IME.

## Accesibilidad

La barra usa colores y elevaciones de Material 3, por lo que sigue los temas Día, Noche, Sistema y alto contraste. El botón mantiene el objetivo táctil estándar de Material, una etiqueta visible y una descripción accesible.

La semántica diferencia **Disponible**, **No disponible** y **Guardado en progreso**. Cuando guarda, muestra un indicador junto con el texto **Guardando…** y publica el cambio como región viva cortés para TalkBack. El estado deshabilitado explica que se requiere un nombre y una actividad, sin depender únicamente del color.

La barra y la lista se verifican en una prueba Compose con alto contraste y escala de texto `1.45`, equivalente al escenario de texto muy grande usado por la aplicación.

## Pruebas

Se añadió `RoutineInteractionAndSaveActionTest`, que comprueba:

- dos rutinas con el mismo nombre y distintos IDs conservan botones independientes;
- solo el botón elegido anuncia **Iniciando rutina**;
- la rutina iniciada corresponde al ID pulsado;
- pulsaciones rápidas sobre el mismo u otro botón no lanzan otra solicitud mientras la primera sigue activa;
- la acción fija permanece visible al abrir el teclado y al llegar a la actividad 30;
- el último contenido es visible sin quedar cubierto por la barra;
- el guardado desde cero conserva nombre, pasos y origen nulo;
- el guardado desde plantilla conserva el ID de plantilla;
- alto contraste y texto muy grande mantienen accesible la acción.

`CreateNavigationTest` se actualizó para usar el botón fijo. Las pruebas Room existentes continúan cubriendo transacciones, validaciones y persistencia; las pruebas de navegación existentes siguen cubriendo Atrás, salida y estados reales de ejecución.

Verificaciones ejecutadas:

```text
.\gradlew.bat testDebugUnitTest                BUILD SUCCESSFUL
.\gradlew.bat assembleDebug                    BUILD SUCCESSFUL
.\gradlew.bat lintDebug                        BUILD SUCCESSFUL
.\gradlew.bat assembleDebugAndroidTest         BUILD SUCCESSFUL
python -B tools/master_bank/validate_master_bank.py
                                                 VALIDACIÓN CORRECTA
python -B -m unittest discover -s tools/master_bank/tests -v
                                                 5 pruebas, OK
```

## Limitaciones

No había dispositivo Android conectado ni AVD configurado en el SDK local (`adb devices -l` no listó dispositivos y `emulator -list-avds` no listó AVD). Las pruebas instrumentadas quedaron compiladas correctamente, pero no pudieron ejecutarse en esta máquina.

No se realizó una auditoría manual con TalkBack real por la misma ausencia de dispositivo o emulador. La cobertura disponible valida semántica, visibilidad, estado, teclado, alto contraste y escala grande mediante Compose.

`.idea/deploymentTargetSelector.xml` ya era un cambio previo del entorno al comenzar la fase; se mantuvo intacto y se excluye del commit.
