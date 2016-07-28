package com.example.shalantor.mediaplayer;

import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ListViewCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Handler;

public class MainActivity extends AppCompatActivity
        implements ListViewFragment.OnSongSelectedListener{

    private MediaPlayer player;
    private int width;
    private int height;
    private boolean isPlaying = false;/*TODO:Mediaplayer has method isPlaying()*/
    private ArrayList<String> songNames = new ArrayList<>();
    private ArrayList<String> songPaths = new ArrayList<>();
    private final static String MEDIA_PATH = new String("/sdcard/");
    private int currentSongDuration;
    private int numSongs;
    private int curSongIndex;
    private android.os.Handler handler = new android.os.Handler();
    private SeekBar seek ;
    private ArrayAdapter<String> adapter;
    static final String CURRENT_SONG = "curSong";
    static final String CURRENT_SEEKBAR_POS = "seekPos";
    static final String ISPLAYING = "isPlaying";
    static final String ISPLAYERNULL = "isPaused";
    private int orientation;
    private ListView list;
    private MediaPlayerFragment mediaPlayer = null;
    private ListViewFragment songList = null;
    private Bundle save;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        orientation = this.getResources().getConfiguration().orientation;
        /*LandScape orientation*/
        this.createPlaylist();
        if(orientation == Configuration.ORIENTATION_LANDSCAPE){
            setContentView(R.layout.activity_main);
            seek = (SeekBar) findViewById(R.id.seekbar);
            this.setupPlayer(savedInstanceState);
            this.addListViewListener();
        }
        else{
            setContentView(R.layout.fragment_container_portrait);
            /*Media player is being recreated*/
            if(savedInstanceState != null) {
                boolean isNull = savedInstanceState.getBoolean(ISPLAYERNULL);
                if(isNull){
                    songList = new ListViewFragment();
                    getSupportFragmentManager().beginTransaction().add(R.id.fragment_container,songList).commit();
                }
                else {
                    mediaPlayer = new MediaPlayerFragment();
                    getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, mediaPlayer).commit();
                    save = savedInstanceState;
                }
            }
            else{/*Create listView for user to choose a song*/
                songList = new ListViewFragment();
                getSupportFragmentManager().beginTransaction().add(R.id.fragment_container,songList).commit();
            }
        }
    }

    @Override
    protected void onStart(){
        super.onStart();
        if(orientation == Configuration.ORIENTATION_PORTRAIT){
            if(save != null) {/*Show list of songs*/
                boolean isNull = save.getBoolean(ISPLAYERNULL);
                if(isNull){
                    this.waitForSong();
                }
                else {
                    seek = mediaPlayer.getSeekBar();
                    this.setupPlayer(save);
                }
            }
            else{/*Show list of songs*/
                this.waitForSong();
            }
        }
    }

    /*Implement method of interface for fragment communication*/
    @Override
    public void onSongSelected(int position){
        /*Replace with new media player fragment and start song selected*/
        mediaPlayer = new MediaPlayerFragment();

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();

        /*Replace and add to back stack*/
        transaction.replace(R.id.fragment_container,mediaPlayer);

        transaction.commit();

        /*Change songList fragment to null since it isnt visible now*/
        songList = null;
        fm.executePendingTransactions();

        /*Strt playing selected song*/
        curSongIndex = position;
        this.setupPlayer(null);
        this.play(null);
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
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        if(seek == null){
            seek = mediaPlayer.getSeekBar();
        }
        /*Check if being recreated*/
        if(savedInstanceState != null){
            curSongIndex = savedInstanceState.getInt(CURRENT_SONG);
            int seekPos = savedInstanceState.getInt(CURRENT_SEEKBAR_POS);
            isPlaying = savedInstanceState.getBoolean(ISPLAYING);
            boolean isPlayerNull = savedInstanceState.getBoolean(ISPLAYERNULL);
            if(!isPlayerNull){
                player = MediaPlayer.create(MainActivity.this,
                        Uri.parse(songPaths.get(curSongIndex)));
                int seekRange = player.getDuration() / 1000;
                seek.setMax(seekRange);
                seek.setProgress(seekPos);
                this.adjustSeekBarMovement();
                player.seekTo(seekPos * 1000);
                this.setDurationText();
                if(isPlaying){
                    /*Set text of animated textview*/
                    TextView curSong;
                    if(orientation == Configuration.ORIENTATION_LANDSCAPE){
                        curSong = (TextView) findViewById(R.id.curSong);
                    }
                    else{
                        curSong = mediaPlayer.getSongView();
                    }
                    curSong.setText(songNames.get(curSongIndex), TextView.BufferType.NORMAL);
                    /*Change image of button to pause*/
                    ImageButton bt = (ImageButton) findViewById(R.id.Pause);
                    bt.setImageResource(R.mipmap.pause);
                    player.start();
                }
            }
        }
        /*Adjust text size and movement in song textview*/
        this.adjustText();

        /*Set volume*/
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,20,0);

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
        list = (ListView) findViewById(R.id.playlist);
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
                /*Start new song*/
                player.start();
                MainActivity.this.adjustText();
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


                    /*String albumName = cursor.getString(cursor
                            .getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    int albumId = cursor
                            .getInt(cursor
                                    .getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));*/
                    songNames.add(songName);
                    songPaths.add(path);
                }while(cursor.moveToNext());
            }
        }
        numSongs = songNames.size();

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
        width = dimensions.x;
        height = dimensions.y;

        /*Now get top textview and song textview*/
        TextView title;
        TextView currentSong;
        TextView currentSongTime;
        TextView songDuration;
        if(orientation == Configuration.ORIENTATION_LANDSCAPE) {
            title = (TextView) findViewById(R.id.title);
            currentSong = (TextView) findViewById(R.id.curSong);
            currentSongTime = (TextView) findViewById(R.id.remaining_song_time);
            songDuration = (TextView) findViewById(R.id.song_duration);
        }
        else{
            currentSong = mediaPlayer.getSongView();
            title = mediaPlayer.getTitleView();
            currentSongTime = mediaPlayer.getCurrentSongTimeView();
            songDuration = mediaPlayer.getSongDurationView();
        }

        /*Set text font sizes of TextViews*/
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX,(int) (0.05 * height));
        currentSong.setTextSize(TypedValue.COMPLEX_UNIT_PX,(int) (0.05 * height));
        currentSong.setSelected(true);
        currentSongTime.setTextSize(TypedValue.COMPLEX_UNIT_PX,(int) (0.03*height));
        songDuration.setTextSize(TypedValue.COMPLEX_UNIT_PX,(int) (0.03*height));

        /*Find out text width in pixels*/
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
        }
        else{
            if(player == null){
                String path = songPaths.get(curSongIndex);
                player = MediaPlayer.create(MainActivity.this,Uri.parse(path));
                /*Get duration for seekbar*/
                currentSongDuration = player.getDuration() / 1000;
                seek.setMax(currentSongDuration);
                TextView songView = (TextView) findViewById(R.id.curSong);
                songView.setText(songNames.get(curSongIndex), TextView.BufferType.NORMAL);
                this.adjustText();
                this.adjustSeekBarMovement();
                this.setDurationText();
            }
            button.setImageResource(R.mipmap.pause);
            player.start();
        }
        /*Change value to opposite*/
        isPlaying = !isPlaying;
    }

    /*Change pointer of seekbar once every second, as song is playing*/
    private void adjustSeekBarMovement(){

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                /*This is for the movement of the seekbar*/
                /*Time to go to next song*/
                if (seek.getProgress() == seek.getMax()) {
                    ImageButton button = (ImageButton) findViewById(R.id.next);
                    button.performClick();
                }
                else if(player != null){/*Update seeker position*/
                    int curPos = player.getCurrentPosition() / 1000;
                    seek.setProgress(curPos);
                }
                else{/*No song is playing , dont let user change seeker*/
                    seek.setProgress(0);
                }

                if(player != null) {
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
                    if(seconds < 10){
                        middle = ":0";
                    }
                    else{
                        middle = ":";
                    }
                    String textToSet = minutes + middle + seconds;
                    songTime.setText(textToSet, TextView.BufferType.NORMAL);
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
    }

    /*Play next song in playlist*/
    public void nextSong(View view){

        curSongIndex = (curSongIndex + 1) % numSongs;

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
        curSongIndex = ((curSongIndex - 1) + numSongs) % numSongs;

        if(player!=null) {
            player.release();
            player = null;
        }

        isPlaying = false;
        ImageButton b = (ImageButton) findViewById(R.id.Pause);
        b.performClick();

     }


    /*Function for the button to change the volume of song playing*/
    public void changeVolume(View view){
        seek.setVisibility(View.INVISIBLE);
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


    /*OnDestroy function Override*/
    @Override
    public void onDestroy(){
        super.onDestroy();
        /*Stop method tracing*/
        if(player != null){
            player.release();
            player = null;
        }
    }

    /*Save data when destroyed*/
    @Override
    public void onSaveInstanceState(Bundle save){
        /*Save current state of media player*/
        save.putInt(CURRENT_SONG,curSongIndex);
        save.putBoolean(ISPLAYING,isPlaying);
        save.putBoolean(ISPLAYERNULL,player == null);
        /*Remove Fragment if screen is in portrait mode*/
        if(orientation == Configuration.ORIENTATION_PORTRAIT) {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            /*Check which is the current active fragment*/
            if(mediaPlayer != null) {
                save.putInt(CURRENT_SEEKBAR_POS, seek.getProgress());
                ft.remove(mediaPlayer);
            }
            else{
                save.putInt(CURRENT_SEEKBAR_POS,0);
                ft.remove(songList);
            }
            ft.commit();
        }
        else{
            save.putInt(CURRENT_SEEKBAR_POS, seek.getProgress());
        }
        /*Call super class same method*/
        super.onSaveInstanceState(save);
    }

}
