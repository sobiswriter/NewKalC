package com.example

import android.Manifest
import android.app.NotificationManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ui.CookingTimer
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CookingTimerTest {
    @Test fun scheduledTimerDeliversItsNotification() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        if(Build.VERSION.SDK_INT>=33) instrumentation.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.POST_NOTIFICATIONS)
        val manager=context.getSystemService(NotificationManager::class.java)
        try {
            CookingTimer.schedule(context,System.currentTimeMillis()+1500,"Synthetic timer check")
            val deadline=System.currentTimeMillis()+30000
            while(System.currentTimeMillis()<deadline && manager.activeNotifications.none {it.id==71}) Thread.sleep(250)
            assertTrue("A real scheduled reminder should arrive while notifications are allowed",manager.activeNotifications.any {it.id==71})
        } finally {CookingTimer.schedule(context,null,"")}
    }
}
