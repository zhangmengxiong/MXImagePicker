package com.mx.imgpicker.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import java.io.File;

/**
 * 自定义Provider
 */
public class ImagePickerProvider extends FileProvider {
    public static Uri createUri(Context context, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return ImagePickerProvider.getUriForFile(context, context.getPackageName() + ".imagePickerProvider", file);
        } else {
            return Uri.parse("file://" + file.getAbsolutePath());
        }
    }
}
