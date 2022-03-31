package com.mx.imgpicker.observer

import android.os.Handler
import android.os.Looper

internal open class MXBaseObservable<T> {
    /**
     * 主线程Handler
     */
    private val mHandler = Handler(Looper.getMainLooper())
    private val lock = Object()
    private val observerList = ArrayList<((T?) -> Unit)>()
    private var _value: T? = null

    open fun notifyChanged(value: T?) {
        _value = value
        mHandler.removeCallbacks(notifyAll)
        mHandler.post(notifyAll)
    }

    private val notifyAll = Runnable {
        val list = synchronized(lock) {
            observerList.toMutableList()
        }
        if (list.isEmpty()) return@Runnable
        list.forEach { it.invoke(_value) }
    }

    fun addObserver(o: ((T?) -> Unit)?) {
        o ?: return
        synchronized(lock) {
            observerList.add(o)
        }
        mHandler.post { o.invoke(_value) }
    }

    fun deleteObserver(o: ((T?) -> Unit)?) {
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