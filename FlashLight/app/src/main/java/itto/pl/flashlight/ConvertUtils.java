package itto.pl.flashlight;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.ByteArrayOutputStream;

/**
 * Created by PL_itto on 2/10/2017.
 */
public class ConvertUtils {
    public static int ConvertPixelToDp(int pixels, Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return ((int) (pixels / displayMetrics.density));
    }

    public static int ConvertPxToDp(int px,
                                    Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static int ConvertDpToPx(int dp, Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static int ConvertDpToPixel(int dp, Context context) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    public static Bitmap arrayToBitmap(byte[] arr) {
        return BitmapFactory.decodeByteArray(arr, 0, arr.length);
    }
}
