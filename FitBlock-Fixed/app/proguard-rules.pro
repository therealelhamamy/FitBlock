# Add project specific ProGuard rules here.
-keep class com.fitblock.app.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
