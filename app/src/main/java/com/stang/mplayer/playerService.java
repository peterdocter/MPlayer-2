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

/**
 * Created by Stanislav on 25.10.2016.
 */

public class PlayerService extends Service {
    public static final String TAG = MainActivity.class.getSimpleName();
    public static final int NOTIFY_ID = 1;
    public static final int UPDATER_DELAY = 1000;
    public static final int REQUEST_CODE = 5;
    public static final String ACTION_PROGRESS_CHANGED = "PROGRESS_CHANGED";
    public static final String ACTION_STATE_CHANGED = "STATE_CHANGED";
    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_PREV = "ACTION_PREV";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_EXIT = "ACTION_EXIT";

    private NotificationManager mNotificationManager;
    private IntentFilter mIntentFilter;
    private BroadcastReceiver mReceiver;

    private final IBinder mBinder = new MusicBinder();
    private Messenger outMessenger;

    private ServiceData mServiceData = new ServiceData();

    private Handler mHandler = new Handler();
    private MediaPlayer mPlayer = new MediaPlayer();

    // todo implement with messages
    private final Runnable mUpdater = new Runnable(){
        public void run(){
            try {
                if((mPlayer != null) && mPlayer.isPlaying()){
                    sendBroadcastProgress();
                    mHandler.postDelayed(this, UPDATER_DELAY);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    public ServiceData getServiceData() {
        return mServiceData;
    }

    public void setServiceData(ServiceData data) {
        mServiceData = data;
    }

    public boolean isPlaying() {
        return ((mPlayer != null) && mPlayer.isPlaying());
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

        sendBroadcastState();

        return mBinder;
    }

    public class MusicBinder extends Binder {
        PlayerService getService() {
            return PlayerService.this;
        }
    }


    public void onCreate() {
        super.onCreate();
        //Log.d(TAG, "onCreate SERVICE");
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        initPlayer();

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(PlayerService.ACTION_PLAY);
        mIntentFilter.addAction(PlayerService.ACTION_NEXT);
        mIntentFilter.addAction(PlayerService.ACTION_EXIT);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "SERVICE mReceiver action=" + action);
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

        registerReceiver(mReceiver, mIntentFilter);

        startForeground(NOTIFY_ID, getEmptyNotification());
        //stopForeground(true);
    }


    public void initPlayer() {
        mPlayer.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(mServiceData.getRepeat()) {
                    play(mServiceData.getCurrentPosition());
                } else {
                    nextTrack();
                }
                showNotify();
            }
        });
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
                mHandler.removeCallbacks(mUpdater);
                mHandler.postDelayed(mUpdater,UPDATER_DELAY);
                onPositionChanged();
                showNotify();
            }
        });
    }


    public void sendBroadcastState() {
        int duration = 0;
        if(mPlayer != null && mPlayer.isPlaying()) {
            duration = mPlayer.getDuration();
        }

        Intent i = new Intent(PlayerService.ACTION_STATE_CHANGED)
                .putExtra("position", mServiceData.getCurrentPosition())
                .putExtra("duration", duration);
        sendBroadcast(i);
        Log.d(TAG, "SERVICE mUpdater, position=" + mServiceData.getCurrentPosition());
    }


    public void sendBroadcastProgress() {
        int progress =  (mPlayer.getCurrentPosition() * 100 / mPlayer.getDuration());
        Intent i = new Intent(PlayerService.ACTION_PROGRESS_CHANGED)
                .putExtra("progress", progress)
                .putExtra("remain", mPlayer.getCurrentPosition());
        sendBroadcast(i);
        Log.d(TAG, "SERVICE mUpdater, progress=" + progress);
    }


    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SERVICE onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }


    public void onDestroy() {
        unregisterReceiver(mReceiver);
        mHandler.removeCallbacks(mUpdater);

        if(mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
        }

        mNotificationManager.cancel(NOTIFY_ID);

        super.onDestroy();
        Log.d(TAG, "SERVICE onDestroy");
    }


    public void seekTo(int seekBarPosition) {
        if (mPlayer != null) {
            if (mPlayer.isPlaying() || mPlayer.getCurrentPosition()>0) {
                int songDuration = mPlayer.getDuration();
                int msec = (int) (songDuration * seekBarPosition / 100F);
                mPlayer.seekTo(msec);
            }
        }
    }


    public void showNotify() {
        //mNotificationManager.cancel(NOTIFY_ID);
        mNotificationManager.notify(NOTIFY_ID, getNotificationWithButtons());
    }


    public void play(int position) {
        mHandler.removeCallbacks(mUpdater);
        if(position < 0 || position >= mServiceData.getPlaylistSize()) return;

        mServiceData.setCurrentPosition(position);

        if(mPlayer != null){
            mPlayer.reset();
        } else {
            mPlayer = new MediaPlayer();
            initPlayer();
        }

        Song song = mServiceData.getCurrentSong();
        Uri fileUri = Uri.parse(song.fileName);

        try {
            mPlayer.setDataSource(getApplicationContext(), fileUri);
            mPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
            if(mPlayer != null) {
                mPlayer.release();
            }
        }

        Log.d(TAG, "PlayService.play() " + song.songTitle);
    }


    public void pause(){
        if (mPlayer != null ){
            if(mPlayer.isPlaying()){
                mHandler.removeCallbacks(mUpdater);
                mPlayer.pause();
            } else {
                mPlayer.start();
                mHandler.removeCallbacks(mUpdater);
                mHandler.postDelayed(mUpdater, UPDATER_DELAY);
            }
        }
        showNotify();
        sendBroadcastState();
    }


    public void nextTrack(){
        int position = mServiceData.getCurrentPosition();
        int nextPosition = RecyclerView.NO_POSITION;

        if(mServiceData.getQueue().size() > 0){
            int pos = mServiceData.getPositionInQueue(position);
            if(pos == -1) {
                nextPosition = mServiceData.getQueue().get(0);
                mServiceData.getQueue().remove(0);
            } else
            {
                if(pos == mServiceData.getQueue().size() -1) {
                    nextPosition = position + 1;
                    mServiceData.getQueue().clear();
                } else if (pos < mServiceData.getQueue().size() -1) {
                    pos++;
                    nextPosition = mServiceData.getQueue().get(pos);
                    if(pos == mServiceData.getQueue().size()-1) {
                        mServiceData.getQueue().clear();
                    } else {
                        for (int i = 0; i < pos; i++) {
                        mServiceData.getQueue().remove(0);
                        }
                    }
                }
            }
        } else {
            nextPosition = position + 1;
        }

        if(nextPosition < mServiceData.getPlaylistSize() ){
            play(nextPosition);
        }
    }


    public void prevTrack(){
        int pos = mServiceData.getCurrentPosition();
        if(pos > 0 ){
            pos--;
            mServiceData.setCurrentPosition(pos);
            play(pos);
        }
    }


    public void onPositionChanged(){
        sendBroadcastState();
    }


    private Notification getEmptyNotification() {
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

        return notification;
    }


    private Notification getNotificationWithButtons() {
        String text = "";
        Song song = mServiceData.getCurrentSong();
        if(song != null) {
            text = song.artistTitle + " :: " + song.songTitle;
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(this,
                0, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        PendingIntent pIntentPlay = PendingIntent.getBroadcast(this, REQUEST_CODE, new Intent(PlayerService.ACTION_PLAY), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pIntentNext = PendingIntent.getBroadcast(this, REQUEST_CODE, new Intent(PlayerService.ACTION_NEXT), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pIntentExit = PendingIntent.getBroadcast(this, REQUEST_CODE, new Intent(PlayerService.ACTION_EXIT), PendingIntent.FLAG_UPDATE_CURRENT);

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

        return notification;
    }
}