package com.example.shalantor.mediaplayer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import org.w3c.dom.Text;

public class MediaPlayerFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle save){
        return inflater.inflate(R.layout.media_player_fragment,container,false);
    }

    public TextView getSongView(){
        return (TextView) getActivity().findViewById(R.id.curSong);
    }

    public TextView getTitleView(){
        return (TextView) getActivity().findViewById(R.id.title);
    }

    public SeekBar getSeekBar(){
        return (SeekBar) getActivity().findViewById(R.id.seekbar);
    }

}
