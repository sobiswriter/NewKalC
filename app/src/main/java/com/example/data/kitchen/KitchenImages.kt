package com.example.data.kitchen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.media.ExifInterface
import android.graphics.Matrix

object KitchenImages {
    fun decode(context: Context, uri: Uri): Bitmap {
        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it,null,bounds) }
        require(bounds.outWidth>0 && bounds.outHeight>0) { "Choose a readable image." }
        val options=BitmapFactory.Options().apply {
            inSampleSize=1
            while(bounds.outWidth/inSampleSize>1600 || bounds.outHeight/inSampleSize>1600) inSampleSize*=2
        }
        val bitmap=context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it,null,options) }?:error("The image could not be opened.")
        val orientation=context.contentResolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL) }?:ExifInterface.ORIENTATION_NORMAL
        val matrix=Matrix().apply { when(orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f,1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f,-1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { postScale(-1f,1f); postRotate(270f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { postScale(-1f,1f); postRotate(90f) }
        } }
        return if(matrix.isIdentity)bitmap else Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true).also { if(it!==bitmap)bitmap.recycle() }
    }
}
