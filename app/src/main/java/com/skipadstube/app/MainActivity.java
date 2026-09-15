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

public final class MainActivity extends Activity {
    private static final String PREFS = "skipadstube";

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
        setContentView(root);
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
