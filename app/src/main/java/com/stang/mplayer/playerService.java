package com.stang.mplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.os.PowerManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Stanislav on 25.10.2016.
 */

public class PlayerService extends Service {
    public static final String ACTION_PROGRESS_CHANGED = "PROGRESS_CHANGED";
    public static final String ACTION_STATUS_CHANGED = "STATUS_CHANGED";
    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_PREV = "ACTION_PREV";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_EXIT = "ACTION_EXIT";

    private static final int NOTIFY_ID = 1;
    private NotificationManager nm;
    private IntentFilter filter;
    private BroadcastReceiver receiver;

    private final IBinder mBinder = new MusicBinder();
    private Messenger outMessenger;

    public static final String TAG = MainActivity.class.getSimpleName();

    public ArrayList<Song> mPlaylist;
    public ArrayList<Song> mSourcelist;
    public String searchPhrase = "";
    public int searchType = RecyclerAdapter.SEARCH_SONG;
    public int sortType = RecyclerAdapter.SORT_SONG;
    public Boolean mRepeat = false;

    private ArrayList<Integer> mQueue;
    private int mCurrentPosition = RecyclerView.NO_POSITION;
    Handler handler;
    private MediaPlayer mPlayer;

    private final Runnable updater = new Runnable(){
        public void run(){
            try {
                if(mPlayer != null && mPlayer.isPlaying()){
                    int progress =  (mPlayer.getCurrentPosition() * 100 / mPlayer.getDuration());
                    Intent i = new Intent(PlayerService.ACTION_PROGRESS_CHANGED)
                            .putExtra("progress", progress)
                            .putExtra("remain", mPlayer.getCurrentPosition());
                    sendBroadcast(i);
                    //
                    handler.postDelayed(this, 1000);
                    Log.d(TAG, "SERVICE updater, progress=" + progress);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public int getCurrentPosition() {
        return mCurrentPosition;
    }


    public int getPositionInQueue(int position) {
        int p=RecyclerView.NO_POSITION;
        for (int i = 0; i < mQueue.size(); i++) {
            if(mQueue.get(i).equals(position)){
                p = i;
                break;
            }
        }
        Log.d(TAG, "Position in Playlist:" + position + " Position in Queue: " + p);
        return p;
    }


    public ArrayList<Song> getPlaylist() {
        return mPlaylist;
    }


    public void setPlayList(ArrayList<Song> list) {
        mPlaylist = list;
    }

    public ArrayList<Song> getSourcelist() {
        return mSourcelist;
    }


    public void setSourceList(ArrayList<Song> list) {
        mSourcelist = list;
    }


    public void setQueue(ArrayList<Integer> queue) {
        mQueue = new ArrayList<Integer>(queue);
    }


    public ArrayList<Integer> getQueue() {
        return mQueue;
    }


    public void setCurrentPosition(int position) {
        mCurrentPosition = position;
    }

    public boolean isPlaying() {
        if(mPlayer != null) {
            return mPlayer.isPlaying();
        } else {
            return false;
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        Bundle extras = arg0.getExtras();
        Log.d(TAG, "service " + "onBind");
        // Get messager from the Activity
        if (extras != null) {
            Log.d(TAG, "service" + "onBind with extra");
            outMessenger = (Messenger) extras.get("MESSENGER");
        }

        sendBroadcastStatus();

        return mBinder;
    }


    public class MusicBinder extends Binder {
        PlayerService getService() {
            return PlayerService.this;
        }
    }


    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate SERVICE");
        mCurrentPosition = RecyclerView.NO_POSITION;
        mPlayer = new MediaPlayer();
        handler = new Handler();
        mQueue = new ArrayList<>();
        mPlaylist = new ArrayList<>();
        mSourcelist = new ArrayList<>();

        initPlayer();

        filter = new IntentFilter();
        filter.addAction(PlayerService.ACTION_PLAY);
        filter.addAction(PlayerService.ACTION_NEXT);
        filter.addAction(PlayerService.ACTION_EXIT);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "SERVICE receiver action=" + action);
                switch (action){
                    case PlayerService.ACTION_EXIT :
                        stopSelf();
                        break;
                    case PlayerService.ACTION_NEXT :
                        nextTrack();
                        break;
                    case PlayerService.ACTION_PLAY :
                        pause();
                        break;
                }
            }
        };

        registerReceiver(receiver, filter);

        //showNotify("");
        //nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(this,
                0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Resources res = getResources();

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ipod_nano)
                // большая картинка
                .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.ipod_player_icon_small))
                //.setTicker(res.getString(R.string.warning)) // текст в строке состояния
                .setTicker("MEDIA PLAYER SERVICE")
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(false)
                //.setContentTitle(res.getString(R.string.notifytitle))
                .setContentTitle("Media Player SERVICE")
                //.setContentText(res.getString(R.string.notifytext))
                .setContentText("...");



        Notification notification;

        if (Build.VERSION.SDK_INT < 16)
            notification = builder.getNotification();
        else
            notification = builder.build();

        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        //nm.showNotify(NOTIFY_ID, notification);

        startForeground(NOTIFY_ID, notification);
        //stopForeground(true);

    }


    public void initPlayer() {
        mPlayer.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(mRepeat) {
                    play(mCurrentPosition);
                } else {
                    nextTrack();
                }
                showNotify(mPlaylist.get(mCurrentPosition).artistTitle + " :: " + mPlaylist.get(mCurrentPosition).songTitle);
                //onPositionChanged();
            }
        });
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mPlayer.start();
                handler.removeCallbacks(updater);
                handler.postDelayed(updater,1000);

                onPositionChanged();
                showNotify(mPlaylist.get(mCurrentPosition).artistTitle + " :: " + mPlaylist.get(mCurrentPosition).songTitle);
                sendBroadcastStatus();

                Log.d(TAG, "SERVICE mPlayer.onPrepared");
            }
        });

    }


    public void sendBroadcastStatus() {
        int duration = 0;
        if(mPlayer!=null && mPlayer.isPlaying()) {
            duration = mPlayer.getDuration();
        }

        Intent i = new Intent(PlayerService.ACTION_STATUS_CHANGED)
                .putExtra("position", mCurrentPosition)
                .putExtra("duration", duration);
        sendBroadcast(i);
    }


    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SERVICE onStartCommand");
        //Toast.makeText(this, "service onStartCommand", Toast.LENGTH_SHORT).show();
        return super.onStartCommand(intent, flags, startId);
    }


    public void onDestroy() {
        unregisterReceiver(receiver);
        handler.removeCallbacks(updater);

        if(mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
        }

        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(NOTIFY_ID);

        super.onDestroy();
        Log.d(TAG, "SERVICE onDestroy");
    }


    public void seekTo(int position) {
        if (mPlayer != null && mPlayer.isPlaying()) {
            int duration = mPlayer.getDuration();
            int msec = (int)((float)duration * (float)((float)position/100F));
            mPlayer.seekTo(msec);
            Log.d(TAG, "SERVICE seekTo: " + position + " msec: " +  msec + " duration: " + duration);
        }
    }


    private void showNotify(String text) {
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(this,
                0, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        PendingIntent pIntentPlay = PendingIntent.getBroadcast(this, 5, new Intent(PlayerService.ACTION_PLAY), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pIntentNext = PendingIntent.getBroadcast(this, 5, new Intent(PlayerService.ACTION_NEXT), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pIntentExit = PendingIntent.getBroadcast(this, 5, new Intent(PlayerService.ACTION_EXIT), PendingIntent.FLAG_UPDATE_CURRENT);

        Resources res = getResources();

        Notification.Builder builder = new Notification.Builder(this);

        int playPauseIcon;
        if(mPlayer!=null && mPlayer.isPlaying()) {
            playPauseIcon = android.R.drawable.ic_media_pause;
        } else {
            playPauseIcon = android.R.drawable.ic_media_play;
        }

        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ipod_nano)
                // большая картинка
                .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.ipod_player_icon_small))
                //.setTicker(res.getString(R.string.warning)) // текст в строке состояния
                .setTicker(text)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(false)
                //.setContentTitle(res.getString(R.string.notifytitle))
                .setContentTitle("Custom Media Player")
                //.setContentText(res.getString(R.string.notifytext))
                .setContentText(text)
                .addAction(playPauseIcon, "", pIntentPlay)
                .addAction(android.R.drawable.ic_media_next, "", pIntentNext)
                .addAction(android.R.drawable.ic_delete, "", pIntentExit);


        Notification notification;

        if (Build.VERSION.SDK_INT < 16)
            notification = builder.getNotification();
        else
            notification = builder.build();

        notification.flags |= Notification.FLAG_ONGOING_EVENT;

        //nm.cancel(NOTIFY_ID);
        nm.notify(NOTIFY_ID, notification);

    }


    public void play(int position) {
        if(mPlaylist==null || position < 0 || position>=mPlaylist.size()) return;

        setCurrentPosition(position);

        if(mPlayer != null){
            mPlayer.reset();
            //mPlayer.release();
        }

        //mAdapter.setCurrentPosition(position);
        Song song = mPlaylist.get(position);

        //showNotify(song.artistTitle + " :: " + song.songTitle);
        Log.d(TAG, "PlayService.play() " + song.songTitle);

        try {
            Uri fileUri = Uri.parse(song.fileName);
            mPlayer.setDataSource(getApplicationContext(), fileUri);
            mPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
            if(mPlayer!=null) {
                mPlayer.release();
            }
        }
    }


    public void pause(){
        if (mPlayer != null ){
            if(mPlayer.isPlaying()){
                mPlayer.pause();
            } else {
                mPlayer.start();
                handler.postDelayed(updater, 1000);
            }
        }
        showNotify(mPlaylist.get(mCurrentPosition).artistTitle + " :: " + mPlaylist.get(mCurrentPosition).songTitle);
        Intent i = new Intent(PlayerService.ACTION_STATUS_CHANGED)
                .putExtra("position", mCurrentPosition)
                .putExtra("duration", mPlayer.getDuration());
        sendBroadcast(i);
    }


    public void nextTrack(){
        int position = getCurrentPosition();
        Log.d(TAG, "currentPosition before: " + getCurrentPosition());
        int nextPosition = RecyclerView.NO_POSITION;

        if(mQueue.size() > 0){
            int pos = getPositionInQueue(position);
            if(pos == -1) {
                nextPosition = mQueue.get(0);
                mQueue.remove(0);
            } else
            {
                if(pos == mQueue.size() -1) {
                    nextPosition = position + 1;
                    mQueue.clear();
                } else if (pos < mQueue.size() -1) {
                    pos++;
                    nextPosition = mQueue.get(pos);
                    for (int i = pos; i >= 0; i--) {
                        mQueue.remove(i);
                    }
                }
            }

        } else {
            nextPosition = position + 1;
        }


        if(nextPosition < mPlaylist.size() ){
            play(nextPosition);
        }
        Log.d(TAG, "currentPosition after: " + getCurrentPosition());
    }


    public void prevTrack(){
        if(mCurrentPosition > 0 ){
            play(--mCurrentPosition);
        }
    }


    public void onPositionChanged(){
        Intent i = new Intent(PlayerService.ACTION_STATUS_CHANGED)
        .putExtra("position", getCurrentPosition())
        .putExtra("duration", mPlayer.getDuration());
        sendBroadcast(i);
    }
}