package com.example.anandprajapati.twitlooktweetsupdemo;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by Anand Prajapati on 3/3/2015.
 */
public class TweetsSearch extends AsyncTask<String, Void, String> {
    final static String TwitterTokenURL = "https://api.twitter.com/oauth2/token";
    final static String TwitterSearchURL = "https://api.twitter.com/1.1/search/tweets.json?q=";
    final static int distanceInMiles = 10;
    private MyMapsActivity callingActivity;

    public TweetsSearch(MyMapsActivity ac) {
        this.callingActivity = ac;
    }

    @Override
    protected String doInBackground(String... searchTerms) {
        String result = null;
        try {

            String encodedUrl1 = URLEncoder.encode( "&geocode=" + searchTerms[0] +
                    "," + searchTerms[1] + "," + distanceInMiles + "mi", "UTF-8");

            String encodedUrl =  "&geocode=" + searchTerms[0] +
                    "," + searchTerms[1] + "," + distanceInMiles + "mi"
                    + "&result_type=recent";

            Log.e("makingrequest--------", TwitterSearchURL + encodedUrl);
            result = getStream(TwitterSearchURL + encodedUrl);
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        } catch (IllegalStateException ex1) {
            ex1.printStackTrace();
        }
        return result;
    }

    private String getStream(String url) {
        String results = null;
        try {
            String urlApiKey = URLEncoder.encode(MyMapsActivity.CONSUMER_KEY, "UTF-8");
            String urlApiSecret = URLEncoder.encode(MyMapsActivity.CONSUMER_KEY_SECRET, "UTF-8");
            // Concatenate the encoded consumer key, a colon character, and the encoded consumer secret
            String combined = urlApiKey + ":" + urlApiSecret;
            // Base64 encode the string
            String base64Encoded = Base64.encodeToString(combined.getBytes(), Base64.NO_WRAP);

            HttpPost httpPost = new HttpPost(TwitterTokenURL);

            httpPost.setHeader("Authorization", "Basic " + base64Encoded);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            httpPost.setEntity(new StringEntity("grant_type=client_credentials"));

            String rawAuthorization = getResponseBody(httpPost);
            Authenticated auth = jsonToAuthenticated(rawAuthorization);
            // Applications should verify that the value associated with the
            // token_type key of the returned object is bearer
            if (auth != null && auth.token_type.equals("bearer")) {
                // Authenticate API requests with bearer token
                HttpGet httpGet = new HttpGet(url);
                // construct a normal HTTPS request and include an Authorization
                // header with the value of Bearer <>
                httpGet.setHeader("Authorization", "Bearer " + auth.access_token);
                httpGet.setHeader("Content-Type", "application/json");
                // update the results with the body of the response
                results = getResponseBody(httpGet);
            }
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        } catch (IllegalStateException ex1) {
            ex1.printStackTrace();
        }
        return results;
    }

    // convert a JSON authentication object into an Authenticated object
    private Authenticated jsonToAuthenticated(String rawAuthorization) {
        Authenticated auth = null;
        if (rawAuthorization != null && rawAuthorization.length() > 0) {
            try {
                Gson gson = new Gson();
                auth = gson.fromJson(rawAuthorization, Authenticated.class);
            } catch (IllegalStateException ex) {
                // just eat the exception for now, but you'll need to add some handling here
                ex.printStackTrace();
            }
        }
        return auth;
    }

    private String getResponseBody(HttpRequestBase request) {
        StringBuilder sb = new StringBuilder();
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient(new BasicHttpParams());
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            String reason = response.getStatusLine().getReasonPhrase();
            Log.e("status@@@", statusCode + "");
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                InputStream inputStream = entity.getContent();
                BufferedReader bReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                String line = null;
                while ((line = bReader.readLine()) != null) {
                    sb.append(line);
                }

            } else {
                sb.append(reason);
            }
            Log.e("content--------", sb.toString());
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        } catch (ClientProtocolException ex1) {
            ex1.printStackTrace();
        } catch (IOException ex2) {
            ex2.printStackTrace();
        }
        return sb.toString();
    }

    @Override
    protected void onPostExecute(String result) {
        Searches searches = jsonToSearches(result);
        Log.e("onpostexecute---------", result + "");

        if(searches != null) {
            callingActivity.updateTweetsList(searches);
        }
        else {
            callingActivity.showMessage("No tweets in your area within "
                    + distanceInMiles + " miles");
        }
     }

    private Searches jsonToSearches(String result) {
        Searches searches = null;
        if (result != null && result.length() > 0) {
            try {
                Gson gson = new Gson();
                // bring back the entire search object
                SearchResults sr = gson.fromJson(result, SearchResults.class);
                // but only pass the list of tweets found (called statuses)
                searches = sr.getStatuses();
            } catch (IllegalStateException ex) {
                ex.printStackTrace();
            }
        }
        return searches;
    }
}
