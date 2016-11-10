package com.stang.mplayer;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

/**
 * Created by Stanislav on 24.10.2016.
 */

public class Song {
    public String songTitle;
    public String artistTitle;
    public String fileName;
    public Drawable albumImage;

    public Song(String song, String artist, String file, Drawable album){
        songTitle = song;
        artistTitle = artist;
        fileName = file;
        albumImage = album;
    }
}