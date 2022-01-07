package com.simplemobiletools.keyboard.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.Paint.Align
import android.graphics.drawable.Drawable
import android.inputmethodservice.Keyboard
import android.media.AudioManager
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.PopupWindow
import android.widget.TextView
import com.simplemobiletools.keyboard.R
import java.util.*

/**
 * A view that renders a virtual [Keyboard]. It handles rendering of keys and
 * detecting key presses and touch movements.
 *
 * @attr ref android.R.styleable#KeyboardView_keyBackground
 * @attr ref android.R.styleable#KeyboardView_keyPreviewLayout
 * @attr ref android.R.styleable#KeyboardView_keyPreviewOffset
 * @attr ref android.R.styleable#KeyboardView_keyPreviewHeight
 * @attr ref android.R.styleable#KeyboardView_labelTextSize
 * @attr ref android.R.styleable#KeyboardView_keyTextSize
 * @attr ref android.R.styleable#KeyboardView_keyTextColor
 * @attr ref android.R.styleable#KeyboardView_verticalCorrection
 * @attr ref android.R.styleable#KeyboardView_popupLayout
 *
 */
class MyKeyboardView @JvmOverloads constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = R.attr.keyboardViewStyle, defStyleRes: Int = 0) :
    View(context, attrs, defStyleAttr, defStyleRes), View.OnClickListener {

    /**
     * Listener for virtual keyboard events.
     */
    interface OnKeyboardActionListener {
        /**
         * Called when the user presses a key. This is sent before the [.onKey] is called.
         * For keys that repeat, this is only called once.
         * @param primaryCode the unicode of the key being pressed. If the touch is not on a valid
         * key, the value will be zero.
         */
        fun onPress(primaryCode: Int)

        /**
         * Called when the user releases a key. This is sent after the [.onKey] is called.
         * For keys that repeat, this is only called once.
         * @param primaryCode the code of the key that was released
         */
        fun onRelease(primaryCode: Int)

        /**
         * Send a key press to the listener.
         * @param primaryCode this is the key that was pressed
         * @param keyCodes the codes for all the possible alternative keys
         * with the primary code being the first. If the primary key code is
         * a single character such as an alphabet or number or symbol, the alternatives
         * will include other characters that may be on the same key or adjacent keys.
         * These codes are useful to correct for accidental presses of a key adjacent to
         * the intended key.
         */
        fun onKey(primaryCode: Int, keyCodes: IntArray?)

        /**
         * Sends a sequence of characters to the listener.
         * @param text the sequence of characters to be displayed.
         */
        fun onText(text: CharSequence?)

        /**
         * Called when the user quickly moves the finger from right to left.
         */
        fun swipeLeft()

        /**
         * Called when the user quickly moves the finger from left to right.
         */
        fun swipeRight()

        /**
         * Called when the user quickly moves the finger from up to down.
         */
        fun swipeDown()

        /**
         * Called when the user quickly moves the finger from down to up.
         */
        fun swipeUp()
    }

    private var mKeyboard: Keyboard? = null
    private var mCurrentKeyIndex: Int = NOT_A_KEY

    private var mLabelTextSize = 0
    private var mKeyTextSize = 0
    private var mKeyTextColor = 0
    private var mShadowRadius = 0f
    private var mShadowColor = 0
    private val mBackgroundDimAmount: Float

    private var mPreviewText: TextView? = null
    private val mPreviewPopup: PopupWindow
    private var mPreviewTextSizeLarge = 0
    private var mPreviewOffset = 0
    private var mPreviewHeight = 0

    // Working variable
    private val mCoordinates = IntArray(2)
    private val mPopupKeyboard: PopupWindow
    private var mMiniKeyboardContainer: View? = null
    private var mMiniKeyboard: MyKeyboardView? = null
    private var mMiniKeyboardOnScreen = false
    private var mPopupParent: View
    private var mMiniKeyboardOffsetX = 0
    private var mMiniKeyboardOffsetY = 0
    private val mMiniKeyboardCache: MutableMap<Keyboard.Key, View?>
    private var mKeys = ArrayList<Keyboard.Key>()
    /**
     * Returns the [OnKeyboardActionListener] object.
     * @return the listener attached to this keyboard
     */
    /** Listener for [OnKeyboardActionListener].  */
    var onKeyboardActionListener: OnKeyboardActionListener? = null
    private var mVerticalCorrection = 0
    private var mProximityThreshold = 0
    private val mPreviewCentered = false
    /**
     * Returns the enabled state of the key feedback popup.
     * @return whether or not the key feedback popup is enabled
     * @see .setPreviewEnabled
     */
    /**
     * Enables or disables the key feedback popup. This is a popup that shows a magnified
     * version of the depressed key. By default the preview is enabled.
     * @param previewEnabled whether or not to enable the key feedback popup
     * @see .isPreviewEnabled
     */
    var isPreviewEnabled = true
    private val mShowTouchPoints = true
    private var mPopupPreviewX = 0
    private var mPopupPreviewY = 0
    private var mLastX = 0
    private var mLastY = 0
    private var mStartX = 0
    private var mStartY = 0
    /**
     * Returns true if proximity correction is enabled.
     */
    /**
     * When enabled, calls to [OnKeyboardActionListener.onKey] will include key
     * codes for adjacent keys.  When disabled, only the primary key code will be
     * reported.
     * @param enabled whether or not the proximity correction is enabled
     */
    var isProximityCorrectionEnabled = false
    private val mPaint: Paint
    private val mPadding: Rect
    private var mDownTime: Long = 0
    private var mLastMoveTime: Long = 0
    private var mLastKey = 0
    private var mLastCodeX = 0
    private var mLastCodeY = 0
    private var mCurrentKey: Int = NOT_A_KEY
    private var mDownKey: Int = NOT_A_KEY
    private var mLastKeyTime: Long = 0
    private var mCurrentKeyTime: Long = 0
    private val mKeyIndices = IntArray(12)
    private var mGestureDetector: GestureDetector? = null
    private var mPopupX = 0
    private var mPopupY = 0
    private var mRepeatKeyIndex: Int = NOT_A_KEY
    private var mPopupLayout = 0
    private var mAbortKey = false
    private var mInvalidatedKey: Keyboard.Key? = null
    private val mClipRegion = Rect(0, 0, 0, 0)
    private var mPossiblePoly = false
    private val mSwipeTracker: SwipeTracker = SwipeTracker()
    private val mSwipeThreshold: Int
    private val mDisambiguateSwipe: Boolean

    // Variables for dealing with multiple pointers
    private var mOldPointerCount = 1
    private var mOldPointerX = 0f
    private var mOldPointerY = 0f

    private var mKeyBackground: Drawable? = null
    private val mDistances = IntArray(MAX_NEARBY_KEYS)

    // For multi-tap
    private var mLastSentIndex = 0
    private var mTapCount = 0
    private var mLastTapTime: Long = 0
    private var mInMultiTap = false
    private val mPreviewLabel = StringBuilder(1)

    /** Whether the keyboard bitmap needs to be redrawn before it's blitted.  */
    private var mDrawPending = false

    /** The dirty region in the keyboard bitmap  */
    private val mDirtyRect = Rect()

    /** The keyboard bitmap for faster updates  */
    private var mBuffer: Bitmap? = null

    /** Notes if the keyboard just changed, so that we could possibly reallocate the mBuffer.  */
    private var mKeyboardChanged = false

    /** The canvas for the above mutable keyboard bitmap  */
    private var mCanvas: Canvas? = null

    /** The accessibility manager for accessibility support  */
    private val mAccessibilityManager: AccessibilityManager

    /** The audio manager for accessibility support  */
    private val mAudioManager: AudioManager

    var mHandler: Handler? = null

    companion object {
        private const val DEBUG = false
        private const val NOT_A_KEY = -1
        private val KEY_DELETE = intArrayOf(Keyboard.KEYCODE_DELETE)
        private val LONG_PRESSABLE_STATE_SET = intArrayOf(R.attr.state_long_pressable)
        private const val MSG_SHOW_PREVIEW = 1
        private const val MSG_REMOVE_PREVIEW = 2
        private const val MSG_REPEAT = 3
        private const val MSG_LONGPRESS = 4
        private const val DELAY_BEFORE_PREVIEW = 0
        private const val DELAY_AFTER_PREVIEW = 70
        private const val DEBOUNCE_TIME = 70
        private const val REPEAT_INTERVAL = 50 // ~20 keys per second
        private const val REPEAT_START_DELAY = 400
        private val LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout()
        private const val MAX_NEARBY_KEYS = 12
        private const val MULTITAP_INTERVAL = 800 // milliseconds
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.MyKeyboardView, defStyleAttr, defStyleRes)
        val inflate = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        var previewLayout = 0
        val keyTextSize = 0
        val n = a.indexCount
        for (i in 0 until n) {
            val attr = a.getIndex(i)
            when (attr) {
                R.styleable.MyKeyboardView_keyBackground -> mKeyBackground = a.getDrawable(attr)
                R.styleable.MyKeyboardView_verticalCorrection -> mVerticalCorrection = a.getDimensionPixelOffset(attr, 0)
                R.styleable.MyKeyboardView_keyPreviewLayout -> previewLayout = a.getResourceId(attr, 0)
                R.styleable.MyKeyboardView_keyPreviewOffset -> mPreviewOffset = a.getDimensionPixelOffset(attr, 0)
                R.styleable.MyKeyboardView_keyPreviewHeight -> mPreviewHeight = a.getDimensionPixelSize(attr, 80)
                R.styleable.MyKeyboardView_keyTextSize -> mKeyTextSize = a.getDimensionPixelSize(attr, 18)
                R.styleable.MyKeyboardView_keyTextColor -> mKeyTextColor = a.getColor(attr, -0x1000000)
                R.styleable.MyKeyboardView_labelTextSize -> mLabelTextSize = a.getDimensionPixelSize(attr, 14)
                R.styleable.MyKeyboardView_popupLayout -> mPopupLayout = a.getResourceId(attr, 0)
                R.styleable.MyKeyboardView_shadowColor -> mShadowColor = a.getColor(attr, 0)
                R.styleable.MyKeyboardView_shadowRadius -> mShadowRadius = a.getFloat(attr, 0f)
            }
        }

        mBackgroundDimAmount = 0.5f
        mPreviewPopup = PopupWindow(context)
        if (previewLayout != 0) {
            mPreviewText = inflate.inflate(previewLayout, null) as TextView
            mPreviewTextSizeLarge = mPreviewText!!.textSize.toInt()
            mPreviewPopup.contentView = mPreviewText
            mPreviewPopup.setBackgroundDrawable(null)
        } else {
            isPreviewEnabled = false
        }

        mPreviewPopup.isTouchable = false
        mPopupKeyboard = PopupWindow(context)
        mPopupKeyboard.setBackgroundDrawable(null)
        //mPopupKeyboard.setClippingEnabled(false);
        mPopupParent = this
        //mPredicting = true;
        mPaint = Paint()
        mPaint.isAntiAlias = true
        mPaint.textSize = keyTextSize.toFloat()
        mPaint.textAlign = Align.CENTER
        mPaint.alpha = 255
        mPadding = Rect(0, 0, 0, 0)
        mMiniKeyboardCache = HashMap()
        mKeyBackground!!.getPadding(mPadding)
        mSwipeThreshold = (500 * resources.displayMetrics.density).toInt()
        mDisambiguateSwipe = false//resources.getBoolean(R.bool.config_swipeDisambiguation)
        mAccessibilityManager = (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager)
        mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        resetMultiTap()
    }

    @SuppressLint("HandlerLeak")
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        initGestureDetector()
        if (mHandler == null) {
            mHandler = object : Handler() {
                override fun handleMessage(msg: Message) {
                    when (msg.what) {
                        MSG_SHOW_PREVIEW -> showKey(msg.arg1)
                        MSG_REMOVE_PREVIEW -> mPreviewText!!.visibility = INVISIBLE
                        MSG_REPEAT -> if (repeatKey()) {
                            val repeat: Message = Message.obtain(this, MSG_REPEAT)
                            sendMessageDelayed(repeat, REPEAT_INTERVAL.toLong())
                        }
                        MSG_LONGPRESS -> openPopupIfRequired(msg.obj as MotionEvent)
                    }
                }
            }
        }
    }

    private fun initGestureDetector() {
        if (mGestureDetector == null) {
            mGestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
                override fun onFling(me1: MotionEvent, me2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                    if (mPossiblePoly) {
                        return false
                    }

                    val absX = Math.abs(velocityX)
                    val absY = Math.abs(velocityY)
                    val deltaX = me2.x - me1.x
                    val deltaY = me2.y - me1.y
                    val travelX = width / 2 // Half the keyboard width
                    val travelY = height / 2 // Half the keyboard height
                    mSwipeTracker.computeCurrentVelocity(1000)
                    val endingVelocityX: Float = mSwipeTracker.xVelocity
                    val endingVelocityY: Float = mSwipeTracker.yVelocity
                    var sendDownKey = false
                    if (velocityX > mSwipeThreshold && absY < absX && deltaX > travelX) {
                        sendDownKey = if (mDisambiguateSwipe && endingVelocityX < velocityX / 4) {
                            true
                        } else {
                            swipeRight()
                            return true
                        }
                    } else if (velocityX < -mSwipeThreshold && absY < absX && deltaX < -travelX) {
                        sendDownKey = if (mDisambiguateSwipe && endingVelocityX > velocityX / 4) {
                            true
                        } else {
                            swipeLeft()
                            return true
                        }
                    } else if (velocityY < -mSwipeThreshold && absX < absY && deltaY < -travelY) {
                        sendDownKey = if (mDisambiguateSwipe && endingVelocityY > velocityY / 4) {
                            true
                        } else {
                            swipeUp()
                            return true
                        }
                    } else if (velocityY > mSwipeThreshold && absX < absY / 2 && deltaY > travelY) {
                        sendDownKey = if (mDisambiguateSwipe && endingVelocityY < velocityY / 4) {
                            true
                        } else {
                            swipeDown()
                            return true
                        }
                    }
                    if (sendDownKey) {
                        detectAndSendKey(mDownKey, mStartX, mStartY, me1.eventTime)
                    }
                    return false
                }
            })
            mGestureDetector!!.setIsLongpressEnabled(false)
        }
    }

    /**
     * Attaches a keyboard to this view. The keyboard can be switched at any time and the
     * view will re-layout itself to accommodate the keyboard.
     * @see Keyboard
     *
     * @see .getKeyboard
     * @param keyboard the keyboard to display in this view
     */
    var keyboard: Keyboard?
        get() = mKeyboard
        set(keyboard) {
            if (mKeyboard != null) {
                showPreview(NOT_A_KEY)
            }

            // Remove any pending messages
            removeMessages()
            mKeyboard = keyboard
            val keys = mKeyboard!!.keys
            mKeys = keys.toMutableList() as ArrayList<Keyboard.Key>
            requestLayout()
            // Hint to reallocate the buffer if the size changed
            mKeyboardChanged = true
            invalidateAllKeys()
            computeProximityThreshold(keyboard)
            mMiniKeyboardCache.clear()
            // Not really necessary to do every time, but will free up views
            // Switching to a different keyboard should abort any pending keys so that the key up
            // doesn't get delivered to the old or new keyboard
            mAbortKey = true // Until the next ACTION_DOWN
        }

    /**
     * Sets the state of the shift key of the keyboard, if any.
     * @param shifted whether or not to enable the state of the shift key
     * @return true if the shift key state changed, false if there was no change
     * @see KeyboardView.isShifted
     */
    fun setShifted(shifted: Boolean): Boolean {
        if (mKeyboard != null) {
            if (mKeyboard!!.setShifted(shifted)) {
                // The whole keyboard probably needs to be redrawn
                invalidateAllKeys()
                return true
            }
        }
        return false
    }

    /**
     * Returns the state of the shift key of the keyboard, if any.
     * @return true if the shift is in a pressed state, false otherwise. If there is
     * no shift key on the keyboard or there is no keyboard attached, it returns false.
     * @see KeyboardView.setShifted
     */
    var isShifted: Boolean = false
        get() = if (mKeyboard != null) {
            mKeyboard!!.isShifted
        } else {
            false
        }

    fun setPopupParent(v: View) {
        mPopupParent = v
    }

    fun setPopupOffset(x: Int, y: Int) {
        mMiniKeyboardOffsetX = x
        mMiniKeyboardOffsetY = y
        if (mPreviewPopup.isShowing) {
            mPreviewPopup.dismiss()
        }
    }

    /**
     * Popup keyboard close button clicked.
     * @hide
     */
    override fun onClick(v: View) {
        dismissPopupKeyboard()
    }

    private fun adjustCase(label: CharSequence): CharSequence? {
        var newLabel: CharSequence? = label
        if (mKeyboard!!.isShifted && newLabel != null && newLabel.length < 3 && Character.isLowerCase(newLabel[0])) {
            newLabel = newLabel.toString().toUpperCase()
        }
        return newLabel
    }

    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Round up a little
        if (mKeyboard == null) {
            setMeasuredDimension(paddingLeft + paddingRight, paddingTop + paddingBottom)
        } else {
            var width: Int = mKeyboard!!.minWidth + paddingLeft + paddingRight
            if (MeasureSpec.getSize(widthMeasureSpec) < width + 10) {
                width = MeasureSpec.getSize(widthMeasureSpec)
            }
            setMeasuredDimension(width, mKeyboard!!.height + paddingTop + paddingBottom)
        }
    }

    /**
     * Compute the average distance between adjacent keys (horizontally and vertically)
     * and square it to get the proximity threshold. We use a square here and in computing
     * the touch distance from a key's center to avoid taking a square root.
     * @param keyboard
     */
    private fun computeProximityThreshold(keyboard: Keyboard?) {
        if (keyboard == null) {
            return
        }

        val keys = mKeys
        val length = keys.size
        var dimensionSum = 0
        for (i in 0 until length) {
            val key = keys[i]
            dimensionSum += Math.min(key.width, key.height) + key.gap
        }

        if (dimensionSum < 0 || length == 0) {
            return
        }

        mProximityThreshold = (dimensionSum * 1.4f / length).toInt()
        mProximityThreshold *= mProximityThreshold // Square it
    }

    public override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (mKeyboard != null) {
            //mKeyboard.resize(w, h)
        }
        // Release the buffer, if any and it will be reallocated on the next draw
        mBuffer = null
    }

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mDrawPending || mBuffer == null || mKeyboardChanged) {
            onBufferDraw()
        }
        canvas.drawBitmap(mBuffer!!, 0f, 0f, null)
    }

    private fun onBufferDraw() {
        if (mBuffer == null || mKeyboardChanged) {
            if (mBuffer == null || mKeyboardChanged && (mBuffer!!.width != width || mBuffer!!.height != height)) {
                // Make sure our bitmap is at least 1x1
                val width = Math.max(1, width)
                val height = Math.max(1, height)
                mBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                mCanvas = Canvas(mBuffer!!)
            }
            invalidateAllKeys()
            mKeyboardChanged = false
        }

        if (mKeyboard == null) {
            return
        }

        mCanvas!!.save()
        val canvas = mCanvas
        canvas!!.clipRect(mDirtyRect)
        val paint = mPaint
        val keyBackground = mKeyBackground
        val clipRegion = mClipRegion
        val padding = mPadding
        val kbdPaddingLeft: Int = paddingLeft
        val kbdPaddingTop: Int = paddingTop
        val keys = mKeys
        val invalidKey = mInvalidatedKey
        paint.color = mKeyTextColor
        var drawSingleKey = false
        if (invalidKey != null && canvas.getClipBounds(clipRegion)) {
            // Is clipRegion completely contained within the invalidated key?
            if (invalidKey.x + kbdPaddingLeft - 1 <= clipRegion.left &&
                invalidKey.y + kbdPaddingTop - 1 <= clipRegion.top &&
                invalidKey.x + invalidKey.width + kbdPaddingLeft + 1 >= clipRegion.right &&
                invalidKey.y + invalidKey.height + kbdPaddingTop + 1 >= clipRegion.bottom
            ) {
                drawSingleKey = true
            }
        }

        canvas.drawColor(0x00000000, PorterDuff.Mode.CLEAR)
        val keyCount = keys.size
        for (i in 0 until keyCount) {
            val key = keys[i]
            if (drawSingleKey && invalidKey !== key) {
                continue
            }

            val drawableState = key.currentDrawableState
            keyBackground!!.state = drawableState

            // Switch the character to uppercase if shift is pressed
            val label = if (key.label == null) {
                null
            } else {
                adjustCase(key.label).toString()
            }

            val bounds = keyBackground.bounds
            if (key.width != bounds.right || key.height != bounds.bottom) {
                keyBackground.setBounds(0, 0, key.width, key.height)
            }

            canvas.translate((key.x + kbdPaddingLeft).toFloat(), (key.y + kbdPaddingTop).toFloat())
            keyBackground.draw(canvas)
            if (label != null) {
                // For characters, use large font. For labels like "Done", use small font.
                if (label.length > 1 && key.codes.size < 2) {
                    paint.textSize = mLabelTextSize.toFloat()
                    paint.typeface = Typeface.DEFAULT_BOLD
                } else {
                    paint.textSize = mKeyTextSize.toFloat()
                    paint.typeface = Typeface.DEFAULT
                }

                // Draw a drop shadow for the text
                paint.setShadowLayer(mShadowRadius, 0f, 0f, mShadowColor)
                // Draw the text
                canvas.drawText(
                    label, ((key.width - padding.left - padding.right) / 2 + padding.left).toFloat(),
                    (key.height - padding.top - padding.bottom) / 2 + (paint.textSize - paint.descent()) / 2 + padding.top, paint
                )
                // Turn off drop shadow
                paint.setShadowLayer(0f, 0f, 0f, 0)
            } else if (key.icon != null) {
                val drawableX = (key.width - padding.left - padding.right - key.icon.intrinsicWidth) / 2 + padding.left
                val drawableY = (key.height - padding.top - padding.bottom - key.icon.intrinsicHeight) / 2 + paddingTop
                canvas.translate(drawableX.toFloat(), drawableY.toFloat())
                key.icon.setBounds(0, 0, key.icon.intrinsicWidth, key.icon.intrinsicHeight)
                key.icon.draw(canvas)
                canvas.translate(-drawableX.toFloat(), -drawableY.toFloat())
            }
            canvas.translate((-key.x - kbdPaddingLeft).toFloat(), (-key.y - kbdPaddingTop).toFloat())
        }
        mInvalidatedKey = null
        // Overlay a dark rectangle to dim the keyboard
        if (mMiniKeyboardOnScreen) {
            paint.color = (mBackgroundDimAmount * 0xFF).toInt() shl 24
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
        if (DEBUG && mShowTouchPoints) {
            paint.alpha = 128
            paint.color = -0x10000
            canvas.drawCircle(mStartX.toFloat(), mStartY.toFloat(), 3f, paint)
            canvas.drawLine(mStartX.toFloat(), mStartY.toFloat(), mLastX.toFloat(), mLastY.toFloat(), paint)
            paint.color = -0xffff01
            canvas.drawCircle(mLastX.toFloat(), mLastY.toFloat(), 3f, paint)
            paint.color = -0xff0100
            canvas.drawCircle(((mStartX + mLastX) / 2).toFloat(), ((mStartY + mLastY) / 2).toFloat(), 2f, paint)
        }
        mCanvas!!.restore()
        mDrawPending = false
        mDirtyRect.setEmpty()
    }

    private fun getKeyIndices(x: Int, y: Int, allKeys: IntArray?): Int {
        val keys = mKeys
        var primaryIndex = NOT_A_KEY
        var closestKey = NOT_A_KEY
        var closestKeyDist = mProximityThreshold + 1
        Arrays.fill(mDistances, Int.MAX_VALUE)
        val nearestKeyIndices = mKeyboard!!.getNearestKeys(x, y)
        val keyCount = nearestKeyIndices.size

        for (i in 0 until keyCount) {
            val key = keys[nearestKeyIndices[i]]
            var dist = 0
            val isInside = key.isInside(x, y)
            if (isInside) {
                primaryIndex = nearestKeyIndices[i]
            }

            if (((isProximityCorrectionEnabled && key.squaredDistanceFrom(x, y).also {
                    dist = it
                } < mProximityThreshold) || isInside) && key.codes[0] > 32) {
                // Find insertion point
                val nCodes = key.codes.size
                if (dist < closestKeyDist) {
                    closestKeyDist = dist
                    closestKey = nearestKeyIndices[i]
                }

                if (allKeys == null) {
                    continue
                }

                for (j in mDistances.indices) {
                    if (mDistances[j] > dist) {
                        // Make space for nCodes codes
                        System.arraycopy(
                            mDistances, j, mDistances, j + nCodes,
                            mDistances.size - j - nCodes
                        )
                        System.arraycopy(
                            allKeys, j, allKeys, j + nCodes,
                            allKeys.size - j - nCodes
                        )

                        for (c in 0 until nCodes) {
                            allKeys[j + c] = key.codes[c]
                            mDistances[j + c] = dist
                        }
                        break
                    }
                }
            }
        }

        if (primaryIndex == NOT_A_KEY) {
            primaryIndex = closestKey
        }

        return primaryIndex
    }

    private fun detectAndSendKey(index: Int, x: Int, y: Int, eventTime: Long) {
        if (index != NOT_A_KEY && index < mKeys.size) {
            val key = mKeys[index]
            if (key.text != null) {
                onKeyboardActionListener!!.onText(key.text)
                onKeyboardActionListener!!.onRelease(NOT_A_KEY)
            } else {
                var code = key.codes[0]
                // TextEntryState.keyPressedAt(key, x, y);
                val codes = IntArray(MAX_NEARBY_KEYS)
                Arrays.fill(codes, NOT_A_KEY)
                getKeyIndices(x, y, codes)
                // Multi-tap
                if (mInMultiTap) {
                    if (mTapCount != -1) {
                        onKeyboardActionListener!!.onKey(Keyboard.KEYCODE_DELETE, KEY_DELETE)
                    } else {
                        mTapCount = 0
                    }
                    code = key.codes[mTapCount]
                }
                onKeyboardActionListener!!.onKey(code, codes)
                onKeyboardActionListener!!.onRelease(code)
            }
            mLastSentIndex = index
            mLastTapTime = eventTime
        }
    }

    /**
     * Handle multi-tap keys by producing the key label for the current multi-tap state.
     */
    private fun getPreviewText(key: Keyboard.Key): CharSequence? {
        return if (mInMultiTap) {
            // Multi-tap
            mPreviewLabel.setLength(0)
            val codeTapCount = if (mTapCount < 0) {
                0
            } else {
                mTapCount
            }

            mPreviewLabel.append(key.codes[codeTapCount].toChar())
            adjustCase(mPreviewLabel)
        } else {
            adjustCase(key.label)
        }
    }

    private fun showPreview(keyIndex: Int) {
        val oldKeyIndex = mCurrentKeyIndex
        val previewPopup = mPreviewPopup
        mCurrentKeyIndex = keyIndex
        // Release the old key and press the new key
        val keys = mKeys
        if (oldKeyIndex != mCurrentKeyIndex) {
            if (oldKeyIndex != NOT_A_KEY && keys.size > oldKeyIndex) {
                val oldKey = keys[oldKeyIndex]
                oldKey.onReleased(mCurrentKeyIndex == NOT_A_KEY)
                invalidateKey(oldKeyIndex)
                val keyCode = oldKey.codes[0]
                sendAccessibilityEventForUnicodeCharacter(
                    AccessibilityEvent.TYPE_VIEW_HOVER_EXIT,
                    keyCode
                )
                // TODO: We need to implement AccessibilityNodeProvider for this view.
                sendAccessibilityEventForUnicodeCharacter(
                    AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED, keyCode
                )
            }
            if (mCurrentKeyIndex != NOT_A_KEY && keys.size > mCurrentKeyIndex) {
                val newKey = keys[mCurrentKeyIndex]
                newKey.onPressed()
                invalidateKey(mCurrentKeyIndex)
                val keyCode = newKey.codes[0]
                sendAccessibilityEventForUnicodeCharacter(AccessibilityEvent.TYPE_VIEW_HOVER_ENTER, keyCode)
                // TODO: We need to implement AccessibilityNodeProvider for this view.
                sendAccessibilityEventForUnicodeCharacter(
                    AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED, keyCode
                )
            }
        }

        // If key changed and preview is on ...
        if (oldKeyIndex != mCurrentKeyIndex && isPreviewEnabled) {
            mHandler!!.removeMessages(MSG_SHOW_PREVIEW)
            if (previewPopup.isShowing) {
                if (keyIndex == NOT_A_KEY) {
                    mHandler!!.sendMessageDelayed(
                        mHandler!!.obtainMessage(MSG_REMOVE_PREVIEW),
                        DELAY_AFTER_PREVIEW.toLong()
                    )
                }
            }

            if (keyIndex != NOT_A_KEY) {
                if (previewPopup.isShowing && mPreviewText!!.visibility == VISIBLE) {
                    // Show right away, if it's already visible and finger is moving around
                    showKey(keyIndex)
                } else {
                    mHandler!!.sendMessageDelayed(
                        mHandler!!.obtainMessage(MSG_SHOW_PREVIEW, keyIndex, 0),
                        DELAY_BEFORE_PREVIEW.toLong()
                    )
                }
            }
        }
    }

    private fun showKey(keyIndex: Int) {
        val previewPopup = mPreviewPopup
        val keys = mKeys
        if (keyIndex < 0 || keyIndex >= mKeys.size) {
            return
        }

        val key = keys[keyIndex]
        if (key.icon != null) {
            val bottomDrawable = if (key.iconPreview != null) {
                key.iconPreview
            } else {
                key.icon
            }
            mPreviewText!!.setCompoundDrawables(null, null, null, bottomDrawable)
            mPreviewText!!.text = null
        } else {
            mPreviewText!!.setCompoundDrawables(null, null, null, null)
            mPreviewText!!.text = getPreviewText(key)
            if (key.label.length > 1 && key.codes.size < 2) {
                mPreviewText!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, mKeyTextSize.toFloat())
                mPreviewText!!.typeface = Typeface.DEFAULT_BOLD
            } else {
                mPreviewText!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, mPreviewTextSizeLarge.toFloat())
                mPreviewText!!.typeface = Typeface.DEFAULT
            }
        }

        mPreviewText!!.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        val popupWidth = Math.max(mPreviewText!!.measuredWidth, key.width + mPreviewText!!.paddingLeft + mPreviewText!!.paddingRight)
        val popupHeight = mPreviewHeight
        val lp = mPreviewText!!.layoutParams
        if (lp != null) {
            lp.width = popupWidth
            lp.height = popupHeight
        }

        if (!mPreviewCentered) {
            mPopupPreviewX = key.x - mPreviewText!!.paddingLeft + paddingLeft
            mPopupPreviewY = key.y - popupHeight + mPreviewOffset
        } else {
            // TODO: Fix this if centering is brought back
            mPopupPreviewX = 160 - mPreviewText!!.measuredWidth / 2
            mPopupPreviewY = -mPreviewText!!.measuredHeight
        }

        mHandler!!.removeMessages(MSG_REMOVE_PREVIEW)
        getLocationInWindow(mCoordinates)
        mCoordinates[0] += mMiniKeyboardOffsetX // Offset may be zero
        mCoordinates[1] += mMiniKeyboardOffsetY // Offset may be zero

        // Set the preview background state
        mPreviewText!!.background.state = if (key.popupResId != 0) {
            LONG_PRESSABLE_STATE_SET
        } else {
            EMPTY_STATE_SET
        }

        mPopupPreviewX += mCoordinates[0]
        mPopupPreviewY += mCoordinates[1]

        // If the popup cannot be shown above the key, put it on the side
        getLocationOnScreen(mCoordinates)
        if (mPopupPreviewY + mCoordinates[1] < 0) {
            // If the key you're pressing is on the left side of the keyboard, show the popup on
            // the right, offset by enough to see at least one key to the left/right.
            if (key.x + key.width <= width / 2) {
                mPopupPreviewX += (key.width * 2.5).toInt()
            } else {
                mPopupPreviewX -= (key.width * 2.5).toInt()
            }
            mPopupPreviewY += popupHeight
        }

        if (previewPopup.isShowing) {
            previewPopup.update(
                mPopupPreviewX, mPopupPreviewY,
                popupWidth, popupHeight
            )
        } else {
            previewPopup.width = popupWidth
            previewPopup.height = popupHeight
            previewPopup.showAtLocation(mPopupParent, Gravity.NO_GRAVITY, mPopupPreviewX, mPopupPreviewY)
        }
        mPreviewText!!.visibility = VISIBLE
    }

    private fun sendAccessibilityEventForUnicodeCharacter(eventType: Int, code: Int) {
        if (mAccessibilityManager.isEnabled) {
            val event = AccessibilityEvent.obtain(eventType)
            onInitializeAccessibilityEvent(event)
            val text: String = when (code) {
                Keyboard.KEYCODE_ALT -> context.getString(R.string.keyboardview_keycode_alt)
                Keyboard.KEYCODE_CANCEL -> context.getString(R.string.keyboardview_keycode_cancel)
                Keyboard.KEYCODE_DELETE -> context.getString(R.string.keyboardview_keycode_delete)
                Keyboard.KEYCODE_DONE -> context.getString(R.string.keyboardview_keycode_done)
                Keyboard.KEYCODE_MODE_CHANGE -> context.getString(R.string.keyboardview_keycode_mode_change)
                Keyboard.KEYCODE_SHIFT -> context.getString(R.string.keyboardview_keycode_shift)
                '\n'.toInt() -> context.getString(R.string.keyboardview_keycode_enter)
                else -> code.toChar().toString()
            }
            event.text.add(text)
            mAccessibilityManager.sendAccessibilityEvent(event)
        }
    }

    /**
     * Requests a redraw of the entire keyboard. Calling [.invalidate] is not sufficient
     * because the keyboard renders the keys to an off-screen buffer and an invalidate() only
     * draws the cached buffer.
     * @see .invalidateKey
     */
    fun invalidateAllKeys() {
        mDirtyRect.union(0, 0, width, height)
        mDrawPending = true
        invalidate()
    }

    /**
     * Invalidates a key so that it will be redrawn on the next repaint. Use this method if only
     * one key is changing it's content. Any changes that affect the position or size of the key
     * may not be honored.
     * @param keyIndex the index of the key in the attached [Keyboard].
     * @see .invalidateAllKeys
     */
    fun invalidateKey(keyIndex: Int) {
        if (keyIndex < 0 || keyIndex >= mKeys.size) {
            return
        }

        val key = mKeys[keyIndex]
        mInvalidatedKey = key
        mDirtyRect.union(
            key.x + paddingLeft, key.y + paddingTop,
            key.x + key.width + paddingLeft, key.y + key.height + paddingTop
        )
        onBufferDraw()
        invalidate(
            key.x + paddingLeft, key.y + paddingTop,
            key.x + key.width + paddingLeft, key.y + key.height + paddingTop
        )
    }

    private fun openPopupIfRequired(me: MotionEvent): Boolean {
        // Check if we have a popup layout specified first.
        if (mPopupLayout == 0) {
            return false
        }

        if (mCurrentKey < 0 || mCurrentKey >= mKeys.size) {
            return false
        }

        val popupKey = mKeys[mCurrentKey]
        val result = onLongPress(popupKey)
        if (result) {
            mAbortKey = true
            showPreview(NOT_A_KEY)
        }

        return result
    }

    /**
     * Called when a key is long pressed. By default this will open any popup keyboard associated
     * with this key through the attributes popupLayout and popupCharacters.
     * @param popupKey the key that was long pressed
     * @return true if the long press is handled, false otherwise. Subclasses should call the
     * method on the base class if the subclass doesn't wish to handle the call.
     */
    protected fun onLongPress(popupKey: Keyboard.Key): Boolean {
        val popupKeyboardId = popupKey.popupResId
        if (popupKeyboardId != 0) {
            mMiniKeyboardContainer = mMiniKeyboardCache[popupKey]
            if (mMiniKeyboardContainer == null) {
                val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                mMiniKeyboardContainer = inflater.inflate(mPopupLayout, null)
                mMiniKeyboard = mMiniKeyboardContainer!!.findViewById<View>(R.id.keyboardView) as MyKeyboardView
                val closeButton = mMiniKeyboardContainer!!.findViewById<View>(R.id.closeButton)
                closeButton?.setOnClickListener(this)

                mMiniKeyboard!!.onKeyboardActionListener = object : OnKeyboardActionListener {
                    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
                        onKeyboardActionListener!!.onKey(primaryCode, keyCodes)
                        dismissPopupKeyboard()
                    }

                    override fun onText(text: CharSequence?) {
                        onKeyboardActionListener!!.onText(text)
                        dismissPopupKeyboard()
                    }

                    override fun swipeLeft() {}
                    override fun swipeRight() {}
                    override fun swipeUp() {}
                    override fun swipeDown() {}
                    override fun onPress(primaryCode: Int) {
                        onKeyboardActionListener!!.onPress(primaryCode)
                    }

                    override fun onRelease(primaryCode: Int) {
                        onKeyboardActionListener!!.onRelease(primaryCode)
                    }
                }

                //mInputView.setSuggest(mSuggest);
                val keyboard: Keyboard = if (popupKey.popupCharacters != null) {
                    Keyboard(context, popupKeyboardId, popupKey.popupCharacters, -1, paddingLeft + paddingRight)
                } else {
                    Keyboard(context, popupKeyboardId)
                }

                mMiniKeyboard!!.keyboard = keyboard
                mMiniKeyboard!!.setPopupParent(this)
                mMiniKeyboardContainer!!.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST)
                )
                mMiniKeyboardCache[popupKey] = mMiniKeyboardContainer
            } else {
                mMiniKeyboard = mMiniKeyboardContainer!!.findViewById<View>(R.id.keyboardView) as MyKeyboardView
            }

            getLocationInWindow(mCoordinates)
            mPopupX = popupKey.x + paddingLeft
            mPopupY = popupKey.y + paddingTop
            mPopupX = mPopupX + popupKey.width - mMiniKeyboardContainer!!.measuredWidth
            mPopupY -= mMiniKeyboardContainer!!.measuredHeight
            val x = mPopupX + mMiniKeyboardContainer!!.paddingRight + mCoordinates[0]
            val y = mPopupY + mMiniKeyboardContainer!!.paddingBottom + mCoordinates[1]
            mMiniKeyboard!!.setPopupOffset(if (x < 0) 0 else x, y)
            mMiniKeyboard!!.isShifted = isShifted
            mPopupKeyboard.contentView = mMiniKeyboardContainer
            mPopupKeyboard.width = mMiniKeyboardContainer!!.measuredWidth
            mPopupKeyboard.height = mMiniKeyboardContainer!!.measuredHeight
            mPopupKeyboard.showAtLocation(this, Gravity.NO_GRAVITY, x, y)
            mMiniKeyboardOnScreen = true
            //mMiniKeyboard.onTouchEvent(getTranslatedEvent(me));
            invalidateAllKeys()
            return true
        }
        return false
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        if (mAccessibilityManager.isTouchExplorationEnabled && event.pointerCount == 1) {
            when (event.action) {
                MotionEvent.ACTION_HOVER_ENTER -> event.action = MotionEvent.ACTION_DOWN
                MotionEvent.ACTION_HOVER_MOVE -> event.action = MotionEvent.ACTION_MOVE
                MotionEvent.ACTION_HOVER_EXIT -> event.action = MotionEvent.ACTION_UP
            }
            return onTouchEvent(event)
        }
        return true
    }

    override fun onTouchEvent(me: MotionEvent): Boolean {
        // Convert multi-pointer up/down events to single up/down events to
        // deal with the typical multi-pointer behavior of two-thumb typing
        val pointerCount = me.pointerCount
        val action = me.action
        var result = false
        val now = me.eventTime
        if (pointerCount != mOldPointerCount) {
            if (pointerCount == 1) {
                // Send a down event for the latest pointer
                val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, me.x, me.y, me.metaState)
                result = onModifiedTouchEvent(down, false)
                down.recycle()
                // If it's an up action, then deliver the up as well.
                if (action == MotionEvent.ACTION_UP) {
                    result = onModifiedTouchEvent(me, true)
                }
            } else {
                // Send an up event for the last pointer
                val up = MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, mOldPointerX, mOldPointerY, me.metaState)
                result = onModifiedTouchEvent(up, true)
                up.recycle()
            }
        } else {
            if (pointerCount == 1) {
                result = onModifiedTouchEvent(me, false)
                mOldPointerX = me.x
                mOldPointerY = me.y
            } else {
                // Don't do anything when 2 pointers are down and moving.
                result = true
            }
        }
        mOldPointerCount = pointerCount
        return result
    }

    private fun onModifiedTouchEvent(me: MotionEvent, possiblePoly: Boolean): Boolean {
        var touchX = me.x.toInt() - paddingLeft
        var touchY = me.y.toInt() - paddingTop
        if (touchY >= -mVerticalCorrection) {
            touchY += mVerticalCorrection
        }

        val action = me.action
        val eventTime = me.eventTime
        val keyIndex = getKeyIndices(touchX, touchY, null)
        mPossiblePoly = possiblePoly

        // Track the last few movements to look for spurious swipes.
        if (action == MotionEvent.ACTION_DOWN) {
            mSwipeTracker.clear()
        }

        mSwipeTracker.addMovement(me)

        // Ignore all motion events until a DOWN.
        if (mAbortKey && action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_CANCEL) {
            return true
        }

        if (mGestureDetector!!.onTouchEvent(me)) {
            showPreview(NOT_A_KEY)
            mHandler!!.removeMessages(MSG_REPEAT)
            mHandler!!.removeMessages(MSG_LONGPRESS)
            return true
        }

        // Needs to be called after the gesture detector gets a turn, as it may have
        // displayed the mini keyboard
        if (mMiniKeyboardOnScreen && action != MotionEvent.ACTION_CANCEL) {
            return true
        }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mAbortKey = false
                mStartX = touchX
                mStartY = touchY
                mLastCodeX = touchX
                mLastCodeY = touchY
                mLastKeyTime = 0
                mCurrentKeyTime = 0
                mLastKey = NOT_A_KEY
                mCurrentKey = keyIndex
                mDownKey = keyIndex
                mDownTime = me.eventTime
                mLastMoveTime = mDownTime
                checkMultiTap(eventTime, keyIndex)

                val onPressKey = if (keyIndex != NOT_A_KEY) {
                    mKeys[keyIndex].codes[0]
                } else {
                    0
                }

                onKeyboardActionListener!!.onPress(onPressKey)

                var wasHandled = false
                if (mCurrentKey >= 0 && mKeys[mCurrentKey].repeatable) {
                    mRepeatKeyIndex = mCurrentKey
                    val msg = mHandler!!.obtainMessage(MSG_REPEAT)
                    mHandler!!.sendMessageDelayed(msg, REPEAT_START_DELAY.toLong())
                    repeatKey()
                    // Delivering the key could have caused an abort
                    if (mAbortKey) {
                        mRepeatKeyIndex = NOT_A_KEY
                        wasHandled = true
                    }
                }

                if (!wasHandled && mCurrentKey != NOT_A_KEY) {
                    val msg = mHandler!!.obtainMessage(MSG_LONGPRESS, me)
                    mHandler!!.sendMessageDelayed(msg, LONGPRESS_TIMEOUT.toLong())
                }
                showPreview(keyIndex)
            }
            MotionEvent.ACTION_MOVE -> {
                var continueLongPress = false
                if (keyIndex != NOT_A_KEY) {
                    if (mCurrentKey == NOT_A_KEY) {
                        mCurrentKey = keyIndex
                        mCurrentKeyTime = eventTime - mDownTime
                    } else {
                        if (keyIndex == mCurrentKey) {
                            mCurrentKeyTime += eventTime - mLastMoveTime
                            continueLongPress = true
                        } else if (mRepeatKeyIndex == NOT_A_KEY) {
                            resetMultiTap()
                            mLastKey = mCurrentKey
                            mLastCodeX = mLastX
                            mLastCodeY = mLastY
                            mLastKeyTime = mCurrentKeyTime + eventTime - mLastMoveTime
                            mCurrentKey = keyIndex
                            mCurrentKeyTime = 0
                        }
                    }
                }
                if (!continueLongPress) {
                    // Cancel old longpress
                    mHandler!!.removeMessages(MSG_LONGPRESS)
                    // Start new longpress if key has changed
                    if (keyIndex != NOT_A_KEY) {
                        val msg = mHandler!!.obtainMessage(MSG_LONGPRESS, me)
                        mHandler!!.sendMessageDelayed(msg, LONGPRESS_TIMEOUT.toLong())
                    }
                }
                showPreview(mCurrentKey)
                mLastMoveTime = eventTime
            }
            MotionEvent.ACTION_UP -> {
                removeMessages()
                if (keyIndex == mCurrentKey) {
                    mCurrentKeyTime += eventTime - mLastMoveTime
                } else {
                    resetMultiTap()
                    mLastKey = mCurrentKey
                    mLastKeyTime = mCurrentKeyTime + eventTime - mLastMoveTime
                    mCurrentKey = keyIndex
                    mCurrentKeyTime = 0
                }

                if (mCurrentKeyTime < mLastKeyTime && mCurrentKeyTime < DEBOUNCE_TIME && mLastKey != NOT_A_KEY) {
                    mCurrentKey = mLastKey
                    touchX = mLastCodeX
                    touchY = mLastCodeY
                }
                showPreview(NOT_A_KEY)
                Arrays.fill(mKeyIndices, NOT_A_KEY)
                // If we're not on a repeating key (which sends on a DOWN event)
                if (mRepeatKeyIndex == NOT_A_KEY && !mMiniKeyboardOnScreen && !mAbortKey) {
                    detectAndSendKey(mCurrentKey, touchX, touchY, eventTime)
                }
                invalidateKey(keyIndex)
                mRepeatKeyIndex = NOT_A_KEY
            }
            MotionEvent.ACTION_CANCEL -> {
                removeMessages()
                dismissPopupKeyboard()
                mAbortKey = true
                showPreview(NOT_A_KEY)
                invalidateKey(mCurrentKey)
            }
        }
        mLastX = touchX
        mLastY = touchY
        return true
    }

    private fun repeatKey(): Boolean {
        val key = mKeys[mRepeatKeyIndex]
        detectAndSendKey(mCurrentKey, key.x, key.y, mLastTapTime)
        return true
    }

    protected fun swipeRight() {
        onKeyboardActionListener!!.swipeRight()
    }

    protected fun swipeLeft() {
        onKeyboardActionListener!!.swipeLeft()
    }

    protected fun swipeUp() {
        onKeyboardActionListener!!.swipeUp()
    }

    protected fun swipeDown() {
        onKeyboardActionListener!!.swipeDown()
    }

    fun closing() {
        if (mPreviewPopup.isShowing) {
            mPreviewPopup.dismiss()
        }
        removeMessages()
        dismissPopupKeyboard()
        mBuffer = null
        mCanvas = null
        mMiniKeyboardCache.clear()
    }

    private fun removeMessages() {
        if (mHandler != null) {
            mHandler!!.removeMessages(MSG_REPEAT)
            mHandler!!.removeMessages(MSG_LONGPRESS)
            mHandler!!.removeMessages(MSG_SHOW_PREVIEW)
        }
    }

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        closing()
    }

    private fun dismissPopupKeyboard() {
        if (mPopupKeyboard.isShowing) {
            mPopupKeyboard.dismiss()
            mMiniKeyboardOnScreen = false
            invalidateAllKeys()
        }
    }

    fun handleBack(): Boolean {
        if (mPopupKeyboard.isShowing) {
            dismissPopupKeyboard()
            return true
        }
        return false
    }

    private fun resetMultiTap() {
        mLastSentIndex = NOT_A_KEY
        mTapCount = 0
        mLastTapTime = -1
        mInMultiTap = false
    }

    private fun checkMultiTap(eventTime: Long, keyIndex: Int) {
        if (keyIndex == NOT_A_KEY) return
        val key = mKeys[keyIndex]
        if (key.codes.size > 1) {
            mInMultiTap = true
            if (eventTime < mLastTapTime + MULTITAP_INTERVAL && keyIndex == mLastSentIndex) {
                mTapCount = (mTapCount + 1) % key.codes.size
                return
            } else {
                mTapCount = -1
                return
            }
        }

        if (eventTime > mLastTapTime + MULTITAP_INTERVAL || keyIndex != mLastSentIndex) {
            resetMultiTap()
        }
    }

    private class SwipeTracker {
        companion object {
            const val NUM_PAST = 4
            const val LONGEST_PAST_TIME = 200
        }

        val mPastX = FloatArray(NUM_PAST)
        val mPastY = FloatArray(NUM_PAST)
        val mPastTime = LongArray(NUM_PAST)
        var yVelocity = 0f
        var xVelocity = 0f

        fun clear() {
            mPastTime[0] = 0
        }

        fun addMovement(ev: MotionEvent) {
            val time = ev.eventTime
            val N = ev.historySize
            for (i in 0 until N) {
                addPoint(ev.getHistoricalX(i), ev.getHistoricalY(i), ev.getHistoricalEventTime(i))
            }
            addPoint(ev.x, ev.y, time)
        }

        private fun addPoint(x: Float, y: Float, time: Long) {
            var drop = -1
            val pastTime = mPastTime
            var i = 0
            while (i < NUM_PAST) {
                if (pastTime[i] == 0L) {
                    break
                } else if (pastTime[i] < time - LONGEST_PAST_TIME) {
                    drop = i
                }
                i++
            }

            if (i == NUM_PAST && drop < 0) {
                drop = 0
            }

            if (drop == i) drop--
            val pastX = mPastX
            val pastY = mPastY
            if (drop >= 0) {
                val start = drop + 1
                val count: Int = NUM_PAST - drop - 1
                System.arraycopy(pastX, start, pastX, 0, count)
                System.arraycopy(pastY, start, pastY, 0, count)
                System.arraycopy(pastTime, start, pastTime, 0, count)
                i -= drop + 1
            }
            pastX[i] = x
            pastY[i] = y
            pastTime[i] = time
            i++

            if (i < NUM_PAST) {
                pastTime[i] = 0
            }
        }

        @JvmOverloads
        fun computeCurrentVelocity(units: Int, maxVelocity: Float = Float.MAX_VALUE) {
            val pastX = mPastX
            val pastY = mPastY
            val pastTime = mPastTime
            val oldestX = pastX[0]
            val oldestY = pastY[0]
            val oldestTime = pastTime[0]
            var accumX = 0f
            var accumY = 0f
            var N = 0
            while (N < NUM_PAST) {
                if (pastTime[N] == 0L) {
                    break
                }
                N++
            }

            for (i in 1 until N) {
                val dur = (pastTime[i] - oldestTime).toInt()
                if (dur == 0) continue
                var dist = pastX[i] - oldestX
                var vel = dist / dur * units // pixels/frame.
                accumX = if (accumX == 0f) {
                    vel
                } else {
                    (accumX + vel) * .5f
                }

                dist = pastY[i] - oldestY
                vel = dist / dur * units // pixels/frame.
                accumY = if (accumY == 0f) {
                    vel
                } else {
                    (accumY + vel) * .5f
                }
            }

            xVelocity = if (accumX < 0.0f) {
                Math.max(accumX, -maxVelocity)
            } else {
                Math.min(accumX, maxVelocity)
            }

            yVelocity = if (accumY < 0.0f) {
                Math.max(accumY, -maxVelocity)
            } else {
                Math.min(accumY, maxVelocity)
            }
        }
    }
}
