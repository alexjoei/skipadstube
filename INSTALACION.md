# Instalar y activar skipadstube

Necesitas **Android 8 o posterior** y la **app oficial de YouTube**. Los nombres de los menús cambian según el teléfono; no está probado en todos los modelos.

1. **Instala el APK.** Abre `skipadstube-0.2.7.apk` en el móvil y pulsa **Instalar**. Si lo pide, permite **Instalar aplicaciones desconocidas** para el navegador o gestor de archivos que estés usando.
2. **Abre skipadstube.** Activa **Silenciar durante anuncios** y, si quieres, **Omitir anuncios automáticamente**. Activa **Sonido suave al empezar y terminar** para los avisos; puedes escucharlos con **Probar sonidos de inicio y fin**. Los ajustes se guardan automáticamente.
3. **Activa el servicio.** Pulsa **Abrir ajustes de Accesibilidad**, busca **skipadstube para YouTube** (puede estar en **Aplicaciones instaladas/descargadas**) y actívalo tras leer el aviso.
4. **Prueba YouTube.** Abre la app oficial y reproduce un vídeo con anuncios. La app intentará silenciarlos y pulsar **Omitir** cuando aparezca; los anuncios sin ese botón no se pueden saltar.

## Si no te deja activarlo o deja de funcionar

- **Icono flotante:** skipadstube no necesita un botón permanente en pantalla. En **Ajustes → Accesibilidad → skipadstube para YouTube**, desactiva **Acceso directo / Botón de Accesibilidad** y deja activado **Usar servicio**. Los nombres varían según el móvil. [Ayuda de Android sobre accesos directos](https://support.google.com/accessibility/android/answer/7650693?hl=es).
- **«Ajuste restringido»:** ve a **Ajustes → Aplicaciones → skipadstube → ⋮ → Permitir ajustes restringidos**, si aparece. Hazlo solo si confías en el APK. Después repite el paso 3. [Ayuda de Android](https://support.google.com/android/answer/12623953?hl=es).
- **Se detiene en segundo plano:** en **Ajustes → Aplicaciones → skipadstube → Batería**, permite el uso en segundo plano o selecciona **Sin restricciones**, si existe. Si tu móvil ofrece **Inicio automático**, actívalo para skipadstube. Comprueba también que su servicio de Accesibilidad siga encendido.
- **Vienes de 0.2.0:** esta versión se instala como una app nueva. Desactiva el servicio antiguo y activa **skipadstube para YouTube**; los ajustes anteriores no se copian.

Para desactivarlo, apaga **skipadstube para YouTube** en Accesibilidad.

El silencio afecta al volumen multimedia del teléfono y lo restaura después. Los avisos usan el volumen de Accesibilidad con el servicio conectado; sin el servicio, la prueba usa multimedia. Ajusta el volumen con las teclas del móvil desde la pantalla de skipadstube.

Si actualizas a 0.2.7, instala el APK encima y desactiva y reactiva **skipadstube para YouTube** en Accesibilidad para cargar la nueva configuración.

Si sigue fallando, reproduce un anuncio y vuelve a skipadstube. Comparte lo que aparece en **Último anuncio**, el estado del servicio y el resultado de la prueba de sonido, junto con el modelo del móvil. La pantalla muestra si se detectó un anuncio y el volumen aplicado; no confirma por sí sola que el audio se haya silenciado.

En el Realme probado, 0.2.7 usa pasos de volumen cuando el teléfono bloquea el ajuste directo. Se comprobó volumen 0 durante dos anuncios y recuperación al volver al vídeo. Los avisos sonoros siguen pendientes; no son necesarios para silenciar.
