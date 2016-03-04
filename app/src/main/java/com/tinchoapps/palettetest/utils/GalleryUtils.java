package com.tinchoapps.palettetest.utils;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;

public final class GalleryUtils
{
    private GalleryUtils()
    {
    }

    public static void startSystemGalleryIntent(@NonNull Activity activity, @NonNull String title, boolean multiple, int requestCode, @Nullable String imageType)
    {
        if (imageType == null)
        {
            imageType = "image/*";
        }

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

        if (multiple)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
            {
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)//ClipData class (to read result) does not exists before this
            {
                intent.putExtra("android.intent.extra.ALLOW_MULTIPLE", true);//some apps (like google gallery) picks this extra even in older versions;
            }
        }

        intent.setType(imageType);

        activity.startActivityForResult(Intent.createChooser(intent, title), requestCode);
    }

    public static ArrayList<Uri> getResultFromGalleryIntent(@NonNull Context context, @NonNull Intent data, boolean getLatestIfFail) throws Exception
    {
        ArrayList<Uri> resultUri = new ArrayList<>();
        Uri fileUri = data.getData();

        if (fileUri != null)
        {
            resultUri.add(fileUri);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            ClipData clipData = data.getClipData();

            if (clipData != null)
            {
                for (int i = 0; i < clipData.getItemCount(); i++)
                {
                    ClipData.Item item = clipData.getItemAt(i);

                    if (item != null)
                    {
                        Uri uri = item.getUri();

                        if (uri != null)
                        {
                            resultUri.add(uri);
                        }
                    }
                }
            }
        }

        //If none of the above works, just get the last image taken from the gallery
        if (getLatestIfFail && resultUri.size() == 0)
        {
            Cursor cursor = null;

            try
            {
                String[] fileProjection = {MediaStore.Images.ImageColumns._ID,
                        MediaStore.Images.ImageColumns.DATA,
                        MediaStore.Images.ImageColumns.ORIENTATION,
                        MediaStore.Images.ImageColumns.DATE_TAKEN};

                String fileSort = MediaStore.Images.ImageColumns._ID + " DESC";
                cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, fileProjection, null, null, fileSort);
                cursor.moveToFirst();

                if (!cursor.isAfterLast())
                {
                    String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATA));

                    if (!TextUtils.isEmpty(imagePath))
                    {
                        fileUri = Uri.fromFile(new File(imagePath));
                        resultUri.add(fileUri);
                    }
                }
            } catch (Exception e)
            {
                if (cursor != null)
                {
                    cursor.close();
                }
            }
        }

        return resultUri;
    }
}
