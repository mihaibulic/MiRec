package com.mihai.mirecs.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.NewPermissionsRequest;
import com.facebook.SessionState;

public class FacebookRecommender implements Recommender {
    private static final String MUSIC = "MUSICIAN/band";
    private static final String TV = "Tv Show";
    private static final String MOVIE = "Movie";

    private static final String PATH = "/fql";
    private static final String ID = "page_id";
    private static final String DATA = "data";
    private static final String RESULTS = "fql_result_set";
    private static final String NAME = "name";
    private static final String PICTURE = "pic_square";

    private static final int ME_SCORE = 2;
    // optionall may filter for only verified entities: AND is_verified = 'true' to be certain we have no dups
    private static final String RES_QUERY = "SELECT page_id, name, pic_square FROM page WHERE page_id IN (SELECT page_id FROM #pageIds)";
    private static final String ME_QUERY = "{ \"pageIds\": \"SELECT page_id FROM page_fan WHERE uid = me() AND (type = '%s' OR type = '%s' OR type = '%s')\"," +
            "\"res\": \""+RES_QUERY+"\" }";

    private static final int CLOSE_FRIENDS_SCORE = 2;
    private static final String FLID_QUERY = "SELECT flid FROM friendlist WHERE owner = me() and type = 'close_friends'";
    private static final String FL_MEMBER_QUERY = "SELECT uid FROM friendlist_member WHERE flid IN ("+FLID_QUERY+")";
    private static final String CLOSE_FRIENDS_QUERY = "{ \"pageIds\": \"SELECT page_id FROM page_fan WHERE uid in ("+FL_MEMBER_QUERY+") AND (type = '%s' OR type = '%s' OR type = '%s')\","+
            "\"res\": \""+RES_QUERY+"\" }";

    private static final int FRIENDS_SCORE = 1;
    private static final String F_QUERY = "SELECT uid1 FROM friend where uid2 = me() LIMIT 50";
    private static final String FRIENDS_QUERY = "{ \"pageIds\": \"SELECT page_id FROM page_fan WHERE uid in ("+F_QUERY+") AND (type = '%s' OR type = '%s' OR type = '%s')\"," +
            "\"res\": \""+RES_QUERY+"\" }";

    private static Session sSession;
    private HashMap<Long, Entity> sEntities = new HashMap<Long, Entity>();

    @Override
    public void authenticate(final Activity act, final ResultListener listener) {
        Session.openActiveSession(act, true, new Session.StatusCallback() {
            @Override
            public void call(Session session, SessionState state, Exception exception) {
                switch(state) {
                    case OPENING:
                    case OPENED_TOKEN_UPDATED:
                    case CREATED:
                    case CREATED_TOKEN_LOADED:
                        // explicitly show all states, for completeness. Don't need to do anything.
                        break;
                    case OPENED:
                        if (!session.getPermissions().contains("user_likes")) {
                            session.requestNewReadPermissions(
                                    new NewPermissionsRequest(act,
                                            "user_friends", "user_likes", "friends_likes", "read_friendlists"));
                        } else {
                            sSession = session;
                            listener.onAuthReady(true);
                        }
                        break;
                    case CLOSED:
                    case CLOSED_LOGIN_FAILED:
                        listener.onAuthReady(false);
                        break;
                }
            }
        });
    }

    @Override
    public void computeRecommendations(final ResultListener listener) {
        computeRecommendations(ME_QUERY, ME_SCORE, listener);
        computeRecommendations(CLOSE_FRIENDS_QUERY, CLOSE_FRIENDS_SCORE, listener);
        computeRecommendations(FRIENDS_QUERY, FRIENDS_SCORE, listener);
    }

    public void computeRecommendations(final String query, final int score, final ResultListener listener) {
        Bundle params = new Bundle();
        params.putString("q", String.format(query, MUSIC, TV, MOVIE));
        new Request(sSession, PATH, params, HttpMethod.GET, new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                try {
                    synchronized(sEntities) {
                        JSONArray pageIds;
                        JSONArray res;
                        // count occurences of each entity
                        try {
                            pageIds = response
                                    .getGraphObject()
                                    .getInnerJSONObject()
                                    .getJSONArray(DATA)
                                    .getJSONObject(0) // we want pageIds
                                    .getJSONArray(RESULTS);
                        } catch (NullPointerException npe) {
                            return;
                        }
                        for (int i = 0; i < pageIds.length(); ++i) {
                            long pageId = pageIds.getJSONObject(i).optLong(ID, -1);
                            Entity entity = sEntities.get(pageId);
                            if (entity == null && pageId > 0) {
                                entity = new Entity(pageId);
                            }
                            entity.mScore += score;
                            sEntities.put(pageId, entity);
                        }

                        // create pages with number of occerences
                        try {
                            res = response
                                    .getGraphObject()
                                    .getInnerJSONObject()
                                    .getJSONArray(DATA)
                                    .getJSONObject(1) // we want res
                                    .getJSONArray(RESULTS);
                        } catch (NullPointerException npe) {
                            return;
                        }
                        JSONObject page = null;
                        for (int i = 0; i < res.length(); ++i) {
                            page = res.getJSONObject(i);

                            Entity entity = sEntities.get(page.optLong(ID, -1));
                            if (entity != null) {
                                entity.mName = page.optString(NAME, "");
                                entity.mPicture = page.optString(PICTURE, "");
                            }
                        }

                        ArrayList<Entity> rankedEntities = new ArrayList<Entity>();
                        for(Long id : sEntities.keySet()) {
                            Entity e = sEntities.get(id);
                            if (e != null && !TextUtils.isEmpty(e.mName) && !TextUtils.isEmpty(e.mPicture)) {
                                rankedEntities.add(e);
                            }
                        }

                        Collections.sort(rankedEntities, new Entity.EntityComparator());
                        listener.onResultsReady(rankedEntities);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).executeAsync();
    }
}
