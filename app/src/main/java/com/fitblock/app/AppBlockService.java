package com.fitblock.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class AppBlockService extends AccessibilityService {

    private Set<String> blockedApps = new HashSet<>();
    private String lastBlockedApp = "";
    private long lastBlockTime = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() != null) {
                String packageName = event.getPackageName().toString();

                loadBlockedApps();

                if (isAppBlocked(packageName)) {
                    long currentTime = System.currentTimeMillis();
                    if (!packageName.equals(lastBlockedApp) ||
                            (currentTime - lastBlockTime) > 1000) {

                        lastBlockedApp = packageName;
                        lastBlockTime = currentTime;

                        if (!hasAvailableTime()) {
                            launchBlockScreen(getAppName(packageName));
                            performGlobalAction(GLOBAL_ACTION_HOME);
                        }
                    }
                }
            }
        }
    }

    private void loadBlockedApps() {
        SharedPreferences prefs = getSharedPreferences("fitblock_state", MODE_PRIVATE);
        String blockedAppsJson = prefs.getString("blockedApps", "[]");

        blockedApps.clear();
        try {
            JSONArray array = new JSONArray(blockedAppsJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject app = array.getJSONObject(i);
                blockedApps.add(app.getString("id"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean isAppBlocked(String packageName) {
        String pkgLower = packageName.toLowerCase();
        if (pkgLower.contains("tiktok")) return blockedApps.contains("tiktok");
        if (pkgLower.contains("instagram")) return blockedApps.contains("instagram");
        if (pkgLower.contains("youtube")) return blockedApps.contains("youtube");
        if (pkgLower.contains("twitter") || pkgLower.contains("x.com")) 
            return blockedApps.contains("twitter");
        if (pkgLower.contains("facebook")) return blockedApps.contains("facebook");
        if (pkgLower.contains("snapchat")) return blockedApps.contains("snapchat");
        if (pkgLower.contains("reddit")) return blockedApps.contains("reddit");
        if (pkgLower.contains("netflix")) return blockedApps.contains("netflix");
        return false;
    }

    private boolean hasAvailableTime() {
        SharedPreferences prefs = getSharedPreferences("fitblock_state", MODE_PRIVATE);
        int availableMinutes = prefs.getInt("availableMinutes", 0);
        return availableMinutes > 0;
    }

    private String getAppName(String packageName) {
        String pkgLower = packageName.toLowerCase();
        if (pkgLower.contains("tiktok")) return "TikTok";
        if (pkgLower.contains("instagram")) return "Instagram";
        if (pkgLower.contains("youtube")) return "YouTube";
        if (pkgLower.contains("twitter")) return "Twitter";
        if (pkgLower.contains("facebook")) return "Facebook";
        if (pkgLower.contains("snapchat")) return "Snapchat";
        if (pkgLower.contains("reddit")) return "Reddit";
        if (pkgLower.contains("netflix")) return "Netflix";
        return "This app";
    }

    private void launchBlockScreen(String appName) {
        Intent intent = new Intent(this, BlockActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("blocked_app_name", appName);
        startActivity(intent);
    }

    @Override
    public void onInterrupt() {
        // Handle service interruption
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        // Service ready
    }
}
