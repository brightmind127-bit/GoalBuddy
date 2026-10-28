package com.goalbuddy.app

import android.app.Application
import com.goalbuddy.app.data.Store

class GoalBuddyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
    }
}
