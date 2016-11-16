package com.stang.mplayer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.provider.MediaStore;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();
    public static final int FILE_SELECT_CODE = 1;

    PlayerService mPlayerService;
    ServiceData mServiceData;
    Intent mServiceIntent;
    private BroadcastReceiver mReceiver;
    private IntentFilter mFilter;

    private RecyclerAdapter mAdapter;

    private LinearLayoutManager mLayoutManager;
    ImageButton prevButton;
    ImageButton nextButton;
    ImageButton pauseButton;
    ImageButton repeatButton;
    SearchView searchView;
    Spinner searchSpinner;
    Spinner sortSpinner;
    ImageView albumImage;
    TextView artistTitle;
    TextView songTitle;
    RecyclerView mPlaylistView;
    SeekBar seekBar;
    TextView duration;
    TextView remain;


    //////////////////////////////////////////// LISTENERS AREA
    private View.OnClickListener mControlClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_pause:
                    pause();
                    break;
                case R.id.button_prev:
                    onPrevTrackClick();
                    break;

                case R.id.button_next:
                    nextTrack();
                    break;

                case R.id.button_repeat:
                    if (mServiceData.getRepeat()) {
                        repeatButton.setImageResource(android.R.drawable.btn_star_big_off);
                        mServiceData.setRepeat(false);
                    } else {
                        repeatButton.setImageResource(android.R.drawable.btn_star_big_on);
                        mServiceData.setRepeat(true);
                    }
                    break;

//            case R.id.button_addFile:
//                addFiles();
//                break;
//
//            case R.id.button_addFolder:
//                addFolder();
//                break;

            }
        }
    };

    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            seekTo(seekBar.getProgress());
        }
    };

    //////////////////////////////////////////// LISTENERS AREA END

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"OnCreate start");
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        mPlaylistView = (RecyclerView) findViewById(R.id.listView_playlist);

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);

        prevButton = (ImageButton) findViewById(R.id.button_prev);
        nextButton = (ImageButton) findViewById(R.id.button_next);
        pauseButton = (ImageButton) findViewById(R.id.button_pause);
        repeatButton = (ImageButton) findViewById(R.id.button_repeat);
        searchView = (SearchView) findViewById(R.id.searchView);
        searchSpinner = (Spinner) findViewById(R.id.spinner_search);
        sortSpinner = (Spinner) findViewById(R.id.spinner_sort);

        albumImage = (ImageView) findViewById(R.id.imageView_album);
        artistTitle = (TextView) findViewById(R.id.textView_artistTitle);
        songTitle = (TextView) findViewById(R.id.textView_songTitle);
        duration = (TextView) findViewById(R.id.textView_duration);
        remain = (TextView) findViewById(R.id.textView_remain);

        prevButton.setOnClickListener(mControlClickListener);
        nextButton.setOnClickListener(mControlClickListener);
        pauseButton.setOnClickListener(mControlClickListener);
        repeatButton.setOnClickListener(mControlClickListener);

        mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mPlaylistView.setLayoutManager(mLayoutManager);


        //PLAYLIST Adapter
        //-------------------------------------
        mAdapter = new RecyclerAdapter(MainActivity.this, null);
        mAdapter.setOnItemClickListener(new RecyclerAdapter.OnClickListener() {
            @Override
            public void onClick(View view, Song song, int position) {
                play(position);
                Log.d(TAG,"playlist OnClick position=" + position + " Song=" + song.fileName);
            }
        });

        mPlaylistView.setAdapter(mAdapter);

        // SEARCH SPINNER
        //--------------------------------------
        String[] searchCriteria = {"Song", "Artist", "Album"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, searchCriteria);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        searchSpinner.setAdapter(adapter);
        searchSpinner.setPrompt("Search");
        searchSpinner.setSelection(RecyclerAdapter.SEARCH_SONG);
        searchSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view,
                                               int position, long id) {
                        mServiceData.setSearchType(position);
                        mServiceData.doSearch();
                        onPlaylistChanged();
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    }
                });
        // SORT SPINNER
        //-------------------------------------
        String[] sortCriteria = {"sort:Song", "sort:Artist", "sort:Date", "sort:Duration"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sortCriteria);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);
        sortSpinner.setPrompt("Search");
        sortSpinner.setSelection(RecyclerAdapter.SEARCH_SONG);
        sortSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view,
                                               int position, long id) {
                        if(mServiceData.getSortType() != position) {
                            mServiceData.setSortType(position);
                            mServiceData.doSort();
                            onPlaylistChanged();
                        }
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    }
                });

        //SEARCH VIEW
        //-----------------------------------------
        searchView.setQueryHint("Search...");
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //Toast.makeText(getBaseContext(), query, Toast.LENGTH_SHORT).show();
                if(!mServiceData.getSearchPhrase().equalsIgnoreCase(query)) {
                    mServiceData.setSearchPhrase(query);
                    mServiceData.doSearch();
                    onPlaylistChanged();
                }
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if(!mServiceData.getSearchPhrase().equalsIgnoreCase(newText)) {
                    mServiceData.setSearchPhrase(newText);
                    mServiceData.doSearch();
                    onPlaylistChanged();
                }
                return false;
            }
        });


        //Broadcast Receiver
        //---------------------------------------------------
        mFilter = new IntentFilter();
        mFilter.addAction(PlayerService.ACTION_PROGRESS_CHANGED);
        mFilter.addAction(PlayerService.ACTION_STATE_CHANGED);
        mFilter.addAction(PlayerService.ACTION_EXIT);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action){
                    case PlayerService.ACTION_PROGRESS_CHANGED :
                        onProgressChanged(intent);
                        break;
                    case PlayerService.ACTION_STATE_CHANGED:
                        onStateChanged(intent);
                        break;
                    case PlayerService.ACTION_EXIT :
                        killService();
                        break;
                }
            }
        };

        mServiceIntent = new Intent(this, PlayerService.class);
        startService(mServiceIntent);

        Log.d(TAG,"OnCreate finish");
    }

    public void onProgressChanged(Intent intent){
        seekBar.setProgress(intent.getIntExtra("progress", 0));
        remain.setText(intToTime(intent.getIntExtra("remain", 0)));
    }


    public void onStateChanged(Intent intent) {
        int position  = intent.getIntExtra("position", -1);
        int dur = intent.getIntExtra("duration", 0);
        if (position > -1) {
            mServiceData.setCurrentPosition(position);
            Song song = mServiceData.getSong(position);
            songTitle.setText(song.songTitle);
            artistTitle.setText(song.artistTitle);
            duration.setText(intToTime(dur));
            ImageLoader.getInstance().displayImage(song.albumImage, albumImage);
            mPlaylistView.scrollToPosition(position);
            mAdapter.notifyDataSetChanged();
            pauseButton.setSelected(!mPlayerService.isPlaying());
        }
    }


    public ServiceConnection myConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            mPlayerService = ((PlayerService.MusicBinder) binder).getService();
            Log.d(TAG, "ServiceConnection " + "connected");
            mServiceData = mPlayerService.getServiceData();
            mAdapter.setServiceData(mServiceData);

            if(mServiceData.isSourceListEmpty()) {
                addFolder();
            } else {

            }
            searchSpinner.setSelection(mServiceData.getSearchType());
            searchView.setQuery(mServiceData.getSearchPhrase(), false);

            Log.d(TAG, "onServiceConnected currentPosition = " + mServiceData.getCurrentPosition());

            if (!mServiceData.isPlaylistEmpty()) {
                Song song = mServiceData.getCurrentSong();
                songTitle.setText(song.songTitle);
                artistTitle.setText(song.artistTitle);
                ImageLoader.getInstance().displayImage(song.albumImage, albumImage);
                //mPlaylistView.scrollToPosition(mServiceData.getCurrentPosition());
            }

            pauseButton.setSelected(!mPlayerService.isPlaying());

            if(mServiceData.getRepeat()) {
                repeatButton.setImageResource(android.R.drawable.btn_star_big_on);
            } else {
                repeatButton.setImageResource(android.R.drawable.btn_star_big_off);
            }

            //mAdapter.notifyDataSetChanged();
            mPlayerService.sendBroadcastState();
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "ServiceConnection " + "disconnected");
            mPlayerService = null;
        }
    };


    public Handler myHandler = new Handler() {
        public void handleMessage(Message message) {
            Bundle data = message.getData();
        }
    };


    public void doBindService() {
        // Create a new Messenger for the communication back
        // From the Service to the Activity
        Messenger messenger = new Messenger(myHandler);
        mServiceIntent.putExtra("MESSENGER", messenger);

        bindService(mServiceIntent, myConnection, Context.BIND_AUTO_CREATE);
    }


    public void seekTo(int progress) {
        if(mPlayerService != null ){
            mPlayerService.seekTo(progress);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mFilter);
        doBindService();
    }


    @Override
    protected void onPause() {
        if(mReceiver != null) {
            unregisterReceiver(mReceiver);
        }

        if(mPlayerService != null){
            unbindService(myConnection);
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    public void play(int position) {
        if(position < 0) return;
        if(mPlayerService != null) {
            mPlayerService.play(position);
        }
        mAdapter.setCurrentPosition(position);
//        Log.d(TAG, "play " + mAdapter.getSong(position));
    }


    public void pause() {
        if(mPlayerService != null) {
            mPlayerService.pause();
            //pauseButton.setSelected(mPlayerService.isPlaying() ? false : true);
            pauseButton.setSelected(!mPlayerService.isPlaying());
        }
    }


    public void onPlaylistChanged() {
        mPlaylistView.scrollToPosition(mServiceData.getCurrentPosition());
        mAdapter.notifyDataSetChanged();
    }


    public String intToTime(int time) {
        SimpleDateFormat df = new SimpleDateFormat("m:ss");
        return df.format(new Date(time));
//        time = time / 1000;
//        int min = time/60;
//        int sec = (time % 60);
//        return String.format("%d:%02d", min, sec);
    }


    public ArrayList<Song> getPlayListFromURI(Uri uri){
        ArrayList<Song> songs =new ArrayList<>();

        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
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

    private Song parseCursor(Cursor cursor) {
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

        Uri mainAlbumArtUri = Uri.parse("content://media/external/audio/albumart");
        Uri albumArtUri = ContentUris.withAppendedId(mainAlbumArtUri, cursor
                .getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
        return new Song(title, artist, album, String.valueOf(filename), albumArtUri.toString());
    }

    public void onPrevTrackClick(){
        if(mPlayerService != null) {
            mPlayerService.prevTrack();
        }
    }


    public void nextTrack(){
        if(mPlayerService != null) {
            mPlayerService.nextTrack();
        }
    }


    private void addFiles(){
        Log.d(TAG, "addFiles");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");      //all files
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to add"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "no FileManager", Toast.LENGTH_SHORT).show();
        }
    }


    private void addFolder(){
        mServiceData.setSourceList(
                getPlayListFromURI(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI));
        onPlaylistChanged();
    }


    private void killService() {
        if(mPlayerService != null) {
            //unbindService(myConnection);
            stopService(mServiceIntent);
        }
        this.finish();
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == FILE_SELECT_CODE) {

                Uri selectedFileUri = data.getData();
                String path = data.getDataString();

                Cursor cursor = null;
                cursor = getApplicationContext().getContentResolver().query(selectedFileUri,  null, null, null, null);
                if(cursor != null) {
                    cursor.moveToFirst();
                }
                mAdapter.addSong(parseCursor(cursor));

                if(mServiceData.getCurrentPosition() < 0) mServiceData.setCurrentPosition(0);

                onPlaylistChanged();
            }
        }
    }

}
