package com.nestvest

import android.app.Application
import com.nestvest.profile.data.ProfileDatabase
import com.nestvest.profile.data.RoomProfileRepository
import com.nestvest.profile.domain.ProfileRepository
import com.nestvest.expense.data.ReceiptFileStore
import com.nestvest.expense.data.RoomExpenseRepository
import com.nestvest.expense.data.RoomReminderRepository
import com.nestvest.expense.reminder.ExpenseReminderNotifier
import com.nestvest.expense.reminder.ReminderScheduler

class NestVestApplication : Application() {
    val database: ProfileDatabase by lazy { ProfileDatabase.open(this) }
    val repository: ProfileRepository by lazy { RoomProfileRepository(database) }
    val expenseRepository by lazy { RoomExpenseRepository(database, receipts = ReceiptFileStore(this)) }
    val reminderRepository by lazy { RoomReminderRepository(database) }
    val reminderNotifier by lazy { ExpenseReminderNotifier(this) }
    val reminderScheduler by lazy { ReminderScheduler(this, reminderRepository, reminderNotifier::available) }
}
