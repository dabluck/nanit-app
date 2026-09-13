package com.dustinbluck.nanit.logging

import android.util.Log

class AndroidLogger : Logger {
    override fun v(
        tag: String,
        msg: String,
        tr: Throwable?
    ) {
        Log.v(
            tag,
            msg,
            tr
        )
    }

    override fun d(
        tag: String,
        msg: String,
        tr: Throwable?
    ) {
        Log.d(
            tag,
            msg,
            tr
        )
    }

    override fun i(
        tag: String,
        msg: String,
        tr: Throwable?
    ) {
        Log.i(
            tag,
            msg,
            tr
        )
    }

    override fun w(
        tag: String,
        msg: String,
        tr: Throwable?
    ) {
        Log.w(
            tag,
            msg,
            tr
        )
    }

    override fun w(
        tag: String,
        tr: Throwable
    ) {
        Log.w(
            tag,
            tr
        )
    }

    override fun e(
        tag: String,
        msg: String,
        tr: Throwable?
    ) {
        Log.e(
            tag,
            msg,
            tr
        )
    }

    override fun wtf(
        tag: String,
        msg: String,
        tr: Throwable?
    ) {
        Log.wtf(
            tag,
            msg,
            tr
        )
    }

    override fun wtf(
        tag: String,
        tr: Throwable
    ) {
        Log.wtf(
            tag,
            tr
        )
    }
}
