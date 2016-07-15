package com.example.shalantor.mediaplayer;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer player;
    private int width;
    private int height;/*TODO:think about necessity of keeping height*/
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*Check if app is being restarted*/
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        this.adjustText();
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,20,0);
        player = MediaPlayer.create(MainActivity.this,R.raw.beyblade);
        player.start();
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
        /*Change value to opposite*/
        isPlaying = !isPlaying;
        ImageButton button = (ImageButton) findViewById(R.id.Pause);
        if(isPlaying){
            button.setImageResource(R.mipmap.pause);
            player.pause();
        }
        else{
            button.setImageResource(R.mipmap.play);
            player.start();
        }
        player.start();
    }

}
