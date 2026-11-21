package com.gpldroid.isslampro;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.view.Gravity;
import java.lang.ref.WeakReference;
import java.util.Collections;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import android.graphics.Color;

public class MainActivity extends Activity {
    private static final String HOME = "https://gpldroid.github.io/isslampro/";
    private static final String INTERNAL_HOST = "gpldroid.github.io";
    private static final String INTERNAL_PATH = "/isslampro/";
    private static final String GEO_ORIGIN = "https://gpldroid.github.io";
    private static final int LOCATION_REQUEST = 4102;
    private static final int NOTIFICATION_REQUEST = 4103;
    private static WeakReference<WebView> activeWebView = new WeakReference<>(null);
    static void sendToPage(String command) {
        WebView view = activeWebView.get();
        if (view != null) view.post(() -> view.evaluateJavascript("if(window.__nativeCmd)window.__nativeCmd('" + command + "')", null));
    }
    private WebView webView;
    private ProgressBar progress;
    private FrameLayout root;
    private View offlinePanel;
    private GeolocationPermissions.Callback pendingGeoCallback;
    private String pendingGeoOrigin;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        } else {
            window.setStatusBarColor(Color.rgb(8, 36, 35));
            window.setNavigationBarColor(Color.rgb(8, 36, 35));
        }
        window.getDecorView().setSystemUiVisibility(0);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(8, 36, 35));
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        } else {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                view.setPadding(insets.getSystemWindowInsetLeft(),
                    insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(),
                    insets.getSystemWindowInsetBottom());
                return insets;
            });
        }

        webView = new WebView(this);
        activeWebView = new WeakReference<>(webView);
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
                && WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addWebMessageListener(webView, "AndroidHost",
                Collections.singleton("https://gpldroid.github.io"),
                (view, message, sourceOrigin, isMainFrame, replyProxy) -> {
                    if (isMainFrame) NativeBridge.handle(this, message.getData());
                });
            String bridgeScript = "(()=>{const send=(method,args=[])=>window.AndroidHost.postMessage(JSON.stringify({method,args}));"
                + "window.UltraQuranNative={playbackState:(...a)=>send('playbackState',a),"
                + "schedulePrayers:(...a)=>send('schedulePrayers',a),clearPrayers:()=>send('clearPrayers'),"
                + "testAdhan:(...a)=>send('testAdhan',a),batteryFree:()=>false,"
                + "openBatterySettings:()=>send('openBatterySettings'),"
                + "openNotificationSettings:()=>send('openNotificationSettings'),openExactAlarmSettings:()=>send('openExactAlarmSettings')};})();";
            WebViewCompat.addDocumentStartJavaScript(webView, bridgeScript,
                Collections.singleton("https://gpldroid.github.io"));
        }
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        FrameLayout.LayoutParams bar = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, dp(3));
        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        root.addView(progress, bar);
        setContentView(root);
        root.requestApplyInsets();

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

        webView.setBackgroundColor(Color.rgb(245, 243, 235));
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                hideOfflinePanel();
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                hideOfflinePanel();
                requestNotificationPermissionOnce();
                super.onPageCommitVisible(view, url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                if (request.isForMainFrame()) showOfflinePanel();
                super.onReceivedError(view, request, error);
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (failingUrl != null && failingUrl.equals(view.getUrl())) showOfflinePanel();
                super.onReceivedError(view, errorCode, description, failingUrl);
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, android.webkit.WebResourceResponse response) {
                if (request.isForMainFrame() && response.getStatusCode() >= 500) showOfflinePanel();
                super.onReceivedHttpError(view, request, response);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return routeUrl(request.getUrl());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return routeUrl(Uri.parse(url));
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int value) {
                progress.setProgress(value);
                progress.setVisibility(value >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (!GEO_ORIGIN.equalsIgnoreCase(origin)) {
                    callback.invoke(origin, false, false);
                    return;
                }
                if (hasLocationPermission()) {
                    callback.invoke(origin, true, false);
                } else {
                    pendingGeoOrigin = origin;
                    pendingGeoCallback = callback;
                    if (Build.VERSION.SDK_INT >= 23) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST);
                    } else {
                        callback.invoke(origin, false, false);
                        pendingGeoCallback = null;
                        pendingGeoOrigin = null;
                    }
                }
            }
        });

        if (state == null) {
            webView.loadUrl(HOME);
        } else {
            webView.restoreState(state);
        }
    }

    private void requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return;
        android.content.SharedPreferences prefs = getSharedPreferences("isslampro_permissions", MODE_PRIVATE);
        if (prefs.getBoolean("notification_prompted", false)) return;
        prefs.edit().putBoolean("notification_prompted", true).apply();
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST);
    }

    private void showOfflinePanel() {
        if (root == null || isFinishing()) return;
        if (offlinePanel == null) {
            LinearLayout panel = new LinearLayout(this);
            panel.setOrientation(LinearLayout.VERTICAL);
            panel.setGravity(Gravity.CENTER);
            panel.setPadding(dp(28), dp(24), dp(28), dp(24));
            panel.setBackgroundColor(Color.rgb(8, 36, 35));

            TextView title = new TextView(this);
            title.setText("تعذر الاتصال بإسلام برو");
            title.setTextColor(Color.rgb(232, 208, 151));
            title.setTextSize(22);
            title.setGravity(Gravity.CENTER);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            panel.addView(title, new LinearLayout.LayoutParams(-1, -2));

            TextView message = new TextView(this);
            message.setText("تحقق من اتصال الإنترنت ثم أعد المحاولة. يمكنك الرجوع إلى التطبيق بمجرد عودة الاتصال.");
            message.setTextColor(Color.WHITE);
            message.setTextSize(16);
            message.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(-1, -2);
            messageParams.topMargin = dp(14);
            panel.addView(message, messageParams);

            Button retry = new Button(this);
            retry.setText("إعادة المحاولة");
            retry.setOnClickListener(v -> {
                hideOfflinePanel();
                if (webView != null) webView.reload();
            });
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-2, -2);
            buttonParams.topMargin = dp(22);
            panel.addView(retry, buttonParams);
            offlinePanel = panel;
            root.addView(offlinePanel, new FrameLayout.LayoutParams(-1, -1));
        }
        offlinePanel.setVisibility(View.VISIBLE);
        progress.setVisibility(View.GONE);
    }

    private void hideOfflinePanel() {
        if (offlinePanel != null) offlinePanel.setVisibility(View.GONE);
    }

    private boolean routeUrl(Uri uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme();
        String host = uri.getHost() == null ? "" : uri.getHost();
        String path = uri.getPath() == null ? "/" : uri.getPath();
        boolean trustedSitePath = path.equals("/isslampro") || path.startsWith(INTERNAL_PATH);
        if ("https".equalsIgnoreCase(scheme)
                && INTERNAL_HOST.equalsIgnoreCase(host)
                && trustedSitePath) {
            return false;
        }
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)
                || "mailto".equalsIgnoreCase(scheme) || "tel".equalsIgnoreCase(scheme)) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (Exception ignored) { }
            return true;
        }
        return true;
    }

    private boolean hasLocationPermission() {
        if (Build.VERSION.SDK_INT < 23) return true;
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == LOCATION_REQUEST && pendingGeoCallback != null) {
            pendingGeoCallback.invoke(pendingGeoOrigin, hasLocationPermission(), false);
            pendingGeoCallback = null;
            pendingGeoOrigin = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        if (webView != null) webView.saveState(out);
        super.onSaveInstanceState(out);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            if (activeWebView.get() == webView) activeWebView.clear();
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
