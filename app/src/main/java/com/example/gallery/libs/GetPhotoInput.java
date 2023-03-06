package com.example.gallery.libs;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class GetPhotoInput {
    public  int First;
    public  String After;
    public  String GroupName;
    public  String AssetType;

    @Nullable
    public  long FromTime;
    @Nullable
    public  long ToTime;
    public List<String> MimeTypes;
    public Set<String> Include;
}
