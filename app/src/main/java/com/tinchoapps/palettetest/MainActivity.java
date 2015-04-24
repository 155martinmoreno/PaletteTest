package com.tinchoapps.palettetest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.tinchoapps.palettetest.utils.BitmapUtils;
import com.tinchoapps.palettetest.utils.GalleryUtils;

import java.io.File;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity
{
    public static final int REQUEST_CODE_GALLERY = 0;
    private ImageView imageView;
    private TextView titleView;
    private TextView subTitleView;
    private View titleContainerView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Drawable pictureBackground = getResources().getDrawable(R.drawable.pick_picture_bg);

        titleContainerView = findViewById(R.id.title_container_view);

        imageView = (ImageView) findViewById(R.id.image_view);
        imageView.setBackground(pictureBackground);
        imageView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(final View v)
            {
                GalleryUtils.startSystemGalleryIntent(MainActivity.this, getString(R.string.choose_picture), false, REQUEST_CODE_GALLERY, null);
            }
        });


        titleView = (TextView) findViewById(R.id.title_text_view);
        subTitleView = (TextView) findViewById(R.id.sub_title_text_view);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK)
        {
            switch (requestCode)
            {
                case REQUEST_CODE_GALLERY:
                    try
                    {
                        ArrayList<Uri> resultFromGallery = GalleryUtils.getResultFromGalleryIntent(this, data, false);
                        Uri uri = resultFromGallery.get(0);
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                        bitmap = BitmapUtils.resizeDownBySideLength(bitmap, Math.max(imageView.getWidth(), imageView.getHeight()), true);

                        imageView.setImageBitmap(bitmap);

                        Palette palette = getPalette(bitmap);

                        Palette.Swatch swatch = palette.getDarkVibrantSwatch();

                        titleContainerView.setBackgroundColor(swatch.getRgb());
                        titleView.setTextColor(swatch.getTitleTextColor());
                        subTitleView.setTextColor(swatch.getBodyTextColor());

                        File file = new File(uri.getPath());
                        titleView.setText(file.getName());
                        subTitleView.setText("Some other data");
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    private Palette getPalette(@NonNull final Bitmap bitmap)
    {
        Palette.Builder builder = new Palette.Builder(bitmap);
        return builder.generate();
    }

}
