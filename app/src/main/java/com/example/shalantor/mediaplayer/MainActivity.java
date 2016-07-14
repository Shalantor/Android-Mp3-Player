package com.example.shalantor.mediaplayer;

import android.graphics.Point;
import android.media.AudioManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Display;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private int width;
    private int height;/*TODO:think about necessity of keeping height*/

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

}
