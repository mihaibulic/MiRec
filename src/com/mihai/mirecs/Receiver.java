package com.mihai.mirecs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class Receiver extends BroadcastReceiver {

    private static final String PACKAGE = "com.mihai.mirecs";
    private static final String POST = PACKAGE + ".POST";
    private static final String UPDATE = PACKAGE + ".UPDATE";
    private static final String DISMISS = PACKAGE + ".DISMISS";

    private MainAdapter mAdapter;

    public Receiver(MainAdapter adapter) {
        super();
        mAdapter = adapter;
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(POST);
        filter.addAction(UPDATE);
        filter.addAction(DISMISS);

        return filter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(POST)) {
            mAdapter.postNextRecommendation();
        } else if (intent.getAction().equals(UPDATE)) {
            mAdapter.updateNextRecommendation();
        } else if (intent.getAction().equals(DISMISS)) {
            mAdapter.dismissRecommendation();
        }
    }
}
