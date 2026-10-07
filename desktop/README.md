# skipadstube Desktop 1.1.0

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

## Estadísticas de anuncios

Cada anuncio (de inicio a fin) se guarda en `chrome.storage.local`. No sale del navegador
y no usa ningún permiso nuevo. Columnas (las mismas que usa la app Android):

- `start` / `end` / `duration_ms`: cuándo empezó, cuándo terminó y cuánto duró en total.
- `declared_seconds`: longitud del anuncio deducida del propio contador en pantalla (p. ej.
  «15» de «Anuncio · 15»), para saber cuánto habría durado si no lo hubiéramos omitido.
  Mejor esfuerzo: vacío si no hay contador visible.
- `time_to_skip_ms`: tiempo desde que empieza el anuncio hasta que aparece el botón de
  omitir. Vacío si nunca apareció.
- `skippable`: si el botón de omitir llegó a aparecer, independientemente de si lo pulsamos.
- `skipped`: si nuestro propio clic de omitir se confirmó enviado.
- `pod_position`: posición del anuncio dentro de una tanda de anuncios seguidos (1, 2, 3…;
  vuelve a 1 si pasan más de 3 segundos desde que terminó el anterior) — para saber si salen
  anuncios sueltos o varios encadenados.
- `content_ms_before_ad`: tiempo de vídeo **realmente reproducido** (el `<video>` no estaba en
  pausa) desde que terminó el anuncio anterior hasta que empezó este. Con la media de esta
  columna puedes calcular «un anuncio cada X minutos de visionado»; con su inversa, anuncios
  por hora. No cuenta el tiempo con el vídeo en pausa ni, para el primer anuncio de la sesión,
  nada anterior a esa sesión.
- `ad_label`: el mismo texto/contador ya usado para detectar el anuncio (normalmente «Anuncio
  · 15»).
- `advertiser_guess`: **experimental**. Recorre todo el texto visible del reproductor
  mientras el anuncio está activo, descartando controles conocidos (silenciar, pantalla
  completa, etc.) y el propio contador, buscando cualquier otra cosa. En las pruebas
  realizadas no ha aparecido ningún nombre de anunciante — puede que YouTube no lo expone
  por accesibilidad/DOM en absoluto, o que la señal esté en un elemento que esta heurística
  todavía no reconoce. Si alguna vez detectas un anunciante en esta columna o, al revés,
  encuentras el elemento correcto inspeccionando la página con las herramientas de
  desarrollador durante un anuncio real, avísanos para afinar la detección.

Para analizarlo, abre el popup de la extensión y pulsa **Descargar estadísticas (CSV)**;
descarga `skipadstube-ad-stats.csv` con las columnas anteriores. Se conservan como máximo
las 5000 filas más recientes.

## Actualizar y comprobar

1. Si instalaste el ZIP antiguo, carga la carpeta `desktop` de este repositorio y desactiva la copia antigua.
2. En `chrome://extensions` o `edge://extensions`, pulsa **Recargar** en skipadstube y comprueba la versión **1.1.0**.
3. Recarga también la pestaña de YouTube para que use el nuevo código.
4. Comprueba que **Omitir automáticamente** está activado y que la extensión tiene acceso a `www.youtube.com`.
5. Prueba un anuncio con botón de omitir: debe pulsarlo cuando esté disponible. Los anuncios sin botón se silencian, pero no se pueden omitir con esta extensión.

Pruebas de regresión: `node --test desktop/tests/*.test.cjs` desde la raíz del repositorio.

Si no omite el anuncio, abre el popup mientras el botón esté visible. El estado indica si detecta el anuncio, encuentra el botón y ha intentado pulsarlo. Los intentos no confirman que YouTube haya omitido el anuncio. El contador pertenece a la pestaña y se reinicia al recargarla; no se guarda ni se envía fuera del navegador.

## Nivelar volumen (iVoox)

`leveler.js` aplica un compresor + limitador de Web Audio a los `<audio>`/`<video>` de ivoox.com
(casilla "Nivelar volumen (iVoox)" del popup). Se salta los elementos de origen cruzado sin CORS,
porque conectarlos los dejaría en silencio; por eso puede no actuar en todos los reproductores.
Sin comprobar todavía en el navegador real.
