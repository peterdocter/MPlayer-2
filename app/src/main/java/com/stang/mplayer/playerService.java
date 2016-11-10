package com.stang.mplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
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
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Stanislav on 25.10.2016.
 */

public class PlayerService extends Service {
    public static final String ACTION_PROGRESS_CHANGED = "PROGRESS_CHANGED";
    public static final String ACTION_STATUS_CHANGED = "STATUS_CHANGED";
    private static final int NOTIFY_ID = 1;
    private NotificationManager nm;

    private final IBinder mBinder = new MusicBinder();
    private Messenger outMessenger;

    public static final String TAG = MainActivity.class.getSimpleName();

    private ArrayList<Song> mPlaylist;
    private ArrayList<Integer> mQueue;
    private int mCurrentPosition = -1;
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



    @Override
    public IBinder onBind(Intent arg0) {
        Bundle extras = arg0.getExtras();
        Log.d(TAG, "service " + "onBind");
        // Get messager from the Activity
        if (extras != null) {
            Log.d(TAG, "service" + "onBind with extra");
            outMessenger = (Messenger) extras.get("MESSENGER");
        }
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
        mCurrentPosition = -1;
        mPlayer = new MediaPlayer();
        handler = new Handler();
        mQueue = new ArrayList<Integer>();
        mPlaylist = new ArrayList<Song>();

        initPlayer();

        //notify("");


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
                .setSmallIcon(R.drawable.ipod_player_icon)
                // большая картинка
                .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.ipod_player_icon))
                //.setTicker(res.getString(R.string.warning)) // текст в строке состояния
                .setTicker("Notification Ticket")
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
        //nm.notify(NOTIFY_ID, notification);

        startForeground(123, notification);
        //stopForeground(true);

    }


    private void notify(String text) {

        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(this,
                0, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        Resources res = getResources();

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ipod_player_icon)
                // большая картинка
                .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.ipod_player_icon))
                //.setTicker(res.getString(R.string.warning)) // текст в строке состояния
                .setTicker("Notification Ticket")
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(false)
                //.setContentTitle(res.getString(R.string.notifytitle))
                .setContentTitle("Media Player SERVICE")
                //.setContentText(res.getString(R.string.notifytext))
                .setContentText(text);


        Notification notification;

        if (Build.VERSION.SDK_INT < 16)
            notification = builder.getNotification();
        else
            notification = builder.build();

        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        nm.notify(NOTIFY_ID, notification);
    }


    public void initPlayer() {
        mPlayer.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                nextTrack();
                onPositionChanged();
            }
        });
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mPlayer.start();
                handler.removeCallbacks(updater);
                handler.postDelayed(updater,1000);
                Intent i = new Intent(PlayerService.ACTION_STATUS_CHANGED)
                        .putExtra("position", mCurrentPosition)
                        .putExtra("duration", mPlayer.getDuration());
                sendBroadcast(i);
                Log.d(TAG, "SERVICE mPlayer.onPrepared");
            }
        });

    }


    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SERVICE onStartCommand");
        //Toast.makeText(this, "service onStartCommand", Toast.LENGTH_SHORT).show();

        return super.onStartCommand(intent, flags, startId);
    }


    public void onDestroy() {
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


    public int getCurrentPosition() {
        return mCurrentPosition;
    }


    public int getPositionInQueue(int position) {
        int p=-1;
        for (int i = 0; i < mQueue.size(); i++) {
            if(mQueue.get(i).equals(position)){
                p = i;
                break;
            }
        }
        Log.d(TAG, "Position:" + position + " in Queue: " + p);
        return p;
    }


    public ArrayList<Song> getPlaylist() {
        return mPlaylist;
    }


    public void setPlayList(ArrayList<Song> list) {
        mPlaylist = list;
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


    public void play(int position) {
        if(position < 0) return;

        setCurrentPosition(position);

        if(mPlayer != null){
            mPlayer.reset();
            //mPlayer.release();
        }

        //mAdapter.setCurrentPosition(position);
        Song song = mPlaylist.get(position);
        /////////////notify(song.artistTitle + " :: " + song.songTitle);
        Log.d(TAG, "PlayService.play() " + song.songTitle);

        try {
            Uri fileUri = Uri.parse(song.fileName);
            mPlayer.setDataSource(getApplicationContext(), fileUri);
            mPlayer.prepare();
            //
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
    }


    public void nextTrack(){
        int position = getCurrentPosition();
        Log.d(TAG, "currentPosition before: " + getCurrentPosition());
        int nextPosition = -1;
        if(mQueue.size() > 0){
            int pos = getPositionInQueue(position);
            if(pos == mQueue.size() -1) {
                nextPosition = position + 1;
                mQueue.remove(pos);
            } else if (pos <= mQueue.size() -2){
                nextPosition = mQueue.get(pos + 1);
                for (int i = 0; i <= pos; i++) {
                    mQueue.remove(i);
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
        Intent i = new Intent(PlayerService.ACTION_STATUS_CHANGED);
        i.putExtra("position", getCurrentPosition());
        i.putExtra("duration", mPlayer.getDuration());
        sendBroadcast(i);
    }
}