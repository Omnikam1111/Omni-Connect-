package com.example.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.dao.ContactDao
import com.example.data.entity.*

@Database(
    entities = [
        Contact::class,
        PhoneNumber::class,
        Email::class,
        CustomAction::class,
        Note::class,
        Address::class,
        PhoneNumberMatch::class,
        ScheduledMessage::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
}
