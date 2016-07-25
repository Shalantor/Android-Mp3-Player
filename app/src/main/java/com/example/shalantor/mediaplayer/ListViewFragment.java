package com.example.shalantor.mediaplayer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class ListViewFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle save){
        return inflater.inflate(R.layout.list_view_fragment,container,false);
    }

    public ListView getListView(){
        return (ListView) getActivity().findViewById(R.id.playlist);
    }

    public interface OnSongselectedListener{
        void onSongSelected(int position);
    }
}
