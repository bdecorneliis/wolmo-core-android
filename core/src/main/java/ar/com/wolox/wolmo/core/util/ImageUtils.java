/*
 * Copyright (c) Wolox S.A
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ar.com.wolox.wolmo.core.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;

import ar.com.wolox.wolmo.core.WoloxApplication;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Utils class to manipulate images, through {@link Bitmap}s or their corresponding {@link Uri}, and
 * for retrieving pictures from gallery/taking them from the camera.
 */
public class ImageUtils {

    @Retention(SOURCE)
    @StringDef({
            PNG,
            JPG
    })
    /**
     * Image compression formats supported.
     */
    public @interface ImageFormat {
    }

    public static final String PNG = "png";
    public static final String JPG = "jpg";

    private ImageUtils() {
    }

    /**
     * Triggers an intent to go to the device's image gallery and returns an URI with the file.
     * <p>
     * <p/>
     * Override the onActivityResult method in your fragment and specify behaviour
     * for the provided request code. The selected image URI will be
     * returned in the data variable of the activity result.
     *
     * @param fragment    A fragment where the get image intent is going to be called
     * @param requestCode of the result call to be identified with
     * @param errorResId  {@link StringRes} to be displayed in case of error
     */
    public static void getImageFromGallery(
            @NonNull Fragment fragment, int requestCode, @StringRes int errorResId) {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        i.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

        // Ensure that there's a gallery app to handle the intent
        if (i.resolveActivity(ContextUtils.getAppContext().getPackageManager()) != null) {
            fragment.startActivityForResult(i, requestCode);
        } else {
            ToastUtils.showToast(errorResId);
        }
    }

    /**
     * Adds a given picture to the device images gallery.
     *
     * @param imageUri The {@link Uri} of the image
     */
    public static void addPictureToDeviceGallery(@NonNull Uri imageUri) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(imageUri);
        ContextUtils.getAppContext().sendBroadcast(mediaScanIntent);
    }

    /**
     * Triggers an intent to go to the device's camera app, stores the image as 'filename'.'format',
     * and returns its {@link Uri}.
     * <p>
     * <p/>
     * Override the onActivityResult method in your fragment and specify behaviour
     * for the provided request code. This method returns a File object that
     * contains the picture taken. You can get the returned image as an {@link Uri} using
     * {@link Uri#fromFile(File)}.
     *
     * @param fragment    A fragment where the get image intent is going to be called
     * @param requestCode A request code for the intent
     * @param filename    Filename for the future stored image
     * @param format      Format (extension) for the future image
     * @param errorResId  Resource id of the error string
     * @return {@link Uri} of the newly stored image
     */
    @Nullable
    public static File getImageFromCamera(
            @NonNull Fragment fragment, int requestCode, @NonNull String filename,
            @ImageFormat String format, @StringRes int errorResId) {

        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera app to handle the intent
        if (i.resolveActivity(ContextUtils.getAppContext().getPackageManager()) == null) {
            ToastUtils.showToast(errorResId);
            return null;
        }

        File photoFile;
        try {
            photoFile = FileUtils.createFile(filename, format);
        } catch (IOException ex) {
            ToastUtils.showToast(errorResId);
            return null;
        }

        // Change in API 24 to get the file
        Uri photoFileUri = FileProvider.getUriForFile(fragment.getContext(),
                fragment.getContext().getPackageName() + ".provider", photoFile);

        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        i.putExtra(MediaStore.EXTRA_OUTPUT, photoFileUri);
        fragment.startActivityForResult(i, requestCode);

        return photoFile;
    }

    /**
     * Get {@link byte[]} representation from a {@link Bitmap}, with boundaries.
     *
     * @param bitmap    target Bitmap
     * @param format    image compress format
     * @param quality   compress quality, between 0 and 100
     * @param maxHeight max height of the target image
     * @param maxWidth  max width of the target image
     * @return byte array with the formatted information of the Bitmap, if the image exceeded
     * boundaries, it's re scaled.
     */
    public static byte[] getImageAsByteArray(
            Bitmap bitmap,
            Bitmap.CompressFormat format,
            @IntRange(from = 0, to = 100) int quality,
            int maxWidth,
            int maxHeight) {

        Bitmap targetBitmap = fit(bitmap, maxWidth, maxHeight);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        targetBitmap.compress(format, sanitizeQuality(quality), bytes);

        return bytes.toByteArray();
    }

    /**
     * Get {@link byte[]} from an image file, represented by its {@link Uri}.
     *
     * @param imageFileUri target image file URI
     * @param format       image compress format
     * @param quality      compress quality, between 0 and 100
     * @param maxWidth     max width of the target image
     * @param maxHeight    max height of the target image
     * @return byte array with the formatted information of the image file, if the image exceeded
     * boundaries, it's re scaled.
     */
    public static byte[] getImageAsByteArray(
            @NonNull Uri imageFileUri,
            @NonNull Bitmap.CompressFormat format,
            @IntRange(from = 0, to = 100) int quality,
            int maxWidth,
            int maxHeight) {

        return getImageAsByteArray(
                BitmapFactory.decodeFile(FileUtils.getRealPathFromUri(imageFileUri)),
                format,
                quality,
                maxWidth,
                maxHeight);
    }

    /**
     * Get {@link byte[]} from an image {@link File}
     *
     * @param file      target image file
     * @param format    image compress format
     * @param quality   compress quality, between 0 and 100
     * @param maxWidth  max width of the target image
     * @param maxHeight max height of the target image
     * @return byte array with the formatted information of the image file, if the image exceeded
     * boundaries, it's re scaled.
     */
    public static byte[] getImageAsByteArray(
            @NonNull File file,
            @NonNull Bitmap.CompressFormat format,
            @IntRange(from = 0, to = 100) int quality,
            int maxWidth,
            int maxHeight) {

        return getImageAsByteArray(
                BitmapFactory.decodeFile(file.getPath()),
                format,
                quality,
                maxWidth,
                maxHeight);
    }

    /**
     * Prevents quality from being outside 0...100 range.
     *
     * @param quality target quality
     * @return if below 0, returns 0, if above 100, return 100, else returns {@code quality} param
     */
    private static int sanitizeQuality(int quality) {
        if (quality < 0) {
            return 0;
        } else if (quality > 100) {
            return 100;
        }

        return quality;
    }

    /**
     * Re-sizes the image, represented as a {@link Bitmap}, to fit the boundaries and keeping its
     * aspect ratio.
     *
     * @param image     {@link Bitmap} to scale
     * @param maxWidth  max width it can occupy
     * @param maxHeight max height it can occupy
     * @return If the re-scaling was necessary, the scaled {@link Bitmap}. Else, it returns the
     * target one.
     */
    public static Bitmap fit(@NonNull Bitmap image, int maxWidth, int maxHeight) {
        int width = image.getWidth();
        int height = image.getHeight();

        if (maxWidth <= 0 || maxHeight <= 0 || (width <= maxWidth && height <= maxHeight)) {
            return image;
        }

        float ratioImage = (float) width / (float) height;

        int finalWidth = maxWidth;
        int finalHeight = maxHeight;
        if (ratioImage > 1) {
            finalHeight = (int) ((float) finalWidth / ratioImage);
        } else {
            finalWidth = (int) ((float) finalHeight * ratioImage);
        }

        return Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
    }


    public static Uri importBySize(@NonNull Uri imageUri,
                                   int maxSize,
                                   @IntRange(from = 0, to = 100) int quality) {

        Context context = WoloxApplication.getInstance();

        //Decode bounds of Image
        BitmapFactory.Options options = decodeBounds(imageUri, context);

        if (options == null) {
            return imageUri;
        }
        int imageWidth = options.outWidth;
        int imageHeight = options.outHeight;
        int imageSize = imageWidth * imageHeight;

        // Calculate image width and size to scale down
        if (maxSize >= imageSize) {
            return imageUri;
        }

        options.inSampleSize = calculateInSampleSize(maxSize, imageWidth, imageHeight);
        options.inJustDecodeBounds = false;

        //Load bitmap with estimated size by inSampleSize
        Bitmap bitmap;
        try {
            InputStream input = context.getContentResolver()
                    .openInputStream(imageUri);
            bitmap = BitmapFactory.decodeStream(input, null, options);
            if (input != null) {
                input.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return imageUri;
        }

        //Scale down to the real calculated size
        Bitmap scaledBitmap = fit(bitmap, imageWidth, imageHeight);

        //Check Image rotation
        scaledBitmap = rotate(scaledBitmap, getSourceExifOrientation(imageUri));

        //Save reduced image in cache
        File reducedImageFile = null;
        OutputStream os;
        try {
            reducedImageFile = File.createTempFile("reduced-image", ".jpg", context.getCacheDir());
            os = new FileOutputStream(reducedImageFile);
            if (scaledBitmap != null) {
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, os);
            }
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
            return imageUri;
        } finally {
            bitmap.recycle();
            if (scaledBitmap != null) {
                scaledBitmap.recycle();
            }
        }
        return Uri.fromFile(reducedImageFile);
    }

    private static BitmapFactory.Options decodeBounds(@NonNull Uri imageUri, Context context) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            InputStream input = context.getContentResolver()
                    .openInputStream(imageUri);
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);
            if (input != null) {
                input.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return options;
    }

    public static int calculateInSampleSize(int maxSize, int imageWidth, int imageHeight) {

        int imageEstimatedSize = imageWidth * imageHeight;
        float reduceRatio = (float) Math.sqrt(maxSize / (double) imageEstimatedSize);

        int reqHeight;
        int reqWidth;

        if (imageHeight > imageWidth) {
            reqHeight = (int) (imageHeight * reduceRatio);
            reqWidth = (int) ((float) imageEstimatedSize / imageHeight);
        } else {
            reqWidth = (int) (imageWidth * reduceRatio);
            reqHeight = (int) ((float) imageEstimatedSize / imageWidth);
        }

        int inSampleSize = 1;

        if (imageHeight > reqHeight || imageWidth > reqWidth) {

            final int halfHeight = imageHeight / 2;
            final int halfWidth = imageWidth / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * Get {@link ExifInterface} Exif rotation of the source
     *
     * @param sourceUri {@link Uri} of the source.
     * @return The Exif rotation {@link ExifInterface}.
     */
    public static int getSourceExifOrientation(Uri sourceUri) {
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(sourceUri.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);
    }

    /**
     * Rotate the image, represented as a {@link Bitmap}, to the given orientation
     *
     * @param bitmap      {@link Bitmap} to rotate.
     * @param orientation new orientation based on {@link ExifInterface} ORIENTATION.
     * @return The rotated {@link Bitmap}.
     */
    public static Bitmap rotate(Bitmap bitmap, int orientation) {

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
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
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

}
