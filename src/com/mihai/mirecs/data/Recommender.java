package com.mihai.mirecs.data;

import android.app.Activity;

public interface Recommender {
    public void authenticate(final Activity act, final ResultListener listener);
    public void computeRecommendations(final ResultListener listener);
}
