package com.mx.imgpicker.observer

import android.os.Handler
import android.os.Looper

internal open class MXBaseObservable {
    /**
     * 主线程Handler
     */
    private val mHandler = Handler(Looper.getMainLooper())
    private val lock = Object()
    private val observerList = ArrayList<(() -> Unit)>()

    fun notifyChanged() {
        mHandler.removeCallbacks(notifyAll)
        mHandler.post(notifyAll)
    }

    private val notifyAll = Runnable {
        val list = synchronized(lock) {
            observerList.toMutableList()
        }
        if (list.isEmpty()) return@Runnable
        list.forEach { it.invoke() }
    }

    fun addObserver(o: (() -> Unit)?) {
        o ?: return
        synchronized(lock) {
            observerList.add(o)
        }
        mHandler.post { o.invoke() }
    }

    fun deleteObserver(o: (() -> Unit)?) {
        o ?: return
        synchronized(lock) {
            observerList.remove(o)
        }
    }

    fun deleteObservers() {
        synchronized(lock) {
            observerList.clear()
        }
    }
}