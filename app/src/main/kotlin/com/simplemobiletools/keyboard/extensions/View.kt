package com.simplemobiletools.keyboard.extensions

import androidx.recyclerview.widget.RecyclerView

/**
 * Calls the [scroll] callback when the receiving RecyclerView's scroll position is changed.
 */
fun RecyclerView.onScroll(scroll: (Int) -> Unit) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(
            recyclerView: RecyclerView,
            dx: Int,
            dy: Int,
        ) {
            super.onScrolled(recyclerView, dx, dy)
            scroll(computeVerticalScrollOffset())
        }
    })
}
