package com.stang.mplayer;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;

/**
 * Created by Stanislav on 24.10.2016.
 */

public class Song {
    public String songTitle;
    public String artistTitle;
    public String albumTitle;
    public String fileName;
    public String albumImage;

    public Song(String song, String artist, String album, String file, String albumId){
        songTitle = song;
        artistTitle = artist;
        albumTitle = album;
        fileName = file;
        albumImage = albumId;
    }
}