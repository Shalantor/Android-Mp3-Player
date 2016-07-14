package com.example.shalantor.mediaplayer;

import android.graphics.Point;
import android.media.AudioManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private int width;
    private int height;/*TODO:think about necessity of keeping height*/
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*Check if app is being restarted*/
        if(getIntent() == null){
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }
        this.adjustTextSize();
    }

    /*This function is used to adjust the text size in the textviews
     *to match the screen size of a device better.
     */
    private void adjustTextSize(){

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

        title.setTextSize(TypedValue.COMPLEX_UNIT_PX,(int) (0.05 * height));
        currentSong.setTextSize(TypedValue.COMPLEX_UNIT_PX,(int) (0.05 * height));

    }

    /*Method that is called when play button is clicked*/
    public void play(View view){
        /*Change value to opposite*/
        isPlaying = !isPlaying;
        ImageButton button = (ImageButton) findViewById(R.id.Pause);
        if(isPlaying){
            button.setBackgroundResource(R.mipmap.pause);
        }
        else{
            button.setBackgroundResource(R.mipmap.play);
        }
    }

}
