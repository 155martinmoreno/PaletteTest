package com.tinchoapps.palettetest.utils;


import android.content.Context;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.FloatMath;
import android.util.Log;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Copied from Android OpenSource Project, and added some extra methods
 */
public final class BitmapUtils
{
    private static final String TAG = "BitmapUtils";
    private static final int DEFAULT_JPEG_QUALITY = 90;
    public static final int UNCONSTRAINED = -1;

    private BitmapUtils()
    {
    }

    /**
     * Compute the sample size as a function of minSideLength
     * and maxNumOfPixels.
     * minSideLength is used to specify that minimal width or height of a
     * bitmap.
     * maxNumOfPixels is used to specify the maximal size in pixels that is
     * tolerable in terms of memory usage.
     * <p/>
     * The function returns a sample size based on the constraints.
     * Both size and minSideLength can be passed in as UNCONSTRAINED,
     * which indicates no care of the corresponding constraint.
     * The functions prefers returning a sample size that
     * generates a smaller bitmap, unless minSideLength = UNCONSTRAINED.
     * <p/>
     * Also, the function rounds up the sample size to a power of 2 or multiple
     * of 8 because BitmapFactory only honors sample size this way.
     * For example, BitmapFactory downsamples an image by 2 even though the
     * request is 3. So we round up the sample size to avoid OOM.
     */
    public static int computeSampleSize(int width, int height, int minSideLength, int maxNumOfPixels)
    {
        int initialSize = computeInitialSampleSize(width, height, minSideLength, maxNumOfPixels);

        return initialSize <= 8
                ? Util.nextPowerOf2(initialSize)
                : (initialSize + 7) / 8 * 8;
    }

    private static int computeInitialSampleSize(int w, int h, int minSideLength, int maxNumOfPixels)
    {
        if (maxNumOfPixels == UNCONSTRAINED
                && minSideLength == UNCONSTRAINED) return 1;

        int lowerBound = (maxNumOfPixels == UNCONSTRAINED) ? 1 :
                (int) FloatMath.ceil(FloatMath.sqrt((float) (w * h) / maxNumOfPixels));

        if (minSideLength == UNCONSTRAINED)
        {
            return lowerBound;
        } else
        {
            int sampleSize = Math.min(w / minSideLength, h / minSideLength);
            return Math.max(sampleSize, lowerBound);
        }
    }

    // This computes a sample size which makes the longer side at least
    // minSideLength long. If that's not possible, return 1.
    public static int computeSampleSizeLarger(int w, int h, int minSideLength)
    {
        int initialSize = Math.max(w / minSideLength, h / minSideLength);
        if (initialSize <= 1) return 1;

        return initialSize <= 8
                ? Util.prevPowerOf2(initialSize)
                : initialSize / 8 * 8;
    }

    // Find the min x that 1 / x >= scale
    public static int computeSampleSizeLarger(float scale)
    {
        int initialSize = (int) FloatMath.floor(1f / scale);
        if (initialSize <= 1) return 1;

        return initialSize <= 8
                ? Util.prevPowerOf2(initialSize)
                : initialSize / 8 * 8;
    }

    // Find the max x that 1 / x <= scale.
    public static int computeSampleSize(float scale)
    {
        Util.assertTrue(scale > 0);
        int initialSize = Math.max(1, (int) FloatMath.ceil(1 / scale));
        return initialSize <= 8
                ? Util.nextPowerOf2(initialSize)
                : (initialSize + 7) / 8 * 8;
    }

    public static Bitmap resizeBitmapByScale(Bitmap bitmap, float scale, boolean recycle)
    {
        int width = Math.round(bitmap.getWidth() * scale);
        int height = Math.round(bitmap.getHeight() * scale);
        if (width == bitmap.getWidth()
                && height == bitmap.getHeight()) return bitmap;
        Bitmap target = Bitmap.createBitmap(width, height, getConfig(bitmap));
        Canvas canvas = new Canvas(target);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        if (recycle) bitmap.recycle();
        return target;
    }

    private static Bitmap.Config getConfig(Bitmap bitmap)
    {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null)
        {
            config = Bitmap.Config.ARGB_8888;
        }
        return config;
    }

    public static Bitmap resizeDownBySideLength(Bitmap bitmap, int maxLength, boolean recycle)
    {
        int srcWidth = bitmap.getWidth();
        int srcHeight = bitmap.getHeight();
        float scale = Math.min((float) maxLength / srcWidth, (float) maxLength / srcHeight);
        if (scale >= 1.0f) return bitmap;
        return resizeBitmapByScale(bitmap, scale, recycle);
    }

    public static Bitmap resizeAndCropCenter(Bitmap bitmap, int size, boolean recycle)
    {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w == size && h == size) return bitmap;

        // scale the image so that the shorter side equals to the target;
        // the longer side will be center-cropped.
        float scale = (float) size / Math.min(w, h);

        Bitmap target = Bitmap.createBitmap(size, size, getConfig(bitmap));
        int width = Math.round(scale * bitmap.getWidth());
        int height = Math.round(scale * bitmap.getHeight());
        Canvas canvas = new Canvas(target);
        canvas.translate((size - width) / 2f, (size - height) / 2f);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        if (recycle) bitmap.recycle();
        return target;
    }

    public static void recycleSilently(Bitmap bitmap)
    {
        if (bitmap == null) return;
        try
        {
            bitmap.recycle();
        } catch (Throwable t)
        {
            Log.w(TAG, "unable recycle bitmap", t);
        }
    }

    public static Bitmap rotateBitmap(Bitmap source, int rotation, boolean recycle)
    {
        if (rotation == 0) return source;
        int w = source.getWidth();
        int h = source.getHeight();
        Matrix m = new Matrix();
        m.postRotate(rotation);
        Bitmap bitmap = Bitmap.createBitmap(source, 0, 0, w, h, m, true);
        if (recycle) source.recycle();
        return bitmap;
    }

    public static Bitmap transformBitmap(Bitmap source, Matrix matrix, boolean recycle)
    {
        Bitmap rotatedBitmap = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);

        if (recycle) source.recycle();

        return rotatedBitmap;
    }

    public static Bitmap createVideoThumbnail(String filePath)
    {
        // MediaMetadataRetriever is available on API Level 8
        // but is hidden until API Level 10
        Class<?> clazz = null;
        Object instance = null;
        try
        {
            clazz = Class.forName("android.media.MediaMetadataRetriever");
            instance = clazz.newInstance();

            Method method = clazz.getMethod("setDataSource", String.class);
            method.invoke(instance, filePath);

            // The method name changes between API Level 9 and 10.
            if (Build.VERSION.SDK_INT <= 9)
            {
                return (Bitmap) clazz.getMethod("captureFrame").invoke(instance);
            } else
            {
                byte[] data = (byte[]) clazz.getMethod("getEmbeddedPicture").invoke(instance);
                if (data != null)
                {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    if (bitmap != null) return bitmap;
                }
                return (Bitmap) clazz.getMethod("getFrameAtTime").invoke(instance);
            }
        } catch (RuntimeException ex)
        {
            // Assume this is a corrupt video file.
        } catch (InstantiationException | InvocationTargetException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException e)
        {
            Log.e(TAG, "createVideoThumbnail", e);
        } finally
        {
            try
            {
                if (instance != null)
                {
                    clazz.getMethod("release").invoke(instance);
                }
            } catch (Exception ignored)
            {
            }
        }
        return null;
    }

    public static byte[] compressToBytes(Bitmap bitmap)
    {
        return compressToBytes(bitmap, DEFAULT_JPEG_QUALITY);
    }

    public static byte[] compressToBytes(Bitmap bitmap, int quality)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(65536);
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        return baos.toByteArray();
    }

    public static boolean isSupportedByRegionDecoder(String mimeType)
    {
        if (mimeType == null) return false;
        mimeType = mimeType.toLowerCase();
        return mimeType.startsWith("image/") &&
                (!mimeType.equals("image/gif") && !mimeType.endsWith("bmp"));
    }

    public static boolean isRotationSupported(String mimeType)
    {
        if (mimeType == null) return false;
        mimeType = mimeType.toLowerCase();
        return mimeType.equals("image/jpeg");
    }

    public static File saveToFile(@NonNull final Bitmap bitmap, @NonNull final String savePath, @NonNull final Bitmap.CompressFormat format, final int quality, final boolean recycle)
    {
        FileOutputStream out = null;
        File file = null;

        try
        {
            out = new FileOutputStream(savePath);
            bitmap.compress(format, quality, out);

            if (recycle)
            {
                bitmap.recycle();
            }

            file = new File(savePath);
        } catch (Exception e)
        {
            Log.e(TAG, "Error saving bitmap.");
        } finally
        {
            try
            {
                if (out != null)
                {
                    out.close();
                }
            } catch (IOException e)
            {
                Log.e(TAG, "Error saving bitmap.");
            }
        }

        return file;
    }

    public static File copyBitmapToDestination(@NonNull Context context, @NonNull Uri uri, int maxWidth, int maxHeight, @NonNull File destinationFile, boolean fixRotation) throws IOException, OutOfMemoryError
    {
        Bitmap bitmap;
        InputStream input;
        input = context.getContentResolver().openInputStream(uri);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeStream(input, null, options);
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;

        input = context.getContentResolver().openInputStream(uri);//must recreate the stream, resetting it just throws IOException

        //Decode to the nearest power of 2 (max_size *2 as maximum memory)
        options.inJustDecodeBounds = false;
        options.inSampleSize = computeSampleSize(imageWidth, imageHeight, Math.min(maxWidth, maxHeight), maxWidth * maxHeight * 2);
        bitmap = BitmapFactory.decodeStream(input, null, options);

        if (Thread.interrupted())
        {
            return null;
        }

        //Resize to the final size
        bitmap = resizeDownBySideLength(bitmap, Math.max(maxWidth, maxHeight), true);

        if (Thread.interrupted())
        {
            return null;
        }

        //Fix rotation
        if (fixRotation)
        {
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
                bitmap = transformBitmap(bitmap, matrix, true);
            }
        }

        if (Thread.interrupted())
        {
            return null;
        }

        //Save the file
        return saveToFile(bitmap, destinationFile.getAbsolutePath(), Bitmap.CompressFormat.JPEG, 90, true);
    }

    public static Bitmap drawableToBitmap(Drawable drawable)
    {
        if (drawable instanceof BitmapDrawable)
        {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}

