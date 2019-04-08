package net.masonapps.zombiecamera.ar

import android.content.Context
import android.graphics.*
import com.google.ar.sceneform.rendering.ExternalTexture
import net.masonapps.zombiecamera.TextureAsset

private const val CANVAS_SIZE = 1024
private const val SIZE = CANVAS_SIZE.toFloat()

private const val EYES_WIDTH = 768f
private const val EYES_HEIGHT = 256f
private const val EYES_OFFSET_X = (SIZE - EYES_WIDTH) * 0.5f
private const val EYES_OFFSET_Y = 384f - 120f

private const val MOUTH_WIDTH = 384f
private const val MOUTH_HEIGHT = 384f
private const val MOUTH_OFFSET_X = (SIZE - MOUTH_WIDTH) * 0.5f
private const val MOUTH_OFFSET_Y = 320f + 210f

class FaceOverlaySurface(private val context: Context) {

    val externalTexture: ExternalTexture = ExternalTexture()
    var eyeAsset: TextureAsset? = null
    var mouthAsset: TextureAsset? = null
    var skinAsset: TextureAsset? = null

    private val bitmapPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val eyeRect = RectF(EYES_OFFSET_X, EYES_OFFSET_Y, EYES_OFFSET_X + EYES_WIDTH, EYES_OFFSET_Y + EYES_HEIGHT)
    private val mouthRect =
        RectF(MOUTH_OFFSET_X, MOUTH_OFFSET_Y, MOUTH_OFFSET_X + MOUTH_WIDTH, MOUTH_OFFSET_Y + MOUTH_HEIGHT)
    private val skinRect = RectF(0f, 0f, SIZE, SIZE)

    init {
        externalTexture.surfaceTexture.setDefaultBufferSize(CANVAS_SIZE, CANVAS_SIZE)
        updateSurfaceTexture()
    }

    fun updateSurfaceTexture() {
        val canvas = externalTexture.surface.lockHardwareCanvas()
        canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        drawToCanvas(canvas, skinRect, skinAsset)
        drawToCanvas(canvas, eyeRect, eyeAsset)
        drawToCanvas(canvas, mouthRect, mouthAsset)
        externalTexture.surface.unlockCanvasAndPost(canvas)
    }

    private fun drawToCanvas(canvas: Canvas, rect: RectF, asset: TextureAsset?) {
        if (asset == null) return
        val bitmap = context.assets.open(asset.assetPath).use { BitmapFactory.decodeStream(it) }
        canvas.drawBitmap(bitmap, null, rect, bitmapPaint)
        bitmap.recycle()
    }
}