package com.dustinbluck.nanit.logging

// matches android.util.Log surface, even though we don't use them all
interface Logger {
    fun v(
        tag: String,
        msg: String,
        tr: Throwable? = null
    )

    fun d(
        tag: String,
        msg: String,
        tr: Throwable? = null
    )

    fun i(
        tag: String,
        msg: String,
        tr: Throwable? = null
    )

    fun w(
        tag: String,
        msg: String,
        tr: Throwable? = null
    )

    fun w(
        tag: String,
        tr: Throwable
    )

    fun e(
        tag: String,
        msg: String,
        tr: Throwable? = null
    )

    fun wtf(
        tag: String,
        msg: String,
        tr: Throwable? = null
    )

    fun wtf(
        tag: String,
        tr: Throwable
    )
}
