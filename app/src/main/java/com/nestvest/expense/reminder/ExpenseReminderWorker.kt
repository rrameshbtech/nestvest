package com.nestvest.expense.reminder

import android.content.Context
import android.util.Log
import android.database.sqlite.SQLiteException
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nestvest.expense.data.RoomReminderRepository
import com.nestvest.profile.data.ProfileDatabase
import java.util.concurrent.ExecutionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.withLock

class ExpenseReminderWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = ReminderScheduler.lock.withLock {
        val database = ProfileDatabase.open(applicationContext)
        try {
            val repository = RoomReminderRepository(database)
            val notifier = ExpenseReminderNotifier(applicationContext)
            val scheduler = ReminderScheduler(applicationContext)
            val complete = ReminderDelivery(repository, notifier::available,
                { scheduler.appendSuccessor(id, it) }, notifier::post, { isStopped })
                .deliver(inputData.getInt(ReminderScheduler.MINUTES, -1),
                    inputData.getString(ReminderScheduler.ZONE).orEmpty())
            if (complete) Result.success() else Result.retry()
        } catch (error: CancellationException) {
            throw error
        } catch (error: SecurityException) {
            Log.w("NestVestReminder", "Notification access unavailable")
            Result.success()
        } catch (error: IllegalStateException) {
            Log.w("NestVestReminder", "Reminder scheduling unavailable")
            Result.retry()
        } catch (error: ExecutionException) {
            Log.w("NestVestReminder", "Reminder work persistence unavailable")
            Result.retry()
        } catch (error: SQLiteException) {
            Log.w("NestVestReminder", "Reminder storage unavailable")
            Result.retry()
        } finally {
            database.close()
        }
    }
}
