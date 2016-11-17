package com.stang.mplayer;

import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static com.stang.mplayer.MainActivity.TAG;
import static com.stang.mplayer.RecyclerAdapter.SEARCH_ALBUM;
import static com.stang.mplayer.RecyclerAdapter.SEARCH_ARTIST;
import static com.stang.mplayer.RecyclerAdapter.SEARCH_SONG;
import static com.stang.mplayer.RecyclerAdapter.SORT_ALBUM;
import static com.stang.mplayer.RecyclerAdapter.SORT_ARTIST;
import static com.stang.mplayer.RecyclerAdapter.SORT_SONG;

/**
 * Created by Stanislav on 16.11.2016.
 */

public class ServiceData {
    private static volatile ServiceData instance;

    private ArrayList<Song> mPlaylist = new ArrayList<>();
    private ArrayList<Song> mSourcelist = new ArrayList<>();
    private ArrayList<Integer> mQueue = new ArrayList<>();
    private String mSearchPhrase = new String("");
    private Integer mSearchType = SEARCH_SONG;
    private Integer mSortType = SORT_SONG;
    private Boolean mRepeat = false;
    private Integer mCurrentPosition = RecyclerView.NO_POSITION;

    public static ServiceData getInstance() {
        ServiceData localInstance = instance;
        if (localInstance == null) {
            synchronized (ServiceData.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new ServiceData();
                }
            }
        }
        return localInstance;
    }

    public boolean isPlaylistEmpty() {
        return mPlaylist.isEmpty();
    }

    public boolean isSourceListEmpty() {
        return mSourcelist.isEmpty();
    }

    public boolean hasNext() {
        return mCurrentPosition < mPlaylist.size() - 1;
    }

    public int getCurrentPosition() {
        return mCurrentPosition;
    }

    public int getPositionInQueue(int position) {
        int p = -1;
        for (int i = 0; i < mQueue.size(); i++) {
            if (mQueue.get(i).equals(position)) {
                p = i;
                break;
            }
        }
        return p;
    }

    public Song getSong(int positionInPlayList) {
        if (positionInPlayList > -1 && positionInPlayList < mPlaylist.size()) {
            return mPlaylist.get(positionInPlayList);
        } else {
            return null;
        }

    }

    public Song getCurrentSong() {
        if (mCurrentPosition > -1) {
            return mPlaylist.get(mCurrentPosition);
        } else {
            return null;
        }

    }

    public int getPlaylistSize() {
        return mPlaylist.size();
    }

    public ArrayList<Song> getPlaylist() {
        return mPlaylist;
    }

    public void setPlayList(ArrayList<Song> list) {
        mPlaylist = list;
        if (mPlaylist != null && mPlaylist.size() > 0) {
            setCurrentPosition(0);
        }
    }

    public ArrayList<Song> getSourcelist() {
        return mSourcelist;
    }

    public void setSourceList(ArrayList<Song> list) {
        mSourcelist = list;
        doSearch();
    }

    public void setQueue(ArrayList<Integer> queue) {
        mQueue = queue;
    }

    public ArrayList<Integer> getQueue() {
        return mQueue;
    }

    public void setCurrentPosition(int position) {
        mCurrentPosition = position;
    }

    public Boolean getRepeat() {
        return mRepeat;
    }

    public void setRepeat(Boolean mRepeat) {
        this.mRepeat = mRepeat;
    }

    public int getSortType() {
        return mSortType;
    }

    public void setSortType(int mSortType) {
        this.mSortType = mSortType;
    }

    public int getSearchType() {
        return mSearchType;
    }

    public void setSearchType(int mSearchType) {
        this.mSearchType = mSearchType;
    }

    public String getSearchPhrase() {
        return mSearchPhrase;
    }

    public void setSearchPhrase(String mSearchPhrase) {
        this.mSearchPhrase = mSearchPhrase;
    }

    public void doSearch() {
        Log.d(TAG, "doSearch");
        ArrayList<Song> sourceList = getSourcelist();
        ArrayList<Song> searchResult = new ArrayList<>();
        String searchField = null;
        for (int i = 0; i < sourceList.size(); i++) {
            switch (getSearchType()) {
                case SEARCH_SONG:
                    searchField = sourceList.get(i).songTitle.toLowerCase();
                    break;
                case SEARCH_ARTIST:
                    searchField = sourceList.get(i).artistTitle.toLowerCase();
                    break;
                case SEARCH_ALBUM:
                    searchField = sourceList.get(i).songTitle.toLowerCase();
                    break;
            }

            if (getSearchPhrase().equals("") || (searchField != null && searchField.contains(getSearchPhrase().toLowerCase()))) {
                searchResult.add(sourceList.get(i));
            }
        }
        setPlayList(searchResult);
        doSort();
    }

    public void doSort() {
        Log.d(TAG, "doSort");
        Collections.sort(getPlaylist(), new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                int result = 0;
                switch (getSortType()) {
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
        setQueue(new ArrayList<Integer>());
    }
}
