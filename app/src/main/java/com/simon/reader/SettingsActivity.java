package com.simon.reader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {
    private final PointF startPoint = new PointF();
    private SharedPreferences preferences;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                startPoint.set((int) ev.getX(), (int) ev.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                float moveX = ev.getX() - startPoint.x;
                float moveY = ev.getY() - startPoint.y;
                boolean Horizontal = Math.abs(moveX) > Math.abs(moveY);
                if (Horizontal && moveX > 300) {
                    startActivity(new Intent(this, PictureActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                    return true;
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(View.SYSTEM_UI_FLAG_FULLSCREEN);
        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_settings);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);


        Button updateUrl = findViewById(R.id.settings_update_url);
        TextView url = findViewById(R.id.settings_url);
        url.setText(preferences.getString("url", ""));
        url.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                updateUrl.setVisibility(View.VISIBLE);
            }
        });
        updateUrl.setOnClickListener(v -> {
            preferences.edit().putString("url", url.getText().toString().trim()).apply();
            v.setVisibility(View.GONE);
            url.setFocusable(false);
        });

    }
}