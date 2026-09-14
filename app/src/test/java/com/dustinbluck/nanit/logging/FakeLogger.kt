package com.dustinbluck.nanit.logging

internal class FakeLogger : Logger {
    val entries = mutableListOf<Entry>()

    override fun v(
        tag: String,
        msg: String,
        tr: Throwable?
    ) {
        entries.add(
            Entry(
                level = Level.VERBOSE,
                tag = tag,
                msg = msg,
                tr = tr
            )
        )
    }

    override fun d(
        tag: String,
        msg: String,
        tr: Throwable?
    ) {
        entries.add(
            Entry(
                level = Level.DEBUG,
                tag = tag,
                msg = msg,
                tr = tr
            )
        )
    }

    override fun i(
        tag: String,
        msg: String,
        tr: Throwable?
    ) {
        entries.add(
            Entry(
                level = Level.INFO,
                tag = tag,
                msg = msg,
                tr = tr
            )
        )
    }

    override fun w(
        tag: String,
        msg: String,
        tr: Throwable?
    ) {
        entries.add(
            Entry(
                level = Level.WARN,
                tag = tag,
                msg = msg,
                tr = tr
            )
        )
    }

    override fun w(
        tag: String,
        tr: Throwable
    ) {
        entries.add(
            Entry(
                level = Level.WARN,
                tag = tag,
                msg = null,
                tr = tr
            )
        )
    }

    override fun e(
        tag: String,
        msg: String,
        tr: Throwable?
    ) {
        entries.add(
            Entry(
                level = Level.ERROR,
                tag = tag,
                msg = msg,
                tr = tr
            )
        )
    }

    override fun wtf(
        tag: String,
        msg: String,
        tr: Throwable?
    ) {
        entries.add(
            Entry(
                level = Level.ASSERT,
                tag = tag,
                msg = msg,
                tr = tr
            )
        )
    }

    override fun wtf(
        tag: String,
        tr: Throwable
    ) {
        entries.add(
            Entry(
                level = Level.ASSERT,
                tag = tag,
                msg = null,
                tr = tr
            )
        )
    }

    data class Entry(
        val level: Level,
        val tag: String,
        val msg: String?,
        val tr: Throwable?
    )

    enum class Level {
        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        ASSERT
    }
}
