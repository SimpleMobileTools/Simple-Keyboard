package com.simplemobiletools.keyboard.views

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnDrawListener
import android.widget.FrameLayout
import android.widget.inline.InlineContentView
import androidx.annotation.AttrRes
import androidx.annotation.RequiresApi
import androidx.collection.ArraySet

/**
 * This class is a container for showing [InlineContentView]s for cases
 * where you want to ensure they appear only in a given area in your app. An
 * example is having a scrollable list of items. Note that without this container
 * the InlineContentViews' surfaces would cover parts of your app as these surfaces
 * are owned by another process and always appearing on top of your app.
 */
@RequiresApi(api = Build.VERSION_CODES.R)
class InlineContentClipView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? =  /*attrs*/null,
    @AttrRes defStyleAttr: Int =  /*defStyleAttr*/0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val clippedDescendants = ArraySet<InlineContentView>()
    private val onDrawListener = OnDrawListener { this.clipDescendantInlineContentViews() }
    private val parentBounds = Rect()
    private val contentBounds = Rect()
    private val backgroundView: SurfaceView
    private var backgroundColor = 0

    init {
        backgroundView = SurfaceView(context)
        backgroundView.setZOrderOnTop(true)
        backgroundView.holder.setFormat(PixelFormat.TRANSPARENT)
        backgroundView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        backgroundView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                drawBackgroundColorIfReady()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder, format: Int, width: Int,
                height: Int
            ) { /*do nothing*/
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                /*do nothing*/
            }
        })
        addView(backgroundView)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnDrawListener(onDrawListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewTreeObserver.removeOnDrawListener(onDrawListener)
    }

    override fun setBackgroundColor(color: Int) {
        backgroundColor = color
        Choreographer.getInstance()
            .postFrameCallback { drawBackgroundColorIfReady() }
    }

    private fun drawBackgroundColorIfReady() {
        val surface = backgroundView.holder.surface
        if (surface.isValid) {
            val canvas = surface.lockCanvas(null)
            try {
                canvas.drawColor(backgroundColor)
            } finally {
                surface.unlockCanvasAndPost(canvas)
            }
        }
    }

    /**
     * Sets whether the surfaces of the [InlineContentView]s wrapped by this view
     * should appear on top or behind this view's window. Normally, they are placed on top
     * of the window, to allow interaction ith the embedded UI. Via this method, you can
     * place the surface below the window. This means that all of the contents of the window
     * this view is in will be visible on top of the  [InlineContentView]s' surfaces.
     *
     * @param onTop Whether to show the surface on top of this view's window.
     * @see InlineContentView
     *
     * @see InlineContentView.setZOrderedOnTop
     */
    fun setZOrderedOnTop(onTop: Boolean) {
        backgroundView.setZOrderOnTop(onTop)
        for (inlineContentView in clippedDescendants) {
            inlineContentView.isZOrderedOnTop = onTop
        }
    }

    private fun clipDescendantInlineContentViews() {
        parentBounds.right = width
        parentBounds.bottom = height
        clippedDescendants.clear()
        clipDescendantInlineContentViews(this)
    }

    private fun clipDescendantInlineContentViews(root: View?) {
        if (root == null) {
            return
        }
        if (root is InlineContentView) {
            contentBounds.set(parentBounds)
            offsetRectIntoDescendantCoords(root, contentBounds)
            root.clipBounds = contentBounds
            clippedDescendants.add(root)
            return
        }
        if (root is ViewGroup) {
            val childCount = root.childCount
            for (i in 0 until childCount) {
                val child = root.getChildAt(i)
                clipDescendantInlineContentViews(child)
            }
        }
    }
}
