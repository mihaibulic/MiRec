package com.mihai.mirecs;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.Session;
import com.mihai.mirecs.data.Entity;
import com.mihai.mirecs.data.FacebookRecommender;
import com.mihai.mirecs.data.ResultListener;

public class MainActivity extends Activity implements ResultListener {

    private FacebookRecommender mFb = new FacebookRecommender();

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
            setText("FB LOGGED IN");
            mFb.computeRecommendations(this);
        } else {
            setText("FB FAILED TO LOG IN");
        }
    }

    @Override
    public void onResultsReady(List<Entity> results) {
        MainAdapter adapter = new MainAdapter(this, results);
        ((ListView) findViewById(R.id.recs)).setAdapter(adapter);
        setText("FB DONE");
    }

    private void setText(String text) {
        ((TextView) findViewById(R.id.info)).setText(text);
    }
}
