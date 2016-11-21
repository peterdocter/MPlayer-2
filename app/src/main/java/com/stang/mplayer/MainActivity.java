package com.stang.mplayer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Bundle;
import android.app.LoaderManager;
import android.content.Loader;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import android.support.design.widget.Snackbar;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    public static final String TAG = MainActivity.class.getSimpleName();
    public static final int FILE_SELECT_CODE = 1;
    private static final int PLAYLIST_LOADER_ID = 1;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private Boolean mExtStoragePermissionGranted = false;
    private String[] mPermissionToRequest = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        //Manifest.permission.READ_SMS
    };

    PlayerService mPlayerService;
    ServiceData mServiceData;
    Intent mServiceIntent;
    private BroadcastReceiver mReceiver;
    private IntentFilter mFilter;

    private RecyclerAdapter mPlaylistAdapter;

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
                    onNextTrackClick();
                    break;

                case R.id.button_repeat:
                    if (mServiceData.getRepeat()) {
                        repeatButton.setImageResource(R.drawable.ic_repeat_white_18dp);
                        mServiceData.setRepeat(false);
                    } else {
                        repeatButton.setImageResource(R.drawable.ic_repeat_black_18dp);
                        mServiceData.setRepeat(true);
                    }
                    break;
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

    private SearchView.OnQueryTextListener mSearchlistener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            if (!mServiceData.getSearchPhrase().equalsIgnoreCase(query)) {
                mServiceData.setSearchPhrase(query);
                mServiceData.doSearch();
                onPlaylistChanged();
            }
            return false;
        }
        @Override
        public boolean onQueryTextChange(String newText) {
            if (!mServiceData.getSearchPhrase().equalsIgnoreCase(newText)) {
                mServiceData.setSearchPhrase(newText);
                mServiceData.doSearch();
                onPlaylistChanged();
            }
            return false;
        }
    };

    private RecyclerAdapter.OnClickListener mPlaylistOnClickListener = new RecyclerAdapter.OnClickListener() {
        @Override
        public void onClick(View view, Song song, int position) {
            play(position);
        }
    };

    private LoaderManager.LoaderCallbacks<Playlist> mPlaylistLoaderCalbacks = new LoaderManager.LoaderCallbacks<Playlist>() {
        @Override
        public Loader<Playlist> onCreateLoader(int id, Bundle args) {
            Loader<Playlist> loader = null;
            if (id == PLAYLIST_LOADER_ID) {
                loader = new PlaylistLoader(getApplicationContext(), args);
            }
            return loader;
        }

        @Override
        public void onLoaderReset(Loader<Playlist> loader) {
        }

        @Override
        public void onLoadFinished(Loader<Playlist> loader, Playlist data) {
            mServiceData.setSourceList(data);
            onPlaylistChanged();
        }
    };

    //////////////////////////////////////////// LISTENERS AREA END

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.d(TAG, "OnCreate start");
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        initViews();
        initControls();
        initSortSpinner();
        initSearchSpinner();
        initSearchView();
        initPlaylistView();

        mFilter = getIntentFilterForActivity();
        mReceiver = getBroadcastReceiverForActivity();

        mServiceIntent = new Intent(this, PlayerService.class);
        startService(mServiceIntent);

        //Log.d(TAG, "OnCreate finish");
    }

    private void initPlaylistView() {
        mPlaylistAdapter = new RecyclerAdapter(MainActivity.this);
        mPlaylistAdapter.setOnItemClickListener(mPlaylistOnClickListener);
        mPlaylistView.setAdapter(mPlaylistAdapter);
    }

    private void initSearchView() {
        searchView.setQueryHint(getResources().getString(R.string.search_prompt));
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
            }
        });
        searchView.setOnQueryTextListener(mSearchlistener);
    }

    private BroadcastReceiver getBroadcastReceiverForActivity() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case PlayerService.ACTION_PROGRESS_CHANGED:
                        onProgressChanged(intent);
                        break;
                    case PlayerService.ACTION_STATE_CHANGED:
                        onStateChanged(intent);
                        break;
                    case PlayerService.ACTION_POSITION_CHANGED:
                        onPositionChanged(intent);
                        break;
                    case PlayerService.ACTION_EXIT:
                        killService();
                        break;
                }
            }
        };
    }

    private IntentFilter getIntentFilterForActivity() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PlayerService.ACTION_PROGRESS_CHANGED);
        intentFilter.addAction(PlayerService.ACTION_STATE_CHANGED);
        intentFilter.addAction(PlayerService.ACTION_POSITION_CHANGED);
        intentFilter.addAction(PlayerService.ACTION_EXIT);
        return intentFilter;
    }

    private void initControls() {
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);

        searchSpinner = (Spinner) findViewById(R.id.spinner_search);
        sortSpinner = (Spinner) findViewById(R.id.spinner_sort);
        searchView = (SearchView) findViewById(R.id.searchView);

        prevButton = (ImageButton) findViewById(R.id.button_prev);
        nextButton = (ImageButton) findViewById(R.id.button_next);
        pauseButton = (ImageButton) findViewById(R.id.button_pause);
        repeatButton = (ImageButton) findViewById(R.id.button_repeat);

        prevButton.setOnClickListener(mControlClickListener);
        nextButton.setOnClickListener(mControlClickListener);
        pauseButton.setOnClickListener(mControlClickListener);
        repeatButton.setOnClickListener(mControlClickListener);
    }

    private void initViews() {
        mPlaylistView = (RecyclerView) findViewById(R.id.listView_playlist);
        mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mPlaylistView.setLayoutManager(mLayoutManager);

        albumImage = (ImageView) findViewById(R.id.imageView_album);
        artistTitle = (TextView) findViewById(R.id.textView_artistTitle);
        songTitle = (TextView) findViewById(R.id.textView_songTitle);
        duration = (TextView) findViewById(R.id.textView_duration);
        remain = (TextView) findViewById(R.id.textView_remain);
    }

    private void initSortSpinner() {
        // SORT SPINNER
        //-------------------------------------

        String[] sortCriteria = getResources().getStringArray(R.array.sort_criteria);
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sortCriteria);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);
        sortSpinner.setPrompt(getResources().getString(R.string.sort_prompt));
        sortSpinner.setSelection(RecyclerAdapter.SEARCH_SONG);
        sortSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view,
                                               int position, long id) {
                        if (mServiceData.getSortType() != position) {
                            mServiceData.setSortType(position);
                            mServiceData.doSort();
                            onPlaylistChanged();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    }
                });
    }

    private void initSearchSpinner() {
        // SEARCH SPINNER
        //--------------------------------------
        searchSpinner = (Spinner) findViewById(R.id.spinner_search);
        String[] searchCriteria = getResources().getStringArray(R.array.search_criteria);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, searchCriteria);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        searchSpinner.setAdapter(adapter);
        searchSpinner.setPrompt(getResources().getString(R.string.search_prompt));
        searchSpinner.setSelection(RecyclerAdapter.SEARCH_SONG);
        searchSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view,
                                               int position, long id) {
                        if (mServiceData.getSearchType() != position) {
                            mServiceData.setSearchType(position);
                            mServiceData.doSearch();
                            onPlaylistChanged();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    }
                });
    }

    private void checkRuntimePermissions(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            //final File externalStorage = Environment.getExternalStorageDirectory();
            mExtStoragePermissionGranted = true;
//            if (externalStorage != null) {
//                mExtStoragePermissionGranted = true;
//            } else {
//                mExtStoragePermissionGranted = false;
//            }
        } else {
            mExtStoragePermissionGranted = false;
        }
    }

    private void requestReadExtStoragePermission(){
        Log.d(TAG, "requestReadExtStoragePermission");
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //mExtStoragePermissionGranted = true;
                Toast.makeText(MainActivity.this, "GRANTED", Toast.LENGTH_LONG).show();
                loadPlaylistFromExtStorage();
            }   else {
                showSnackbarRequestPermission();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showSnackbarRequestPermission(){
        final String message = getString(R.string.storage_permission_needed);
        //Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE)
                            .setAction("GRANT", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    requestReadExtStoragePermission();
                                }
                            })
                            .show();
    }

    public void onProgressChanged(Intent intent) {
        seekBar.setProgress(intent.getIntExtra(PlayerService.ARG_INTENT_PROGRESS, 0));
        remain.setText(intToTime(intent.getIntExtra(PlayerService.ARG_INTENT_REMAIN, 0)));
    }

    public void onPositionChanged(Intent intent) {
        int position = intent.getIntExtra(PlayerService.ARG_INTENT_POSITION, -1);
        int dur = intent.getIntExtra(PlayerService.ARG_INTENT_DURATION, 0);
        mPlaylistAdapter.setCurrentPosition(position);
        mPlaylistView.scrollToPosition(position);

        Song song = mServiceData.getSong(position);
        if (song == null)
            song = new Song();

        songTitle.setText(song.songTitle);
        artistTitle.setText(song.artistTitle);
        ImageLoader.getInstance().displayImage(song.albumImage, albumImage);

        duration.setText(intToTime(dur));
        remain.setText(intToTime(0));
        seekBar.setProgress(0);

        if (!mServiceData.isQueueEmpty()) mPlaylistAdapter.notifyDataSetChanged();
    }

    public void onStateChanged(Intent intent) {
        if (mPlayerService != null) {
            pauseButton.setSelected(!mPlayerService.isPlaying());
        }
        int dur = intent.getIntExtra(PlayerService.ARG_INTENT_DURATION, 0);
        duration.setText(intToTime(dur));
    }

    public ServiceConnection myConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.d(TAG, "ServiceConnection " + "connected");

            mPlayerService = ((PlayerService.MusicBinder) binder).getService();
            mServiceData = ServiceData.getInstance();

            searchSpinner.setSelection(mServiceData.getSearchType());
            searchView.setQuery(mServiceData.getSearchPhrase(), false);

            //Log.d(TAG, "onServiceConnected currentPosition = " + mServiceData.getCurrentPosition());

            pauseButton.setSelected(!mPlayerService.isPlaying());

            if (mServiceData.getRepeat()) {
                repeatButton.setImageResource(R.drawable.ic_repeat_black_18dp);
            } else {
                repeatButton.setImageResource(R.drawable.ic_repeat_white_18dp);
            }

            if (mServiceData.isSourceListEmpty()) {
                //checkRuntimePermissions();
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    loadPlaylistFromExtStorage();
                } else {
                    if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        showSnackbarRequestPermission();
                    } else {
                        requestReadExtStoragePermission();
                    }
                }
            }

            onPlaylistChanged();
        }

        public void onServiceDisconnected(ComponentName className) {
            //Log.d(TAG, "ServiceConnection " + "disconnected");
            mPlayerService = null;
        }
    };

    private void loadPlaylistFromExtStorage(){
        Bundle bndl = new Bundle();
        bndl.putString(PlaylistLoader.ARGS_PLAYLIST_URI, PlaylistLoader.DEFAULT_DATA_URI.toString());
        getLoaderManager().initLoader(PLAYLIST_LOADER_ID, bndl, mPlaylistLoaderCalbacks)
                .forceLoad();
    }

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
        if (mPlayerService != null) {
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
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }

        if (mPlayerService != null) {
            unbindService(myConnection);
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void play(int position) {
        if (position < 0) return;
        if (mPlayerService != null) {
            mPlayerService.play(position);
        }
    }

    public void pause() {
        if (mPlayerService != null) {
            mPlayerService.pause();
            //pauseButton.setSelected(mPlayerService.isPlaying() ? false : true);
            pauseButton.setSelected(!mPlayerService.isPlaying());
        }
    }

    public void onPlaylistChanged() {
        mPlaylistAdapter.notifyDataSetChanged();
        mPlayerService.sendBroadcastState();
        mPlayerService.sendBroadcastPosition(mServiceData.getCurrentPosition());
        mPlayerService.sendBroadcastProgress();
    }

    public String intToTime(int time) {
        SimpleDateFormat df = new SimpleDateFormat("m:ss");
        return df.format(new Date(time));
//        time = time / 1000;
//        int min = time/60;
//        int sec = (time % 60);
//        return String.format("%d:%02d", min, sec);
    }

    public void onPrevTrackClick() {
        if (mPlayerService != null) {
            mPlayerService.prevTrack();
        }
    }

    public void onNextTrackClick() {
        if (mPlayerService != null) {
            mPlayerService.nextTrack();
        }
    }

    private void killService() {
        if (mPlayerService != null) {
            stopService(mServiceIntent);
        }
        this.finish();
    }

    private void addFiles() {
        //Log.d(TAG, "addFiles");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");      //all audio files
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, getString(R.string.add_files_prompt)), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "no FileManager", Toast.LENGTH_SHORT).show();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == FILE_SELECT_CODE) {

                Uri selectedFileUri = data.getData();
                String path = data.getDataString();

                Cursor cursor = null;
                cursor = getApplicationContext().getContentResolver().query(selectedFileUri, null, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                }
                mPlaylistAdapter.addSong(PlaylistLoader.parseCursor(cursor));

                if (mServiceData.getCurrentPosition() < 0) mServiceData.setCurrentPosition(0);

                onPlaylistChanged();
            }
        }
    }

}
