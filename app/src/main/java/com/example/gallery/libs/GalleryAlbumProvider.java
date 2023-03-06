package com.example.gallery.libs;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.util.ArrayList;
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
    public static Map<String, Integer> getAlbums(Context context) {
        String assetType = ASSET_TYPE_ALL;
        StringBuilder selection = new StringBuilder("1");
        List<String> selectionArgs = new ArrayList<>();

        selection.append(" AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + " IN ("
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO + ","
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + ")");


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

                    return  albums;

                }
            } finally {
                media.close();

            }

        } catch (Exception e) {

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
    public void getPhotos(final GetPhotoInput params, Context context) {
        new GetMediaTask(
                context,
               params)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static class GetMediaTask extends AsyncTask<Void, Void> {
        private final Context mContext;
        GetPhotoInput searchInput;

        private GetMediaTask(
                Context context,
                GetPhotoInput searchInput) {
            super();
            mContext = context;
            this.searchInput = searchInput;
        }

        @Override
        protected void doInBackground(Void... params) {
            StringBuilder selection = new StringBuilder("1");
            List<String> selectionArgs = new ArrayList<>();
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
                return;
            }

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

            WritableMap response = new WritableNativeMap();
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

                if (media == null) {
                    return null;
                } else {
                    try {
                        putEdges(resolver, media, response, mFirst, mInclude);
                        putPageInfo(media, response, mFirst, !TextUtils.isEmpty(mAfter) ? Integer.parseInt(mAfter) : 0);
                    } finally {
                        media.close();
                        mPromise.resolve(response);
                    }
                }
            } catch (SecurityException e) {
                mPromise.reject(
                        ERROR_UNABLE_TO_LOAD_PERMISSION,
                        "Could not get media: need READ_EXTERNAL_STORAGE permission",
                        e);
            }
        }
    }
    private static void putEdges(
            ContentResolver resolver,
            Cursor media,
            WritableMap response,
            int limit,
            Set<String> include) {
        WritableArray edges = new WritableNativeArray();
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
            WritableMap edge = new WritableNativeMap();
            WritableMap node = new WritableNativeMap();
            boolean imageInfoSuccess =
                    putImageInfo(resolver, media, node, widthIndex, heightIndex, sizeIndex, dataIndex, orientationIndex,
                            mimeTypeIndex, includeFilename, includeFileSize, includeFileExtension, includeImageSize,
                            includePlayableDuration, includeOrientation);
            if (imageInfoSuccess) {
                putBasicNodeInfo(media, node, mimeTypeIndex, groupNameIndex, dateTakenIndex, dateAddedIndex, dateModifiedIndex);
                putLocationInfo(media, node, dataIndex, includeLocation, mimeTypeIndex, resolver);

                edge.putMap("node", node);
                edges.pushMap(edge);
            } else {
                // we skipped an image because we couldn't get its details (e.g. width/height), so we
                // decrement i in order to correctly reach the limit, if the cursor has enough rows
                i--;
            }
            media.moveToNext();
        }
        response.putArray("edges", edges);
    }

    private static boolean putImageInfo(
            ContentResolver resolver,
            Cursor media,
            WritableMap node,
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
        WritableMap image = new WritableNativeMap();
        Uri photoUri = Uri.parse("file://" + media.getString(dataIndex));
        image.putString("uri", photoUri.toString());
        String mimeType = media.getString(mimeTypeIndex);

        boolean isVideo = mimeType != null && mimeType.startsWith("video");
        boolean putImageSizeSuccess = putImageSize(resolver, media, image, widthIndex, heightIndex, orientationIndex,
                photoUri, isVideo, includeImageSize);
        boolean putPlayableDurationSuccess = putPlayableDuration(resolver, image, photoUri, isVideo,
                includePlayableDuration);

        if (includeFilename) {
            File file = new File(media.getString(dataIndex));
            String strFileName = file.getName();
            image.putString("filename", strFileName);
        } else {
            image.putNull("filename");
        }

        if (includeFileSize) {
            image.putDouble("fileSize", media.getLong(sizeIndex));
        } else {
            image.putNull("fileSize");
        }

        if (includeFileExtension) {
            image.putString("extension", Utils.getExtension(mimeType));
        } else {
            image.putNull("extension");
        }

        if (includeOrientation) {
            if(media.isNull(orientationIndex)) {
                image.putInt("orientation", media.getInt(orientationIndex));
            } else {
                image.putInt("orientation", 0);
            }
        } else {
            image.putNull("orientation");
        }

        node.putMap("image", image);
        return putImageSizeSuccess && putPlayableDurationSuccess;
    }

}
