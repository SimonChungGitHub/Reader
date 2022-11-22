package com.simon.reader;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.PointF;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class NFCActivity extends AppCompatActivity {
    private final PointF startPoint = new PointF();
    private NfcAdapter adapter = null;
    private PendingIntent pendingIntent = null;
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private TextView tagID;

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
                    startActivity(new Intent(this, QRCodeActivity.class));
                    overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out);
                    finish();
                    return true;
                } else if (Horizontal && moveX > 100) {
                    startActivity(new Intent(this, PictureActivity.class));
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
        setContentView(R.layout.activity_nfc);
        adapter = NfcAdapter.getDefaultAdapter(this);
        tagID = findViewById(R.id.tag_id);
        tagID.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
    }

    @SuppressLint({"SetTextI18n", "UnspecifiedImmutableFlag"})
    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            if (!adapter.isEnabled()) {
                new AlertDialog.Builder(this)
                        .setTitle("NFC功能未開啟")
                        .setCancelable(false)
                        .setMessage("無法感應標籤")
                        .setNegativeButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("開啟", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                return;
            }
            if (pendingIntent == null) {
                pendingIntent = PendingIntent.getActivity(this, 0,
                        new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT);
                tagID.setText("請感應標籤");
            }
            adapter.enableForegroundDispatch(this, pendingIntent, null, null);
        } else {
            tagID.setText("未發現NFC功能");

            new AlertDialog.Builder(this)
                    .setTitle("未發現NFC功能")
                    .setCancelable(true)
                    .setMessage("無法感應標籤")
                    .setNegativeButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (adapter != null) {
            adapter.disableForegroundDispatch(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.menu_qrcode).setVisible(true);
        menu.findItem(R.id.menu_qrcode_gen).setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_qrcode) {
            startActivity(new Intent(this, QRCodeActivity.class));
        } else if (id == R.id.menu_qrcode_gen) {
            startActivity(new Intent(this, QRCodeGenerateActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }


    @SuppressLint("SetTextI18n")
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            String tagId = bytesToHex(tag.getId());
            tagID.setText(tagId);
        }
    }

    public final String bytesToHex(byte[] bytes) {
        if (bytes == null) return null;
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return "0x" + new String(hexChars);
    }

}