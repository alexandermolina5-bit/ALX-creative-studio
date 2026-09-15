package com.alx.creativestudio;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 41;
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private Uri selectedFileUri;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        webView = new WebView(this);
        setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new AndroidFiles(), "AndroidFiles");
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = params.createIntent();
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try { startActivityForResult(intent, FILE_CHOOSER_REQUEST); }
                catch (Exception e) {
                    fileCallback = null;
                    Toast.makeText(MainActivity.this, "No se encontró un selector de archivos.", Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            }
        });
        if (state == null) webView.loadUrl("file:///android_asset/index.html");
        else webView.restoreState(state);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && fileCallback != null) {
            if (resultCode == RESULT_OK && data != null) selectedFileUri = data.getData();
            fileCallback.onReceiveValue(resultCode == RESULT_OK ? WebChromeClient.FileChooserParams.parseResult(resultCode, data) : null);
            fileCallback = null;
        }
    }

    private class AndroidFiles {
        @JavascriptInterface public void openSelectedDocument() {
            runOnUiThread(() -> {
                if (selectedFileUri == null) {
                    Toast.makeText(MainActivity.this, "Primero abre un archivo PDF.", Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, selectedFileUri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intent, "Abrir o imprimir patrón"));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Instala un lector PDF para abrir e imprimir este patrón.", Toast.LENGTH_LONG).show();
                }
            });
        }

        @JavascriptInterface public void printImage(String dataUrl, String paper, String orientation, int scale) {
            runOnUiThread(() -> {
                final WebView printView = new WebView(MainActivity.this);
                String html = "<html><head><meta name='viewport' content='width=device-width'/><style>body{margin:0;text-align:center}img{width:" + scale + "%;height:auto}</style></head><body><img src='" + dataUrl + "'/></body></html>";
                printView.setWebViewClient(new WebViewClient() {
                    @Override public void onPageFinished(WebView view, String url) {
                        PrintManager manager = (PrintManager) getSystemService(PRINT_SERVICE);
                        PrintAttributes.MediaSize size = PrintAttributes.MediaSize.ISO_A4;
                        if ("Letter".equals(paper)) size = PrintAttributes.MediaSize.NA_LETTER;
                        else if ("Legal".equals(paper)) size = PrintAttributes.MediaSize.NA_LEGAL;
                        else if ("A3".equals(paper)) size = PrintAttributes.MediaSize.ISO_A3;
                        size = "landscape".equals(orientation) ? size.asLandscape() : size.asPortrait();
                        PrintAttributes attributes = new PrintAttributes.Builder().setMediaSize(size).setColorMode(PrintAttributes.COLOR_MODE_COLOR).build();
                        manager.print("Patrón ALX Creative Studio", view.createPrintDocumentAdapter("Patrón ALX"), attributes);
                    }
                });
                printView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            });
        }

        @JavascriptInterface public void saveDataUrl(String dataUrl, String fileName) {
            new Thread(() -> {
                try {
                    int comma = dataUrl.indexOf(',');
                    byte[] bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT);
                    OutputStream out;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                        values.put(MediaStore.Downloads.MIME_TYPE, "image/png");
                        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ALX Creative Studio");
                        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                        if (uri == null) throw new Exception("No se pudo crear el archivo");
                        out = getContentResolver().openOutputStream(uri);
                    } else {
                        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ALX Creative Studio");
                        if (!dir.exists()) dir.mkdirs();
                        out = new FileOutputStream(new File(dir, fileName));
                    }
                    if (out == null) throw new Exception("No se pudo abrir el archivo");
                    out.write(bytes); out.close();
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Guardado en Descargas/ALX Creative Studio", Toast.LENGTH_LONG).show());
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "No se pudo guardar la imagen.", Toast.LENGTH_LONG).show());
                }
            }).start();
        }
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
    @Override protected void onSaveInstanceState(Bundle out) { webView.saveState(out); super.onSaveInstanceState(out); }
    @Override protected void onDestroy() { if (webView != null) webView.destroy(); super.onDestroy(); }
}
