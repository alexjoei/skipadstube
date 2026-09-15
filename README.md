# skipadstube MVP 0.2.7 (Android)

Aplicación local que observa exclusivamente la interfaz de la app oficial de YouTube,
silencia el audio al detectar un anuncio y pulsa el botón de omitir cuando aparece.

## Estado

- MVP 0.2.7, con el nombre e identificador skipadstube.
- Sin permiso de Internet, analítica ni recopilación de datos.
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
