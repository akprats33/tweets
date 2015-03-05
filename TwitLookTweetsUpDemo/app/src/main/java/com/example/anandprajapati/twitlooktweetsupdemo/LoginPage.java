package com.example.anandprajapati.twitlooktweetsupdemo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by Anand Prajapati on 3/3/2015.
 */
public class LoginPage extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_login);
        WebView login = (WebView) findViewById(R.id.webView_login);

        login.getSettings().setJavaScriptEnabled(true);

        String url = getIntent().getStringExtra("url");
        login.loadUrl(url);

        login.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Here put your code
                Log.d("My Webview", url);

                // return true; //Indicates WebView to NOT load the url;
                return false; //Allow WebView to load url
            }
        });
    }
}
