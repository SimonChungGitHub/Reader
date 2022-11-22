package com.simon.reader;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class QRCodeActivity extends AppCompatActivity {
    private final PointF startPoint = new PointF();
    private CaptureManager captureManager;
    private TextView qrCodeMsg;
    private boolean torchStatus = false;
    private ImageButton torch;
    private SoundPool sounds;
    private int sound_scan;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                startPoint.set((int) ev.getX(), (int) ev.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                float moveX = ev.getX() - startPoint.x;
                float moveY = ev.getY() - startPoint.y;
                boolean Horizontal = Math.abs(moveX) > Math.abs(moveY);
                if (Horizontal && moveX < -100) {
                    startActivity(new Intent(this, QRCodeGenerateActivity.class));
                    overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out);
                    finish();
                    return true;
                } else if (Horizontal && moveX > 100) {
                    startActivity(new Intent(this, NFCActivity.class));
                    overridePendingTransition(R.anim.slide_in_left, android.R.anim.fade_out);
                    finish();
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);
        sounds = new SoundPool.Builder()
                .setMaxStreams(AudioManager.STREAM_MUSIC)
                .build();
        sound_scan = sounds.load(this, R.raw.beep, 1);
        qrCodeMsg = findViewById(R.id.qrCodeMsg);
        qrCodeMsg.setMovementMethod(new ScrollingMovementMethod());
        qrCodeMsg.setText("");
        DecoratedBarcodeView barcodeView = findViewById(R.id.barcode_scanner);
        captureManager = new CaptureManager(this, barcodeView);
        captureManager.initializeFromIntent(getIntent(), savedInstanceState);
        barcodeView.decodeContinuous(result -> {
            qrCodeMsg.setText(result.getText());
            vibrate();
            sounds.play(sound_scan, 5.0F, 5.0F, 1, 0, 1.0F);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        torch = findViewById(R.id.torch_view);
        torch.setOnClickListener(view -> {
            if (torchStatus) {
                barcodeView.setTorchOff();
                torchStatus = false;
                torch.setImageResource(R.drawable.ic_baseline_flash_off_24);
            } else {
                barcodeView.setTorchOn();
                torchStatus = true;
                torch.setImageResource(R.drawable.ic_baseline_flash_on_24);
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
            qrCodeMsg.setText("無相機權限");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        captureManager.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        captureManager.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        captureManager.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    new AlertDialog.Builder(this)
                            .setTitle("提示")
                            .setMessage("掃描必須開啟相機的權限")
                            .setNegativeButton(android.R.string.ok, (dialog, which) -> {
                                dialog.cancel();
                                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
                            })
                            .show();
                }
            } else {
                qrCodeMsg.setText("");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.menu_nfc).setVisible(true);
        menu.findItem(R.id.menu_qrcode_gen).setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_nfc) {
            startActivity(new Intent(this, NFCActivity.class));
            overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out);
            finish();
        } else if (id == R.id.menu_qrcode_gen) {
            startActivity(new Intent(this, QRCodeGenerateActivity.class));
            overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

}