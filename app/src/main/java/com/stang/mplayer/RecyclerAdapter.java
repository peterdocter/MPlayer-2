package com.stang.mplayer;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by Stanislav on 24.10.2016.
 */

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    public static final String TAG = RecyclerAdapter.class.getSimpleName();

     public static final int COLOR_SELECTED = Color.GREEN;

    public static final int SEARCH_SONG = 0;
    public static final int SEARCH_ARTIST = 1;
    public static final int SEARCH_ALBUM = 2;
    public static final int SEARCH_CURRENT = -1;

    public static final int SORT_SONG = 0;
    public static final int SORT_ARTIST = 1;
    public static final int SORT_ALBUM = 2;
    public static final int SORT_DURATION = 3;
    public static final int SORT_DATE = 4;

    public static final int[] backColors = {Color.rgb(230, 230, 230), Color.rgb(255, 255, 255)};

    private OnClickListener mOnClickListener;
    private OnQueueChangeListener mOnQueueChangeListener;
    private Context mContext;
    public ServiceData mServiceData = ServiceData.getInstance();
    public View mSelectedItem = null;
    public int mPrevPosition = -1;

    public RecyclerAdapter(Context context) {
        mContext = context;
    }

    public interface OnClickListener {
        void onClick(View view, Song song, int position);
    }

    public interface OnQueueChangeListener {
        void onChange();
    }

    @Override
    public int getItemCount() {
        return mServiceData.getPlaylistSize();
    }

    @Override
    public RecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.song, parent, false);

        // тут можно программно менять атрибуты лэйаута (size, margins, paddings и др.)
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        Song s = mServiceData.getSong(position);
        holder.onBind(s, position);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public void setOnItemClickListener(OnClickListener l) {
        mOnClickListener = l;
    }

    public void setCurrentPosition(int newPosition) {
        if (!mServiceData.isPlaylistEmpty() && (newPosition < 0 || newPosition >= mServiceData.getPlaylistSize())) {
            newPosition = 0;
        }

        if (mPrevPosition != newPosition) {
            mServiceData.setCurrentPosition(newPosition);
            notifyItemChanged(mPrevPosition);
            notifyItemChanged(newPosition);
            mPrevPosition = newPosition;
        }
    }

    public void setOnQueueChangeListener(OnQueueChangeListener l) {
        mOnQueueChangeListener = l;
    }

    public void addToQueue(int position) {
        int p = mServiceData.getPositionInQueue(position);
        if (p > RecyclerView.NO_POSITION) {
            mServiceData.getQueue().remove(p);
            notifyDataSetChanged();
        } else {
            mServiceData.getQueue().add(position);
            notifyItemChanged(position);
        }

        if (mOnQueueChangeListener != null) {
            mOnQueueChangeListener.onChange();
        }
    }

    public void addSong(Song song) {
        mServiceData.getSourcelist().add(song);
        mServiceData.doSearch();
        if (mOnQueueChangeListener != null) {
            mOnQueueChangeListener.onChange();
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mArtist;
        private TextView mTitle;
        private ImageView mImage;
        private TextView mOrder;
        private int mBoundPosition;
        private Song mBoundSong;

        public ViewHolder(View v) {
            super(v);

            mTitle = ((TextView) v.findViewById(R.id.songItem_song));
            mArtist = ((TextView) v.findViewById(R.id.songItem_artist));
            mImage = ((ImageView) v.findViewById(R.id.songItem_album));
            mOrder = (TextView) v.findViewById(R.id.songItem_order);

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnClickListener != null)
                        mOnClickListener.onClick(v, mBoundSong, mBoundPosition);
                }
            });

            mOrder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = (int) v.getTag();
                    addToQueue(position);
                }
            });
        }

        public void onBind(Song song, int position) {
            mBoundPosition = position;
            mBoundSong = song;
            mTitle.setText(song.songTitle);
            mArtist.setText(song.artistTitle);

            //mImage.setImageDrawable(song.albumImage);
            ImageLoader.getInstance().displayImage(song.albumImage, mImage);

            Log.d(TAG, "imageLoader uri: " + song.albumImage);

            mOrder.setTag(position);
            String orderText = "";
            int pos = mServiceData.getPositionInQueue(position);
            if (pos != RecyclerView.NO_POSITION) {
                orderText = String.valueOf(pos + 1);
            }
            mOrder.setText(orderText);

            itemView.setBackgroundColor(getBackgroundItemColor(position));
        }

        public void setSelected(boolean selected) {
            // todo handle selection;
        }


        public int getBackgroundItemColor(int position) {
            int color;

            if (mServiceData.getCurrentPosition() == position) {
                color = COLOR_SELECTED;
            } else {
                color = backColors[position % 2];
            }

            return color;
        }
    }


}