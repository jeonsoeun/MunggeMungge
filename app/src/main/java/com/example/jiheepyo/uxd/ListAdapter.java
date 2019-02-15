package com.example.jiheepyo.uxd;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by JeonSoEun on 2016-11-30.
 */

public class ListAdapter extends ArrayAdapter<Place>{
    ArrayList<Place> items;
    Context context;
    public ListAdapter(Context context, int resource, ArrayList<Place> objects) {
        super(context, resource, objects);
        items  = objects;
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if(v==null){
            LayoutInflater vi = (LayoutInflater)context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.list_row,null);
        }
        Place p = items.get(position);
        if(p != null){
            TextView tn = (TextView)v.findViewById(R.id.name);
            ImageView imageView = (ImageView)v.findViewById(R.id.image);
            if(imageView != null){
                //임시 이미지.
                if(p.m_type ==0){
                    imageView.setImageResource(R.drawable.marker_booth);
                }else if(p.m_type == 2){
                    imageView.setImageResource(R.drawable.marker_cafe);
                }else if(p.m_type == 1){
                    imageView.setImageResource(R.drawable.marker_pc);
                }
                else if(p.m_type == 3){
                    imageView.setImageResource(R.drawable.marker_outside);
                }
            }
            if(tn != null){
                tn.setText(p.getName());
            }
        }
        return v;
    }
}
