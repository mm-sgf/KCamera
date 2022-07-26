package com.sgf.demo.reader

import com.sgf.kcamera.surface.ImageReaderProvider
import com.sgf.kcamera.log.KLog
import android.graphics.ImageFormat
import android.media.Image
import android.media.ImageReader
import android.os.Environment
import android.util.Size
import com.sgf.demo.config.ConfigKey
import com.sgf.demo.utils.FilePathUtils
import com.sgf.demo.utils.ImageUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CaptureJPEGImageReader(private var picSize : Size , private var listener: ImageDataListener? = null) : ImageReaderProvider(TYPE.CAPTURE) {
    override fun createImageReader(previewSize: Size, captureSize: Size): ImageReader {
        KLog.d("createImageReader: captureSize width:" + picSize.width + "  captureSize height:" + picSize.height)
        return ImageReader.newInstance(picSize.width, picSize.height, ImageFormat.JPEG, 2)
    }

    override fun onImageAvailable(reader: ImageReader) {
        val captureTime = System.currentTimeMillis()
        val format = SimpleDateFormat("'/PIC_JPEG'_yyyyMMdd_HHmmss'.jpeg'", Locale.getDefault())
        val fileName = format.format(Date())
        val filePath = FilePathUtils.getRootPath()
        FilePathUtils.checkFolder(filePath)
        KLog.d("createImageReader: pic file path:" + (filePath + fileName))
        ImageSaver(reader.acquireNextImage(), File(filePath + fileName), captureTime, listener).run()
    }

    private class ImageSaver(
        private val mImage: Image,
        private val mFile: File,
        private val captureTime : Long,
        private val listener: ImageDataListener? = null
    ) : Runnable {
        override fun run() {
            val buffer = mImage.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer[bytes]
            val bitmap = ImageUtil.getPicFromBytes(bytes)
            listener?.onCaptureBitmap(ConfigKey.SHOW_JPEG_VALUE, bitmap, mFile.path, captureTime)
            var output: FileOutputStream? = null
            try {
                output = FileOutputStream(mFile)
                output.write(bytes)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                mImage.close()
                if (null != output) {
                    try {
                        output.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}