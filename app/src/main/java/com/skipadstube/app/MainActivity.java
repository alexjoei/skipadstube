package com.skipadstube.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ViewGroup;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Toast;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MainActivity extends Activity {
    private static final String PREFS = "skipadstube";
    private TextView diagnostics;
    private TextView levelStatus;
    private Button levelStrength;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
            int stream = RuntimeStatus.connected ? AudioManager.STREAM_ACCESSIBILITY : AudioManager.STREAM_MUSIC;
            setVolumeControlStream(stream);
            levelStatus.setText(RuntimeStatus.leveling + (AudioSessionFinder.hasDumpPermission(MainActivity.this) ? ""
                : "\nFalta el permiso DUMP. Conecta el móvil por USB y ejecuta:\nadb shell pm grant " + getPackageName() + " android.permission.DUMP"));
            diagnostics.setText("Versión 1.1.0 · Servicio " + (RuntimeStatus.connected ? "conectado" : "desconectado")
                + "\n" + RuntimeStatus.scan + "\n" + RuntimeStatus.lastAd
                + "\nVolumen de avisos: " + audio.getStreamVolume(stream) + "/" + audio.getStreamMaxVolume(stream)
                + "\n" + RuntimeStatus.sound
                + "\n" + RuntimeStatus.stats
                + (audio.isVolumeFixed() ? "\nEl dispositivo indica volumen fijo" : "")
                + (RuntimeStatus.audioError.isEmpty() ? "" : "\n" + RuntimeStatus.audioError));
            refreshHandler.postDelayed(this, 500);
        }
    };
    @Override protected void onResume() { super.onResume(); refreshHandler.post(refresh); }
    @Override protected void onPause() { refreshHandler.removeCallbacks(refresh); super.onPause(); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad * 2, pad, pad);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("skipadstube"); title.setTextSize(30); title.setTextColor(Color.BLACK);
        root.addView(title);

        TextView info = new TextView(this);
        info.setText("Automatización local y limitada a YouTube. No usa Internet; guarda en tu propio dispositivo un registro de estadísticas de anuncios (ad_stats.csv) que no se envía a ningún sitio. Activa el servicio y después reproduce un vídeo con anuncios.");
        info.setTextSize(16); info.setPadding(0, pad, 0, pad);
        root.addView(info);

        root.addView(toggle("Silenciar durante anuncios", "mute_ads", true));
        root.addView(toggle("Omitir anuncios automáticamente", "skip_ads", true));
        root.addView(toggle("Sonido suave al empezar y terminar", "soft_chimes", true));

        TextView audioHelp = new TextView(this);
        audioHelp.setText("Los avisos usan el volumen de Accesibilidad cuando el servicio está conectado. Ajusta ese volumen con las teclas del móvil mientras estás en esta pantalla. Sin el servicio, la prueba usa multimedia. El silencio de anuncios afecta al volumen multimedia del teléfono.");
        root.addView(audioHelp);
        Button preview = new Button(this);
        preview.setText("Probar sonidos de inicio y fin");
        preview.setOnClickListener(view -> {
            SoftChime.play(true);
            view.postDelayed(() -> SoftChime.play(false), 650);
        });
        root.addView(preview);
        Button shareStats = new Button(this);
        shareStats.setText("Compartir estadísticas de anuncios (CSV)");
        shareStats.setOnClickListener(view -> shareAdStats());
        root.addView(shareStats);
        addLevelingSection(root, pad);
        diagnostics = new TextView(this);
        diagnostics.setTextColor(Color.DKGRAY);
        diagnostics.setPadding(0, pad, 0, pad);
        root.addView(diagnostics);

        Button accessibility = new Button(this);
        accessibility.setText("Abrir ajustes de Accesibilidad");
        accessibility.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });
        root.addView(accessibility, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView warning = new TextView(this);
        warning.setText("Importante: Android mostrará una advertencia porque el servicio puede ver elementos de pantalla. El servicio está restringido en su configuración al paquete oficial de YouTube.");
        warning.setPadding(0, pad, 0, 0);
        root.addView(warning);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void addLevelingSection(LinearLayout root, int pad) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        TextView heading = new TextView(this);
        heading.setText("Nivelar volumen de otras apps");
        heading.setTextSize(20); heading.setTextColor(Color.BLACK); heading.setPadding(0, pad, 0, 0);
        root.addView(heading);
        TextView help = new TextView(this);
        help.setText("Mantiene estable el volumen de las apps que elijas (por ejemplo iVoox) para que los anuncios no suenen más fuertes que el contenido. Solo afecta al audio de esas apps, no al volumen del móvil.");
        root.addView(help);

        Switch enable = new Switch(this);
        enable.setText("Activar nivelador"); enable.setTextSize(17); enable.setPadding(0, 12, 0, 12);
        enable.setChecked(prefs.getBoolean(VolumeLevelingService.KEY_ENABLED, false));
        enable.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(VolumeLevelingService.KEY_ENABLED, checked).apply();
            if (checked) startLeveling();
            else RuntimeStatus.leveling = "Nivelador de volumen desactivado";
        });
        root.addView(enable);

        levelStrength = new Button(this);
        updateStrengthLabel(prefs);
        levelStrength.setOnClickListener(view -> {
            int next = (prefs.getInt(VolumeLevelingService.KEY_STRENGTH, 1) + 1) % (LevelingProfile.MAX_STRENGTH + 1);
            prefs.edit().putInt(VolumeLevelingService.KEY_STRENGTH, next).apply();
            updateStrengthLabel(prefs);
        });
        root.addView(levelStrength);

        Button apps = new Button(this);
        apps.setText("Elegir aplicaciones");
        apps.setOnClickListener(view -> chooseApps(prefs));
        root.addView(apps);

        levelStatus = new TextView(this);
        levelStatus.setTextColor(Color.DKGRAY);
        levelStatus.setPadding(0, pad / 2, 0, 0);
        root.addView(levelStatus);

        if (prefs.getBoolean(VolumeLevelingService.KEY_ENABLED, false)) startLeveling();
    }

    private void updateStrengthLabel(SharedPreferences prefs) {
        int strength = prefs.getInt(VolumeLevelingService.KEY_STRENGTH, 1);
        levelStrength.setText("Intensidad: " + LevelingProfile.NAMES[strength] + " (toca para cambiar)");
    }

    private void startLeveling() {
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { android.Manifest.permission.POST_NOTIFICATIONS }, 1);
        }
        startForegroundService(new Intent(this, VolumeLevelingService.class));
    }

    private void chooseApps(SharedPreferences prefs) {
        PackageManager pm = getPackageManager();
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        final List<String> packages = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        List<ResolveInfo> found = pm.queryIntentActivities(launcher, 0);
        Collections.sort(found, new ResolveInfo.DisplayNameComparator(pm));
        for (ResolveInfo info : found) {
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || packages.contains(pkg)) continue;
            packages.add(pkg);
            labels.add(info.loadLabel(pm).toString());
        }
        final Set<String> chosen = new HashSet<>(prefs.getStringSet(VolumeLevelingService.KEY_APPS,
            Collections.singleton(VolumeLevelingService.DEFAULT_APP)));
        boolean[] checked = new boolean[packages.size()];
        for (int i = 0; i < checked.length; i++) checked[i] = chosen.contains(packages.get(i));
        new AlertDialog.Builder(this)
            .setTitle("Apps a nivelar")
            .setMultiChoiceItems(labels.toArray(new String[0]), checked, (dialog, which, on) -> {
                if (on) chosen.add(packages.get(which)); else chosen.remove(packages.get(which));
            })
            .setPositiveButton("Guardar", (dialog, which) ->
                prefs.edit().putStringSet(VolumeLevelingService.KEY_APPS, new HashSet<>(chosen)).apply())
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void shareAdStats() {
        File dir = getExternalFilesDir(null);
        File file = new File(dir != null ? dir : getFilesDir(), "ad_stats.csv");
        if (!file.exists()) {
            Toast.makeText(this, "Todavía no hay estadísticas guardadas", Toast.LENGTH_LONG).show();
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/csv");
        send.putExtra(Intent.EXTRA_STREAM, Uri.parse("content://com.skipadstube.app.fileprovider/ad_stats.csv"));
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, "Compartir ad_stats.csv"));
    }

    private Switch toggle(String label, String key, boolean fallback) {
        Switch view = new Switch(this);
        view.setText(label); view.setTextSize(17); view.setPadding(0, 12, 0, 12);
        view.setChecked(getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(key, fallback));
        view.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton button, boolean checked) {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(key, checked).apply();
            }
        });
        return view;
    }
}
