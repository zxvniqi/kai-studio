package io.github.zxvniqi.naistudio;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    static final String HOME = "https://zxvniqi.github.io/kai-studio/";
    static final String HOST = "zxvniqi.github.io";
    static final int PICK = 7;

    WebView web;
    ValueCallback<Uri[]> pickCb;
    OutputStream saveOut;
    Uri saveUri;
    String saveName;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        web.setBackgroundColor(0xff0e0f21);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSupportMultipleWindows(false);
        s.setUserAgentString(s.getUserAgentString() + " NAIStudioApp");

        web.addJavascriptInterface(new Bridge(), "NAIApp");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if (isOurs(u)) return false;
                openExternal(u.toString());
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (pickCb != null) pickCb.onReceiveValue(null);
                pickCb = cb;
                try {
                    startActivityForResult(p.createIntent(), PICK);
                } catch (Exception e) {
                    pickCb = null;
                    return false;
                }
                return true;
            }
        });

        if (b != null) web.restoreState(b);
        else web.loadUrl(HOME);
    }

    @Override
    protected void onSaveInstanceState(Bundle o) {
        super.onSaveInstanceState(o);
        web.saveState(o);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == PICK && pickCb != null) {
            pickCb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            pickCb = null;
            return;
        }
        super.onActivityResult(req, res, data);
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("(window.naiBack&&naiBack())?1:0", r -> {
            if ("1".equals(r)) return;
            if (web.canGoBack()) web.goBack();
            else finish();
        });
    }

    static boolean isOurs(Uri u) {
        String p = u.getPath();
        return "https".equals(u.getScheme()) && HOST.equals(u.getHost()) && p != null && (p.startsWith("/kai-studio/") || p.startsWith("/nai-studio"));
    }

    boolean fromUs() {
        String cur = web.getUrl();
        return cur != null && isOurs(Uri.parse(cur));
    }

    void openExternal(String url) {
        Uri u = Uri.parse(url);
        String sc = u.getScheme();
        if (!"https".equals(sc) && !"http".equals(sc)) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {
        }
    }

    void toast(String m) {
        runOnUiThread(() -> Toast.makeText(this, m, Toast.LENGTH_SHORT).show());
    }

    class Bridge {
        // the page URL is read on the UI thread; bridge calls arrive on a background thread
        boolean onPage() {
            final boolean[] ok = {false};
            final java.util.concurrent.CountDownLatch l = new java.util.concurrent.CountDownLatch(1);
            runOnUiThread(() -> { ok[0] = fromUs(); l.countDown(); });
            try { l.await(2, java.util.concurrent.TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
            return ok[0];
        }

        @JavascriptInterface
        public void open(String url) {
            if (!onPage()) return;
            runOnUiThread(() -> openExternal(url));
        }

        @JavascriptInterface
        public void copy(String t) {
            if (!onPage()) return;
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("text", t));
        }

        // file save in pieces: begin -> chunk (base64) ... -> end
        @JavascriptInterface
        public boolean begin(String name, String mime) {
            try {
                if (!onPage()) return false;
                name = name.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1f]", "_");
                if (name.startsWith(".") || name.isEmpty()) name = "file_" + name;
                if (saveOut != null) try { saveOut.close(); } catch (Exception ignored) {}
                saveName = name;
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                    cv.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                    boolean img = mime != null && mime.startsWith("image/");
                    cv.put(MediaStore.MediaColumns.RELATIVE_PATH,
                            (img ? Environment.DIRECTORY_PICTURES : Environment.DIRECTORY_DOWNLOADS) + "/Kai Studio");
                    Uri col = img ? MediaStore.Images.Media.EXTERNAL_CONTENT_URI : MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                    saveUri = getContentResolver().insert(col, cv);
                    saveOut = getContentResolver().openOutputStream(saveUri);
                } else {
                    File d = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Kai Studio");
                    d.mkdirs();
                    saveOut = new FileOutputStream(new File(d, name));
                }
                return true;
            } catch (Exception e) {
                saveOut = null;
                return false;
            }
        }

        @JavascriptInterface
        public boolean chunk(String b64) {
            try {
                saveOut.write(Base64.decode(b64, Base64.DEFAULT));
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public boolean end() {
            try {
                saveOut.close();
                saveOut = null;
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
