package com.example.anandprajapati.twitlooktweetsupdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class MyMapsActivity extends Activity implements GoogleMap.OnMarkerClickListener{
    public final static String CONSUMER_KEY = "jOMctKQE7KfQuMmRwICqFLsGe";
    public final static String CONSUMER_KEY_SECRET = "ywU8xLZO7hcIZYO2vFNxcdc4y9ZZoUBZEdAHAeNpiIWgNBJmP8";

    private static Twitter twitter;
    private static RequestToken requestToken;
    public static final int rCode = 33;

    private SharedPreferences mSharedPreferences;
    public static final String PREF_KEY_LOGIN = "Pref_Login" ;
    public static final String PREF_NAME = "app_settings";
    public static final String PREF_Oauth = "Pref_oauth_key";
    public static final String PREF_Oauth_secret = "Pref_oauth_secret";
    static final String Oauth_Verifier = "oauth_verifier";

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    public static String location_context = Context.LOCATION_SERVICE;
    LocationManager lManager;
    private double lati = 28.61;
    private double longi = 77.23;
    private LocationProvider gpsProvider;
    private static boolean running = false;
    private static HashMap<Long,Tweet> tweets;
    private HashMap<Long, Marker> tweetMarkers;
    public Handler handler = new Handler();
    private Marker yourMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        lManager = (LocationManager)getSystemService(location_context);
        String providerName = LocationManager.GPS_PROVIDER;
        boolean enabled = lManager.isProviderEnabled(providerName);

        if(! enabled) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        this.tweetMarkers = new HashMap<Long, Marker>();
        tweets = new HashMap<Long,Tweet>();

        gpsProvider = lManager.getProvider(providerName);
        Location location = lManager.getLastKnownLocation(providerName);

        if (location != null) {
//            onLocationChanged(location);
            this.lati = location.getLatitude();
            this.longi = location.getLongitude();
        }

        setUpMapIfNeeded();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(this.lati,this.longi), 15));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);

        mSharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        Uri uri = getIntent().getData();
        Thread loginDetails = new Thread(
                new updateLoginDetails(uri));
        loginDetails.start();

        Authenticate login = new Authenticate();
        new Thread(login).start();

        if(!running) {
            searchTweets();
        }
    }

    public void updateTweetsList(Searches newTweets){
        boolean changed = false;
        for(Search s : newTweets) {
            if(!tweets.containsKey(s.getId())){
                changed = true;
                if(tweets.size() >= 100) {
                    Log.e("Deleting---","removing some tweet");
                    removeOldest();
                }

                Tweet tweetFromSearch = new Tweet();
                tweetFromSearch.data = s;

                Date creationDate = new Date(s.getDateCreated());

                tweetFromSearch.creationTime = creationDate.getTime();

                tweets.put(s.getId(), tweetFromSearch);

                Thread addition = new Thread(
                        new tweetAdd(tweetFromSearch));
                addition.start();
            }
        }
    }

    public void removeOldest() {
        long min = 1<<30;
        long minId = -1;
        for (Map.Entry<Long, Tweet> entry : tweets.entrySet()) {
            Tweet someTweet = entry.getValue();
            long creation = someTweet.creationTime;
            if(creation < min) {
                min = creation;
                minId = entry.getKey();
            }
        }

        if( minId != -1) {
            tweets.remove(minId);
            Thread removal = new Thread(
                    new tweetRemoval(minId));
            removal.start();
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if(! marker.equals(yourMarker)) {

            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));

            AlertDialog.Builder builder = new AlertDialog.Builder(MyMapsActivity.this);

            long id = Long.parseLong(marker.getTitle());
            Tweet clicked = tweets.get(id);

            builder.setMessage(clicked.data.getId() + "\n" +
                            clicked.data.getText() + "\n" +
                            clicked.data.getSource() + "\n" +
                            clicked.data.getDateCreated());

            builder.setNegativeButton("Close", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
            builder.show();
        }
        return true;
    }

    /**
     * remove tweet marker from the map
     */
    class tweetRemoval implements  Runnable {

        long tweetID;
        public tweetRemoval(long id) {
            this.tweetID = id;
        }

        @Override
        public void run() {
            if(tweetMarkers.containsKey(tweetID)) {
                if(mMap != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tweetMarkers.get(tweetID).remove();
                            tweetMarkers.remove(tweetID);
                        }
                    });
                }
            }
        }
    }

    /**
     * add new tweet's marker
     */
    class tweetAdd implements Runnable {

        private Tweet newTweet;

        public tweetAdd(Tweet someTweet) {
            this.newTweet = someTweet;
        }

        @Override
        public void run() {
            if(! tweetMarkers.containsKey(newTweet.data.getId())) {
                if(mMap != null) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            double tLatitude = newTweet.data.getGeo().getPoints().get(0);
                            double tLongitude = newTweet.data.getGeo().getPoints().get(1);
                            String title = newTweet.data.getId() + "";

                            Marker tMarker = mMap.addMarker(
                                    new MarkerOptions().
                                            position(
                                                    new LatLng(tLatitude, tLongitude)).
                                            title(title).
                                            icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

                            tweetMarkers.put(newTweet.data.getId(), tMarker);
                        }
                    });
                }
            }
        }
    }

    public void showMessage(String msg) {
        final String message = String.valueOf(msg);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MyMapsActivity.this,message,Toast.LENGTH_SHORT).show();
            }
        });
    }

    public class updateLoginDetails implements Runnable {

        private Uri uri;
        public updateLoginDetails(Uri localuri) {
            this.uri = localuri;
        }

        @Override
        public void run() {
            if(!isLoggedIn()) {
                Log.e("uri @@@@@@@@","" + uri);
//            Toast.makeText(MyMapsActivity.this,"URI "+ uri ,Toast.LENGTH_SHORT).show();
                if (uri != null) {
                    // oAuth verifier
                    String verifier = uri
                            .getQueryParameter(Oauth_Verifier);

                    try {
                        ConfigurationBuilder builder = new ConfigurationBuilder();
                        builder.setOAuthConsumerKey(CONSUMER_KEY);
                        builder.setOAuthConsumerSecret(CONSUMER_KEY_SECRET);
                        Configuration config = builder.build();
                        TwitterFactory factory = new TwitterFactory(config);
                        twitter = factory.getInstance();
//                    // Access Token
//                    String access_token = mSharedPreferences.getString(PREF_Oauth, "");
//                    // Access Token Secret
//                    String access_token_secret = mSharedPreferences.getString(PREF_Oauth_secret, "");
//
//                    AccessToken accessToken = new AccessToken(access_token, access_token_secret);

//                        Toast.makeText(MyMapsActivity.this, "verifier :" + verifier, Toast.LENGTH_SHORT).show();
//                        Toast.makeText(MyMapsActivity.this, "token :" + twitter, Toast.LENGTH_SHORT).show();
                        // Get the access token
                        AccessToken accessToken = twitter.getOAuthAccessToken(
                                requestToken, verifier);
//                        Toast.makeText(MyMapsActivity.this, "after token :" + accessToken, Toast.LENGTH_SHORT).show();
                        SharedPreferences.Editor e = mSharedPreferences.edit();

//                        Toast.makeText(MyMapsActivity.this, "token :" + accessToken.getToken(), Toast.LENGTH_SHORT).show();
                        e.putString(PREF_Oauth, accessToken.getToken());
                        e.putString(PREF_Oauth_secret, accessToken.getTokenSecret());

                        // Store login status - true
                        e.putBoolean(PREF_KEY_LOGIN, true);
                        e.commit();
                        //Toast.makeText(MyMapsActivity.this, "updated " + accessToken.getToken(), Toast.LENGTH_SHORT).show();

                    } catch (TwitterException te) {
                        te.printStackTrace();
                    }
                }
            }
        }
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (resultCode == RESULT_OK) {
//            switch (requestCode) {
//                case rCode:
//                    Uri uri = data.getData();
//                    if (uri != null) {
//                        // oAuth verifier
//                        String verifier = uri
//                                .getQueryParameter(Oauth_Verifier);
//
//                        try {
//                            // Get the access token
//                            AccessToken accessToken = twitter.getOAuthAccessToken(
//                                    requestToken, verifier);
//                            SharedPreferences.Editor e = mSharedPreferences.edit();
//
//                            e.putString(PREF_Oauth, accessToken.getToken());
//                            e.putString(PREF_Oauth_secret, accessToken.getTokenSecret());
//
//                            // Store login status - true
//                            e.putBoolean(PREF_KEY_LOGIN, true);
//                            e.commit();
//                        } catch(Exception ee) {
//                            ee.printStackTrace();
//                        }
//                    }
//                break;
//            }
//        }
//    }

    class Authenticate implements  Runnable {
        @Override
        public void run() {
            Log.e("Authenticating","false");
            if(!isLoggedIn()) {
                Log.e("Authenticating","true");
                ConfigurationBuilder builder = new ConfigurationBuilder();
                builder.setOAuthConsumerKey(CONSUMER_KEY);
                builder.setOAuthConsumerSecret(CONSUMER_KEY_SECRET);
                Configuration configuration = builder.build();

                TwitterFactory factory = new TwitterFactory(configuration);
                twitter = factory.getInstance();

                try {
                    requestToken = twitter.getOAuthRequestToken();

                    Intent loginIntent = new Intent(Intent.ACTION_VIEW, Uri
                            .parse(requestToken.getAuthenticationURL()));

                    MyMapsActivity.this.startActivity(loginIntent);

                } catch (TwitterException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isLoggedIn() {
        // return twitter login status from Shared Preferences
        return mSharedPreferences.getBoolean(PREF_KEY_LOGIN, false);
    }

    public void searchTweets() {

        Thread lookup = new Thread(new Runnable() {
            @Override
            public void run() {
                running = true;
                while(running) {

                    if(isNetworkOnline(MyMapsActivity.this) ) {

                        if (isLoggedIn()) {
                            try {

                                TweetsSearch finder = new TweetsSearch(MyMapsActivity.this);
                                Log.e("!!!!!!!!!!!!!!", "searching for tweets" + running);
                                finder.execute(lati + "", longi + "");
//                              Toast.makeText(MyMapsActivity.this,"lati :" +lati , Toast.LENGTH_SHORT).show();
                                Thread.sleep(10000);
                            } catch (InterruptedException ie) {
                                ie.printStackTrace();
                            }
                        }
                    }else {
                        running = false;
                        showMessage("Please check you Internet Connection !!");
                    }
                }
            }
        });

        lookup.start();
    }

    public boolean isNetworkOnline(Context ctx) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return (activeNetworkInfo != null  && activeNetworkInfo.isConnected());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Uri uri = getIntent().getData();
        Thread loginDetails = new Thread(
                new updateLoginDetails(uri));
        loginDetails.start();
        setUpMapIfNeeded();

        if(!running)
            searchTweets();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        Log.e("Maps--",mMap+"");
        if (mMap == null) {

            // Try to obtain the map from the SupportMapFragment.
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {

        yourMarker = mMap.addMarker(new MarkerOptions().
                position(new LatLng(this.lati, this.longi)).
                title("You").
                icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)) );

        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.setOnMarkerClickListener(this);
    }
}
