# skipadstube Desktop 0.1.2

Extensión local para Chrome y Edge. Detecta el estado de publicidad del reproductor,
silencia solo el elemento de vídeo, conserva el estado de silencio previo y pulsa el
botón de omitir en cuanto está visible.

## Instalación de prueba

1. Descomprimir el ZIP.
2. Chrome: abrir `chrome://extensions`. Edge: abrir `edge://extensions`.
3. Activar **Modo de desarrollador**.
4. Pulsar **Cargar descomprimida** y elegir la carpeta `desktop`.
5. Abrir YouTube. El icono de la extensión permite cambiar los tres ajustes.

No envía información ni solicita acceso fuera de `youtube.com`.

## Actualizar y comprobar

1. Si instalaste el ZIP antiguo, carga la carpeta `desktop` de este repositorio y desactiva la copia antigua.
2. En `chrome://extensions` o `edge://extensions`, pulsa **Recargar** en skipadstube y comprueba la versión **0.1.2**.
3. Recarga también la pestaña de YouTube para que use el nuevo código.
4. Comprueba que **Omitir automáticamente** está activado y que la extensión tiene acceso a `www.youtube.com`.
5. Prueba un anuncio con botón de omitir: debe pulsarlo cuando esté disponible. Los anuncios sin botón se silencian, pero no se pueden omitir con esta extensión.

Pruebas de regresión: `node --test desktop/tests/content.test.cjs` desde la raíz del repositorio.

Si no omite el anuncio, abre el popup mientras el botón esté visible. El estado indica si detecta el anuncio, encuentra el botón y ha intentado pulsarlo. Los intentos no confirman que YouTube haya omitido el anuncio. El contador pertenece a la pestaña y se reinicia al recargarla; no se guarda ni se envía fuera del navegador.
