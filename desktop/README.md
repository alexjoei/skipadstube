# skipadstube Desktop 0.1.6

La versión 0.1.5 añade acceso explícito a `https://www.youtube.com/*` para comprobar la URL de la pestaña. Sin ese permiso, Chrome puede ocultar la URL y la versión anterior mostraba incorrectamente «Activa la pestaña de YouTube».

## Método de omisión y permisos

Esta versión declara `debugger`, un permiso potente que permite inspeccionar y controlar pestañas. Chrome no permite solicitarlo como permiso opcional. Cargar esta versión concede esa capacidad aunque el interruptor esté apagado. El código limita su uso a pulsar el botón de omitir en la pestaña activa de YouTube y se desconecta después de cada intento.

El clic de navegador es el método predeterminado, controlado únicamente por **Omitir automáticamente**. No hay un ajuste adicional ni se usa el antiguo valor guardado de modo experimental. El usuario ha confirmado que este método omite anuncios en Chrome. Mantén activa la pestaña de YouTube y cierra DevTools. Chrome puede mostrar un aviso de depuración; los errores aparecen en el estado del popup.

Extensión local para Chrome y Edge. Detecta el estado de publicidad del reproductor,
silencia solo el elemento de vídeo, conserva el estado de silencio previo y pulsa el
botón de omitir en cuanto está visible.

## Instalación de prueba

1. Descomprimir el ZIP.
2. Chrome: abrir `chrome://extensions`. Edge: abrir `edge://extensions`.
3. Activar **Modo de desarrollador**.
4. Pulsar **Cargar descomprimida** y elegir la carpeta `desktop`.
5. Abrir YouTube. El icono de la extensión permite cambiar los tres ajustes.

No envía información fuera del navegador. El acceso de sitio se limita a `www.youtube.com`; el permiso `debugger` tiene capacidades más amplias, aunque el código limita su uso a YouTube.

## Actualizar y comprobar

1. Si instalaste el ZIP antiguo, carga la carpeta `desktop` de este repositorio y desactiva la copia antigua.
2. En `chrome://extensions` o `edge://extensions`, pulsa **Recargar** en skipadstube y comprueba la versión **0.1.6**.
3. Recarga también la pestaña de YouTube para que use el nuevo código.
4. Comprueba que **Omitir automáticamente** está activado y que la extensión tiene acceso a `www.youtube.com`.
5. Prueba un anuncio con botón de omitir: debe pulsarlo cuando esté disponible. Los anuncios sin botón se silencian, pero no se pueden omitir con esta extensión.

Pruebas de regresión: `node --test desktop/tests/*.test.cjs` desde la raíz del repositorio.

Si no omite el anuncio, abre el popup mientras el botón esté visible. El estado indica si detecta el anuncio, encuentra el botón y ha intentado pulsarlo. Los intentos no confirman que YouTube haya omitido el anuncio. El contador pertenece a la pestaña y se reinicia al recargarla; no se guarda ni se envía fuera del navegador.
