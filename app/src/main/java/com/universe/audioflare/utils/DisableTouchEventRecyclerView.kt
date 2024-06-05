package com.universe.audioflare.utils

import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

/**
 * An empty [RecyclerView.OnItemTouchListener] to disable touch event on [RecyclerView]
 * Disable user scrolling but smooth scrolling still works
 */

class DisableTouchEventRecyclerView: RecyclerView.OnItemTouchListener {
    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        return true
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
    }
}