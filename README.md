# skipadstube MVP 0.2.2 (Android)

Aplicación local que observa exclusivamente la interfaz de la app oficial de YouTube,
silencia el audio al detectar un anuncio y pulsa el botón de omitir cuando aparece.

## Estado

- MVP 0.2.2, con el nombre e identificador skipadstube.
- Sin permiso de Internet, analítica ni recopilación de datos.
- Android 8 o posterior; pendiente de probar en un dispositivo con el nuevo identificador.
- Reglas desacopladas en `DetectionRules.java` para poder corregir cambios de interfaz.
- Avisos sonoros suaves de inicio y fin configurables.
- La detección usa tanto el árbol visible como el texto de los eventos de Accesibilidad.

## Compilar e instalar

**Para instalarlo en el móvil:** [guía rápida de instalación y activación](INSTALACION.md).

APK actualizado: `dist/skipadstube-0.2.2.apk` (compilación debug firmada para pruebas).
Incluye el código Android actual y el nuevo identificador `com.skipadstube.app`.
Se instala como una app distinta de la anterior: abre skipadstube y activa su servicio
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

- Android source remains at the repository root (app/), version 0.2.2 (version code 4).
- Desktop Chrome/Edge extension is in desktop/, version 0.1.7; see its README for installation.
- The Android source was compared with the supplied 0.2.0 source archive and matched before renaming.
- Android now uses application ID com.skipadstube.app and preference key skipadstube. Android treats this as a new app: the previous installation and its settings are not upgraded or migrated.
- The supplied skipadstube-0.2.0.apk is a historical binary; renaming source does not change that APK.
- Open the root project in Android Studio with JDK 17 and Android SDK 35. The Gradle wrapper pins Gradle 8.9 for Android Gradle Plugin 8.7.3. Run `./gradlew :app:testDebugUnitTest :app:assembleDebug` (`.\gradlew.bat` on Windows) once the toolchain is configured. The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
- Android 0.2.2: five unit tests pass; APK label, package, version and signature verified. Device testing remains pending.
- Record improvements as GitHub issues and implement them in branches with pull requests. Never commit signing keys or local SDK paths.
