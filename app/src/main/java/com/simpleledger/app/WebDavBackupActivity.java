package com.simpleledger.app;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

/**
 * 6.0 WebDAV 云备份：支持坚果云/NextCloud 等 WebDAV 服务。
 * 用户自行填写 WebDAV URL、账号、密码（应用本地保存），通过用户手机联网进行备份/恢复。
 * 不依赖任何服务器，所有数据均通过 HTTPS 直接传输到用户自己的 WebDAV 网盘。
 */
public class WebDavBackupActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "webdav_backup";
    private static final String KEY_URL = "url";
    private static final String KEY_USER = "user";
    private static final String KEY_PWD = "pwd";
    private static final String KEY_LAST = "last_backup";

    private EditText etUrl, etUser, etPwd;
    private Button btnSave, btnTest, btnBackup, btnRestore;
    private TextView tvStatus;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_webdav);

        sp = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        LinearLayout topNav = findViewById(R.id.topNav);
        ViewCompat.setOnApplyWindowInsetsListener(topNav, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), statusBarHeight, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            HapticHelper.light(this);
            finish();
        });

        etUrl = findViewById(R.id.etUrl);
        etUser = findViewById(R.id.etUser);
        etPwd = findViewById(R.id.etPwd);
        btnSave = findViewById(R.id.btnSave);
        btnTest = findViewById(R.id.btnTest);
        btnBackup = findViewById(R.id.btnBackup);
        btnRestore = findViewById(R.id.btnRestore);
        tvStatus = findViewById(R.id.tvStatus);

        // 加载已保存的配置
        etUrl.setText(sp.getString(KEY_URL, ""));
        etUser.setText(sp.getString(KEY_USER, ""));
        etPwd.setText(sp.getString(KEY_PWD, ""));
        updateStatus();

        btnSave.setOnClickListener(v -> {
            HapticHelper.light(this);
            sp.edit()
                    .putString(KEY_URL, etUrl.getText().toString().trim())
                    .putString(KEY_USER, etUser.getText().toString().trim())
                    .putString(KEY_PWD, etPwd.getText().toString().trim())
                    .apply();
            Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show();
        });

        btnTest.setOnClickListener(v -> {
            HapticHelper.light(this);
            new DavTask(DavTask.ACTION_TEST, progressDialog("正在测试连接...")).execute();
        });

        btnBackup.setOnClickListener(v -> {
            HapticHelper.medium(this);
            new AlertDialog.Builder(this)
                    .setTitle("备份")
                    .setMessage("将上传当前数据库到 WebDAV，覆盖远程同名文件。继续？")
                    .setPositiveButton("继续", (d, w) -> {
                        new DavTask(DavTask.ACTION_UPLOAD, progressDialog("正在上传备份...")).execute();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        btnRestore.setOnClickListener(v -> {
            HapticHelper.medium(this);
            new AlertDialog.Builder(this)
                    .setTitle("恢复")
                    .setMessage("将从 WebDAV 下载数据库并覆盖本地数据，建议先备份。继续？")
                    .setPositiveButton("继续", (d, w) -> {
                        new DavTask(DavTask.ACTION_DOWNLOAD, progressDialog("正在下载备份...")).execute();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    private ProgressDialog progressDialog(String msg) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage(msg);
        pd.setCancelable(false);
        return pd;
    }

    private void updateStatus() {
        String last = sp.getString(KEY_LAST, "");
        if (TextUtils.isEmpty(last)) {
            tvStatus.setText("尚未备份");
        } else {
            tvStatus.setText("上次备份：" + last);
        }
    }

    /** 获取本地数据库文件路径 */
    private File getDbFile() {
        return getDatabasePath("simple_ledger.db");
    }

    /**
     * 拼接远程 WebDAV 文件 URL。
     * 用户填的 URL 末尾如果不是 / 则自动补 /，并附加文件名 simple_ledger.db
     */
    private String buildRemoteUrl() {
        String base = sp.getString(KEY_URL, "").trim();
        if (base.isEmpty()) return "";
        if (!base.endsWith("/")) base += "/";
        return base + "simple_ledger.db";
    }

    /**
     * WebDAV 操作任务：测试/上传/下载。
     * 使用 HttpURLConnection 发送 HTTP BASIC 认证请求，兼容 HTTP/HTTPS。
     * 对于 HTTPS 自签名证书场景，使用宽松的 TrustManager（仅在用户主动配置时生效）。
     */
    private class DavTask extends AsyncTask<Void, Void, String> {
        static final int ACTION_TEST = 0;
        static final int ACTION_UPLOAD = 1;
        static final int ACTION_DOWNLOAD = 2;
        final int action;
        final ProgressDialog pd;

        DavTask(int action, ProgressDialog pd) {
            this.action = action;
            this.pd = pd;
        }

        @Override
        protected void onPreExecute() {
            if (pd != null) pd.show();
        }

        @Override
        protected String doInBackground(Void... voids) {
            String urlStr = buildRemoteUrl();
            if (urlStr.isEmpty()) return "请先填写 WebDAV URL";
            String user = sp.getString(KEY_USER, "");
            String pwd = sp.getString(KEY_PWD, "");
            String basic = android.util.Base64.encodeToString(
                    (user + ":" + pwd).getBytes(), android.util.Base64.NO_WRAP);

            HttpURLConnection conn = null;
            try {
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "Basic " + basic);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);

                switch (action) {
                    case ACTION_TEST: {
                        // 用 PROPFIND 测试目录是否可访问，但因为 buildRemoteUrl 指向文件，
                        // 这里用 HEAD 请求检测是否能访问远程路径
                        conn.setRequestMethod("HEAD");
                        int code = conn.getResponseCode();
                        // 200/204 表示存在，404 表示不存在（但 WebDAV 可写），401 表示认证失败
                        if (code == 401) return "认证失败，请检查账号密码";
                        if (code == 403) return "无权限访问";
                        if (code == 200 || code == 204 || code == 404) {
                            return "连接成功";
                        }
                        return "连接返回码：" + code;
                    }
                    case ACTION_UPLOAD: {
                        // 上传：PUT
                        conn.setRequestMethod("PUT");
                        conn.setDoOutput(true);
                        File dbFile = getDbFile();
                        if (!dbFile.exists()) return "本地数据库不存在";
                        // 先关闭数据库连接
                        try { DatabaseHelper dh = new DatabaseHelper(WebDavBackupActivity.this); dh.close(); } catch (Exception ignored) {}
                        conn.setRequestProperty("Content-Type", "application/octet-stream");
                        OutputStream os = conn.getOutputStream();
                        FileInputStream fis = new FileInputStream(dbFile);
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = fis.read(buf)) > 0) os.write(buf, 0, n);
                        fis.close();
                        os.flush();
                        os.close();
                        int code = conn.getResponseCode();
                        if (code == 200 || code == 201 || code == 204) {
                            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(new Date());
                            sp.edit().putString(KEY_LAST, now).apply();
                            return "备份成功";
                        }
                        return "上传失败，返回码：" + code;
                    }
                    case ACTION_DOWNLOAD: {
                        // 下载：GET
                        conn.setRequestMethod("GET");
                        int code = conn.getResponseCode();
                        if (code == 404) return "远程备份不存在，请先备份";
                        if (code != 200) return "下载失败，返回码：" + code;
                        File dbFile = getDbFile();
                        // 先关闭数据库连接
                        try { DatabaseHelper dh = new DatabaseHelper(WebDavBackupActivity.this); dh.close(); } catch (Exception ignored) {}
                        InputStream is = conn.getInputStream();
                        FileOutputStream fos = new FileOutputStream(dbFile);
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
                        fos.flush();
                        fos.close();
                        is.close();
                        return "恢复成功";
                    }
                }
            } catch (Exception e) {
                return "错误：" + e.getMessage();
            } finally {
                if (conn != null) conn.disconnect();
            }
            return "未知错误";
        }

        @Override
        protected void onPostExecute(String result) {
            if (pd != null && pd.isShowing()) pd.dismiss();
            Toast.makeText(WebDavBackupActivity.this, result, Toast.LENGTH_LONG).show();
            updateStatus();
        }
    }
}
