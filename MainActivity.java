package com.my.newproject;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.ContactsContract;
import android.view.View;
import android.webkit.*;
import android.widget.ProgressBar;
import android.widget.Toast;
import org.json.*;

public class MainActivity extends Activity {
    
    private WebView webView;
    private ProgressBar loader; // Loader ko yahan define kiya

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        setContentView(R.layout.main);

        // Views ko find karna
        webView = (WebView) findViewById(R.id.webView);
        loader = (ProgressBar) findViewById(R.id.loader);

        // Permissions maangna
        checkAndRequestPermissions();

        // WebView setup
        initWebView();
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CALL_PHONE
            };
            requestPermissions(permissions, 101);
        }
    }

    private void initWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        
        // --- Sabse Important Part: Loader ko Hide karna ---
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Jab page load ho jaye:
                loader.setVisibility(View.GONE);  // Loader ko hide kar do
                webView.setVisibility(View.VISIBLE); // WebView ko show kar do
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                request.grant(request.getResources());
            }
        });

        webView.addJavascriptInterface(new AIBridge(), "AndroidInterface");
        webView.loadUrl("file:///android_asset/index.html");
    }

    // --- JavaScript Bridge (Contact List aur Calling ke liye) ---
    public class AIBridge {
        @JavascriptInterface
        public String getContactNames() {
            JSONArray namesList = new JSONArray();
            try {
                Cursor cur = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
                if (cur != null) {
                    while (cur.moveToNext()) {
                        namesList.put(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));
                    }
                    cur.close();
                }
            } catch (Exception e) {}
            return namesList.toString();
        }

        @JavascriptInterface
        public void dialByName(final String targetName) {
            try {
                Cursor cur = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " = ?",
                    new String[]{targetName}, null);

                if (cur != null && cur.moveToFirst()) {
                    String num = cur.getString(0);
                    cur.close();
                    Intent intent = new Intent(Intent.ACTION_CALL);
                    intent.setData(Uri.parse("tel:" + num.replace(" ", "")));
                    startActivity(intent);
                }
            } catch (Exception e) {}
        }
    }
}
