package com.sgf.demo

import android.content.Context
import android.graphics.ImageFormat
import android.util.Size
import com.sgf.demo.config.ConfigKey
import com.sgf.demo.reader.*
import com.sgf.kcamera.CameraID
import com.sgf.kcamera.request.FlashState
import com.sgf.kcamera.request.PreviewRequest
import com.sgf.kcamera.surface.PreviewSurfaceProvider

class CameraRequest(private val ctx: Context) {
    private var fontImageReader: PreviewYuvImageReader? = null
    private var backImageReader: PreviewYuvImageReader? = null

    private var fontPreviewSize = Size(1280,960)
    private var fontYuvSize = Size(1280,960)
    private var fontPicSize = Size(1280,960)

    private var backPreviewSize = Size(1280,960)
    private var backYuvSize = Size(1280,960)
    private var backPicSize = Size(1280,960)
    fun reloadSize() {
        fontPreviewSize = ConfigKey.getSize(ConfigKey.FONT_PREVIEW_SIZE, ConfigKey.DEF_FONT_PREVIEW_SIZE)
        fontYuvSize = ConfigKey.getSize(ConfigKey.FONT_YUV_SIZE, ConfigKey.DEF_FONT_YUV_SIZE)
        fontPicSize = ConfigKey.getSize(ConfigKey.FONT_PIC_SIZE, ConfigKey.DEF_FONT_PIC_SIZE)

        backPreviewSize = ConfigKey.getSize(ConfigKey.BACK_PREVIEW_SIZE, ConfigKey.DEF_BACK_PREVIEW_SIZE)
        backYuvSize = ConfigKey.getSize(ConfigKey.BACK_YUV_SIZE, ConfigKey.DEF_BACK_YUV_SIZE)
        backPicSize = ConfigKey.getSize(ConfigKey.BACK_PIC_SIZE, ConfigKey.DEF_BACK_PIC_SIZE)
    }


    fun getFontFrameCount(): Int {
        return fontImageReader?.getFrameCount() ?: 0
    }

    fun getBackFrameCount(): Int {
        return backImageReader?.getFrameCount() ?: 0
    }

    fun getFontPreviewSize(): Size {
        return fontPreviewSize
    }

    fun getFontYuvSize() : Size {
        return fontYuvSize;
    }

    fun getFontPicSize() : Size {
        return fontPicSize;
    }


    fun getBackPreviewSize(): Size {
        return backPreviewSize
    }

    fun getBackYuvSize() : Size {
        return backYuvSize;
    }

    fun getBackPicSize() : Size {
        return backPicSize;
    }

    fun getBackRequest(provider : PreviewSurfaceProvider,  listener: ImageDataListener): PreviewRequest.Builder {
        backImageReader = PreviewYuvImageReader(backYuvSize, listener)
        val builder = PreviewRequest.createBuilder()
            .setPreviewSize(backPreviewSize)
            .openBackCamera()
            .addPreviewSurfaceProvider(provider)
            .setPictureSize(backPicSize, ImageFormat.YUV_420_888)
            .setFlash(FlashState.OFF)
            .setCustomerRequestStrategy(BackCustomerRequestStrategy())
        if (ConfigKey.getBoolean(ConfigKey.SHOW_PRE_YUV, false)) {
            builder.addSurfaceProvider(backImageReader)
        }

        setTakeBuild(builder, listener)

        return builder
    }


    fun getBackRequest(size: Size, provider : PreviewSurfaceProvider, listener: ImageDataListener): PreviewRequest.Builder {
        backPreviewSize = size
        backYuvSize = size
        backPicSize = size
        return getBackRequest(provider, listener)
    }

    fun getFontRequest(provider : PreviewSurfaceProvider, listener: ImageDataListener): PreviewRequest.Builder{
        fontImageReader = PreviewYuvImageReader(fontYuvSize,listener)
        val builder = PreviewRequest.createBuilder()
            .setPreviewSize(fontPreviewSize)
            .openFontCamera()
            .addPreviewSurfaceProvider(provider)
            .setPictureSize(fontPicSize, ImageFormat.JPEG)
            .setFlash(FlashState.OFF)
            .setCustomerRequestStrategy(FontCustomerRequestStrategy())
        if (ConfigKey.getBoolean(ConfigKey.SHOW_PRE_YUV, false)) {
            builder.addSurfaceProvider(fontImageReader)
        }

        setTakeBuild(builder, listener)
        return builder
    }

    fun getFontRequest(size: Size, provider : PreviewSurfaceProvider, listener: ImageDataListener): PreviewRequest.Builder {
        fontPreviewSize = size
        fontYuvSize = size
        fontPicSize = size
        return getFontRequest(provider, listener)
    }

    private fun setTakeBuild(builder: PreviewRequest.Builder,listener: ImageDataListener) {
        if (ConfigKey.getBoolean(ConfigKey.TAKE_JPEG_PIC, false)) {
            builder.addSurfaceProvider(CaptureJPEGImageReader(listener))
        }

        if (ConfigKey.getBoolean(ConfigKey.TAKE_YUV_TO_JPEG_PIC, false)) {
            builder.addSurfaceProvider(CaptureYUVImageReader(listener))
        }
        if (ConfigKey.getBoolean(ConfigKey.TAKE_PNG_PIC, false)) {
            builder.addSurfaceProvider(CapturePNGImageReader(listener))
        }
    }
}