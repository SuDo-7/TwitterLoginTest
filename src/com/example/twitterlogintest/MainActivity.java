package com.example.twitterlogintest;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	// Constants
    /**
     * Register your here app https://dev.twitter.com/apps/new and get your
     * consumer key and secret
     * */
    static String TWITTER_CONSUMER_KEY = "KtKnDDoMK9g1XIE5c0HF9gNVJ";
    static String TWITTER_CONSUMER_SECRET = "3tTzV8sWb4wYCDSYvRsYuyJRrxcjggiXzyXIMjUrTncdBhCMvj";
 
    // Preference Constants
    static String PREFERENCE_NAME = "twitter_oauth";
    static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
    static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
    static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLogedIn";
 
    static final String TWITTER_CALLBACK_URL = "oauth://t4jsample";
 
    // Twitter oauth urls
    static final String URL_TWITTER_AUTH = "auth_url";
    static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
    static final String URL_TWITTER_OAUTH_TOKEN = "oauth_token";
 
    public static ArrayList<String> searchList=new ArrayList<String>();
    public ArrayAdapter<String> adapter;
    
    // Login button
    Button btnLoginTwitter;
    // Search hashtag button
    Button btnSearchHashtag;
    // Logout button
    Button btnLogoutTwitter;
    // EditText for Search
    EditText txtSearch;
    // lbl search
    TextView lblSearch;
    TextView lblUserName;
    // ListView
    ListView searchListView;
 
    // Progress dialog
    ProgressDialog pDialog;
    
    // Twitter
    private static Twitter twitter;
    private static RequestToken requestToken;
     
    // Shared Preferences
    private static SharedPreferences mSharedPreferences;
     
    // Internet Connection detector
    private ConnectionDetector cd;
     
    // Alert Dialog Manager
    AlertDialogManager alert = new AlertDialogManager();
 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
         
        cd = new ConnectionDetector(getApplicationContext());
 
        // Check if Internet present
        if (!cd.isConnectingToInternet()) {
            // Internet Connection is not present
            alert.showAlertDialog(MainActivity.this, "Internet Connection Error",
                    "Please connect to working Internet connection", false);
            // stop executing code by return
            return;
        }
         
        // Check if twitter keys are set
        if(TWITTER_CONSUMER_KEY.trim().length() == 0 || TWITTER_CONSUMER_SECRET.trim().length() == 0){
            // Internet Connection is not present
            alert.showAlertDialog(MainActivity.this, "Twitter oAuth tokens", "Please set your twitter oauth tokens first!", false);
            // stop executing code by return
            return;
        }
 
        // All UI elements
        btnLoginTwitter = (Button) findViewById(R.id.btnLoginTwitter);
        btnSearchHashtag = (Button) findViewById(R.id.btnSearchHashtag);
        btnLogoutTwitter = (Button) findViewById(R.id.btnLogoutTwitter);
        txtSearch = (EditText) findViewById(R.id.txtSearchHashtag);
        lblSearch = (TextView) findViewById(R.id.lblSearch);
        lblUserName = (TextView) findViewById(R.id.lblUserName);
        searchListView = (ListView)findViewById(R.id.searchListView);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, searchList);
        searchListView.setAdapter(adapter);
 
        // Shared Preferences
        mSharedPreferences = getApplicationContext().getSharedPreferences(
                "MyPref", 0);
        
        /**
         * Twitter login button click event will call loginToTwitter() function
         * */
        btnLoginTwitter.setOnClickListener(new View.OnClickListener() {
     
            @Override
            public void onClick(View arg0) {
                // Call login twitter function
                loginToTwitter();
            }
        });
        
        /**
         * Button click event to Search Hashtag, will call SeachTwitterHashtags()
         * function
         * */
        btnSearchHashtag.setOnClickListener(new View.OnClickListener() {
 
            @Override
            public void onClick(View v) {
                // Call search hashtag function
                // Get the status from EditText
                String status = txtSearch.getText().toString();
 
                // Check for blank text
                if (status.trim().length() > 0) {
                    // search hashtag
                	searchTwitterHashtag(status);
                	
                } else {
                    // EditText is empty
                    Toast.makeText(getApplicationContext(),
                            "Please enter search hashtag", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
 
        /**
         * Button click event for logout from twitter
         * */
        btnLogoutTwitter.setOnClickListener(new View.OnClickListener() {
 
            @Override
            public void onClick(View arg0) {
                // Call logout twitter function
                logoutFromTwitter();
            }
        });
     
        /** This if conditions is tested once is
         * redirected from twitter page. Parse the uri to get oAuth
         * Verifier
         * */
        if (!isTwitterLoggedInAlready()) {
            Uri uri = getIntent().getData();
            if (uri != null && uri.toString().startsWith(TWITTER_CALLBACK_URL)) {
                // oAuth verifier
                String verifier = uri
                        .getQueryParameter(URL_TWITTER_OAUTH_VERIFIER);
     
                try {
                    // Get the access token
                    AccessToken accessToken = twitter.getOAuthAccessToken(
                            requestToken, verifier);
     
                    // Shared Preferences
                    Editor e = mSharedPreferences.edit();
     
                    // After getting access token, access token secret
                    // store them in application preferences
                    e.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
                    e.putString(PREF_KEY_OAUTH_SECRET,
                            accessToken.getTokenSecret());
                    // Store login status - true
                    e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
                    e.commit(); // save changes
     
                    Log.e("Twitter OAuth Token", "> " + accessToken.getToken());
     
                    // Hide login button
                    btnLoginTwitter.setVisibility(View.GONE);
     
                    // Show Search Hashtag
                    lblSearch.setVisibility(View.VISIBLE);
                    txtSearch.setVisibility(View.VISIBLE);
                    btnSearchHashtag.setVisibility(View.VISIBLE);
                    btnLogoutTwitter.setVisibility(View.VISIBLE);
                    searchListView.setVisibility(View.VISIBLE);
                     
                    // Getting user details from twitter
                    // For now i am getting his name only
                    long userID = accessToken.getUserId();
                    User user = twitter.showUser(userID);
                    String username = user.getName();
                     
                    // Displaying in xml ui
                    lblUserName.setText(Html.fromHtml("<b>Welcome " + username + "</b>"));
                } catch (Exception e) {
                    // Check log for login errors
                    Log.e("Twitter Login Error", "> " + e.getMessage());
                }
            }
        }
    }
    /**
     * Function to login twitter
     * */
    private void loginToTwitter() {
        // Check if already logged in
        if (!isTwitterLoggedInAlready()) {
            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
            builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
            Configuration configuration = builder.build();
             
            TwitterFactory factory = new TwitterFactory(configuration);
            twitter = factory.getInstance();
 
            try {
                requestToken = twitter
                        .getOAuthRequestToken(TWITTER_CALLBACK_URL);
                this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                        .parse(requestToken.getAuthenticationURL())));
            } catch (TwitterException e) {
                e.printStackTrace();
            }
        } else {
            // user already logged into twitter
            Toast.makeText(getApplicationContext(),
                    "Already Logged into twitter", Toast.LENGTH_LONG).show();
        }
    }
 
    /**
     * Function to search hashtags
     * */
    private void searchTwitterHashtag(String... args){
	    ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
        builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
        builder.setOAuthAccessToken(mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN, ""));
        builder.setOAuthAccessTokenSecret(mSharedPreferences.getString(PREF_KEY_OAUTH_SECRET, ""));
        
        Configuration configuration = builder.build();
         
        TwitterFactory factory = new TwitterFactory(configuration);
        twitter = factory.getInstance();
        
        // Clear previous elements if any
        searchList.clear();
        
        try {
        	Query query = new Query(args[0]);        	
        	query.setResultType(Query.POPULAR);
            QueryResult result;
            do {
                result = twitter.search(query);
                List<Status> tweets = result.getTweets();
                for (Status tweet : tweets) {
                	String msg = "@" + tweet.getUser().getScreenName() + " - " + tweet.getText();
                	System.out.println(msg);
                	if(msg.trim()!="")
                	{
                		searchList.add(msg);
                		adapter.notifyDataSetChanged();
                	}                				
                }
            } while ((query = result.nextQuery()) != null);
	    }catch (TwitterException e) {
	    	// Unable to search
	    	Log.d("Failed to search tweets:", e.getMessage());
	    }
		
    	txtSearch.setText("");    	
    }
     
    /**
     * Function to logout from twitter
     * It will just clear the application shared preferences
     * */
    private void logoutFromTwitter() {
        // Clear the shared preferences
        Editor e = mSharedPreferences.edit();
        e.remove(PREF_KEY_OAUTH_TOKEN);
        e.remove(PREF_KEY_OAUTH_SECRET);
        e.remove(PREF_KEY_TWITTER_LOGIN);
        e.commit();
 
        // After this take the appropriate action
        // I am showing the hiding/showing buttons again
        // You might not needed this code
        btnLogoutTwitter.setVisibility(View.GONE);
        btnSearchHashtag.setVisibility(View.GONE);
        txtSearch.setVisibility(View.GONE);
        lblSearch.setVisibility(View.GONE);
        lblUserName.setText("");
        lblUserName.setVisibility(View.GONE);
        searchListView.setVisibility(View.GONE);
 
        btnLoginTwitter.setVisibility(View.VISIBLE);
    }
 
    /**
     * Check user already logged in your application using twitter Login flag is
     * fetched from Shared Preferences
     * */
    private boolean isTwitterLoggedInAlready() {
        // return twitter login status from Shared Preferences
        return mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
    }
 
    protected void onResume() {
        super.onResume();
    }
 }
