package com.dustinbluck.nanit

import android.app.Application
import com.dustinbluck.nanit.deps.NanitDeps
import com.dustinbluck.nanit.deps.NanitDepsImpl

class NanitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NanitDeps.instance = NanitDepsImpl(this)
    }
}
