package com.example.gallery.ui.main;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallery.R;
import com.example.gallery.libs.domain.Asset;
import com.example.gallery.libs.domain.GetPhotoOutput;

import java.io.IOException;


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
        return  new ViewHolder(v,parent.getContext());
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            holder.setData(this.photos.assets.get(position));
        } catch (IOException e) {
            //throw new RuntimeException(e);
            Log.println(Log.DEBUG,"Error",e.toString());
        }
    }


    @Override
    public int getItemCount() {
        return this.photos.assets.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {


        ImageView imageView=null;
        BitmapFactory.Options options;
        Context context;
        public ViewHolder(@NonNull View itemView,Context context) {
            super(itemView);
            imageView = itemView.findViewById(R.id.list_item_imageview);
            options = new BitmapFactory.Options();
            this.context = context;
            //options.inSampleSize=true
        }


        public void  setData(Asset asset) throws IOException {
            Bitmap thumbBitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                thumbBitmap = this.context.getContentResolver().loadThumbnail(asset.image.imageUri, new Size(300,300), null);
            } else {
                thumbBitmap = MediaStore.Images.Thumbnails.getThumbnail(this.context.getContentResolver(),
                        asset.image.imageId, MediaStore.Images.Thumbnails.MINI_KIND, null);
            }
            this.imageView.setImageBitmap(thumbBitmap);
        }
    }
}
