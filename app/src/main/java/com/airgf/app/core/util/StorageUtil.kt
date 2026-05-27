package com.airgf.app.core.util

import android.content.Context
import android.os.StatFs

object StorageUtil {

    fun hasMinimumFreeSpace(context: Context, minBytes: Long): Boolean {
        return getAvailableBytes(context) >= minBytes
    }

    fun getAvailableBytes(context: Context): Long {
        val statFs = StatFs(context.filesDir.absolutePath)
        return statFs.availableBlocksLong * statFs.blockSizeLong
    }
}
