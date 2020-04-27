/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package com.groupon.katmaps.katmaps_library.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.groupon.katmaps.katmaps_library.R
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Custom TextView for map pin labels that has 2 extra features:
 * 1. It supports text outline (via `outlineWidth` and `outlineColor` attrs)
 * 2. When using maxWidth on a regular TextView that's multiline, the TextView will always take up the maxWidth,
 * creating un-necessary padding on the sides - this has a custom onMeasure that ensures the view is only as wide as it needs to be
 */
internal class MapLabelTextView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyle: Int = 0) : AppCompatTextView(context, attrs, defStyle) {
    private var hasMaxWidth = false
    private var isDrawing = false

    private val outlineStrokeWidth: Float
    private val outlineColor: Int

    init {
        if (attrs != null) {
            val attrArray = context?.obtainStyledAttributes(attrs, R.styleable.MapLabelTextView)
            // multiply by 2 since the stroke is always centered around the line, but we really only care about the outside portion of the stroke,
            // the inside is going to be covered by the text's fill
            outlineStrokeWidth = (attrArray?.getDimension(R.styleable.MapLabelTextView_outlineWidth, 0f) ?: 0f) * 2
            outlineColor = attrArray?.getColor(R.styleable.MapLabelTextView_outlineColor, 0) ?: 0
            attrArray?.recycle()
        } else {
            outlineStrokeWidth = 0f
            outlineColor = 0
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (!hasMaxWidth || MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            return
        }

        if (layout.lineCount > 1) {
            var textRealMaxWidth = 0f
            for (n in 0 until layout.lineCount) {
                textRealMaxWidth = max(textRealMaxWidth, layout.getLineWidth(n))
            }
            val width = textRealMaxWidth.roundToInt()
            if (width < measuredWidth) {
                super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), heightMeasureSpec)
            }
        }
    }

    override fun invalidate() {
        if (isDrawing) return
        super.invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        isDrawing = true
        val currentTextColor = currentTextColor

        paint.strokeWidth = outlineStrokeWidth
        paint.style = Paint.Style.STROKE
        setTextColor(outlineColor)
        super.onDraw(canvas)

        setTextColor(currentTextColor)

        paint.style = Paint.Style.FILL
        super.onDraw(canvas)

        isDrawing = false
    }

    override fun setMaxWidth(maxpixels: Int) {
        super.setMaxWidth(maxpixels)
        hasMaxWidth = true
    }

    override fun setMaxEms(maxems: Int) {
        super.setMaxEms(maxems)
        hasMaxWidth = true
    }
}
