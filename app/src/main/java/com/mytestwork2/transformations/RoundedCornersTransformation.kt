package com.mytestwork2.transformations

import android.graphics.*
import com.squareup.picasso.Transformation

class RoundedCornersTransformation(private val radius: Float) : Transformation {

    override fun transform(source: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint().apply {
            isAntiAlias = true
            shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }

        val rect = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
        canvas.drawRoundRect(rect, radius, radius, paint)

        if (source != output) {
            source.recycle() // Recycle the source bitmap if it's not the same as the output
        }

        return output
    }

    override fun key(): String {
        return "rounded_corners(radius=$radius)"
    }
}