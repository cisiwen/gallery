package com.example.gallery.ui.main;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.gallery.R;
import com.example.gallery.databinding.FragmentMainBinding;
import com.example.gallery.libs.GalleryAlbumProvider;
import com.example.gallery.libs.domain.GetPhotoInput;
import com.example.gallery.libs.domain.GetPhotoOutput;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;
    private FragmentMainBinding binding;

    private RecyclerView recyclerView;
    private GalleryListRecylerviewDataAdaptor dataAdaptor;
    int index = 1;
    public static PlaceholderFragment newInstance(int index) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = new ViewModelProvider(this).get(PageViewModel.class);

        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
        if(index==1) {
            loadPhotos(this.getContext());
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        binding = FragmentMainBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        recyclerView = (RecyclerView) root.findViewById(R.id.listview);
        //RecyclerView.LayoutManager mLayoutManager = new StaggeredGridLayoutManager(,StaggeredGridLayoutManager.VERTICAL);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this.getContext(),4);
        recyclerView.setLayoutManager(mLayoutManager);
        return root;
    }

    private  void loadPhotos(Context context){
        GetPhotoInput input = new GetPhotoInput();
        input.AssetType="All";
        input.First = 5000;
        GalleryAlbumProvider.getPhotos(input, context, new GalleryAlbumProvider.GetPhotoCallback() {
            @Override
            public void onResult(GetPhotoOutput result) {

                dataAdaptor = new GalleryListRecylerviewDataAdaptor(result);
                recyclerView.setAdapter(dataAdaptor);
            }
        });
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}