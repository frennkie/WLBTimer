package de.rhab.wlbtimer

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

/*
Source https://stackoverflow.com/questions/31617398/floatingactionbutton-hide-on-list-scroll
This hides the Floating Action Button when scrolling.
This is needed because otherwise the FAB overlays over the last entries of the RecyclerViews
Caution: Prevents auto move of FAB for Snackbar
*/


class FabBehaviour : CoordinatorLayout.Behavior<FloatingActionButton> {
    private var mHandler: Handler? = null

    constructor(context: Context, attrs: AttributeSet) : super()

    constructor() : super()

    override fun onStopNestedScroll(coordinatorLayout: CoordinatorLayout, child: FloatingActionButton, target: View, type: Int) {
        super.onStopNestedScroll(coordinatorLayout, child, target, type)
        if (mHandler == null)
            mHandler = Handler()


        mHandler!!.postDelayed({
            child.animate().translationY(0f).setInterpolator(LinearInterpolator()).start()
            Log.d("FabAnim", "startHandler()")
        }, 1000)
    }

    override fun onNestedScroll(coordinatorLayout: CoordinatorLayout, child: FloatingActionButton, target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type)
        if (dyConsumed > 0) {
            Log.d("Scrolling", "Up")
            val layoutParams = child.layoutParams as CoordinatorLayout.LayoutParams
            val fabBottomMargin = layoutParams.bottomMargin
            child.animate().translationY((child.height + fabBottomMargin).toFloat()).setInterpolator(LinearInterpolator()).start()
        } else if (dyConsumed < 0) {
            Log.d("Scrolling", "down")
            child.animate().translationY(0f).setInterpolator(LinearInterpolator()).start()
        }
    }

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout, child: FloatingActionButton, directTargetChild: View, target: View, axes: Int, type: Int): Boolean {
        if (mHandler != null) {
            mHandler!!.removeMessages(0)
            Log.d("Scrolling", "stopHandler()")
        }
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    companion object {
        private val TAG = "ScrollingFABBehavior"
    }

}