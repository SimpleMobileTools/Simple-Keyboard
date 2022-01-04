package com.simplemobiletools.keyboard.activities

import android.os.Bundle
import com.simplemobiletools.commons.extensions.appLaunched
import com.simplemobiletools.keyboard.BuildConfig
import com.simplemobiletools.keyboard.R

class MainActivity : SimpleActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appLaunched(BuildConfig.APPLICATION_ID)
    }
}
