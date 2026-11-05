package hr.eltrans.evidencija;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private WebView view;
    private ValueCallback<Uri[]> fileCallback;
    private String pendingName, pendingContent, pendingMime;
    private static final int OPEN_FILE = 120;
    private static final int SAVE_FILE = 121;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = new WebView(this);
        setContentView(view);
        WebSettings s = view.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        view.addJavascriptInterface(new Object() {
            @JavascriptInterface public void exportFile(String name, String content, String mime) {
                runOnUiThread(() -> {
                    pendingName = name;
                    pendingContent = content;
                    pendingMime = mime;
                    Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType(mime);
                    i.putExtra(Intent.EXTRA_TITLE, name);
                    startActivityForResult(i, SAVE_FILE);
                });
            }
        }, "EltransAndroid");
        view.setWebViewClient(new WebViewClient());
        view.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if(fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.setType("application/json");
                i.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(i, OPEN_FILE);
                return true;
            }
        });
        view.loadUrl("file:///android_asset/index.html");
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == OPEN_FILE) {
            if(fileCallback != null) {
                fileCallback.onReceiveValue(resultCode == RESULT_OK && data != null && data.getData() != null ? new Uri[]{data.getData()} : null);
                fileCallback = null;
            }
        } else if (requestCode == SAVE_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try(OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                if(out != null) out.write(pendingContent.getBytes(StandardCharsets.UTF_8));
            } catch(Exception e) {
                new android.app.AlertDialog.Builder(this).setMessage("Datoteka nije spremljena: " + e.getMessage()).setPositiveButton("U redu",null).show();
            }
            pendingContent = null;
        }
    }
}
