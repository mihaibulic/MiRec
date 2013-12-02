package com.mihai.mirecs.data;

import java.util.List;

public interface ResultListener {
    public void onAuthReady(boolean successful);
    public void onResultsReady(List<Entity> results);
}
