package com.example.shalantor.mediaplayer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ListViewFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle save){
        return inflater.inflate(R.layout.list_view_fragment,container,false);
    }
}
