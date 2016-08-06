package com.example.shalantor.mediaplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity
        implements ListViewFragment.OnSongSelectedListener{

    private MediaPlayer player;
    private boolean isPlaying = false;                              /*is player playing something?*/
    private ArrayList<String> songNames = new ArrayList<>();
    private ArrayList<String> songPaths = new ArrayList<>();
    private ArrayList<String> albumNames = new ArrayList<>();
    private int numSongs;
    private int curSongIndex;                                       /*Index of current song*/
    private android.os.Handler handler = new android.os.Handler();  /*Handler for extra thread on UI*/
    private SeekBar seek ;                                          /*Seekbar which shows song progress*/
    private ArrayAdapter<String> adapter;                           /*Adapter to populate listview with items*/
    /*Various strings used as bundle tags*/
    static final String CURRENT_SONG = "curSong";
    static final String CURRENT_SEEKBAR_POS = "seekPos";
    static final String ISPLAYING = "isPlaying";
    static final String ISPLAYERNULL = "isPaused";
    static final String VOLUME = "volume";
    static final String VOLUMER_BAR_PROGRESS = "volumeBar";
    static final String IS_LOOPING = "isLooping";
    static final String IS_SHUFFLING = "isShuffling";
    private int orientation;                                        /*Orientation of screen*/
    private MediaPlayerFragment mediaPlayer = null;
    private ListViewFragment songList = null;
    private Bundle save;                                            /*Used when portrait mode is active*/
    static final int MAX_VOLUME = 10;                               /*Volume range is 0 - 10*/
    private final static int SECONDS_TO_DISAPPEAR = 5;              /*How long to show seekbar for volume*/
    private float currentVolume = (float)(Math.log(MAX_VOLUME/2)
                                    /Math.log(MAX_VOLUME));         /*Starting volume*/
    private Timer timer;                                            /*Timer for volume seekbar*/
    private boolean isRepeating = false;                            /*Is song on repeat mode?*/
    /*Constants for swiping*/
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    /*Constants for recreating play list */
    private static final String LIST_SIZE = "size";
    private static final String ITEM = "item";
    private static final String PATH = "path";
    private static final String ALBUM = "album";
    private GestureDetector detector ;                              /*Detects swipes*/
    private AudioManager.OnAudioFocusChangeListener afChangeListener;/*Listener for audio focus*/
    private AudioManager am;
    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private NoisyAudioStreamReceiver noisyReceiver = new NoisyAudioStreamReceiver();
    private boolean isRegistered = false;                           /*Is audio receiver registered?*/
    private boolean isShuffling = false;                            /*Is shuffle mode active?*/


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*Get screen orientation*/
        orientation = this.getResources().getConfiguration().orientation;

        /*Set audio stream and instantiate audiochangelistener*/
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        /*Ad listener for audio focus change*/
        this.addAudioListener();

        /*Create the playlist */
        if(savedInstanceState == null) {
            this.createPlaylist();
        }
        else{/*If recreated retrieve playlist from SharedPreferences*/
            this.retrievePlaylist();
            /*Get loop status*/
            isRepeating = savedInstanceState.getBoolean(IS_LOOPING);
            /*get shuffle status*/
            isShuffling = savedInstanceState.getBoolean(IS_SHUFFLING);
        }

        /*if screen is horizontal*/
        if(orientation == Configuration.ORIENTATION_LANDSCAPE){
            setContentView(R.layout.activity_main);
            seek = (SeekBar) findViewById(R.id.seekbar);            /*Instantiate seekbar*/
            this.setupVolumeListener();                             /*listener for volume seekbar*/
            this.setupPlayer(savedInstanceState);                   /*setup player*/
            this.addListViewListener();                             /*listener for playlist right of player*/
        }
        else{
            setContentView(R.layout.fragment_container_portrait);
            /*Media player is being recreated*/
            if(savedInstanceState != null) {
                boolean isNull = savedInstanceState.getBoolean(ISPLAYERNULL);
                if(isNull){                                                     /*player was not playing any song before recreation*/
                    songList = new ListViewFragment();
                    getSupportFragmentManager().beginTransaction().add(R.id.fragment_container,songList).commit();
                }
                else {                                                          /*player was playing a song before recreation*/
                    mediaPlayer = new MediaPlayerFragment();
                    getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, mediaPlayer).commit();
                    save = savedInstanceState; /*Will be used in onStart to setupPlayer*/
                }
            }
            else{/*Create listView for user to choose a song,since player was created for first time*/
                songList = new ListViewFragment();
                getSupportFragmentManager().beginTransaction().add(R.id.fragment_container,songList).commit();
            }
        }
    }

    /*Some methods like finding a view by id cannot be called in
    the onCreate function because we deal with fragments. So they have
    to be executed after the gui has been created. In this case this only
    happens in portrait mode , because only then some fragments will be
    added dynamically to the activity.
     */

    @Override
    protected void onStart(){
        super.onStart();

        if(orientation == Configuration.ORIENTATION_PORTRAIT){

            if(save != null) { /*Being recreated or not?*/
                boolean isNull = save.getBoolean(ISPLAYERNULL);

                if(isNull){/*Show list of songs*/
                    this.waitForSong();
                }
                else {
                    seek = mediaPlayer.getSeekBar(); /*seekbar for song */
                    this.setupVolumeListener();
                    this.setupPlayer(save);
                }
                /*Instantiate seekbar for volume*/
                SeekBar volumeBar = mediaPlayer.getVolumeSeekBar();
                volumeBar.setProgress(save.getInt(VOLUMER_BAR_PROGRESS));
            }
            else{/*Show list of songs*/
                this.waitForSong();
            }
        }

    }

    /*Method to add listener for audio focus*/
    private void addAudioListener(){
                /*Setup audio focus listener*/
        afChangeListener = new AudioManager.OnAudioFocusChangeListener(){
            public void onAudioFocusChange(int focusChange){
                if(focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){                          /*Temporary loss of audio focus, pause player*/
                    if(player != null && isPlaying) {
                        /*Check which fragments are visible*/
                        if (orientation == Configuration.ORIENTATION_LANDSCAPE ||
                                getSupportFragmentManager().findFragmentById(R.id.player) != null) {
                            MainActivity.this.play(null);
                        }
                        else{
                            MainActivity.this.simplePlay(null);
                        }
                    }
                }
                else if(focusChange == AudioManager.AUDIOFOCUS_GAIN){                               /*Got audio focus, play song*/
                    if(player != null && !isPlaying) {
                        /*Check which fragments are visible*/
                        if (orientation == Configuration.ORIENTATION_LANDSCAPE ||
                                getSupportFragmentManager().findFragmentById(R.id.player) != null) {
                            MainActivity.this.play(null);
                        }
                        else{
                            MainActivity.this.simplePlay(null);
                        }
                    }
                }
                else if(focusChange == AudioManager.AUDIOFOCUS_LOSS){                               /*lost audiofocus for good, stop player release resources*/
                    am.abandonAudioFocus(afChangeListener);
                    /*Check which fragments are visible*/
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE ||
                            getSupportFragmentManager().findFragmentById(R.id.player) != null) {
                        MainActivity.this.stop(null);
                        if(isRegistered) {
                            unregisterReceiver(noisyReceiver);          /*Unregister receiver*/
                            isRegistered = false;
                        }
                    }
                    else{
                        if(player != null){                             /*release player resource*/
                            player.release();
                            player = null;
                            if(isRegistered) {
                                unregisterReceiver(noisyReceiver);          /*Unregister receiver*/
                                isRegistered = false;
                            }
                        }
                    }
                }
            }
        };
    }

    /*Method to request audio focus before starting playback*/
    private boolean requestFocus(){
        int result = am.requestAudioFocus(afChangeListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);

        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    /*Implement method of interface for fragment communication*/
    @Override
    public void onSongSelected(int position){
        /*Replace with new media player fragment and start song selected*/
        mediaPlayer = new MediaPlayerFragment();

        /*Get progress of volumebar*/
        int volumeProgress = songList.getVolumeSeekbar().getProgress();

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();


        /*Replace and add to back stack*/
        transaction.replace(R.id.fragment_container,mediaPlayer);

        /*Change songList fragment to null since it isnt visible now*/
        songList = null;

        transaction.commit();

        fm.executePendingTransactions();

        boolean setVolumeProgress = false;
        if(player != null){
            player.release();
            player = null;
            isPlaying = false;
            setVolumeProgress = true;
        }
        seek = mediaPlayer.getSeekBar();

        if(isRepeating){
            ImageButton bt = mediaPlayer.getVolumeButton();
            bt.setImageResource(R.mipmap.repeat_active);
        }

        if(isShuffling){
            ImageButton button = mediaPlayer.getShuffleButton();
            button.setImageResource(R.mipmap.shuffle_active);
        }

        /*Start playing selected song*/
        curSongIndex = position;
        this.setupPlayer(null);
        this.setupVolumeListener();
        this.play(null);

        /*Set progress of volume seekbar*/
        if(setVolumeProgress) {
            mediaPlayer.getVolumeSeekBar().setProgress(volumeProgress);
        }
    }

    /*Method that waits for user to choose a song to play*/
    private void waitForSong(){
        ListView playList = songList.getListView();
        adapter = new ArrayAdapter<>(MainActivity.this,R.layout.list_item,songNames);
        playList.setAdapter(adapter);
        songList.waitForSongSelect();

    }

    /*Method to set up the media player and the UI*/
    private void setupPlayer( Bundle savedInstanceState){

        /*Listener for swipes*/
        detector = new GestureDetector(this,new MyGestureDetector());
        if(orientation == Configuration.ORIENTATION_LANDSCAPE ||
                getSupportFragmentManager().findFragmentById(R.id.player) != null) {
            ImageView musicNoteImage = (ImageView) findViewById(R.id.image);
            musicNoteImage.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    detector.onTouchEvent(motionEvent);
                    return true;
                }
            });
        }

        if(seek == null){
            seek = mediaPlayer.getSeekBar();
        }

        /*Check if being recreated*/
        if(savedInstanceState != null){
            curSongIndex = savedInstanceState.getInt(CURRENT_SONG);
            int seekPos = savedInstanceState.getInt(CURRENT_SEEKBAR_POS);
            isPlaying = savedInstanceState.getBoolean(ISPLAYING);
            currentVolume = savedInstanceState.getFloat(VOLUME);
            boolean isPlayerNull = savedInstanceState.getBoolean(ISPLAYERNULL);
            if(!isPlayerNull){
                player = MediaPlayer.create(MainActivity.this,
                        Uri.parse(songPaths.get(curSongIndex)));
                int seekRange = player.getDuration() / 1000;
                player.setVolume(1-currentVolume,1-currentVolume);
                seek.setMax(seekRange);
                seek.setProgress(seekPos);
                this.adjustSeekBarMovement();
                player.seekTo(seekPos * 1000);
                this.setDurationText();
                if(isPlaying){
                    /*Set text of animated textview and image of repeat button*/
                    TextView curSong;
                    SeekBar volumeBar;
                    ImageButton volButton;
                    ImageButton shuffleButton;
                    TextView albumView;
                    if(orientation == Configuration.ORIENTATION_LANDSCAPE){
                        curSong = (TextView) findViewById(R.id.curSong);
                        volumeBar = (SeekBar) findViewById(R.id.volumeControl);
                        volButton = (ImageButton) findViewById(R.id.loop_song);
                        shuffleButton = (ImageButton) findViewById(R.id.shuffle);
                        albumView = (TextView) findViewById(R.id.albumName);
                    }
                    else{
                        curSong = mediaPlayer.getSongView();
                        volumeBar = mediaPlayer.getVolumeSeekBar();
                        volButton = mediaPlayer.getVolumeButton();
                        shuffleButton = mediaPlayer.getShuffleButton();
                        albumView = mediaPlayer.getAlbumView();
                    }

                    boolean loops = savedInstanceState.getBoolean(IS_LOOPING);
                    if(loops){
                        volButton.setImageResource(R.mipmap.repeat_active);
                    }

                    boolean shuffles = savedInstanceState.getBoolean(IS_SHUFFLING);
                    if(shuffles){
                        shuffleButton.setImageResource(R.mipmap.shuffle_active);
                    }

                    volumeBar.setProgress(savedInstanceState.getInt(VOLUMER_BAR_PROGRESS));
                    curSong.setText(songNames.get(curSongIndex), TextView.BufferType.NORMAL);

                    /*Set text of albumName*/
                    String albumText = "Album: " + albumNames.get(curSongIndex);
                    albumView.setText(albumText, TextView.BufferType.NORMAL);

                    /*Change image of button to pause*/
                    ImageButton bt = (ImageButton) findViewById(R.id.Pause);
                    bt.setImageResource(R.mipmap.pause);

                    if(this.requestFocus()) {
                        registerReceiver(noisyReceiver,intentFilter);
                        isRegistered = true;
                        player.start();
                    }

                }
            }
        }
        /*Adjust text size and movement in song textview*/
        this.adjustText();

        /*Now add listener to seekbar, so that the user can change the position of audio*/
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(player != null && fromUser){
                    player.seekTo(progress * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    /*Adds listener to listview when in landscape orientation*/
    private void addListViewListener(){
        /*New create listview and add listener*/
        ListView list = (ListView) findViewById(R.id.playlist);
        adapter = new ArrayAdapter<>(MainActivity.this,R.layout.list_item,songNames);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                /*If there is player before release it*/
                if(player != null){
                    player.release();
                    player = null;
                }
                /*Create new player with selected song*/
                player = MediaPlayer.create(MainActivity.this,
                        Uri.parse(songPaths.get(position)));
                /*Change current song index*/
                curSongIndex = position;
                MainActivity.this.setDurationText();
                /*Start new song*/
                player.setVolume(1-currentVolume,1-currentVolume);
                if(MainActivity.this.requestFocus()) {
                    registerReceiver(noisyReceiver,intentFilter);
                    isRegistered = true;
                    player.start();
                }
                MainActivity.this.adjustText();
                MainActivity.this.adjustSeekBarMovement();
                ImageButton bt = (ImageButton) findViewById(R.id.Pause);
                bt.setImageResource(R.mipmap.pause);
            }
        });

    }

    /*Scans the sd card and saves all songs in a list of hashmaps*/
    private void createPlaylist(){
        String[] STAR = {"*"};
        Cursor cursor;
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!=0";

        cursor = getContentResolver().query(uri,STAR,selection,null,null);

        if(cursor != null){
            if(cursor.moveToFirst()){
                do {
                    String songName = cursor.getString(cursor
                            .getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                    String path = cursor.getString(cursor
                            .getColumnIndex(MediaStore.Audio.Media.DATA));
                    String albumName = cursor.getString(cursor
                            .getColumnIndex(MediaStore.Audio.Media.ALBUM));

                    /*Remove file extension from songname*/
                    int dotPosition = songName.lastIndexOf('.');
                    if(dotPosition == songName.length() - 4){/*Can remove extension*/
                        songName = songName.substring(0,songName.length() - 4);
                    }

                    songNames.add(songName);
                    songPaths.add(path);
                    albumNames.add(albumName);
                }while(cursor.moveToNext());
            }
            cursor.close();
        }
        numSongs = songNames.size();
    }

    /*retrieves playlist from shared preferences so that it is not created again every time*/
    private void retrievePlaylist(){
        SharedPreferences preferences = getSharedPreferences("SKATA",Context.MODE_PRIVATE);
        int size = preferences.getInt(LIST_SIZE,0);
        String listItem;
        String itemPath;
        String albumName;

        /*Loop through items of preferences*/
        for(int i = 0; i < size; i++){
            listItem = preferences.getString(ITEM + i,null);
            itemPath = preferences.getString(PATH + i,null);
            albumName = preferences.getString(ALBUM + i,null);
            songNames.add(listItem);
            songPaths.add(itemPath);
            albumNames.add(albumName);
        }
        numSongs = size;
    }

    /*Method to save the playlist into sharedpreferences*/
    private  void savePlaylist(){
        SharedPreferences preferences = getSharedPreferences("SKATA",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        int size = songNames.size();

        /*No need to save, data is saved already*/
        if(preferences.contains(LIST_SIZE)){
            return;
        }

        editor.putInt(LIST_SIZE,size);

        for(int i = 0; i < size; i++){
            editor.putString(ITEM + i,songNames.get(i));
            editor.putString(PATH + i,songPaths.get(i));
            editor.putString(ALBUM + i,albumNames.get(i));
        }

        editor.apply();
    }


    /*This function is used to adjust the text size in the textviews
     *to match the screen size of a device better.
     * It also begins animating the text to move from right to left
     */
    private void adjustText(){

        /*Check size of screen to set textview font size*/
        Display display = getWindowManager().getDefaultDisplay();
        Point dimensions = new Point();
        display.getSize(dimensions);

        /*Now get each dimension*/
        int width = dimensions.x;
        int height = dimensions.y;

        /*Now get top textview and song textview*/
        TextView title;
        TextView currentSong;
        TextView currentSongTime;
        TextView songDuration;
        TextView albumNameView;
        if(orientation == Configuration.ORIENTATION_LANDSCAPE) {
            title = (TextView) findViewById(R.id.title);
            currentSong = (TextView) findViewById(R.id.curSong);
            currentSongTime = (TextView) findViewById(R.id.remaining_song_time);
            songDuration = (TextView) findViewById(R.id.song_duration);
            albumNameView = (TextView) findViewById(R.id.albumName);
        }
        else{
            currentSong = mediaPlayer.getSongView();
            title = mediaPlayer.getTitleView();
            currentSongTime = mediaPlayer.getCurrentSongTimeView();
            songDuration = mediaPlayer.getSongDurationView();
            albumNameView = mediaPlayer.getAlbumView();
        }

        /*Set text font sizes of TextViews*/
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX,(int) (0.05 * height));
        currentSong.setTextSize(TypedValue.COMPLEX_UNIT_PX,(int) (0.05 * height));
        currentSong.setSelected(true);
        currentSongTime.setTextSize(TypedValue.COMPLEX_UNIT_PX,(int) (0.03*height));
        songDuration.setTextSize(TypedValue.COMPLEX_UNIT_PX,(int) (0.03*height));
        albumNameView.setTextSize(TypedValue.COMPLEX_UNIT_PX,(int) (0.04*height));

        /*Find out text width in pixels for animation*/
        Rect bounds = new Rect();
        Paint textPaint = currentSong.getPaint();
        textPaint.getTextBounds(currentSong.getText().toString(),
                0,currentSong.getText().toString().length(),bounds);
        int textWidth = bounds.width();

        /*Now animate text in song textview*/
        /*Choose start based on screen orientation*/
        Animation animation;
        if(orientation == Configuration.ORIENTATION_LANDSCAPE) {
            animation = new TranslateAnimation(width / 2, -textWidth, 0, 0);
        }
        else{
            animation = new TranslateAnimation(width,-textWidth,0,0);
        }
        animation.setDuration(15000);
        animation.setRepeatMode(Animation.RESTART);
        animation.setRepeatCount(Animation.INFINITE);
        currentSong.setAnimation(animation);

    }

    /*Method that is called when play button is clicked, to change its icon*/
    public void play(View view){
        ImageButton button = (ImageButton) findViewById(R.id.Pause);
        if(isPlaying){
            button.setImageResource(R.mipmap.play);
            player.pause();
            if(isRegistered) {
                unregisterReceiver(noisyReceiver);
                isRegistered = false;
            }
        }
        else{
            if(player == null){
                String path = songPaths.get(curSongIndex);
                player = MediaPlayer.create(MainActivity.this,Uri.parse(path));
                player.setVolume(1-currentVolume,1-currentVolume);
                /*Get duration for seekbar*/
                int currentSongDuration = player.getDuration() / 1000;
                seek.setMax(currentSongDuration);
                TextView songView = (TextView) findViewById(R.id.curSong);
                songView.setText(songNames.get(curSongIndex), TextView.BufferType.NORMAL);
                TextView albumView = (TextView) findViewById(R.id.albumName);

                String albumText = "Album: " + albumNames.get(curSongIndex);
                albumView.setText(albumText,TextView.BufferType.NORMAL);
                this.adjustText();
                this.adjustSeekBarMovement();
                this.setDurationText();
            }
            button.setImageResource(R.mipmap.pause);
            if(this.requestFocus()) {
                registerReceiver(noisyReceiver,intentFilter);
                isRegistered = true;
                player.start();
            }
        }
        /*Change value to opposite*/
        isPlaying = !isPlaying;
    }

    /*Change pointer of seekbar once every second, as song is playing*/
    private void adjustSeekBarMovement(){

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(songList == null) {
                    /*This is for the movement of the seekbar*/
                    /*Time to go to next song*/
                    if (seek.getProgress() == seek.getMax()) {
                        if (!isRepeating) {
                            ImageButton button = (ImageButton) findViewById(R.id.next);
                            button.performClick();
                        } else {
                            player.seekTo(0);
                            seek.setProgress(0);
                            if(MainActivity.this.requestFocus()) {
                                registerReceiver(noisyReceiver,intentFilter);
                                isRegistered = true;
                                player.start();
                            }
                        }
                    } else if (player != null) {/*Update seeker position*/
                        int curPos = player.getCurrentPosition() / 1000;
                        seek.setProgress(curPos);
                    } else {/*No song is playing , dont let user change seeker*/
                        seek.setProgress(0);
                    }

                    if (player != null) {
                        /*This is for changing the text of textviews next to seekbar*/
                        int curPos = player.getCurrentPosition() / 1000;
                        int minutes = curPos / 60;
                        int seconds = curPos % 60;

                        TextView songTime;
                        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            songTime = (TextView) findViewById(R.id.remaining_song_time);
                        } else {
                            songTime = mediaPlayer.getCurrentSongTimeView();
                        }
                        String middle;
                        if (seconds < 10) {
                            middle = ":0";
                        } else {
                            middle = ":";
                        }
                        String textToSet = minutes + middle + seconds;
                        songTime.setText(textToSet, TextView.BufferType.NORMAL);
                    }
                }
                else{
                    if(player != null) {
                        SeekBar songSeek = songList.getSeekBar();
                        if(songSeek.getMax() == songSeek.getProgress()){
                            if(!isRepeating) {
                                MainActivity.this.simpleNext(null);
                            }
                            else{
                                MainActivity.this.simplePlay(null);
                            }
                            songSeek.setProgress(0);
                        }
                        else {
                            int curPos = player.getCurrentPosition() / 1000;
                            songSeek.setProgress(curPos);
                        }
                    }
                }
                handler.postDelayed(this,1000);
            }
        });
    }

    /*Stop current song*/
    public void stop(View view){

        /*If we have playback stop it*/
        if(player == null){
            return;
        }
        isPlaying = false;
        player.release();
        player = null;/*Change play button icon*/
        ImageButton button  = (ImageButton) findViewById(R.id.Pause);
        button.setImageResource(R.mipmap.play);
        seek.setProgress(0);
        /*Empty textview that shows current song*/
        TextView currentSong = (TextView) findViewById(R.id.curSong);
        currentSong.setText("", TextView.BufferType.NORMAL);

        /*Set textview of current minute and second to 0:00*/
        TextView curTime;
        if(orientation == Configuration.ORIENTATION_LANDSCAPE){
            curTime = (TextView) findViewById(R.id.remaining_song_time);
        }
        else{
            curTime = mediaPlayer.getCurrentSongTimeView();
        }
        String s = "0:00";
        curTime.setText(s, TextView.BufferType.NORMAL);

        am.abandonAudioFocus(afChangeListener);
    }

    /*Play next song in playlist*/
    public void nextSong(View view){

        /*Check if shuffle is active*/
        if(isShuffling){
            Random generator = new Random();
            curSongIndex = generator.nextInt(numSongs);
        }
        else{
            curSongIndex = (curSongIndex + 1) % numSongs;
        }

        if(player != null) {
            player.release();
            player = null;
        }

        isPlaying=false;
        ImageButton b = (ImageButton) findViewById(R.id.Pause);
        b.performClick();
    }

    /*Play previous song in playlist*/
    public void prevSong(View view){

        if(isShuffling){
            Random generator = new Random();
            curSongIndex = generator.nextInt(numSongs);
        }
        else {
            curSongIndex = ((curSongIndex - 1) + numSongs) % numSongs;
        }

        if(player!=null) {
            player.release();
            player = null;
        }

        isPlaying = false;
        ImageButton b = (ImageButton) findViewById(R.id.Pause);
        b.performClick();

     }

    /*button function to show the seekbar for volume control*/
    public void showVolumeControl(View view){
        final SeekBar volumeSeekBar;
        /*make seekbar visible*/
        if(orientation == Configuration.ORIENTATION_LANDSCAPE){
            volumeSeekBar = (SeekBar) findViewById(R.id.volumeControl);
        }
        else{
            volumeSeekBar = mediaPlayer.getVolumeSeekBar();
        }
        volumeSeekBar.setVisibility(View.VISIBLE);

        /*Set up a timer for the seekbar to disappear after a while*/
        timer = new Timer(false);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        volumeSeekBar.setVisibility(View.INVISIBLE);
                        timer = null;
                    }
                });
            }
        },SECONDS_TO_DISAPPEAR * 1000);
    }


    /*Method to change the volume of song playing*/

    private void setupVolumeListener(){

        SeekBar volumeSeekBar;
        /*Now change the volume seekbar progress*/
        if(orientation == Configuration.ORIENTATION_LANDSCAPE){
            volumeSeekBar = (SeekBar) findViewById(R.id.volumeControl);
        }
        else{
            volumeSeekBar = mediaPlayer.getVolumeSeekBar();
        }
        volumeSeekBar.setMax(MAX_VOLUME);
        volumeSeekBar.setProgress(MAX_VOLUME / 2);

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int position, boolean b) {
                /*Extend time the seekbar is visible*/
                    if(timer != null){
                        timer.cancel();
                        MainActivity.this.showVolumeControl(null);
                    }
                    currentVolume = (float)(Math.log(MAX_VOLUME - position)/Math.log(MAX_VOLUME));
                    if(player != null){
                        player.setVolume(1 - currentVolume,1 - currentVolume);
                    }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


    }

    /*Sets the textview text for the duration of the current song*/
    private void setDurationText(){

        /*Get correspoding textview*/
        TextView durationView;
        if(orientation == Configuration.ORIENTATION_LANDSCAPE) {
            durationView = (TextView) findViewById(R.id.song_duration);
        }
        else{
            durationView = mediaPlayer.getSongDurationView();
        }

        /*Get song duration in minutes and seconds*/
        int duration = player.getDuration() / 1000;
        int minutes = duration / 60;
        int seconds = duration % 60;

        /*Adjust string*/
        String middle;
        if(seconds < 10){
            middle = ":0";
        }
        else{
            middle = ":";
        }

        /*Set text*/
        String durationText = minutes + middle + seconds;
        durationView.setText(durationText, TextView.BufferType.NORMAL);
    }

    /*Method to repeat song*/
    public void repeat(View view){
        ImageButton bt ;
        if(orientation == Configuration.ORIENTATION_LANDSCAPE ){
            bt = (ImageButton) findViewById(R.id.loop_song);
        }
        else{
            bt = mediaPlayer.getVolumeButton();
        }
        if(isRepeating){
            bt.setImageResource(R.mipmap.repeat_not_active);
        }
        else{
            bt.setImageResource(R.mipmap.repeat_active);
        }
        isRepeating = !isRepeating;
    }

    /*method to show list view with songs*/
    public void showPlaylist(View view){

        /*If in landscape do nothing*/
        if(orientation == Configuration.ORIENTATION_PORTRAIT) {
            /*Replace with listview*/
            songList = new ListViewFragment();

            /*Get progress of volume seekbar for later*/
            int volumeProgress = mediaPlayer.getVolumeSeekBar().getProgress();

            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction transaction = fm.beginTransaction();

            /*Replace and add to back stack*/
            transaction.replace(R.id.fragment_container, songList);

            transaction.commit();
            fm.executePendingTransactions();

            /*Check if song is playing*/
            if(player != null) {
                /*Set linearlayout visible*/
                LinearLayout layout = songList.getButtonsBar();
                layout.setVisibility(View.VISIBLE);
                /*Set text of name view*/
                TextView txt = songList.getNameTextView();
                txt.setText(songNames.get(curSongIndex));
                SeekBar songSeek = songList.getSeekBar();
                songSeek.setMax(player.getDuration() / 1000);
                songList.getVolumeSeekbar().setProgress(volumeProgress);
            }

            mediaPlayer = null;
            this.waitForSong();
        }

    }

    /*The following methods are used for the buttons
      in the list view where the user can reselect a song while
      already listening to another on. They basically are a simpler
       version of the play,next and previous song methods.
     */

    /*Play song*/
    public void simplePlay(View view){
        ImageButton button = (ImageButton) findViewById(R.id.simple_play);
        if(isPlaying){
            button.setImageResource(R.mipmap.play);
            player.pause();
            if(isRegistered) {
                unregisterReceiver(noisyReceiver);
                isRegistered = false;
            }
        }
        else{
            button.setImageResource(R.mipmap.pause);
            if(this.requestFocus()) {
                registerReceiver(noisyReceiver,intentFilter);
                isRegistered = true;
                player.start();
            }
        }
        isPlaying = !isPlaying;
    }

    /*Play previous song*/
    public void simplePrevious(View view){

        /*Check if shuffle active*/
        if(isShuffling){
            Random generator = new Random();
            curSongIndex = generator.nextInt(numSongs);
        }
        else{
            curSongIndex = ((curSongIndex - 1) + numSongs) % numSongs;
        }

        if(player!=null) {
            player.release();
            player = null;
        }
        isPlaying = true;

        player = MediaPlayer.create(MainActivity.this,Uri.parse(songPaths.get(curSongIndex)));
        player.setVolume(currentVolume,currentVolume);
        if(this.requestFocus()) {
            registerReceiver(noisyReceiver,intentFilter);
            isRegistered = true;
            player.start();
        }

        /*Update text of textview*/
        songList.getNameTextView().setText(songNames.get(curSongIndex), TextView.BufferType.NORMAL);

    }

    /*Play next song*/
    public void simpleNext(View view){

        if(isShuffling){
            Random generator = new Random();
            curSongIndex = generator.nextInt(numSongs);
        }
        else{
            curSongIndex = ((curSongIndex - 1) + numSongs) % numSongs;
        }

        if(player!=null) {
            player.release();
            player = null;
        }
        isPlaying = true;

        player = MediaPlayer.create(MainActivity.this,Uri.parse(songPaths.get(curSongIndex)));
        player.setVolume(currentVolume,currentVolume);
        if(this.requestFocus()) {
            registerReceiver(noisyReceiver,intentFilter);
            isRegistered = true;
            player.start();
        }

        /*Update text of textview*/
        songList.getNameTextView().setText(songNames.get(curSongIndex), TextView.BufferType.NORMAL);
    }

    /*Method for shuffle button*/
    public void shuffle(View view){

        ImageButton bt ;
        if(orientation == Configuration.ORIENTATION_LANDSCAPE ){
            bt = (ImageButton) findViewById(R.id.shuffle);
        }
        else{
            bt = mediaPlayer.getShuffleButton();
        }

        if (isShuffling){
            bt.setImageResource(R.mipmap.shuffle_not_active);
        }
        else{
            bt.setImageResource(R.mipmap.shuffle_active);
        }

        isShuffling = !isShuffling;

    }

    /*OnDestroy function Override*/
    @Override
    public void onDestroy(){
        super.onDestroy();
        /*Stop method tracing*/
        if (player != null) {
            player.release();
            player = null;
        }
        if(isFinishing()) {
            /*delete preferences*/
            getSharedPreferences("SKATA",Context.MODE_PRIVATE).edit().clear().apply();
        }
        if(isRegistered) {
            unregisterReceiver(noisyReceiver);
            isRegistered = false;
        }
    }

    /*Save data when destroyed*/
    @Override
    public void onSaveInstanceState(Bundle save){
        /*Save current state of media player*/
        save.putInt(CURRENT_SONG,curSongIndex);
        save.putBoolean(ISPLAYING,isPlaying);
        save.putBoolean(ISPLAYERNULL,player == null);
        save.putFloat(VOLUME,currentVolume);
        save.putBoolean(IS_LOOPING,isRepeating);
        save.putBoolean(IS_SHUFFLING,isShuffling);
        /*Remove Fragment if screen is in portrait mode*/
        if(orientation == Configuration.ORIENTATION_PORTRAIT) {
            SeekBar volumeBar = (SeekBar) findViewById(R.id.volumeControl);
            if(volumeBar != null) {
                save.putInt(VOLUMER_BAR_PROGRESS, volumeBar.getProgress());
            }
            else{
                if(player == null) {
                    save.putInt(VOLUMER_BAR_PROGRESS, MAX_VOLUME / 2);
                }
                else{/*Extended list fragment case*/
                    volumeBar = songList.getVolumeSeekbar();
                    save.putInt(VOLUMER_BAR_PROGRESS,volumeBar.getProgress());
                }
            }
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            /*Check which is the current active fragment*/
            if(mediaPlayer != null) {
                save.putInt(CURRENT_SEEKBAR_POS, seek.getProgress());
                ft.remove(mediaPlayer);
            }
            else{
                if(player == null) {
                    save.putInt(CURRENT_SEEKBAR_POS, 0);
                }
                else{
                    save.putInt(CURRENT_SEEKBAR_POS,songList.getSeekBar().getProgress());
                }
                ft.remove(songList);
            }
            ft.commit();
        }
        else{
            save.putInt(CURRENT_SEEKBAR_POS, seek.getProgress());
            SeekBar volumeBar = (SeekBar) findViewById(R.id.volumeControl);
            save.putInt(VOLUMER_BAR_PROGRESS,volumeBar.getProgress());
        }

        /*Save playlist*/
        this.savePlaylist();

        /*Call super class same method*/
        super.onSaveInstanceState(save);
    }

    /*Inner class to detect swipe gestures to right and left
      for going to next or previous song
     */

    class MyGestureDetector extends android.view.GestureDetector.SimpleOnGestureListener{

        @Override
        public boolean onFling(MotionEvent e1,MotionEvent e2, float velocityX, float velocityY){

            if(Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH){/*Too much Y distance between swipe start and end*/
                return false;
            }
            /*Right to left swipe*/
            if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY){
                if(MainActivity.this.orientation == Configuration.ORIENTATION_LANDSCAPE
                        || MainActivity.this.getSupportFragmentManager().findFragmentById(R.id.player) != null) {
                    MainActivity.this.nextSong(null);
                    return true;
                }
            }
            /*Left to right swipe*/
            else if(e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY){
                if(MainActivity.this.orientation == Configuration.ORIENTATION_LANDSCAPE
                        || MainActivity.this.getSupportFragmentManager().findFragmentById(R.id.player) != null) {
                    MainActivity.this.prevSong(null);
                    return true;
                }
            }
            return false;
        }
    }

    private class NoisyAudioStreamReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context,Intent intent){
            if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())){
                if(player != null && isPlaying){
                    if(MainActivity.this.orientation == Configuration.ORIENTATION_LANDSCAPE
                            || MainActivity.this.getSupportFragmentManager().findFragmentById(R.id.player) != null) {
                        MainActivity.this.play(null);
                    }
                    else{
                        MainActivity.this.simplePlay(null);
                    }
                }
            }
        }
    }

}
