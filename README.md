# TubeQuiet MVP (Android)

Aplicación local que observa exclusivamente la interfaz de la app oficial de YouTube,
silencia el audio al detectar un anuncio y pulsa el botón de omitir cuando aparece.

## Estado

- MVP 0.1, listo para abrir y compilar en Android Studio.
- Sin permiso de Internet, analítica ni recopilación de datos.
- Android 8 o posterior; APK compilado y firma verificada, pendiente de prueba real en el dispositivo.
- Reglas desacopladas en `DetectionRules.java` para poder corregir cambios de interfaz.

## Compilar e instalar

1. Abrir esta carpeta con la versión reciente de Android Studio.
2. Esperar a que Gradle sincronice y pulsar **Run** con el móvil conectado y depuración USB activa.
3. Abrir TubeQuiet y pulsar **Abrir ajustes de Accesibilidad**.
4. Activar **TubeQuiet para YouTube**.
5. En Realme UI: permitir ejecución en segundo plano y excluir TubeQuiet de optimización de batería.
6. Reproducir varios vídeos con anuncios y comprobar anuncio inicial, doble y mid-roll.

## Pruebas que debemos registrar

| Caso | Resultado esperado |
|---|---|
| Anuncio no saltable | Volumen 0; se restaura al volver el vídeo |
| Anuncio saltable | Volumen 0; clic inmediato al habilitarse; restauración |
| Dos anuncios consecutivos | Permanece silenciado hasta terminar ambos |
| Salir de YouTube durante anuncio | Restaura el volumen |
| Vídeo o título que contiene «anuncio» | No debe silenciar por falso positivo |

## Camino hacia una versión pública

1. Telemetría voluntaria y anonimizada solo de nombres/IDs de controles desconocidos.
2. Reglas remotas firmadas, con versión, rollback y lista permitida de acciones.
3. Pantalla de divulgación de Accesibilidad y consentimiento antes de abrir Ajustes.
4. Declaración de AccessibilityService y vídeo demostrativo para revisión de Google Play.
5. Revisión legal/de políticas de YouTube antes de monetizar.

El motor seguirá siendo determinista: señal reconocida → silenciar o pulsar un botón
concreto. No toma decisiones abiertas ni ejecuta acciones fuera de YouTube.
