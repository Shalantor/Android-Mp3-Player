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
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer player;
    private int width;
    private int height;/*TODO:think about necessity of keeping height*/
    private boolean isPlaying = false;/*TODO:Mediaplayer has method isPlaying()*/
    private ArrayList<HashMap<String, String>> songs = new ArrayList<>();
    private final static String MEDIA_PATH = new String("/sdcard/");
    private int currentSongDuration;
    private int numSongs;
    private int curSongIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*Check if app is being restarted*/
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        this.adjustText();
        /*Set volume*/
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,20,0);
        /*Get mp3 files on sd card*/
        this.createPlaylist();
        /*Now update listview to show elements*/
    }

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
                    int albumId = cursor
                            .getInt(cursor
                                    .getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                    HashMap<String, String> song = new HashMap<>();
                    song.put("songTitle", albumName + " " + songName + "___" + albumId);
                    song.put("songPath", path);
                    songs.add(song);
                }while(cursor.moveToNext());
            }
        }
        numSongs = songs.size();
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
                String path = songs.get(curSongIndex).get("songPath");
                player = MediaPlayer.create(MainActivity.this,Uri.parse(path));
                /*Get duration for seekbar*/
                currentSongDuration = player.getDuration() / 1000;
                SeekBar seek = (SeekBar) findViewById(R.id.seekbar);
                seek.setMax(currentSongDuration);
                TextView songView = (TextView) findViewById(R.id.curSong);
                /*TODO:Change animation duration of text because text changes*/
                songView.setText(songs.get(curSongIndex).get("songTitle"), TextView.BufferType.NORMAL);
                this.adjustText();
            }
            button.setImageResource(R.mipmap.pause);
            player.start();
        }
        /*Change value to opposite*/
        isPlaying = !isPlaying;
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

}
