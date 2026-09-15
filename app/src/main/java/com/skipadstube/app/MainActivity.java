package com.skipadstube.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
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
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;

public final class MainActivity extends Activity {
    private static final String PREFS = "skipadstube";
    private TextView diagnostics;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
            int stream = RuntimeStatus.connected ? AudioManager.STREAM_ACCESSIBILITY : AudioManager.STREAM_MUSIC;
            setVolumeControlStream(stream);
            diagnostics.setText("Versión 0.2.7 · Servicio " + (RuntimeStatus.connected ? "conectado" : "desconectado")
                + "\n" + RuntimeStatus.scan + "\n" + RuntimeStatus.lastAd
                + "\nVolumen de avisos: " + audio.getStreamVolume(stream) + "/" + audio.getStreamMaxVolume(stream)
                + "\n" + RuntimeStatus.sound
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
        info.setText("Automatización local y limitada a YouTube. No usa Internet ni recopila datos. Activa el servicio y después reproduce un vídeo con anuncios.");
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
