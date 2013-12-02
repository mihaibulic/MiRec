package com.mihai.mirecs;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
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

    private Activity sActivity;
    private List<Entity> mContent;
    LruCache<String, Bitmap> mBitmaps = new LruCache<String, Bitmap>(200);

    public MainAdapter(Activity act, List<Entity> content) {
        sActivity = act;
        mContent = content;
    }

    @Override
    public int getCount() {
        return mContent.size();
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
            new BitmapTask(e.mPicture, imageView).execute();
        }
        ((TextView) convertView.findViewById(R.id.name)).setText(e.mName);

        return convertView;
    }

    private class BitmapTask extends AsyncTask<Void, Void, Bitmap> {
        private String mUrl;
        private ImageView mView;

        public BitmapTask(String url, ImageView view) {
            mUrl = url;
            mView = view;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            Bitmap bm = getBitmap(mUrl);
            mBitmaps.put(mUrl, bm);
            return bm;
        }

        @Override
        protected void onPostExecute(Bitmap bm) {
            if (mUrl.equals(mView.getTag(R.integer.url))) {
                mView.setImageBitmap(bm);
            } else {
                mView.setImageResource(R.drawable.ic_blank);
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
            Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
            intent.putExtra(SearchManager.QUERY, (String) name);
            sActivity.startActivity(intent);
        }
    }
}
