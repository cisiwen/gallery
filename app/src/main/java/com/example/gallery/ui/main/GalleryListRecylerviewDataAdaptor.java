package com.example.gallery.ui.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallery.R;
import com.example.gallery.libs.domain.Asset;
import com.example.gallery.libs.domain.GetPhotoOutput;


public class GalleryListRecylerviewDataAdaptor extends RecyclerView.Adapter<GalleryListRecylerviewDataAdaptor.ViewHolder> {

    private static final String TAG = "GalleryListRecylerviewDataAdaptor";
    private  GetPhotoOutput photos;

    public  GalleryListRecylerviewDataAdaptor(GetPhotoOutput photos) {
        this.photos = photos;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.photo_list_item, parent, false);
        return  new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.setData(this.photos.assets.get(position));
    }


    @Override
    public int getItemCount() {
        return this.photos.assets.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void  setData(Asset asset){

        }
    }
}
