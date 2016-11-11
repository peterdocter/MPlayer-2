package com.stang.mplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.support.annotation.BoolRes;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

import java.util.ArrayList;
import java.util.Collection;
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

    public static final int SORT_SONG = 0;
    public static final int SORT_ARTIST = 1;
    public static final int SORT_ALBUM = 2;
    public static final int SORT_DURATION = 3;
    public static final int SORT_DATE = 4;

    public static final int[] backColors = { Color.rgb(230,230,230), Color.rgb(255,255,255)};

    ImageLoader imageLoader;
    private OnClickListener mOnClickListener;
    private OnQueueChangeListener mOnQueueChangeListener;
    private Context mContext;
    private ArrayList<Song> mDataset;
    private ArrayList<Song> mSourceDataset;
    private ArrayList<Integer> mQueue;
    public String searchPhrase = "";
    public int searchType = SEARCH_SONG;
    public int sortType = SORT_SONG;
    private int mCurrentPosition = RecyclerView.NO_POSITION;
    public View mSelectedItem = null;


    public interface OnClickListener {
        void onClick(View view, int position);
    }
    public interface OnQueueChangeListener {
        void onChange(ArrayList<Integer> newQueue);
    }

    public void setOnItemClickListener(OnClickListener l) {
        mOnClickListener = l;
    }

    public Song getSong(int position) {
        return mDataset.get(position);
    }

    public int getCurrentPosition() {
        return mCurrentPosition;
    }

    public void setCurrentPosition(int position) {
        if((mDataset.size() > 0) && (position < 0 || position >= mDataset.size())) {
            position = 0;
        }
        if(position > -1 && position < mDataset.size()){
            notifyItemChanged(position);
            notifyItemChanged(mCurrentPosition);
        }
        mCurrentPosition = position;
    }

    // класс view holder-а с помощью которого мы получаем ссылку на каждый элемент
    // отдельного пункта списка
    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mArtist;
        public TextView mTitle;
        public ImageView mImage;
        public TextView mOrder;

        public ViewHolder(View v) {
            super(v);
            int position = getAdapterPosition();
            v.setTag(position);

            mTitle = ((TextView) v.findViewById(R.id.songItem_song));
            mArtist = ((TextView) v.findViewById(R.id.songItem_artist));
            mImage = ((ImageView) v.findViewById(R.id.songItem_album));
            mOrder = (TextView) v.findViewById(R.id.songItem_order);

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();

                    v.setBackgroundColor(COLOR_SELECTED);

                    if(mSelectedItem!=null) {
                        mSelectedItem.setBackgroundColor(backColors[mCurrentPosition%2]);
                    }

                    mSelectedItem = v;
                    if(mOnClickListener != null) {
                        if (position != RecyclerView.NO_POSITION) {
                            mOnClickListener.onClick(v, position);
                            mCurrentPosition = position;
                        }
                    }
                }
            });

            mOrder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = (int)v.getTag();
                    addToQueue(position);
                    Log.d(TAG, "addToQueue: " + String.valueOf(position) );
                    //Toast.makeText(v.getContext(), "set next: " + String.valueOf(position), Toast.LENGTH_SHORT);
                }
            });
        }

    }


    // Конструктор
    public RecyclerAdapter(Context context, ArrayList<Song> dataset) {
        mContext = context;
        if(dataset != null){
            mDataset = dataset;
        } else {
            mDataset = new ArrayList<Song>();
        }

        imageLoader = ImageLoader.getInstance(); // Получили экземпляр
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .showStubImage(R.drawable.android_delete)
                .showImageOnFail(R.drawable.ipod_player_icon1)
                .showImageForEmptyUri(R.drawable.ipod_player_icon1)
                //.imageScaleType(ImageScaleType.EXACTLY_STRETCHED)
                //.resetViewBeforeLoading()
                //.cacheInMemory()
                //.cacheOnDisc()
                //.decodingType(ImageScaleType.EXACTLY)
                .build();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                //.memoryCacheExtraOptions(480, 800) // width, height
                //.discCacheExtraOptions(480, 800, Bitmap.CompressFormat.JPEG, 75) // width, height, compress format, quality
                .threadPoolSize(5)
                .threadPriority(Thread.MIN_PRIORITY + 2)
                .denyCacheImageMultipleSizesInMemory()
                .memoryCache(new UsingFreqLimitedMemoryCache(2 * 1024 * 1024)) // 2 Mb
                //.discCache(new UnlimitedDiscCache(cacheDir))
                //.discCacheFileNameGenerator(new HashCodeFileNameGenerator())
                //.imageDownloader(new BaseImageDownloader(5 * 1000, 30 * 1000)) // connectTimeout (5 s), readTimeout (30 s)
                .defaultDisplayImageOptions(options)
                //.enableLogging()
                .build();
        imageLoader.init(config); // Проинициализировали конфигом по умолчанию

//        if(mDataset!=null && mDataset.size()>0) {
//            mCurrentPosition = 0;
//        }

        mQueue = new ArrayList<>();
    }

    public void addToQueue(int position) {
        int p = positionInQueue(position);
        if( p > RecyclerView.NO_POSITION) {
            mQueue.remove(p);
            notifyDataSetChanged();
        } else {
            mQueue.add(position);
            notifyItemChanged(position);
        }

        if(mOnQueueChangeListener != null) {
            mOnQueueChangeListener.onChange(mQueue);
        }
    }

    public int positionInQueue(int position) {
        int p=RecyclerView.NO_POSITION;
        for (int i = 0; i < mQueue.size(); i++) {
            if(mQueue.get(i).equals(position)){
                p = i;
                break;
            }
        }
        Log.d(TAG, "Position in PlayList:" + position + "; in Queue:" + p);
        return p;
    }

    public ArrayList<Integer> getQueue() {
        return mQueue;
    }

    public ArrayList<Song> getPlaylist() {
        return mDataset;
    }

    public void setPlaylist(ArrayList<Song> list) {
        mSourceDataset = list;
        doSearch();
        //doSort();
        //notifyDataSetChanged();
    }

    public void setQueue(ArrayList<Integer> queue) {
        mQueue = queue;
        notifyDataSetChanged();
    }

    public void setOnQueueChangeListener(OnQueueChangeListener l) {
        mOnQueueChangeListener = l;
    }

    public void addSong(Song song) {
        mSourceDataset.add(song);
        doSearch();
    }

    public void doSearch() {
        Log.d(TAG, "doSearch");
        setCurrentPosition(0);
        ArrayList<Song> searchResult = new ArrayList<>();
        String searchField = null;
        for (int i = 0; i < mSourceDataset.size(); i++) {
            switch (searchType) {
                case SEARCH_SONG:
                    searchField = mSourceDataset.get(i).songTitle;
                    break;
                case SEARCH_ARTIST:
                    searchField = mSourceDataset.get(i).artistTitle;
                    break;
                case SEARCH_ALBUM://
                    searchField = mSourceDataset.get(i).songTitle;
                    break;
            }
            searchField = searchField.toLowerCase();
            if(searchField!=null && searchField.contains(searchPhrase.toLowerCase())) {
                searchResult.add(mSourceDataset.get(i));
            }
        }
        mDataset = searchResult;
        doSort();
    }

    public void doSort() {
        Log.d(TAG, "doSort");
        Collections.sort(mDataset,new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                int result = 0;
                switch (sortType) {
                    case SORT_SONG:
                        result = o1.songTitle.compareToIgnoreCase(o2.songTitle);
                        break;
                    case SORT_ARTIST:
                        result = o1.artistTitle.compareToIgnoreCase(o2.artistTitle);
                        break;
                    case SORT_ALBUM:
                        result = o1.albumTitle.compareToIgnoreCase(o2.albumTitle);
                        break;
//                    case SORT_DURATION:
//                        result = o1.songDuration.compareToIgnoreCase(o2.songDuration);
//                        break;
//                    case SORT_DATE:
//                        result = o1.songDate.compareToIgnoreCase(o2.songDate);
//                        break;
                }
                return result;
            }
        });
        mQueue = new ArrayList<>();
        //notifyDataSetChanged();
    }

    // Создает новые views (вызывается layout manager-ом)
    @Override
    public RecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.song, parent, false);

        // тут можно программно менять атрибуты лэйаута (size, margins, paddings и др.)
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Заменяет контент отдельного view (вызывается layout manager-ом)
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        Song s = mDataset.get(position);

        holder.mTitle.setText(s.songTitle);
        holder.mArtist.setText(s.artistTitle);

        //holder.mImage.setImageDrawable(s.albumImage);
        imageLoader.displayImage(s.albumImage, holder.mImage);

        holder.mOrder.setTag(position);
        String orderText = "";
        int pos = positionInQueue(position);
        if(pos != RecyclerView.NO_POSITION) {
            orderText = String.valueOf(pos+1);
        }
        holder.mOrder.setText(orderText);

        holder.itemView.setBackgroundColor(getBackgroundItemColor(position));
        int aPos = holder.getAdapterPosition();
        //Log.d(TAG, "onBindViewHolder getAdapterPosition: " + aPos + " position: " + position);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    // Возвращает размер данных (вызывается layout manager-ом)
    @Override
    public int getItemCount() {
        if(mDataset != null) return mDataset.size();
        else return 0;
    }


    public int getBackgroundItemColor(int position) {
        int color;

        if(getCurrentPosition() == position) {
            color = COLOR_SELECTED;
        } else {
            color =  backColors[position%2];
        }

        return color;
    }


}