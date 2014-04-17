package com.mihai.mirecs;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mihai.mirecs.data.Entity;

public class MainAdapter extends BaseAdapter implements OnClickListener {

    private final Activity mActivity;
    private final int mMaxNotifications;
    private final ArrayList<Integer> mPostedNotifications = new ArrayList<Integer>();
    private final NotificationManager mNotificationManager;
    private final List<Entity> mContent;
    private HashSet<String> mTasks = new HashSet<String>();
    private final LruCache<String, Bitmap> mBitmaps = new LruCache<String, Bitmap>(200);
    private int mPosted = 0;
    private int mUpdated = 0;

    public MainAdapter(Activity act, List<Entity> content) {
        mActivity = act;
        mContent = content;
        mMaxNotifications = mActivity.getResources().getInteger(R.integer.max_notifications);
        mNotificationManager = (NotificationManager) mActivity.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int getCount() {
        return Math.min(mContent.size(), mMaxNotifications);
    }

    @Override
    public Entity getItem(int position) {
        return mContent.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mContent.get(position).mId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position < 0 || position >= mContent.size()) {
            return null;
        }

        if (convertView == null || convertView.getId() != R.id.entity) {
            convertView = LayoutInflater.from(mActivity).inflate(R.layout.entity, null);
        }

        Entity e = mContent.get(position);
        String name = new String(e.mName);
        ImageView imageView = ((ImageView) convertView.findViewById(R.id.picture));
        imageView.setTag(R.integer.url, e.mPicture);
        imageView.setTag(R.integer.name, name);
        imageView.setOnClickListener(this);
        Bitmap bm = null;
        synchronized (mBitmaps) {
            bm = mBitmaps.get(e.mPicture);
        }
        if (bm != null) {
            android.util.Log.v("bulic", "SETTING PIC " + position + " " + e.mPicture);
            imageView.setImageBitmap(bm);
        } else {
            android.util.Log.v("bulic", "QUERY FOR PIC "  + position + " " + e.mPicture);
            imageView.setImageResource(R.drawable.ic_blank);
            synchronized (mTasks) {
                if (!mTasks.contains(e.mPicture)) {
                    mTasks.add(e.mPicture);
                    new BitmapTask(e.mPicture, imageView, (int) e.mId, null, false).execute();
                }
            }
        }
        ((TextView) convertView.findViewById(R.id.name)).setText(name);

        return convertView;
    }

    public void postRecommendations() {
        for (int x = 0; x < mMaxNotifications; x++) {
            postNextRecommendation();
        }
    }

    public void postNextRecommendation() {
        android.util.Log.v("MiRecs", "posting next recommendation");
        postNextRecommendation(-1);
    }

    private void postNextRecommendation(int id) {
        Entity e = mContent.get(mPosted++);
        boolean posting = (id == -1);

        android.util.Log.v("MiRecs", "posting next recommendation | " + id + " , " + (int) e.mId);

        if (posting) {
            id = (int) e.mId;
        }

        String name = new String(e.mName);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mActivity);
        builder.setContentTitle(name);
        builder.setContentText(mActivity.getString(R.string.mihai));
        builder.setContentIntent(getPendingIntent((int) e.mId, name));
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setPriority((((mMaxNotifications-mPosted-1)-1)/2)-2);

        Bitmap bm = mBitmaps.get(e.mPicture);
        if (bm != null) {
            builder.setLargeIcon(bm);
            mNotificationManager.notify(id, builder.build());
            if (posting) {
                mPostedNotifications.add(id);
            }
        } else {
            new BitmapTask(e.mPicture, null, id, builder, posting).execute();
        }
    }

    public void updateNextRecommendation() {
        int position = mUpdated % mPostedNotifications.size();
        android.util.Log.v("MiRecs", "updaing recommendation at position " + position);
        postNextRecommendation(mPostedNotifications.get(position));
        mUpdated++;
    }

    public void dismissRecommendation() {
        if (!mPostedNotifications.isEmpty()) {
            android.util.Log.v("MiRecs", "dismissing first recommendation");
            mNotificationManager.cancel(mPostedNotifications.remove(0));
        }
    }

    private class BitmapTask extends AsyncTask<Void, Void, Bitmap> {
        private String mUrl;
        private ImageView mView;
        private int mId;
        private NotificationCompat.Builder mBuilder;
        private boolean mPosting;

        public BitmapTask(String url, ImageView view, int id, NotificationCompat.Builder builder, boolean posting) {
            mUrl = url;
            mView = view;
            mId = id;
            mBuilder = builder;
            mPosting = posting;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            Bitmap bm = getBitmap(mUrl);
            if (bm == null) android.util.Log.v("bulic", "NULL???");
            synchronized (mTasks) {
                mTasks.remove(mUrl);
            }
            synchronized (mBitmaps) {
                android.util.Log.v("bulic", "GOT " + mUrl);
                mBitmaps.put(mUrl, bm);
            }
            return bm;
        }

        @Override
        protected void onPostExecute(Bitmap bm) {
            if (mView != null) {
                if (mUrl.equals(mView.getTag(R.integer.url))) {
                    mView.setImageBitmap(bm);
                } else {
                    mView.setImageResource(R.drawable.ic_blank);
                }
            }

            if (mBuilder != null){
                mBuilder.setLargeIcon(bm);
                mNotificationManager.notify(mId, mBuilder.build());
                if (mPosting) {
                    mPostedNotifications.add(mId);
                }
            }
        }

        private Bitmap getBitmap(String bitmapUrl) {
            try {
                URL url = new URL(bitmapUrl);
                return BitmapFactory.decodeStream(url.openConnection().getInputStream());
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    @Override
    public void onClick(View v) {
        Object name = v.getTag(R.integer.name);
        if (name instanceof String) {
            mActivity.startActivity(getPhoneIntent((String) name));
        }
    }

    @SuppressWarnings("unused")
    private Intent getPhoneIntent(String name) {
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra(SearchManager.QUERY, name);

        return intent;
    }

    @SuppressWarnings("unused")
    private Intent getIntent(String name) {
        Intent intent = new Intent(Intent.ACTION_ASSIST);
        intent.putExtra("search_term", name);

        return intent;
    }

    private PendingIntent getPendingIntent(int reqCode, String name) {
        return PendingIntent.getActivity(mActivity, reqCode, getPhoneIntent(name), 0);
    }
}
