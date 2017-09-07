package com.example.shalantor.mediaplayer;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ConfigurationHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

public class ListViewFragment extends Fragment {

    OnSongSelectedListener mCallback;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle save){
        return inflater.inflate(R.layout.list_view_fragment,container,false);
    }

    public ListView getListView(){
        return (ListView) getActivity().findViewById(R.id.playlist);
    }

    public interface OnSongSelectedListener{
        public void  onSongSelected(int position);
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);

        /*instantiate mcallback method*/
        mCallback = (OnSongSelectedListener) context;
    }

    /*Wait for the user to select a song*/
    public void waitForSongSelect(){
        ListView list = (ListView) getActivity().findViewById(R.id.playlist);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                mCallback.onSongSelected(position);
            }
        });

    }

    /*Getter for textview with song name*/
    public TextView getNameTextView(){
        return (TextView) getActivity().findViewById(R.id.list_show_song);
    }

    /*Getter for linearlayout with buttons*/
    public LinearLayout getButtonsBar(){
        return (LinearLayout) getActivity().findViewById(R.id.button_bar);
    }

    /*Get seekbar*/
    public SeekBar getSeekBar(){
        return (SeekBar) getActivity().findViewById(R.id.list_player_seekbar);
    }

    /*Get volume seekbar*/
    public SeekBar getVolumeSeekbar(){
        return (SeekBar) getActivity().findViewById(R.id.list_volume_control);
    }
}
