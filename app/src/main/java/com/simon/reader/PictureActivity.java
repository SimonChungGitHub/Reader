package com.simon.reader;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class PictureActivity extends AppCompatActivity {
    private final Handler handler = new Handler();
    private int columns = 2;
    private String url = "http://192.168.0.238/okhttp/api/values/FileUpload";
    private PointF startPoint = new PointF();

    private ArrayList<File> pictures;
    private ArrayList<File> picturesCheck;
    private BaseAdapter adapter;
    private LinearLayout linearLayout;
    private ProgressBar progressbar;
    private int totalCount = 0;
    private int successCount = 0;
    private Dialog dialog;
    private Snackbar snackbar;
    private SharedPreferences preferences;

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
                    startActivity(new Intent(this, NFCActivity.class));
                    overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out);
                    finish();
                    return true;
                } else if (Horizontal && moveX > 100) {
                    startActivity(new Intent(this, QRCodeGenerateActivity.class));
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
        setContentView(R.layout.activity_picture);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        url = preferences.getString("url", "");
        pictures = getPictures();
        picturesCheck = new ArrayList<>();
        adapter = getAdapter();
        GridView gridView = findViewById(R.id.gridview);
        gridView.setNumColumns(columns);
        gridView.setAdapter(adapter);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        url = preferences.getString("url", "");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pictures.clear();
        picturesCheck.clear();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.menu_delete).setVisible(true);
        menu.findItem(R.id.menu_upload).setVisible(true);
        menu.findItem(R.id.menu_settings).setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.menu_delete) {
            if (picturesCheck.size() > 0) {
                new AlertDialog.Builder(this)
                        .setTitle("檔案刪除")
                        .setMessage("刪除後檔案無法回復")
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            deletePictures();
                            adapter.notifyDataSetChanged();
                        })
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                        .show();
            }
        } else if (id == R.id.menu_upload) {
            if (url == null || url.equals("")) {
                snackbar = Snackbar.make(findViewById(R.id.gridview), "url 未設定", Snackbar.LENGTH_INDEFINITE);
                snackbar.show();
                return true;
            }
            if (picturesCheck.size() > 0) {
                new AlertDialog.Builder(this)
                        .setTitle("檔案傳送")
                        .setMessage("傳送後將刪除已傳送檔案")
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> new Thread(this::okhttpAsyncUpload).start())
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                        .show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private ArrayList<File> getPictures() {
        ArrayList<File> list = new ArrayList<>();
        Path path = QRCodeGenerateActivity.QRCode_Save_Path;
        File file = path.toFile();
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File childFile : files) {
                    if (!childFile.isDirectory() && childFile.toString().endsWith(".jpg")) {
                        list.add(childFile);
                    }
                }
            }
        }
        return list;
    }

    private BaseAdapter getAdapter() {
        return new BaseAdapter() {
            @Override
            public int getCount() {
                return pictures.size();
            }

            @Override
            public File getItem(int position) {
                return pictures.get(position);
            }

            @Override
            public long getItemId(int i) {
                return i;
            }

            @SuppressLint({"ViewHolder", "UseCompatLoadingForDrawables"})
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                File file = getItem(position);
                convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.imageview_for_grid, parent, false);
                ImageView imageView = convertView.findViewById(R.id.imageView_for_grid);
                ImageView check = convertView.findViewById(R.id.imageView_for_check);
                check.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_check_24, null));
                check.setVisibility(View.GONE);
                for (File checkFile : picturesCheck) {
                    if (checkFile.equals(file)) {
                        check.setVisibility(View.VISIBLE);
                    }
                }
                Bitmap bitmap = BitmapFactory.decodeFile(file.toString());
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageView.setImageBitmap(bitmap);
                convertView.setOnClickListener(view -> {
                    if (check.getVisibility() == View.VISIBLE) {
                        check.setVisibility(View.GONE);
                        picturesCheck.remove(file);
                    } else {
                        check.setVisibility(View.VISIBLE);
                        picturesCheck.add(file);
                    }
                });
                convertView.setOnLongClickListener(v -> {
                    Log.e("aaa", "long click");
                    return true;
                });
                return convertView;
            }
        };
    }

    private void deletePictures() {
        Iterator<File> it = picturesCheck.iterator();
        while (it.hasNext()) {
            File file = it.next();
            it.remove();
            try {
                Files.deleteIfExists(file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            pictures.remove(file);
        }
    }

    @SuppressLint("SetTextI18n")
    protected void okhttpAsyncUpload() {
        totalCount = picturesCheck.size();
        successCount = 0;
        ArrayList<File> readyImages = new ArrayList<>();
        ArrayList<Thread> threads = new ArrayList<>();
        String Ready = "Ready... ";
        String Uploading = "Uploading... ";

        linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPaddingRelative(20, 5, 20, 0);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(linearLayout)
                .setCancelable(false);
        TextView uploading = new TextView(this);
        uploading.setText("Uploading");
        uploading.setTextColor(Color.RED);
        uploading.setTextSize(20f);
        progressbar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressbar.setProgress(0);
        progressbar.setMax(totalCount);
        handler.post(() -> {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            uploading.setText(Ready + progressbar.getProgress() + "/" + totalCount);
            linearLayout.addView(uploading);
            linearLayout.addView(progressbar);
            dialog = builder.create();
            dialog.show();
        });

        long start = new Date().getTime();
        for (File file : picturesCheck) {
            handler.post(() -> {
                progressbar.incrementProgressBy(1);
                uploading.setText(Ready + progressbar.getProgress() + "/" + totalCount);
            });
            if (searchICC_PROFILE(Paths.get(file.toString()))) {
                removeICC_PROFILE(Paths.get(file.toString()));
            }
            readyImages.add(file);
            uploading.setText(Ready + progressbar.getProgress() + "/" + totalCount);
        }

        totalCount = readyImages.size();

        handler.post(() -> {
            progressbar.setProgress(0);
            progressbar.setMax(totalCount);
            uploading.setText(Uploading + progressbar.getProgress() + "/" + totalCount);
        });
        for (File file : readyImages) {
            Thread thread = new Thread(() -> {
                ProgressBar subProgressbar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                subProgressbar.setProgress(0);
                handler.post(() -> linearLayout.addView(subProgressbar));
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("dept", "upload")
                        .addFormDataPart("image", file.getName(),
                                RequestBody.create(MediaType.parse("image/jpeg"), file))
                        .build();
                final CountingRequestBody.Listener progressListener = (bytesRead, contentLength) -> {
                    final int progress = (int) (((double) bytesRead / contentLength) * 100);
                    handler.post(() -> subProgressbar.setProgress(progress));
                };
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(3, TimeUnit.SECONDS)
                        .writeTimeout(5, TimeUnit.MINUTES)
                        .readTimeout(5, TimeUnit.MINUTES)
                        .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                        .addNetworkInterceptor(chain -> {
                            Request originalRequest = chain.request();
                            if (originalRequest.body() == null) {
                                return chain.proceed(originalRequest);
                            }
                            Request progressRequest = originalRequest.newBuilder()
                                    .method(originalRequest.method(),
                                            new CountingRequestBody(originalRequest.body(), progressListener))
                                    .build();
                            return chain.proceed(progressRequest);
                        })
                        .build();
                Request request = new Request.Builder()
                        .header("Content-Type", "multipart/form-data")
                        .url(url)
                        .post(requestBody)
                        .build();
                try {
                    Response response = client.newCall(request).execute();
                    if (response.code() == 204) {
                        try {
                            Files.deleteIfExists(file.toPath());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        pictures.remove(file);
                        successCount++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                handler.post(() -> {
                    linearLayout.removeView(subProgressbar);
                    progressbar.incrementProgressBy(1);
                    uploading.setText(Uploading + progressbar.getProgress() + "/" + totalCount);
                });
            });
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Log.e("aaa", (new Date().getTime() - start) + "---");
        handler.post(() -> {
            dialog.dismiss();
            linearLayout.removeAllViews();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            snackbar = Snackbar.make(findViewById(R.id.gridview), "成功 " + successCount + "    失敗 " + (totalCount - successCount), Snackbar.LENGTH_INDEFINITE);
            snackbar.show();
        });


    }

    private boolean searchICC_PROFILE(Path path) {
        try (FileInputStream fis = new FileInputStream(path.toFile());
             FileChannel fc = fis.getChannel()) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 1024);
            while (fc.read(byteBuffer) != -1) {
                byteBuffer.flip();
                String searchString = new String(byteBuffer.array());
                if (searchString.contains("ICC_PROFILE")) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void removeICC_PROFILE(Path path) {
        Path temp = Paths.get(path.toString().replace(".jpg", ""));
        try {
            Files.copy(path, temp, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (FileInputStream fis = new FileInputStream(temp.toFile());
             FileChannel readChannel = fis.getChannel();
             FileOutputStream fos = new FileOutputStream(path.toFile());
             FileChannel writeChannel = fos.getChannel()) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(256 * 1024);
            while (readChannel.read(byteBuffer) != -1) {
                String searchString = new String(byteBuffer.array());
                if (searchString.contains("ICC_PROFILE")) {
                    ByteBuffer byteBuffer2 = ByteBuffer.wrap(byteBuffer.array());
                    byteBuffer.clear();
                    boolean marker = false;
                    byte byte_0xFF = 0;
                    for (byte b : byteBuffer2.array()) {
                        if (marker) {
                            marker = false;
                            if ((b & 0xFF) != 0xE2) {
                                byteBuffer.put(byte_0xFF);
                                byteBuffer.put(b);
                            }
                        } else if ((b & 0xFF) == 0xFF) {
                            marker = true;
                            byte_0xFF = b;
                        } else {
                            byteBuffer.put(b);
                        }
                    }
                }
                byteBuffer.flip();
                writeChannel.write(byteBuffer);
                byteBuffer.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                Files.delete(temp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}