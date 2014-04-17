package com.mihai.mirecs;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Session;
import com.mihai.mirecs.data.Entity;
import com.mihai.mirecs.data.FacebookRecommender;
import com.mihai.mirecs.data.ResultListener;

public class MainActivity extends Activity implements ResultListener {

    private static final int TOTAL_QUERIES = 3;
    private static final int POST_DELAY = 5000;
    private static final int UPDATE_DELAY = 6000;
    private static final int DISMISS_DELAY = 7000;

    private static int sQueryCount = 0;

    private FacebookRecommender mFb = new FacebookRecommender();
    private Handler mHandler = new Handler();
    private MainAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFb.authenticate(this, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // required for Facebook
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    public void onAuthReady(boolean successful) {
        if (successful) {
            setText("Successfully logged in, now querying...");
            sQueryCount = 0;
            mFb.computeRecommendations(this);
        } else {
            setText("Whoops, couldn't log in.");
        }
    }

    @Override
    public void onResultsReady(List<Entity> results) {
        mAdapter = new MainAdapter(this, results);
        ((ListView) findViewById(R.id.recs)).setAdapter(mAdapter);
        setText("Progress: " + Math.round((100.0*(++sQueryCount))/TOTAL_QUERIES) + "%");

        if (sQueryCount == TOTAL_QUERIES) {
            (Toast.makeText(this, R.string.done, Toast.LENGTH_SHORT)).show();
            mAdapter.postRecommendations();
            mHandler.postDelayed(new Post(), POST_DELAY);
            mHandler.postDelayed(new Update(), UPDATE_DELAY);
            mHandler.postDelayed(new Dismiss(), POST_DELAY);
        }
    }

    private void setText(String text) {
        ((TextView) findViewById(R.id.info)).setText(text);
    }

    private class Post implements Runnable {
        @Override
        public void run() {
            mAdapter.postNextRecommendation();
            mHandler.postDelayed(new Post(), POST_DELAY);
        }
    }

    private class Update implements Runnable {
        @Override
        public void run() {
            mAdapter.updateNextRecommendation();
            mHandler.postDelayed(new Update(), UPDATE_DELAY);
        }
    }

    private class Dismiss implements Runnable {
        @Override
        public void run() {
            mAdapter.dismissRecommendation();
            mHandler.postDelayed(new Dismiss(), DISMISS_DELAY);
        }
    }
}
