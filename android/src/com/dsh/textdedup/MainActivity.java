package com.dsh.textdedup;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private static final int LIGHT_BG = 0xFFF2F5FB;
    private static final int DARK_BG = 0xFF070B15;

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(LIGHT_BG);
        window.setNavigationBarColor(Color.WHITE);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );

        webView = new WebView(this);
        webView.setBackgroundColor(LIGHT_BG);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setTextZoom(100);
        settings.setMediaPlaybackRequiresUserGesture(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new Bridge(), "AndroidApp");

        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("AndroidApp");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private void applyBars(final boolean dark) {
        runOnUiThread(() -> {
            Window window = getWindow();
            int bg = dark ? DARK_BG : LIGHT_BG;
            window.setStatusBarColor(bg);
            window.setNavigationBarColor(dark ? DARK_BG : Color.WHITE);

            int flags = window.getDecorView().getSystemUiVisibility();
            if (dark) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            window.getDecorView().setSystemUiVisibility(flags);
            if (webView != null) webView.setBackgroundColor(bg);
        });
    }

    private final class Bridge {

        @JavascriptInterface
        public boolean copy(String text) {
            try {
                ClipboardManager manager =
                        (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (manager == null) return false;
                manager.setPrimaryClip(ClipData.newPlainText("文字去重", text == null ? "" : text));
                return true;
            } catch (Exception error) {
                return false;
            }
        }

        @JavascriptInterface
        public String readClipboard() {
            try {
                ClipboardManager manager =
                        (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (manager == null || !manager.hasPrimaryClip()) return "";
                ClipData clip = manager.getPrimaryClip();
                if (clip == null || clip.getItemCount() == 0) return "";
                CharSequence text = clip.getItemAt(0).coerceToText(MainActivity.this);
                return text == null ? "" : text.toString();
            } catch (Exception error) {
                return "";
            }
        }

        @JavascriptInterface
        public boolean saveText(String name, String text) {
            String fileName = (name == null || name.trim().isEmpty())
                    ? "文字去重.txt"
                    : name.trim();
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
                values.put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/文字去重"
                );

                Uri uri = getContentResolver().insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                );
                if (uri == null) return false;

                try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                    if (output == null) return false;
                    output.write(0xEF);
                    output.write(0xBB);
                    output.write(0xBF);
                    output.write((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
                    output.flush();
                }
                return true;
            } catch (Exception error) {
                return false;
            }
        }

        @JavascriptInterface
        public void setTheme(boolean dark) {
            applyBars(dark);
        }
    }
}
