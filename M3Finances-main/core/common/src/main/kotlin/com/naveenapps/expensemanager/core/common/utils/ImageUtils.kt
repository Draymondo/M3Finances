package com.naveenapps.expensemanager.core.common.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File

object ImageUtils {
    fun resizeAndCompressImage(
        context: Context,
        uri: Uri,
        destFile: File,
        maxDimension: Int = 1280,
        quality: Int = 80
    ): ByteArray? {
        try {
            // 1. Decode bounds
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { 
                BitmapFactory.decodeStream(it, null, options) 
            }

            // 2. Calculate sample size
            var inSampleSize = 1
            val height: Int = options.outHeight
            val width: Int = options.outWidth
            if (height > maxDimension || width > maxDimension) {
                val halfHeight: Int = height / 2
                val halfWidth: Int = width / 2
                while (halfHeight / inSampleSize >= maxDimension && halfWidth / inSampleSize >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            // 3. Decode with sample size
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            val sampledBitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            // 4. Exact scale
            val ratio = Math.min(maxDimension.toFloat() / sampledBitmap.width, maxDimension.toFloat() / sampledBitmap.height)
            val finalBitmap = if (ratio < 1f) {
                val scaled = Bitmap.createScaledBitmap(
                    sampledBitmap,
                    (sampledBitmap.width * ratio).toInt(),
                    (sampledBitmap.height * ratio).toInt(),
                    true
                )
                if (scaled != sampledBitmap) sampledBitmap.recycle()
                scaled
            } else {
                sampledBitmap
            }

            // 5. Compress to file & byte array
            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val bytes = outputStream.toByteArray()
            
            destFile.writeBytes(bytes)
            finalBitmap.recycle()
            
            return bytes
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getResizedBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1280
    ): Bitmap? {
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { 
                BitmapFactory.decodeStream(it, null, options) 
            }

            var inSampleSize = 1
            val height: Int = options.outHeight
            val width: Int = options.outWidth
            if (height > maxDimension || width > maxDimension) {
                val halfHeight: Int = height / 2
                val halfWidth: Int = width / 2
                while (halfHeight / inSampleSize >= maxDimension && halfWidth / inSampleSize >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            val sampledBitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            val ratio = Math.min(maxDimension.toFloat() / sampledBitmap.width, maxDimension.toFloat() / sampledBitmap.height)
            return if (ratio < 1f) {
                val scaled = Bitmap.createScaledBitmap(
                    sampledBitmap,
                    (sampledBitmap.width * ratio).toInt(),
                    (sampledBitmap.height * ratio).toInt(),
                    true
                )
                if (scaled != sampledBitmap) sampledBitmap.recycle()
                scaled
            } else {
                sampledBitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
