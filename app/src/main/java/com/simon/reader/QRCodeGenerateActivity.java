package com.simon.reader;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class QRCodeGenerateActivity extends AppCompatActivity {
    public static Path QRCode_Save_Path;
    private final PointF startPoint = new PointF();
    private Snackbar snackbar;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (snackbar != null) {
            snackbar.dismiss();
        }
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                startPoint.set((int) ev.getX(), (int) ev.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                float moveX = ev.getX() - startPoint.x;
                float moveY = ev.getY() - startPoint.y;
                boolean Horizontal = Math.abs(moveX) > Math.abs(moveY);
                if (Horizontal && moveX < -100) {
                    startActivity(new Intent(this, PictureActivity.class));
                    overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out);
                    finish();
                    return true;
                } else if (Horizontal && moveX > 100) {
                    startActivity(new Intent(this, QRCodeActivity.class));
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
        setContentView(R.layout.activity_qrcode_generate);
        QRCode_Save_Path = Paths.get(getFilesDir().toString(), "qrcode");
        EditText editText = findViewById(R.id.qrcode_content);
        ImageView ivCode = findViewById(R.id.qrcode);

        Button qrcodeSave = findViewById(R.id.qrcode_save);
        qrcodeSave.setOnClickListener(view -> {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (manager != null) {
                manager.hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
            if (Files.notExists(QRCode_Save_Path)) {
                try {
                    Files.createDirectory(QRCode_Save_Path);
                } catch (IOException e) {
                    snackbar = Snackbar.make(view, e.toString(), Snackbar.LENGTH_LONG);
                    snackbar.show();
                    return;
                }
            }
            Path path = Paths.get(QRCode_Save_Path.toString(), new Date().getTime() + ".jpg");
            if (!editText.getText().toString().equals("")) {
                Bitmap bitmap = genCode(editText.getText().toString());
                if (bitmap != null) {
                    ivCode.setImageBitmap(bitmap);
                    try {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(path.toFile()));
                        snackbar = Snackbar.make(view, "圖檔已儲存", Snackbar.LENGTH_LONG);
                        snackbar.show();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        snackbar = Snackbar.make(view, e.toString(), Snackbar.LENGTH_INDEFINITE);
                        snackbar.show();
                    }
                }
            }
        });
        Button qrcodeGenerate = findViewById(R.id.qrcode_gen);
        qrcodeGenerate.setOnClickListener(view -> {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (manager != null) {
                manager.hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
            if (!editText.getText().toString().equals("")) {
                Bitmap bitmap = genCode(editText.getText().toString());
                if (bitmap != null) {
                    ivCode.setImageBitmap(bitmap);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.menu_qrcode).setVisible(true);
        menu.findItem(R.id.menu_nfc).setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_qrcode) {
            startActivity(new Intent(this, QRCodeActivity.class));
            overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out);
            finish();
        } else if (id == R.id.menu_nfc) {
            startActivity(new Intent(this, NFCActivity.class));
            overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public Bitmap genCode(String content) {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.putIfAbsent(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8);
        BarcodeEncoder encoder = new BarcodeEncoder();
        try {
            return encoder.encodeBitmap(content, BarcodeFormat.QR_CODE,
                    500, 500, hints);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }

}