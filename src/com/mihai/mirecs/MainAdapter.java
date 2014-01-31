package com.mihai.mirecs;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
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

    private final int mMaxNotifications;

    private NotificationManager mNotificationManager;
    private Activity sActivity;
    private List<Entity> mContent;
    LruCache<String, Bitmap> mBitmaps = new LruCache<String, Bitmap>(200);

    public MainAdapter(Activity act, List<Entity> content) {
        sActivity = act;
        mContent = content;
        mMaxNotifications = sActivity.getResources().getInteger(R.integer.max_notifications);
        mNotificationManager = (NotificationManager) sActivity.getSystemService(Context.NOTIFICATION_SERVICE);
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
        if (position < 0 || position >= mContent.size() || position > mMaxNotifications) {
            return null;
        }

        if (convertView == null || convertView.getId() != R.id.entity) {
            convertView = LayoutInflater.from(sActivity).inflate(R.layout.entity, null);
        }

        Entity e = mContent.get(position);
        ImageView imageView = ((ImageView) convertView.findViewById(R.id.picture));
        imageView.setTag(R.integer.url, e.mPicture);
        imageView.setTag(R.integer.name, e.mName);
        imageView.setOnClickListener(this);
        Bitmap bm = mBitmaps.get(e.mPicture);
        if (bm != null) {
            imageView.setImageBitmap(bm);
        } else {
            imageView.setImageResource(R.drawable.ic_blank);
        }
        ((TextView) convertView.findViewById(R.id.name)).setText(e.mName);

        return convertView;
    }

    public void postRecommendations() {
        for (int x = 0; x < mMaxNotifications; x++) {
            Entity e = mContent.get(x);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(sActivity);
            builder.setContentTitle(e.mName);
            builder.setContentInfo(sActivity.getString(R.string.mihai));
            builder.setContentIntent(getPendingIntent(e.mName));
            builder.setSmallIcon(R.drawable.ic_launcher);
            builder.setPriority(mMaxNotifications - x);

            Bitmap bm = mBitmaps.get(e.mPicture);
            if (bm != null) {
                builder.setLargeIcon(bm);
                mNotificationManager.notify((int) e.mId, builder.build());
            } else {
                new BitmapTask(e.mPicture, null, (int) e.mId, builder).execute();
            }
        }
    }

    private class BitmapTask extends AsyncTask<Void, Void, Bitmap> {
        private String mUrl;
        private ImageView mView;
        private int mId;
        private NotificationCompat.Builder mBuilder;

        public BitmapTask(String url, ImageView view, int id, NotificationCompat.Builder builder) {
            mUrl = url;
            mView = view;
            mId = id;
            mBuilder = builder;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            Bitmap bm = getBitmap(mUrl);
            mBitmaps.put(mUrl, bm);
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
            sActivity.startActivity(getIntent((String) name));
        }
    }

    private Intent getIntent(String name) {
//        Intent intent = new Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH);
//        intent.putExtra(SearchManager.QUERY, name);
        Intent intent = new Intent(Intent.ACTION_ASSIST);
        intent.putExtra("search_term", name);

        return intent;
    }

    private PendingIntent getPendingIntent(String name) {
        return PendingIntent.getActivity(sActivity, 0, getIntent(name), 0);
    }
}
