package com.fitblock.app;
import android.content.SharedPreferences;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final int CAMERA_PERMISSION_CODE = 100;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    runOnUiThread(() -> {
                        String[] resources = request.getResources();
                        for (String resource : resources) {
                            if (resource.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                                if (ContextCompat.checkSelfPermission(MainActivity.this,
                                        Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                                } else {
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                                }
                                return;
                            }
                        }
                    });
                }
            }
        });

        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
        webView.loadUrl("file:///android_asset/fitblock.html");

        checkPermissions();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }

        if (!hasUsageStatsPermission()) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission Needed")
                    .setMessage("FitBlock needs Usage Access to detect when blocked apps open.")
                    .setPositiveButton("Grant", (dialog, which) -> {
                        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                    })
                    .setNegativeButton("Later", null)
                    .show();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission Needed")
                    .setMessage("FitBlock needs permission to display over other apps.")
                    .setPositiveButton("Grant", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    })
                    .setNegativeButton("Later", null)
                    .show();
        }

        if (!isAccessibilityServiceEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("Enable Accessibility")
                    .setMessage("Enable FitBlock in Accessibility settings to block apps.")
                    .setPositiveButton("Open Settings", (dialog, which) -> {
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    })
                    .setNegativeButton("Later", null)
                    .show();
        }
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private boolean isAccessibilityServiceEnabled() {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        if (accessibilityEnabled == 1) {
            String services = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (services != null) {
                return services.toLowerCase().contains(getPackageName().toLowerCase());
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                webView.reload();
            } else {
                Toast.makeText(this, "Camera permission needed for exercise tracking",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    public class WebAppInterface {
        Context context;

        WebAppInterface(Context c) {
            context = c;
        }

        @JavascriptInterface
        public String getState() {
            SharedPreferences prefs = context.getSharedPreferences("fitblock_state", MODE_PRIVATE);
            return prefs.getString("fitblock_state", "{}");
        }

        @JavascriptInterface
        public void setState(String jsonState) {
            SharedPreferences prefs = context.getSharedPreferences("fitblock_state", MODE_PRIVATE);
            prefs.edit().putString("fitblock_state", jsonState).apply();
        }

        @JavascriptInterface
        public void startMonitoring() {
            Intent intent = new Intent(context, MonitoringService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        }

        @JavascriptInterface
        public void stopMonitoring() {
            context.stopService(new Intent(context, MonitoringService.class));
        }
    }
}
