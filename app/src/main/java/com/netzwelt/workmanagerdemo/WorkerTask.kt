package com.netzwelt.workmanagerdemo

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap


class WorkerTask(context: Context, workParam: WorkerParameters) : Worker(context, workParam) {

    private var lastTime: Date? = null

    override fun doWork(): Result {
        var db = FirebaseFirestore.getInstance()
        // Create a new user with a first and last name
        val user = HashMap<String, Any>()
        if (lastTime == null) {
            lastTime = Date()
            user[lastTime.toString()] = getBatteryPercentage(applicationContext)
        } else {
            var currentDate = Date()
            user[Date().toString()] = (currentDate.time - lastTime!!.time) / 1000
        }

// Add a new document with a generated ID
        db.collection(Build.DEVICE)
            .add(user)
            .addOnSuccessListener { documentReference ->
                Log.d(
                    "TAG",
                    "DocumentSnapshot added with ID: " + documentReference.id
                )
            }
            .addOnFailureListener { e -> Log.w("TAG", "Error adding document", e) }


        val dailyWorkRequest =
            OneTimeWorkRequestBuilder<WorkerTask>().setInitialDelay(16, TimeUnit.MINUTES)
                .build()
        WorkManager.getInstance(applicationContext)
            .enqueue(dailyWorkRequest)

        return Result.success()
    }

    fun getBatteryPercentage(context: Context): Int {

        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, iFilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        val batteryPct = level / scale.toFloat()

        return (batteryPct * 100).toInt()
    }
}