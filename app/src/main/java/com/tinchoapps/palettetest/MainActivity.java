package com.tinchoapps.palettetest;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>
{
    private static final int LOAD_GALLERY = 0;
    private final String[] projection = new String[]{
            MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.SIZE, MediaStore.Images.Media.DATA
    };

    private CursorPagerAdapter<MainFragment> galleryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportLoaderManager().restartLoader(LOAD_GALLERY, null, this);

        ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
        galleryAdapter = new CursorPagerAdapter<>(getSupportFragmentManager(), MainFragment.class, projection, null);
        viewPager.setAdapter(galleryAdapter);
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
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args)
    {
        CursorLoader cursorLoader = null;

        switch (id)
        {
            case LOAD_GALLERY:
                Uri imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                cursorLoader = new CursorLoader(this, imagesUri, projection, null, null, null);
                break;
        }

        return cursorLoader;
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor data)
    {
        switch (loader.getId())
        {
            case LOAD_GALLERY:
                galleryAdapter.changeCursor(data);
                break;
        }
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader)
    {
        galleryAdapter.changeCursor(null);
    }
}
