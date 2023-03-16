package com.example.gallery.libs;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.gallery.libs.domain.Asset;
import com.example.gallery.libs.domain.GetPhotoInput;
import com.example.gallery.libs.domain.GetPhotoOutput;
import com.example.gallery.libs.domain.Image;
import com.example.gallery.libs.domain.Location;
import com.example.gallery.libs.domain.PageInfo;
import com.example.gallery.libs.utility.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class GalleryAlbumProvider {

    private static final String ASSET_TYPE_ALL = "All";
    private  static  final  String ASSET_TYPE_VIDEOS="VIDEOS";
    private  static  final  String ASSET_TYPE_PHOTOS="PHOTOS";
    private static final String SELECTION_BUCKET = Images.Media.BUCKET_DISPLAY_NAME + " = ?";


    private static final String INCLUDE_FILENAME = "filename";
    private static final String INCLUDE_FILE_SIZE = "fileSize";
    private static final String INCLUDE_FILE_EXTENSION = "fileExtension";
    private static final String INCLUDE_LOCATION = "location";
    private static final String INCLUDE_IMAGE_SIZE = "imageSize";
    private static final String INCLUDE_PLAYABLE_DURATION = "playableDuration";
    private static final String INCLUDE_ORIENTATION = "orientation";
    public static Map<String, Integer> getAlbums(Context context) {
        String assetType = ASSET_TYPE_ALL;
        StringBuilder selection = new StringBuilder("1");
        List<String> selectionArgs = new ArrayList<>();

        if (assetType.equals(ASSET_TYPE_PHOTOS)) {
            selection.append(" AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + " = "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE);
        } else if (assetType.equals(ASSET_TYPE_VIDEOS)) {
            selection.append(" AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + " = "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO);
        } else if (assetType.equals(ASSET_TYPE_ALL)) {
            selection.append(" AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + " IN ("
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO + ","
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + ")");
        }


        final String[] projection = {MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME};

        try {
            Cursor media = context.getContentResolver().query(
                    MediaStore.Files.getContentUri("external"),
                    projection,
                    selection.toString(),
                    selectionArgs.toArray(new String[selectionArgs.size()]),
                    null);


            try {
                if (media.moveToFirst()) {
                    Map<String, Integer> albums = new HashMap<>();
                    do {
                        int column = media.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME);
                        if (column < 0) {
                            throw new IndexOutOfBoundsException();
                        }
                        String albumName = media.getString(column);
                        if (albumName != null) {
                            Integer albumCount = albums.get(albumName);
                            if (albumCount == null) {
                                albums.put(albumName, 1);
                            } else {
                                albums.put(albumName, albumCount + 1);
                            }
                        }
                    } while (media.moveToNext());

                    return albums;

                }
            } catch (Exception ex) {
                Log.e(GalleryAlbumProvider.class.getName(), "Error get album", ex);
            } finally {
                media.close();

            }

        } catch (Exception e) {
            Log.e(GalleryAlbumProvider.class.getName(), "Error get album", e);
        }
        return null;
    }
    public static List<String> getAllAlbums(Context context){
        String[] projection = new String[] {MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME};
        Uri collection =MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection= MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }
        Cursor cursor = context.getContentResolver().query(collection,projection, null, null, null);
        List<String> albums= new ArrayList<String>();
        if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
            while (cursor.moveToNext()) {
                int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME);
                albums.add(cursor.getString(index));
            }
        }
        return  albums;
    }


    public  interface  GetPhotoCallback {
            void onResult(GetPhotoOutput result);
    }
    private static final String[] PROJECTION = {
            Images.Media._ID,
            Images.Media.MIME_TYPE,
            Images.Media.BUCKET_DISPLAY_NAME,
            Images.Media.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.ORIENTATION,
    };
    public static void getPhotos(final GetPhotoInput params, Context context,GetPhotoCallback callback) {
        if(params.Include==null){
            params.Include =new HashSet<String>();
            params.Include.add("INCLUDE_FILENAME");
        }
        new GetMediaTask(
                context,
                params, callback)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static class GetMediaTask extends AsyncTask<Void,Void,GetPhotoOutput> {
        private final Context mContext;
        GetPhotoInput searchInput;
        GetPhotoCallback callback;

        private GetMediaTask(
                Context context,
                GetPhotoInput searchInput,
                GetPhotoCallback callback) {
            super();
            mContext = context;
            this.searchInput = searchInput;
            this.callback = callback;
        }

        @Override
        protected GetPhotoOutput doInBackground(Void... params) {
            StringBuilder selection = new StringBuilder("1");
            List<String> selectionArgs = new ArrayList<>();
            boolean shouldContinue = true;
            if (!TextUtils.isEmpty(searchInput.GroupName)) {
                selection.append(" AND " + SELECTION_BUCKET);
                selectionArgs.add(searchInput.GroupName);
            }

            if (searchInput.AssetType.equals(ASSET_TYPE_PHOTOS)) {
                selection.append(" AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + " = "
                        + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE);
            } else if (searchInput.AssetType.equals(ASSET_TYPE_VIDEOS)) {
                selection.append(" AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + " = "
                        + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO);
            } else if (searchInput.AssetType.equals(ASSET_TYPE_ALL)) {
                selection.append(" AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + " IN ("
                        + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO + ","
                        + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + ")");
            } else {
                shouldContinue = false;
            }

            if (shouldContinue) {
                if (searchInput.MimeTypes != null && searchInput.MimeTypes.size() > 0) {
                    selection.append(" AND " + Images.Media.MIME_TYPE + " IN (");
                    for (int i = 0; i < searchInput.MimeTypes.size(); i++) {
                        selection.append("?,");
                        selectionArgs.add(searchInput.MimeTypes.get(i));
                    }
                    selection.replace(selection.length() - 1, selection.length(), ")");
                }

                if (searchInput.FromTime > 0) {
                    long addedDate = searchInput.FromTime / 1000;
                    selection.append(" AND (" + MediaStore.Images.Media.DATE_TAKEN + " > ? OR ( " + MediaStore.Images.Media.DATE_TAKEN
                            + " IS NULL AND " + MediaStore.Images.Media.DATE_ADDED + "> ? ))");
                    selectionArgs.add(searchInput.FromTime + "");
                    selectionArgs.add(addedDate + "");
                }
                if (searchInput.ToTime > 0) {
                    long addedDate = searchInput.ToTime / 1000;
                    selection.append(" AND (" + Images.Media.DATE_TAKEN + " <= ? OR ( " + Images.Media.DATE_TAKEN
                            + " IS NULL AND " + Images.Media.DATE_ADDED + " <= ? ))");
                    selectionArgs.add(searchInput.ToTime + "");
                    selectionArgs.add(addedDate + "");
                }

                GetPhotoOutput response = new GetPhotoOutput();
                ContentResolver resolver = mContext.getContentResolver();

                try {
                    Cursor media;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bundle bundle = new Bundle();
                        bundle.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection.toString());
                        bundle.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                                selectionArgs.toArray(new String[selectionArgs.size()]));
                        bundle.putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, Images.Media.DATE_ADDED + " DESC, " + Images.Media.DATE_MODIFIED + " DESC");
                        bundle.putInt(ContentResolver.QUERY_ARG_LIMIT, searchInput.First + 1);
                        if (!TextUtils.isEmpty(searchInput.After)) {
                            bundle.putInt(ContentResolver.QUERY_ARG_OFFSET, Integer.parseInt(searchInput.After));
                        }
                        media = resolver.query(
                                MediaStore.Files.getContentUri("external"),
                                PROJECTION,
                                bundle,
                                null);
                    } else {
                        // set LIMIT to first + 1 so that we know how to populate page_info
                        String limit = "limit=" + (searchInput.First + 1);
                        if (!TextUtils.isEmpty(searchInput.After)) {
                            limit = "limit=" + searchInput.After + "," + (searchInput.First + 1);
                        }
                        media = resolver.query(
                                MediaStore.Files.getContentUri("external").buildUpon().encodedQuery(limit).build(),
                                PROJECTION,
                                selection.toString(),
                                selectionArgs.toArray(new String[selectionArgs.size()]),
                                Images.Media.DATE_ADDED + " DESC, " + Images.Media.DATE_MODIFIED + " DESC");
                    }

                    if (media != null) {
                        try {
                            putEdges(resolver, media, response, searchInput.First, searchInput.Include);
                            putPageInfo(media, response, searchInput.First, !TextUtils.isEmpty(searchInput.After) ? Integer.parseInt(searchInput.After) : 0);
                        } finally {
                            media.close();

                        }
                    }
                } catch (SecurityException e) {
                    Log.e(
                            GalleryAlbumProvider.class.getName(),
                            "ERROR_UNABLE_TO_LOAD_PERMISSION->Could not get media: need READ_EXTERNAL_STORAGE permission",
                            e);
                }
                return response;
            }
            return null;
        }

        @Override
        protected void onPostExecute(GetPhotoOutput result) {
            callback.onResult(result);
        }
    }

    private static void putPageInfo(Cursor media, GetPhotoOutput response, int limit, int offset) {
        PageInfo pageInfo = new PageInfo();
        pageInfo.has_next_page = limit < media.getCount();
        if (limit < media.getCount()) {
            pageInfo.end_cursor = Integer.toString(offset + limit);
        }
        response.page_info = pageInfo;
    }

    private static void putEdges(
            ContentResolver resolver,
            Cursor media,
            GetPhotoOutput response,
            int limit,
            Set<String> include) {
        List<Asset> edges = new ArrayList<Asset>();
        media.moveToFirst();
        int mimeTypeIndex = media.getColumnIndex(Images.Media.MIME_TYPE);
        int groupNameIndex = media.getColumnIndex(Images.Media.BUCKET_DISPLAY_NAME);
        int dateTakenIndex = media.getColumnIndex(Images.Media.DATE_TAKEN);
        int dateAddedIndex = media.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED);
        int dateModifiedIndex = media.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED);
        int widthIndex = media.getColumnIndex(MediaStore.MediaColumns.WIDTH);
        int heightIndex = media.getColumnIndex(MediaStore.MediaColumns.HEIGHT);
        int sizeIndex = media.getColumnIndex(MediaStore.MediaColumns.SIZE);
        int dataIndex = media.getColumnIndex(MediaStore.MediaColumns.DATA);
        int orientationIndex = media.getColumnIndex(MediaStore.MediaColumns.ORIENTATION);

        boolean includeLocation = include.contains(INCLUDE_LOCATION);
        boolean includeFilename = include.contains(INCLUDE_FILENAME);
        boolean includeFileSize = include.contains(INCLUDE_FILE_SIZE);
        boolean includeFileExtension = include.contains(INCLUDE_FILE_EXTENSION);
        boolean includeImageSize = include.contains(INCLUDE_IMAGE_SIZE);
        boolean includePlayableDuration = include.contains(INCLUDE_PLAYABLE_DURATION);
        boolean includeOrientation = include.contains(INCLUDE_ORIENTATION);

        for (int i = 0; i < limit && !media.isAfterLast(); i++) {
            //WritableMap edge = new WritableNativeMap();
            Asset node = new Asset();
            boolean imageInfoSuccess =
                    putImageInfo(resolver, media, node, widthIndex, heightIndex, sizeIndex, dataIndex, orientationIndex,
                            mimeTypeIndex, includeFilename, includeFileSize, includeFileExtension, includeImageSize,
                            includePlayableDuration, includeOrientation);
            if (imageInfoSuccess) {
                putBasicNodeInfo(media, node, mimeTypeIndex, groupNameIndex, dateTakenIndex, dateAddedIndex, dateModifiedIndex);
                putLocationInfo(media, node, dataIndex, includeLocation, mimeTypeIndex, resolver);

                //edge.putMap("node", node);
                edges.add(node);
            } else {
                // we skipped an image because we couldn't get its details (e.g. width/height), so we
                // decrement i in order to correctly reach the limit, if the cursor has enough rows
                i--;
            }
            media.moveToNext();
        }
        response.assets= edges;
    }

    private static void putLocationInfo(
            Cursor media,
            Asset node,
            int dataIndex,
            boolean includeLocation,
            int mimeTypeIndex,
            ContentResolver resolver) {


        if (!includeLocation) {
            return;
        }

        try {
            String mimeType = media.getString(mimeTypeIndex);
            boolean isVideo = mimeType != null && mimeType.startsWith("video");
            if (isVideo) {
                Uri photoUri = Uri.parse("file://" + media.getString(dataIndex));
                @Nullable AssetFileDescriptor photoDescriptor = null;
                try {
                    photoDescriptor = resolver.openAssetFileDescriptor(photoUri, "r");
                } catch (FileNotFoundException e) {
                    Log.e(GalleryAlbumProvider.class.getName(), "Could not open asset file " + photoUri.toString(), e);
                }

                if (photoDescriptor != null) {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    try {
                        retriever.setDataSource(photoDescriptor.getFileDescriptor());
                    } catch (RuntimeException e) {
                        // Do nothing. We can't handle this, and this is usually a system problem
                    }
                    try {
                        String videoGeoTag = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION);
                        if (videoGeoTag != null) {
                            String filtered = videoGeoTag.replaceAll("/", "");
                            Location location = new Location();
                            location.latitude = Double.parseDouble(filtered.split("[+]|[-]")[1]);
                            location.longitude = Double.parseDouble(filtered.split("[+]|[-]")[2]);
                            node.location = location;
                        }
                    } catch (NumberFormatException e) {
                        Log.e(GalleryAlbumProvider.class.getName(), "Number format exception occurred while trying to fetch video metadata for " + photoUri.toString(), e);
                    }
                    try {
                        retriever.release();
                    } catch (
                            Exception e) { // Use general Exception here, see: https://developer.android.com/reference/android/media/MediaMetadataRetriever#release()
                        // Do nothing. We can't handle this, and this is usually a system problem
                    }
                }
                if (photoDescriptor != null) {
                    try {
                        photoDescriptor.close();
                    } catch (IOException e) {
                        // Do nothing. We can't handle this, and this is usually a system problem
                    }
                }
            } else {
                // location details are no longer indexed for privacy reasons using string Media.LATITUDE, Media.LONGITUDE
                // we manually obtain location metadata using ExifInterface#getLatLong(float[]).
                // ExifInterface is added in API level 5
                final ExifInterface exif = new ExifInterface(media.getString(dataIndex));
                float[] imageCoordinates = new float[2];
                boolean hasCoordinates = exif.getLatLong(imageCoordinates);
                if (hasCoordinates) {
                    double longitude = imageCoordinates[1];
                    double latitude = imageCoordinates[0];
                    Location location = new Location();
                    location.longitude = longitude;
                    location.latitude = latitude;
                    node.location = location;
                }
            }
        } catch (IOException e) {
            Log.e(GalleryAlbumProvider.class.getName(), "Could not read the metadata", e);
        }
    }
    private static void putBasicNodeInfo(
            Cursor media,
            Asset node,
            int mimeTypeIndex,
            int groupNameIndex,
            int dateTakenIndex,
            int dateAddedIndex,
            int dateModifiedIndex) {
        node.type = media.getString(mimeTypeIndex);
        node.group_name = media.getString(groupNameIndex);
        long dateTaken = media.getLong(dateTakenIndex);
        if (dateTaken == 0L) {
            //date added is in seconds, date taken in milliseconds, thus the multiplication
            dateTaken = media.getLong(dateAddedIndex) * 1000;
        }
        node.timestamp = dateTaken / 1000d;
        node.modified = media.getLong(dateModifiedIndex);
    }
    private static boolean putImageInfo(
            ContentResolver resolver,
            Cursor media,
            Asset node,
            int widthIndex,
            int heightIndex,
            int sizeIndex,
            int dataIndex,
            int orientationIndex,
            int mimeTypeIndex,
            boolean includeFilename,
            boolean includeFileSize,
            boolean includeFileExtension,
            boolean includeImageSize,
            boolean includePlayableDuration,
            boolean includeOrientation) {
        Image image = new Image();
        Uri photoUri = Uri.parse("file:///" + media.getString(dataIndex));
        image.uri = "file://"+media.getString(dataIndex); //photoUri.toString();

        int idx =media.getColumnIndex(Images.ImageColumns._ID);
        int _thumpId = media.getInt(idx);
        image.imageId = media.getLong(idx);
        Uri imageUri_t = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,_thumpId);
        image.uri = imageUri_t.toString();//media.getString(idx);
        image.imageUri = Uri.parse(image.uri);
        String mimeType = media.getString(mimeTypeIndex);

        boolean isVideo = mimeType != null && mimeType.startsWith("video");
        boolean putImageSizeSuccess = putImageSize(resolver, media, image, widthIndex, heightIndex, orientationIndex,
                photoUri, isVideo, includeImageSize);
        boolean putPlayableDurationSuccess = putPlayableDuration(resolver, image, photoUri, isVideo,
                includePlayableDuration);

        if (includeFilename) {
            File file = new File(media.getString(dataIndex));
            String strFileName = file.getName();
            image.filename = strFileName;
        }

        if (includeFileSize) {
            image.fileSize = media.getLong(sizeIndex);
        }

        if (includeFileExtension) {
            image.extension = Util.getExtension(mimeType);
        }

        if (includeOrientation) {
            if (media.isNull(orientationIndex)) {
                image.orientation = media.getInt(orientationIndex);
            } else {
                image.orientation = 0;
            }
        }

        node.image = image;
        return putImageSizeSuccess && putPlayableDurationSuccess;
    }

    private static boolean putImageSize(
            ContentResolver resolver,
            Cursor media,
            Image image,
            int widthIndex,
            int heightIndex,
            int orientationIndex,
            Uri photoUri,
            boolean isVideo,
            boolean includeImageSize) {


        if (!includeImageSize) {
            return true;
        }

        boolean success = true;

        int width = media.getInt(widthIndex);
        int height = media.getInt(heightIndex);

        /* If the columns don't contain the size information, read the media file */
        if (width <= 0 || height <= 0) {
            @Nullable AssetFileDescriptor mediaDescriptor = null;
            try {
                mediaDescriptor = resolver.openAssetFileDescriptor(photoUri, "r");
            } catch (FileNotFoundException e) {
                success = false;
                //FLog.e(ReactConstants.TAG, "Could not open asset file " + photoUri.toString(), e);
            }
            if (mediaDescriptor != null) {
                if (isVideo) {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    try {
                        retriever.setDataSource(mediaDescriptor.getFileDescriptor());
                    } catch (RuntimeException e) {
                        // Do nothing. We can't handle this, and this is usually a system problem
                    }
                    try {
                        width = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                        height = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                    } catch (NumberFormatException e) {
                        success = false;
                        Log.e(GalleryAlbumProvider.class.getName(),
                                "Number format exception occurred while trying to fetch video metadata for "
                                        + photoUri.toString(),
                                e);
                    }
                    try {
                        retriever.release();
                    } catch (
                            Exception e) { // Use general Exception here, see: https://developer.android.com/reference/android/media/MediaMetadataRetriever#release()
                        // Do nothing. We can't handle this, and this is usually a system problem
                    }
                } else {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    // Set inJustDecodeBounds to true so we don't actually load the Bitmap, but only get its
                    // dimensions instead.
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFileDescriptor(mediaDescriptor.getFileDescriptor(), null, options);
                    width = options.outWidth;
                    height = options.outHeight;
                }

                try {
                    mediaDescriptor.close();
                } catch (IOException e) {
                    Log.e(
                            GalleryAlbumProvider.class.getName(),
                            "Can't close media descriptor "
                                    + photoUri.toString(),
                            e);
                }
            }

        }

        if (!media.isNull(orientationIndex)) {
            int orientation = media.getInt(orientationIndex);
            if (orientation >= 0 && orientation % 180 != 0) {
                int temp = width;
                width = height;
                height = temp;
            }
        }

        image.width = width;
        image.height = height;
        return success;
    }

    private static boolean putPlayableDuration(
            ContentResolver resolver,
            Image image,
            Uri photoUri,
            boolean isVideo,
            boolean includePlayableDuration) {

        if (!includePlayableDuration || !isVideo) {
            return true;
        }

        boolean success = true;
        @Nullable Integer playableDuration = null;
        @Nullable AssetFileDescriptor photoDescriptor = null;
        try {
            photoDescriptor = resolver.openAssetFileDescriptor(photoUri, "r");
        } catch (FileNotFoundException e) {
            success = false;
            Log.e(GalleryAlbumProvider.class.getName(), "Could not open asset file " + photoUri.toString(), e);
        }

        if (photoDescriptor != null) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(photoDescriptor.getFileDescriptor());
            } catch (RuntimeException e) {
                // Do nothing. We can't handle this, and this is usually a system problem
            }
            try {
                int timeInMillisecond = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                playableDuration = timeInMillisecond / 1000;
            } catch (NumberFormatException e) {
                success = false;
                Log.e(GalleryAlbumProvider.class.getName(),
                        "Number format exception occurred while trying to fetch video metadata for "
                                + photoUri.toString(),
                        e);
            }
            try {
                retriever.release();
            } catch (Exception e) { // Use general Exception here, see: https://developer.android.com/reference/android/media/MediaMetadataRetriever#release()
                // Do nothing. We can't handle this, and this is usually a system problem
            }
        }

        if (photoDescriptor != null) {
            try {
                photoDescriptor.close();
            } catch (IOException e) {
                // Do nothing. We can't handle this, and this is usually a system problem
            }
        }

        if (playableDuration != null) {
            image.playableDuration = playableDuration;
        }
        return success;
    }
}
