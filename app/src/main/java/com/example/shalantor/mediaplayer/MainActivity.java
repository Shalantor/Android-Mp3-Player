package com.example.shalantor.mediaplayer;

import android.content.Context;
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

public class MainActivity extends AppCompatActivity {

    private MediaPlayer player;
    private int width;
    private int height;/*TODO:think about necessity of keeping height*/
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*Check if app is being restarted*/
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        /*Instantiate seekbar variable*/
        seek = (SeekBar) findViewById(R.id.seekbar);
        /*Get mp3 files on sd card*/
        this.createPlaylist();
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
                if(isPlaying){
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
                /*Start new song*/
                player.start();
                MainActivity.this.adjustText();
            }
        });
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
        TextView title = (TextView) findViewById(R.id.title);
        TextView currentSong = (TextView) findViewById(R.id.curSong);

        /*Set sizes*/
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX,(int) (0.05 * height));
        currentSong.setTextSize(TypedValue.COMPLEX_UNIT_PX,(int) (0.05 * height));
        currentSong.setSelected(true);

        /*Find out text width in pixels*/
        Rect bounds = new Rect();
        Paint textPaint = currentSong.getPaint();
        textPaint.getTextBounds(currentSong.getText().toString(),
                0,currentSong.getText().toString().length(),bounds);
        int textWidth = bounds.width();
        Log.d("HELLO","text width is: " + textWidth);

        /*Now animate text in song textview*/
        Animation animation = new TranslateAnimation(width,-textWidth,0,0);
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
                if (seek.getProgress() == seek.getMax()) {
                    ImageButton button = (ImageButton) findViewById(R.id.next);
                    button.performClick();
                }
                else if(player != null){
                    int curPos = player.getCurrentPosition() / 1000;
                    seek.setProgress(curPos);
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
        player = null;
        ImageButton button  = (ImageButton) findViewById(R.id.Pause);
        button.setImageResource(R.mipmap.play);
        seek.setProgress(0);
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
        save.putInt(CURRENT_SEEKBAR_POS,seek.getProgress());
        save.putBoolean(ISPLAYING,isPlaying);
        save.putBoolean(ISPLAYERNULL,player == null);
        /*Call super class same method*/
        super.onSaveInstanceState(save);
    }

}
