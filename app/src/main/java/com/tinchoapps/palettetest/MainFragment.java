package com.tinchoapps.palettetest;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import com.tinchoapps.palettetest.utils.BitmapUtils;

public class MainFragment extends Fragment
{
    private ImageView imageView;
    private TextView titleView;
    private TextView subTitleView;
    private View titleContainerView;

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        Drawable pictureBackground = getResources().getDrawable(R.drawable.pick_picture_bg);

        titleContainerView = view.findViewById(R.id.title_container_view);

        imageView = (ImageView) view.findViewById(R.id.image_view);
        imageView.setBackground(pictureBackground);

        titleView = (TextView) view.findViewById(R.id.title_text_view);
        subTitleView = (TextView) view.findViewById(R.id.sub_title_text_view);

        imageView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener()
        {
            @Override
            public boolean onPreDraw()
            {
                imageView.getViewTreeObserver().removeOnPreDrawListener(this);

                new DecodeBitmapTask().execute();

                return true;
            }
        });
    }

    private Palette getPalette(@NonNull final Bitmap bitmap)
    {
        Palette.Builder builder = new Palette.Builder(bitmap);
        return builder.generate();
    }

    private class DecodeBitmapTask extends AsyncTask<Void, Void, Bitmap>
    {
        @Override
        protected Bitmap doInBackground(final Void... params)
        {
            Bitmap bitmap = null;

            try
            {
                Context context = getActivity();

                Uri uri = Uri.parse(getArguments().getString(MediaStore.Images.Media.DATA));
                bitmap = BitmapFactory.decodeFile(uri.getPath());
                bitmap = BitmapUtils.resizeDownBySideLength(bitmap, Math.max(imageView.getWidth(), imageView.getHeight()), true);

                ExifInterface ei = new ExifInterface(uri.getPath());
                int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                Matrix matrix = new Matrix();

                switch (orientation)
                {
                    case ExifInterface.ORIENTATION_UNDEFINED:

                        //Unknown orientation reported, so we check if ContentResolver says different (SAMSUNG BUG, I'm looking at you!)

                        String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
                        Cursor cur = context.getContentResolver().query(uri, orientationColumn, null, null, null);

                        if (cur != null && cur.moveToFirst())
                        {
                            int mediaOrientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));
                            matrix.setRotate(mediaOrientation);
                            cur.close();
                        }
                        break;

                    case ExifInterface.ORIENTATION_NORMAL:

                        break;

                    case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                        matrix.setScale(-1, 1);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_180:
                        matrix.setRotate(180);
                        break;

                    case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                        matrix.setRotate(180);
                        matrix.postScale(-1, 1);
                        break;

                    case ExifInterface.ORIENTATION_TRANSPOSE:
                        matrix.setRotate(90);
                        matrix.postScale(-1, 1);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_90:
                        matrix.setRotate(90);
                        break;

                    case ExifInterface.ORIENTATION_TRANSVERSE:
                        matrix.setRotate(-90);
                        matrix.postScale(-1, 1);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_270:
                        matrix.setRotate(-90);
                        break;
                }

                if (!matrix.isIdentity())
                {
                    bitmap = BitmapUtils.transformBitmap(bitmap, matrix, true);
                }


            } catch (Exception e)
            {
                e.printStackTrace();
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(final Bitmap bitmap)
        {
            super.onPostExecute(bitmap);

            imageView.setImageBitmap(bitmap);

            Palette palette = getPalette(bitmap);

            Palette.Swatch swatch = palette.getDarkVibrantSwatch();

            if (swatch == null)
            {
                swatch = palette.getDarkMutedSwatch();
            }

            if (swatch != null)
            {
                titleContainerView.setBackgroundColor(swatch.getRgb());
                titleView.setTextColor(swatch.getTitleTextColor());
                subTitleView.setTextColor(swatch.getBodyTextColor());
            }

            Bundle arguments = getArguments();
            titleView.setText(arguments.getString(MediaStore.Images.Media.DISPLAY_NAME));
            subTitleView.setText(String.format("From %s\nSize: %d Kb", arguments.getString(MediaStore.Images.Media.BUCKET_DISPLAY_NAME), Integer.valueOf(arguments.getString(MediaStore.Images.Media.SIZE)) / 1024));
        }
    }
}
