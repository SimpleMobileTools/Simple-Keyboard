package com.simplemobiletools.keyboard.helpers

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import android.util.SparseArray
import android.util.TypedValue
import android.util.Xml
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.EditorInfo.IME_ACTION_NONE
import androidx.annotation.XmlRes
import com.simplemobiletools.keyboard.R
import java.util.*

/**
 * Loads an XML description of a keyboard and stores the attributes of the keys. A keyboard
 * consists of rows of keys.
 *
 * The layout file for a keyboard contains XML that looks like the following snippet:
 * <pre>
 * &lt;Keyboard
 * android:keyWidth="%10p"
 * android:keyHeight="50px"
 * android:horizontalGap="2px"
 * android:verticalGap="2px" &gt;
 * &lt;Row android:keyWidth="32px" &gt;
 * &lt;Key android:keyLabel="A" /&gt;
 * ...
 * &lt;/Row&gt;
 * ...
 * &lt;/Keyboard&gt;
</pre> *
 * @attr ref android.R.styleable#Keyboard_keyWidth
 * @attr ref android.R.styleable#Keyboard_keyHeight
 * @attr ref android.R.styleable#Keyboard_horizontalGap
 * @attr ref android.R.styleable#Keyboard_verticalGap
 */
class MyKeyboard {
    /** Horizontal gap default for all rows  */
    protected var mDefaultHorizontalGap = 0

    /** Default key width  */
    protected var mDefaultWidth = 0

    /** Default key height  */
    protected var mDefaultHeight = 0

    /** Default gap between rows  */
    protected var mDefaultVerticalGap = 0

    /** Is the keyboard in the shifted state  */
    var shiftState = SHIFT_OFF

    /** Key instance for the shift key, if present  */
    private val mShiftKeys = arrayOf<Key?>(null, null)

    /** Key index for the shift key, if present  */
    private val shiftKeyIndices = intArrayOf(-1, -1)

    /** Total height of the keyboard, including the padding and keys  */
    var height = 0

    /** Total width of the keyboard, including left side gaps and keys, but not any gaps on the right side. */
    var minWidth = 0

    /** List of keys in this keyboard  */
    var mKeys: MutableList<Key?>? = null

    /** List of modifier keys such as Shift & Alt, if any  */
    private var mModifierKeys = ArrayList<Key?>()

    /** Width of the screen available to fit the keyboard  */
    private var mDisplayWidth = 0

    /** Height of the screen  */
    private var mDisplayHeight = 0

    /** What icon should we show at Enter key */
    private var mEnterKeyType = IME_ACTION_NONE

    /** Keyboard mode, or zero, if none.   */
    private var mKeyboardMode = 0
    private var mCellWidth = 0
    private var mCellHeight = 0
    private var mGridNeighbors: SparseArray<IntArray?>? = null
    private var mProximityThreshold = 0
    private val rows = ArrayList<Row?>()

    companion object {
        private const val TAG_KEYBOARD = "Keyboard"
        private const val TAG_ROW = "Row"
        private const val TAG_KEY = "Key"
        private const val EDGE_LEFT = 0x01
        private const val EDGE_RIGHT = 0x02
        private const val EDGE_TOP = 0x04
        private const val EDGE_BOTTOM = 0x08
        const val KEYCODE_SHIFT = -1
        const val KEYCODE_MODE_CHANGE = -2
        const val KEYCODE_ENTER = -4
        const val KEYCODE_DELETE = -5
        const val KEYCODE_SPACE = 32

        // Variables for pre-computing nearest keys.
        private const val GRID_WIDTH = 10
        private const val GRID_HEIGHT = 5
        private const val GRID_SIZE = GRID_WIDTH * GRID_HEIGHT

        /** Number of key widths from current touch point to search for nearest keys.  */
        private const val SEARCH_DISTANCE = 1.8f
        fun getDimensionOrFraction(a: TypedArray, index: Int, base: Int, defValue: Int): Int {
            val value = a.peekValue(index) ?: return defValue
            if (value.type == TypedValue.TYPE_DIMENSION) {
                return a.getDimensionPixelOffset(index, defValue)
            } else if (value.type == TypedValue.TYPE_FRACTION) {
                // Round it to avoid values like 47.9999 from getting truncated
                return Math.round(a.getFraction(index, base, base, defValue.toFloat()))
            }
            return defValue
        }
    }

    /**
     * Container for keys in the keyboard. All keys in a row are at the same Y-coordinate.
     * Some of the key size defaults can be overridden per row from what the [MyKeyboard]
     * defines.
     * @attr ref android.R.styleable#Keyboard_keyWidth
     * @attr ref android.R.styleable#Keyboard_keyHeight
     * @attr ref android.R.styleable#Keyboard_horizontalGap
     * @attr ref android.R.styleable#Keyboard_verticalGap
     * @attr ref android.R.styleable#Keyboard_Row_rowEdgeFlags
     * @attr ref android.R.styleable#Keyboard_Row_keyboardMode
     */
    class Row {
        /** Default width of a key in this row.  */
        var defaultWidth = 0

        /** Default height of a key in this row.  */
        var defaultHeight = 0

        /** Default horizontal gap between keys in this row.  */
        var defaultHorizontalGap = 0

        /** Vertical gap following this row.  */
        var verticalGap = 0

        var mKeys = ArrayList<Key>()

        /**
         * Edge flags for this row of keys. Possible values that can be assigned are
         * [EDGE_TOP][MyKeyboard.EDGE_TOP] and [EDGE_BOTTOM][MyKeyboard.EDGE_BOTTOM]
         */
        var rowEdgeFlags = 0

        /** The keyboard mode for this row  */
        var mode = 0
        var parent: MyKeyboard

        constructor(parent: MyKeyboard) {
            this.parent = parent
        }

        constructor(res: Resources, parent: MyKeyboard, parser: XmlResourceParser?) {
            this.parent = parent
            var a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.MyKeyboard)
            defaultWidth = getDimensionOrFraction(a, R.styleable.MyKeyboard_keyWidth, parent.mDisplayWidth, parent.mDefaultWidth)
            defaultHeight = getDimensionOrFraction(a, R.styleable.MyKeyboard_keyHeight, parent.mDisplayHeight, parent.mDefaultHeight)
            defaultHorizontalGap = getDimensionOrFraction(a, R.styleable.MyKeyboard_horizontalGap, parent.mDisplayWidth, parent.mDefaultHorizontalGap)
            verticalGap = getDimensionOrFraction(a, R.styleable.MyKeyboard_verticalGap, parent.mDisplayHeight, parent.mDefaultVerticalGap)

            a.recycle()
            a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.MyKeyboard_Row)
            rowEdgeFlags = a.getInt(R.styleable.MyKeyboard_Row_rowEdgeFlags, 0)
            mode = a.getResourceId(R.styleable.MyKeyboard_Row_keyboardMode, 0)
        }
    }

    /**
     * Class for describing the position and characteristics of a single key in the keyboard.
     *
     * @attr ref android.R.styleable#Keyboard_keyWidth
     * @attr ref android.R.styleable#Keyboard_keyHeight
     * @attr ref android.R.styleable#Keyboard_horizontalGap
     * @attr ref android.R.styleable#Keyboard_Key_codes
     * @attr ref android.R.styleable#Keyboard_Key_keyIcon
     * @attr ref android.R.styleable#Keyboard_Key_keyLabel
     * @attr ref android.R.styleable#Keyboard_Key_iconPreview
     * @attr ref android.R.styleable#Keyboard_Key_isSticky
     * @attr ref android.R.styleable#Keyboard_Key_isRepeatable
     * @attr ref android.R.styleable#Keyboard_Key_isModifier
     * @attr ref android.R.styleable#Keyboard_Key_popupKeyboard
     * @attr ref android.R.styleable#Keyboard_Key_popupCharacters
     * @attr ref android.R.styleable#Keyboard_Key_keyOutputText
     * @attr ref android.R.styleable#Keyboard_Key_keyEdgeFlags
     */
    class Key(parent: Row) {
        /** All the key codes (unicode or custom code) that this key could generate, zero'th being the most important.  */
        var codes = ArrayList<Int>()

        /** Label to display  */
        var label: CharSequence = ""

        /** First row of letters can also be used for inserting numbers by long pressing them, show those numbers */
        var topSmallNumber: String = ""

        /** Icon to display instead of a label. Icon takes precedence over a label  */
        var icon: Drawable? = null

        /** Preview version of the icon, for the preview popup  */
        var iconPreview: Drawable? = null

        /** Width of the key, not including the gap  */
        var width: Int

        /** Height of the key, not including the gap  */
        var height: Int

        /** The horizontal gap before this key  */
        var gap: Int

        /** Whether this key is sticky, i.e., a toggle key  */
        var sticky = false

        /** X coordinate of the key in the keyboard layout  */
        var x = 0

        /** Y coordinate of the key in the keyboard layout  */
        var y = 0

        /** The current pressed state of this key  */
        var pressed = false

        /** Focused state, used after long pressing a key and swiping to alternative keys */
        var focused = false

        /** Text to output when pressed. This can be multiple characters, like ".com"  */
        var text: CharSequence? = null

        /** Popup characters  */
        var popupCharacters: CharSequence? = null

        /**
         * Flags that specify the anchoring to edges of the keyboard for detecting touch events
         * that are just out of the boundary of the key. This is a bit mask of
         * [MyKeyboard.EDGE_LEFT], [MyKeyboard.EDGE_RIGHT], [MyKeyboard.EDGE_TOP] and [MyKeyboard.EDGE_BOTTOM].
         */
        var edgeFlags: Int

        /** Whether this is a modifier key, such as Shift or Alt  */
        var modifier = false

        /** The keyboard that this key belongs to  */
        private val keyboard: MyKeyboard = parent.parent

        /** If this key pops up a mini keyboard, this is the resource id for the XML layout for that keyboard.  */
        var popupResId = 0

        /** Whether this key repeats itself when held down  */
        var repeatable = false

        /** Create a key with the given top-left coordinate and extract its attributes from
         * the XML parser.
         * @param res resources associated with the caller's context
         * @param parent the row that this key belongs to. The row must already be attached to a [MyKeyboard].
         * @param x the x coordinate of the top-left
         * @param y the y coordinate of the top-left
         * @param parser the XML parser containing the attributes for this key
         */
        constructor(res: Resources, parent: Row, x: Int, y: Int, parser: XmlResourceParser?) : this(parent) {
            this.x = x
            this.y = y
            var a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.MyKeyboard)
            width = getDimensionOrFraction(a, R.styleable.MyKeyboard_keyWidth, keyboard.mDisplayWidth, parent.defaultWidth)
            height = getDimensionOrFraction(a, R.styleable.MyKeyboard_keyHeight, keyboard.mDisplayHeight, parent.defaultHeight)
            gap = getDimensionOrFraction(a, R.styleable.MyKeyboard_horizontalGap, keyboard.mDisplayWidth, parent.defaultHorizontalGap)

            a.recycle()
            a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.MyKeyboard_Key)
            this.x += gap
            val codesValue = TypedValue()

            a.getValue(R.styleable.MyKeyboard_Key_codes, codesValue)
            if (codesValue.type == TypedValue.TYPE_INT_DEC || codesValue.type == TypedValue.TYPE_INT_HEX) {
                codes = arrayListOf(codesValue.data)
            } else if (codesValue.type == TypedValue.TYPE_STRING) {
                codes = parseCSV(codesValue.string.toString())
            }

            iconPreview = a.getDrawable(R.styleable.MyKeyboard_Key_iconPreview)
            iconPreview?.setBounds(0, 0, iconPreview!!.intrinsicWidth, iconPreview!!.intrinsicHeight)

            popupCharacters = a.getText(R.styleable.MyKeyboard_Key_popupCharacters)
            popupResId = a.getResourceId(R.styleable.MyKeyboard_Key_popupKeyboard, 0)
            repeatable = a.getBoolean(R.styleable.MyKeyboard_Key_isRepeatable, false)
            modifier = a.getBoolean(R.styleable.MyKeyboard_Key_isModifier, false)
            sticky = a.getBoolean(R.styleable.MyKeyboard_Key_isSticky, false)
            edgeFlags = a.getInt(R.styleable.MyKeyboard_Key_keyEdgeFlags, 0)
            edgeFlags = edgeFlags or parent.rowEdgeFlags
            icon = a.getDrawable(R.styleable.MyKeyboard_Key_keyIcon)
            icon?.setBounds(0, 0, icon!!.intrinsicWidth, icon!!.intrinsicHeight)

            label = a.getText(R.styleable.MyKeyboard_Key_keyLabel) ?: ""
            text = a.getText(R.styleable.MyKeyboard_Key_keyOutputText)
            topSmallNumber = a.getString(R.styleable.MyKeyboard_Key_topSmallNumber) ?: ""

            if (label.isNotEmpty() && codes.firstOrNull() != KEYCODE_MODE_CHANGE && codes.firstOrNull() != KEYCODE_SHIFT) {
                codes = arrayListOf(label[0].toInt())
            }
            a.recycle()
        }

        /** Create an empty key with no attributes.  */
        init {
            height = parent.defaultHeight
            width = parent.defaultWidth
            gap = parent.defaultHorizontalGap
            edgeFlags = parent.rowEdgeFlags
        }

        fun onPressed() {
            pressed = true
        }

        fun onReleased() {
            pressed = false
        }

        fun parseCSV(value: String): ArrayList<Int> {
            var count = 0
            var lastIndex = 0
            if (value.isNotEmpty()) {
                count++
                while (value.indexOf(",", lastIndex + 1).also { lastIndex = it } > 0) {
                    count++
                }
            }

            val values = ArrayList<Int>(count)
            count = 0
            val st = StringTokenizer(value, ",")
            while (st.hasMoreTokens()) {
                try {
                    values[count++] = st.nextToken().toInt()
                } catch (nfe: NumberFormatException) {
                }
            }
            return values
        }

        /**
         * Detects if a point falls inside this key.
         * @param x the x-coordinate of the point
         * @param y the y-coordinate of the point
         * @return whether or not the point falls inside the key. If the key is attached to an edge,
         * it will assume that all points between the key and the edge are considered to be inside
         * the key.
         */
        fun isInside(x: Int, y: Int): Boolean {
            val leftEdge = edgeFlags and EDGE_LEFT > 0
            val rightEdge = edgeFlags and EDGE_RIGHT > 0
            val topEdge = edgeFlags and EDGE_TOP > 0
            val bottomEdge = edgeFlags and EDGE_BOTTOM > 0
            return ((x >= this.x || leftEdge && x <= this.x + width)
                && (x < this.x + width || rightEdge && x >= this.x)
                && (y >= this.y || topEdge && y <= this.y + height)
                && (y < this.y + height || bottomEdge && y >= this.y))
        }

        /**
         * Returns the square of the distance between the center of the key and the given point.
         * @param x the x-coordinate of the point
         * @param y the y-coordinate of the point
         * @return the square of the distance of the point from the center of the key
         */
        fun squaredDistanceFrom(x: Int, y: Int): Int {
            val xDist = this.x + width / 2 - x
            val yDist = this.y + height / 2 - y
            return xDist * xDist + yDist * yDist
        }
    }

    /**
     * Creates a keyboard from the given xml key layout file. Weeds out rows
     * that have a keyboard mode defined but don't match the specified mode.
     * @param context the application or service context
     * @param xmlLayoutResId the resource file that contains the keyboard layout and keys.
     * @param modeId keyboard mode identifier
     */
    /**
     * Creates a keyboard from the given xml key layout file.
     * @param context the application or service context
     * @param xmlLayoutResId the resource file that contains the keyboard layout and keys.
     */
    @JvmOverloads
    constructor(context: Context, @XmlRes xmlLayoutResId: Int, enterKeyType: Int, modeId: Int = 0) {
        val dm = context.resources.displayMetrics
        mDisplayWidth = dm.widthPixels
        mDisplayHeight = dm.heightPixels
        mDefaultHorizontalGap = 0
        mDefaultWidth = mDisplayWidth / 10
        mDefaultVerticalGap = 0
        mDefaultHeight = mDefaultWidth
        mKeys = ArrayList()
        mEnterKeyType = enterKeyType
        mKeyboardMode = modeId
        loadKeyboard(context, context.resources.getXml(xmlLayoutResId))
    }

    /**
     *
     * Creates a blank keyboard from the given resource file and populates it with the specified
     * characters in left-to-right, top-to-bottom fashion, using the specified number of columns.
     *
     *
     * If the specified number of columns is -1, then the keyboard will fit as many keys as
     * possible in each row.
     * @param context the application or service context
     * @param layoutTemplateResId the layout template file, containing no keys.
     * @param characters the list of characters to display on the keyboard. One key will be created
     * for each character.
     */
    constructor(context: Context, layoutTemplateResId: Int, characters: CharSequence) :
        this(context, layoutTemplateResId, 0) {
        var x = 0
        var y = 0
        var column = 0
        minWidth = 0
        val row = Row(this)
        row.defaultHeight = mDefaultHeight
        row.defaultWidth = mDefaultWidth
        row.defaultHorizontalGap = mDefaultHorizontalGap
        row.verticalGap = mDefaultVerticalGap
        row.rowEdgeFlags = EDGE_TOP or EDGE_BOTTOM

        characters.forEachIndexed { index, character ->
            val key = Key(row)
            if (column >= MAX_KEYS_PER_MINI_ROW) {
                column = 0
                x = 0
                y += mDefaultHeight
                rows.add(row)
                row.mKeys.clear()
            }

            key.x = x
            key.y = y
            key.label = character.toString()
            key.codes = arrayListOf(character.toInt())
            column++
            x += key.width + key.gap
            mKeys!!.add(key)
            row.mKeys.add(key)
            if (x > minWidth) {
                minWidth = x
            }
        }
        height = y + mDefaultHeight
        rows.add(row)
    }

    fun resize(newWidth: Int, newHeight: Int) {
        val numRows = rows.size
        for (rowIndex in 0 until numRows) {
            val row = rows[rowIndex] ?: continue
            val numKeys: Int = row.mKeys.size
            var totalGap = 0
            var totalWidth = 0
            for (keyIndex in 0 until numKeys) {
                val key: Key = row.mKeys.get(keyIndex)
                if (keyIndex > 0) {
                    totalGap += key.gap
                }
                totalWidth += key.width
            }

            if (totalGap + totalWidth > newWidth) {
                var x = 0
                val scaleFactor = (newWidth - totalGap).toFloat() / totalWidth
                for (keyIndex in 0 until numKeys) {
                    val key = row.mKeys[keyIndex]
                    key.width *= scaleFactor.toInt()
                    key.x = x
                    x += key.width + key.gap
                }
            }
        }

        minWidth = newWidth
        // TODO: This does not adjust the vertical placement according to the new size.
        // The main problem in the previous code was horizontal placement/size, but we should
        // also recalculate the vertical sizes/positions when we get this resize call.
    }

    fun setShifted(shiftState: Int): Boolean {
        if (this.shiftState != shiftState) {
            this.shiftState = shiftState
            return true
        }

        return false
    }

    private fun computeNearestNeighbors() {
        // Round-up so we don't have any pixels outside the grid
        mCellWidth = (minWidth + GRID_WIDTH - 1) / GRID_WIDTH
        mCellHeight = (height + GRID_HEIGHT - 1) / GRID_HEIGHT
        mGridNeighbors = SparseArray<IntArray?>(GRID_SIZE)
        val indices = IntArray(mKeys!!.size)
        val gridWidth: Int = GRID_WIDTH * mCellWidth
        val gridHeight: Int = GRID_HEIGHT * mCellHeight
        var x = 0
        while (x < gridWidth) {
            var y = 0
            while (y < gridHeight) {
                var count = 0
                for (i in mKeys!!.indices) {
                    val key = mKeys!![i]
                    if (key!!.squaredDistanceFrom(x, y) < mProximityThreshold || key.squaredDistanceFrom(
                            x + mCellWidth - 1, y
                        ) < mProximityThreshold || (key.squaredDistanceFrom(x + mCellWidth - 1, y + mCellHeight - 1)
                            < mProximityThreshold) || key.squaredDistanceFrom(x, y + mCellHeight - 1) < mProximityThreshold
                    ) {
                        indices[count++] = i
                    }
                }

                val cell = IntArray(count)
                System.arraycopy(indices, 0, cell, 0, count)
                mGridNeighbors!!.put(y / mCellHeight * GRID_WIDTH + x / mCellWidth, cell)
                y += mCellHeight
            }
            x += mCellWidth
        }
    }

    /**
     * Returns the indices of the keys that are closest to the given point.
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @return the array of integer indices for the nearest keys to the given point. If the given
     * point is out of range, then an array of size zero is returned.
     */
    fun getNearestKeys(x: Int, y: Int): IntArray {
        if (mGridNeighbors == null) {
            computeNearestNeighbors()
        }

        if (x in 0 until minWidth && y >= 0 && y < height) {
            val index = y / mCellHeight * GRID_WIDTH + x / mCellWidth
            if (index < GRID_SIZE) {
                return mGridNeighbors!![index]!!
            }
        }
        return IntArray(0)
    }

    protected fun createRowFromXml(res: Resources, parser: XmlResourceParser?): Row {
        return Row(res, this, parser)
    }

    protected fun createKeyFromXml(res: Resources, parent: Row, x: Int, y: Int, parser: XmlResourceParser?): Key {
        return Key(res, parent, x, y, parser)
    }

    private fun loadKeyboard(context: Context, parser: XmlResourceParser) {
        var inKey = false
        var inRow = false
        var row = 0
        var x = 0
        var y = 0
        var key: Key? = null
        var currentRow: Row? = null
        val res = context.resources
        try {
            var event: Int
            while (parser.next().also { event = it } != XmlResourceParser.END_DOCUMENT) {
                if (event == XmlResourceParser.START_TAG) {
                    val tag = parser.name
                    if (TAG_ROW == tag) {
                        inRow = true
                        x = 0
                        currentRow = createRowFromXml(res, parser)
                        rows.add(currentRow)
                        val skipRow = currentRow.mode != 0 && currentRow.mode != mKeyboardMode
                        if (skipRow) {
                            skipToEndOfRow(parser)
                            inRow = false
                        }
                    } else if (TAG_KEY == tag) {
                        inKey = true
                        key = createKeyFromXml(res, currentRow!!, x, y, parser)
                        mKeys!!.add(key)
                        if (key.codes[0] == KEYCODE_SHIFT) {
                            // Find available shift key slot and put this shift key in it
                            for (i in mShiftKeys.indices) {
                                if (mShiftKeys[i] == null) {
                                    mShiftKeys[i] = key
                                    shiftKeyIndices[i] = mKeys!!.size - 1
                                    break
                                }
                            }
                            mModifierKeys.add(key)
                        } else if (key.codes[0] == KEYCODE_ENTER) {
                            val enterResourceId = when (mEnterKeyType) {
                                EditorInfo.IME_ACTION_SEARCH -> R.drawable.ic_search_vector
                                EditorInfo.IME_ACTION_NEXT, EditorInfo.IME_ACTION_GO -> R.drawable.ic_arrow_right_vector
                                EditorInfo.IME_ACTION_SEND -> R.drawable.ic_send_vector
                                else -> R.drawable.ic_enter_vector
                            }
                            key.icon = context.resources.getDrawable(enterResourceId, context.theme)
                        }
                        currentRow.mKeys.add(key)
                    } else if (TAG_KEYBOARD == tag) {
                        parseKeyboardAttributes(res, parser)
                    }
                } else if (event == XmlResourceParser.END_TAG) {
                    if (inKey) {
                        inKey = false
                        x += key!!.gap + key.width
                        if (x > minWidth) {
                            minWidth = x
                        }
                    } else if (inRow) {
                        inRow = false
                        y += currentRow!!.verticalGap
                        y += currentRow.defaultHeight
                        row++
                    }
                }
            }
        } catch (e: Exception) {
        }
        height = y - mDefaultVerticalGap
    }

    private fun skipToEndOfRow(parser: XmlResourceParser) {
        var event: Int
        while (parser.next().also { event = it } != XmlResourceParser.END_DOCUMENT) {
            if (event == XmlResourceParser.END_TAG && parser.name == TAG_ROW) {
                break
            }
        }
    }

    private fun parseKeyboardAttributes(res: Resources, parser: XmlResourceParser) {
        val a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.MyKeyboard)
        mDefaultWidth = getDimensionOrFraction(a, R.styleable.MyKeyboard_keyWidth, mDisplayWidth, mDisplayWidth / 10)
        mDefaultHeight = getDimensionOrFraction(a, R.styleable.MyKeyboard_keyHeight, mDisplayHeight, 50)
        mDefaultHorizontalGap = getDimensionOrFraction(a, R.styleable.MyKeyboard_horizontalGap, mDisplayWidth, 0)
        mDefaultVerticalGap = getDimensionOrFraction(a, R.styleable.MyKeyboard_verticalGap, mDisplayHeight, 0)
        mProximityThreshold = (mDefaultWidth * SEARCH_DISTANCE).toInt()
        mProximityThreshold *= mProximityThreshold // Square it for comparison
        a.recycle()
    }
}
