package com.stang.mplayer;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.content.AsyncTaskLoader;
import android.provider.MediaStore;


/**
 * Created by Stanislav on 18.11.2016.
 */

public class PlaylistLoader extends AsyncTaskLoader<Playlist> {
    public final static String ARGS_PLAYLIST_URI = "playlist_uri";
    public final static String ALBUMART_URI_STRING = "content://media/external/audio/albumart";
    public final static Uri DEFAULT_DATA_URI = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    public final static Uri INTERNAL_DATA_URI = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
    private Uri mDataUri;
    private Context mContext;

    public PlaylistLoader(Context context, Bundle args) {
        super(context);
        mContext = context;
        mDataUri = (args == null) ? INTERNAL_DATA_URI : Uri.parse((String) args.get(ARGS_PLAYLIST_URI));
    }

    @Override
    public Playlist loadInBackground() {
        Playlist songs = new Playlist();

        ContentResolver contentResolver = mContext.getContentResolver();
        Cursor cursor = contentResolver.query(mDataUri, null, null, null, null);

        if (cursor == null) {
            // query failed, handle error.
        } else if (!cursor.moveToFirst()) {
            // no media on the device
        } else {
            do {
                songs.add(parseCursor(cursor));
            } while (cursor.moveToNext());
        }
        return songs;
    }

    public static Song parseCursor(Cursor cursor) {
        long id = cursor.getLong(cursor
                .getColumnIndex(android.provider.MediaStore.Audio.Media._ID));
        String title = cursor.getString(cursor
                .getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE));
        String artist = cursor.getString(cursor
                .getColumnIndex(MediaStore.Audio.Media.ARTIST));
        String album = cursor.getString(cursor
                .getColumnIndex(MediaStore.Audio.Media.ALBUM));
        String filename = "file://" + cursor.getString(cursor
                .getColumnIndex(MediaStore.Audio.Media.DATA));
        String duration = cursor.getString(cursor
                .getColumnIndex(MediaStore.Audio.Media.DURATION));

        Uri mainAlbumArtUri = Uri.parse(ALBUMART_URI_STRING);
        Uri albumArtUri = ContentUris.withAppendedId(mainAlbumArtUri, cursor
                .getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
        return new Song(title, artist, album, String.valueOf(filename), albumArtUri.toString());
    }
}
