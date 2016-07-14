package com.example.shalantor.mediaplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

/**
 * Created by shalantor on 13/7/2016.
 */
public class RemoteControlReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent){

        if( Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())){
            KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if(KeyEvent.KEYCODE_MEDIA_PLAY == event.getKeyCode()){
                Log.d("TEMP","temp");
            }
        }

    }
}
