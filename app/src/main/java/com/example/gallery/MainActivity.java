package com.example.gallery;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.example.gallery.libs.GalleryAlbumProvider;
import com.example.gallery.libs.domain.GetPhotoInput;
import com.example.gallery.libs.domain.GetPhotoOutput;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.gallery.ui.main.SectionsPagerAdapter;
import com.example.gallery.databinding.ActivityMainBinding;

import java.util.List;
import java.util.Map;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = binding.viewPager;
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = binding.tabs;
        tabs.setupWithViewPager(viewPager);
        FloatingActionButton fab = binding.fab;

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        loadPhotos();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        //EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
        //Map<String,Integer> albums = GalleryAlbumProvider.getAlbums(this);
        //Log.println(Log.DEBUG,"wweng", Integer.toString(albums.size()));
    }
    private void loadAlbums() {
        Map<String,Integer> albums = null;// GalleryAlbumProvider.getAlbums(this);
        if(albums!=null) {
            Log.println(Log.DEBUG, "wweng", Integer.toString(albums.size()));
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
            ) {
                 Log.println(Log.INFO,"wweng","need rational");
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        45
                );

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
           albums = GalleryAlbumProvider.getAlbums(this);
           Log.println(Log.DEBUG,"wweng", Integer.toString(albums.size()));
        }
    }

    private  void loadPhotos(){
        GetPhotoInput input = new GetPhotoInput();
        input.AssetType="All";
        GalleryAlbumProvider.getPhotos(input, this, new GalleryAlbumProvider.GetPhotoCallback() {
            @Override
            public void onResult(GetPhotoOutput result) {
                Log.println(Log.DEBUG,"wweng","result");
            }
        });
    }
}