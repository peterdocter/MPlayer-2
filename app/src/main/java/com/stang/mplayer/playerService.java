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
    public static final String ACTION_POSITION_CHANGED = "POSITION_CHANGED";
    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_PREV = "ACTION_PREV";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_EXIT = "ACTION_EXIT";

    private NotificationManager mNotificationManager;
    private IntentFilter mIntentFilter;
    private BroadcastReceiver mReceiver;

    private final IBinder mBinder = new MusicBinder();
    private Messenger outMessenger;

    private ServiceData mServiceData = ServiceData.getInstance();

    private Handler mHandler = new Handler();
    private MediaPlayer mPlayer;
    private boolean mMediaPlayerPrepared;

    // todo implement with messages
    private final Runnable mUpdater = new Runnable() {
        public void run() {
            try {
                if (mMediaPlayerPrepared && mPlayer.isPlaying()) {
                    sendBroadcastProgress();
                    mHandler.postDelayed(this, UPDATER_DELAY);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


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
                switch (action) {
                    case PlayerService.ACTION_EXIT:
                        stopSelf();
                        break;
                    case PlayerService.ACTION_NEXT:
                        nextTrack();
                        break;
                    case PlayerService.ACTION_PLAY:
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
        mPlayer = new MediaPlayer();
        mPlayer.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayerPrepared = false;
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (mServiceData.getRepeat()) {
                    mPlayer.start();
                } else {
                    if (mServiceData.hasNext()) {
                        nextTrack();
                    } else {
                        seekTo(0);
                        sendBroadcastState();
                    }

                }
                showNotify();
            }
        });
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mMediaPlayerPrepared = true;
                mp.start();
                mHandler.removeCallbacks(mUpdater);
                mHandler.postDelayed(mUpdater, UPDATER_DELAY);
                sendBroadcastState();
                showNotify();
            }
        });
    }


    public void sendBroadcastState() {
        int duration = 0;
        if (mMediaPlayerPrepared) {
            duration = mPlayer.getDuration();
        }
        Intent i = new Intent(PlayerService.ACTION_STATE_CHANGED)
                .putExtra("position", mServiceData.getCurrentPosition())
                .putExtra("duration", duration);
        sendBroadcast(i);
    }


    public void sendBroadcastPosition(int position) {
        Intent i = new Intent(PlayerService.ACTION_POSITION_CHANGED)
                .putExtra("position", position);
        sendBroadcast(i);
        Log.d(TAG, "SERVICE mUpdater,  newPosition=" + position);
    }


    public void sendBroadcastProgress() {
        int progress = 0;
        int remain = 0;
        if (mMediaPlayerPrepared) {
            progress = (mPlayer.getCurrentPosition() * 100 / mPlayer.getDuration());
            remain = mPlayer.getCurrentPosition();
        }

        Intent i = new Intent(PlayerService.ACTION_PROGRESS_CHANGED)
                .putExtra("progress", progress)
                .putExtra("remain", remain);
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

        if (mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
        }

        mNotificationManager.cancel(NOTIFY_ID);

        super.onDestroy();
        Log.d(TAG, "SERVICE onDestroy");
    }


    public void seekTo(int seekBarPosition) {
        if (mMediaPlayerPrepared) {
            int songDuration = mPlayer.getDuration();
            int msec = (int) (songDuration * seekBarPosition / 100F);
            mPlayer.seekTo(msec);
            sendBroadcastProgress();
        }
    }


    public void showNotify() {
        //mNotificationManager.cancel(NOTIFY_ID);
        mNotificationManager.notify(NOTIFY_ID, getNotificationWithButtons());
    }


    public void play(int newPosition) {
        Log.d(TAG, "PlayerService.play(" + newPosition + ")");
        if (newPosition < 0 || newPosition >= mServiceData.getPlaylistSize()) return;
        mMediaPlayerPrepared = false;
        mHandler.removeCallbacks(mUpdater);

        if (mPlayer != null) {
            try {
                mPlayer.reset();
            } catch (Exception e) {
                initPlayer();
            }
        } else {
            initPlayer();
        }

        mServiceData.setCurrentPosition(newPosition);
        sendBroadcastPosition(newPosition);
        Song song = mServiceData.getSong(newPosition);
        Uri fileUri = Uri.parse(song.fileName);

        try {
            Log.d(TAG, "file URI = " + fileUri.toString());
            Log.d(TAG, (mPlayer == null) ? "mPlayer=null" : "mPlayer!=null");
            mPlayer.setDataSource(getApplicationContext(), fileUri);
            mPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            initPlayer();
            play(newPosition + 1);
//            if (mPlayer != null) {
//                mPlayer.release();
//            }
        }
    }


    public void pause() {
        if (mMediaPlayerPrepared) {
            if (mPlayer.isPlaying()) {
                mHandler.removeCallbacks(mUpdater);
                mPlayer.pause();
            } else {
                mPlayer.start();
                mHandler.removeCallbacks(mUpdater);
                mHandler.postDelayed(mUpdater, UPDATER_DELAY);
            }
        } else {
            play(mServiceData.getCurrentPosition());
        }
        showNotify();
        sendBroadcastState();
    }


    public void nextTrack() {
        int position = mServiceData.getCurrentPosition();
        int nextPosition = RecyclerView.NO_POSITION;

        if (mServiceData.getQueue().size() > 0) {
            int pos = mServiceData.getPositionInQueue(position);
            if (pos == -1) {
                nextPosition = mServiceData.getQueue().get(0);
                mServiceData.getQueue().remove(0);
            } else {
                if (pos == mServiceData.getQueue().size() - 1) {
                    nextPosition = position + 1;
                    mServiceData.getQueue().clear();
                } else if (pos < mServiceData.getQueue().size() - 1) {
                    pos++;
                    nextPosition = mServiceData.getQueue().get(pos);
                    if (pos == mServiceData.getQueue().size() - 1) {
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

        if (nextPosition < mServiceData.getPlaylistSize()) {
            play(nextPosition);
        }
    }


    public void prevTrack() {
        int pos = mServiceData.getCurrentPosition();
        if (pos > 0) {
            pos--;
            play(pos);
        }
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
                .setSmallIcon(R.drawable.music_box_white)
                // большая картинка
                .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.music_box_white))
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
        if (song != null) {
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
        if (mPlayer != null && mPlayer.isPlaying()) {
            playPauseIcon = android.R.drawable.ic_media_pause;
        } else {
            playPauseIcon = android.R.drawable.ic_media_play;
        }

        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.music_box_white)
                // большая картинка
                .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.music_box_white))
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