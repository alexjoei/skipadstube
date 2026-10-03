# skipadstube MVP 1.0.0 (Android)

Aplicación local que observa exclusivamente la interfaz de la app oficial de YouTube,
silencia el audio al detectar un anuncio y pulsa el botón de omitir cuando aparece.

## Estado

- MVP 1.0.0, con el nombre e identificador skipadstube.
- Sin permiso de Internet ni analítica. Desde la 0.2.9 guarda localmente, en `ad_stats.csv`
  (ver [Estadísticas de anuncios](#estadísticas-de-anuncios)), un registro de cada anuncio para poder
  analizarlo más adelante; ese archivo nunca sale del dispositivo.
- Funciona mediante el servicio de Accesibilidad, sin ventanas ni botones flotantes. El acceso directo de Android es opcional; puede desactivarse manteniendo el servicio activo.
- Android 8 o posterior. Silencio y restauración comprobados mediante el estado de audio de un Realme RMX3851 con Android 15; otros modelos pendientes.
- Reglas desacopladas en `DetectionRules.java` para poder corregir cambios de interfaz.
- Avisos sonoros de inicio y fin configurables; su reproducción audible sigue pendiente de corregir en el Realme probado.
- La detección revisa los controles visibles del reproductor cada 500 ms y excluye las recomendaciones patrocinadas.

## Compilar e instalar

**Para instalarlo en el móvil:** [guía rápida de instalación y activación](INSTALACION.md).

APK actualizado: `dist/skipadstube-0.2.7.apk` (compilación debug firmada para pruebas).
Incluye el código Android actual y el nuevo identificador `com.skipadstube.app`.
Actualiza sobre versiones 0.2.1 o posteriores. Si vienes de 0.2.0, se instala como otra app: activa su servicio
de Accesibilidad; los ajustes anteriores no se migran.

1. Abrir esta carpeta con la versión reciente de Android Studio.
2. Esperar a que Gradle sincronice y pulsar **Run** con el móvil conectado y depuración USB activa.
3. Abrir skipadstube y pulsar **Abrir ajustes de Accesibilidad**.
4. Activar **skipadstube para YouTube**.
5. En Realme UI: permitir ejecución en segundo plano y excluir skipadstube de optimización de batería.
6. Reproducir varios vídeos con anuncios y comprobar anuncio inicial, doble y mid-roll.

## Pruebas que debemos registrar

| Caso | Resultado esperado |
|---|---|
| Anuncio no saltable | Volumen 0; se restaura al volver el vídeo |
| Anuncio saltable | Volumen 0; clic inmediato al habilitarse; restauración |
| Dos anuncios consecutivos | Permanece silenciado hasta terminar ambos |
| Salir de YouTube durante anuncio | Restaura el volumen |
| Vídeo o título que contiene «anuncio» | No debe silenciar por falso positivo |

## Estadísticas de anuncios

Desde la 0.2.9, `YouTubeAutomationService` registra cada anuncio detectado (de inicio a fin,
no cada sondeo) como una fila CSV en `ad_stats.csv`, dentro del almacenamiento específico de
la app (`getExternalFilesDir(null)`, con `getFilesDir()` como alternativa si no está disponible).
No requiere ningún permiso adicional ni usa Internet.

Columnas: `start,end,duration_ms,declared_seconds,time_to_skip_ms,skippable,skipped,pod_position,content_ms_before_ad,ad_label,advertiser_guess`

- `start` / `end` / `duration_ms`: marca de tiempo ISO 8601 de inicio y fin, y duración observada
  en milisegundos.
- `declared_seconds`: mejor esfuerzo — longitud total del anuncio deducida del propio contador en
  pantalla (p. ej. el «15» de «Anuncio · 15»), para saber cuánto habría durado si no se hubiera
  omitido. Vacío si no hay contador parseable.
- `time_to_skip_ms`: tiempo desde que se detecta el anuncio hasta que el botón de omitir aparece
  disponible por primera vez. Vacío si nunca apareció.
- `skippable`: `true` si el botón de omitir llegó a aparecer en algún momento, con independencia de
  si lo pulsamos o no (permite distinguir anuncios no saltables de saltables que no se alcanzaron a
  pulsar).
- `skipped`: `true` si nuestro propio clic de «Omitir» se disparó durante ese anuncio.
- `pod_position`: posición de este anuncio dentro de una tanda de anuncios seguidos (1, 2, 3…);
  vuelve a 1 si pasan más de 3 segundos entre el fin de un anuncio y el inicio del siguiente. Con
  esto se puede saber si en una pausa salió un solo anuncio o varios encadenados.
- `content_ms_before_ad`: milisegundos con el reproductor de YouTube visible y sin anuncio desde
  que terminó el anuncio anterior hasta que empezó este (0 para el primer anuncio de la sesión).
  Es la base para "cada cuánto tiempo sale un anuncio": con la media de esta columna se calcula
  "un anuncio cada X minutos", y con su inversa, anuncios por hora. Importante: en Android no hay
  forma fiable de saber si el vídeo está en pausa o reproduciéndose (no hemos confirmado ningún
  identificador de accesibilidad para el botón de play/pausa), así que esto cuenta "app en primer
  plano con el reproductor en pantalla", no "reproduciendo activamente" — si dejas el vídeo en
  pausa con la pantalla encendida, ese tiempo sí se contabiliza aquí (a diferencia de la versión de
  escritorio, que sí puede comprobar la pausa real del `<video>`). Si sales de YouTube o cambias de
  app, ese hueco no se cuenta.
- `ad_label`: el primer texto o descripción no vacío de los nodos ya clasificados como señal de
  anuncio por `DetectionRules` (en la mayoría de pantallas observadas, solo el contador «Anuncio ·
  15»).
- `advertiser_guess`: **experimental**, añadido porque `ad_label` no estaba devolviendo nada en
  pruebas reales. Recorre todos los nodos visibles del reproductor mientras el anuncio está activo
  (solo mientras está activo: nunca mira el vídeo normal) y se queda con el primer texto que no
  coincide con controles conocidos del reproductor (silenciar, pantalla completa, etc.) ni con el
  contador. No hemos confirmado que YouTube exponga el nombre del anunciante por accesibilidad en
  ningún punto del árbol; esta columna puede seguir saliendo vacía, o capturar texto irrelevante. Si
  localizas el nodo correcto inspeccionando un volcado de accesibilidad real durante un anuncio
  (`uiautomator dump` con el anuncio en pantalla), compártelo para afinar `DetectionRules`.

Para extraerlo hay dos vías:
1. **Desde la propia app:** botón **«Compartir estadísticas de anuncios (CSV)»** en la pantalla
   principal, que abre el selector de Android para enviarlo por email, guardarlo en Drive, etc.
   (usa un `ContentProvider` propio; no requiere permisos de almacenamiento).
2. **Por USB:** `adb pull /sdcard/Android/data/com.skipadstube.app/files/ad_stats.csv` (el gestor de
   archivos del sistema no puede navegar directamente dentro de `Android/data/` en Android 11+).

El archivo crece de forma indefinida; de momento no hay rotación ni límite de tamaño.

## Camino hacia una versión pública

1. Telemetría voluntaria y anonimizada solo de nombres/IDs de controles desconocidos.
2. Reglas remotas firmadas, con versión, rollback y lista permitida de acciones.
3. Pantalla de divulgación de Accesibilidad y consentimiento antes de abrir Ajustes.
4. Declaración de AccessibilityService y vídeo demostrativo para revisión de Google Play.
5. Revisión legal/de políticas de YouTube antes de monetizar.

El motor seguirá siendo determinista: señal reconocida → silenciar o pulsar un botón
concreto. No toma decisiones abiertas ni ejecuta acciones fuera de YouTube.

## Repository and development

Canonical repository: https://github.com/alexjoei/skipadstube

- Android source remains at the repository root (app/), version 0.2.7 (version code 9).
- Desktop Chrome/Edge extension is in desktop/, version 0.1.7; see its README for installation.
- The Android source was compared with the supplied 0.2.0 source archive and matched before renaming.
- Android now uses application ID com.skipadstube.app and preference key skipadstube. Android treats this as a new app: the previous installation and its settings are not upgraded or migrated.
- The supplied skipadstube-0.2.0.apk is a historical binary; renaming source does not change that APK.
- Open the root project in Android Studio with JDK 17 and Android SDK 35. The Gradle wrapper pins Gradle 8.9 for Android Gradle Plugin 8.7.3. Run `./gradlew :app:testDebugUnitTest :app:assembleDebug` (`.\gradlew.bat` on Windows) once the toolchain is configured. The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
- Android 0.2.7: regression coverage includes rejected volume writes and restoration retries. On Realme RMX3851 / Android 15, two ads reached media volume 0 and content recovered its saved volume; audible chimes remain unresolved.
- Record improvements as GitHub issues and implement them in branches with pull requests. Never commit signing keys or local SDK paths.
