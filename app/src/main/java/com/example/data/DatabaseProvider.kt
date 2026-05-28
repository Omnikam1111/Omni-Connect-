package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.data.db.AppDatabase
import com.example.data.repository.ContactRepository
import java.io.File
import java.io.FileOutputStream

object DatabaseProvider {
    private const val TAG = "DatabaseProvider"
    private var realDatabase: AppDatabase? = null
    private var decoyDatabase: AppDatabase? = null
    private var currentUseDecoy: Boolean = false
    private var repository: ContactRepository? = null
    private val initLock = Any()

    fun getDatabase(context: Context): AppDatabase {
        synchronized(initLock) {
            val db = if (currentUseDecoy) decoyDatabase else realDatabase
            if (db == null) {
                return initDatabase(context, useDecoy = currentUseDecoy)
            }
            return db
        }
    }

    fun getRepository(context: Context): ContactRepository {
        synchronized(initLock) {
            if (repository == null) {
                repository = ContactRepository(context.applicationContext)
            }
            return repository!!
        }
    }
    
    fun getCurrentDecoyMode(): Boolean = currentUseDecoy

    fun initDatabase(context: Context, useDecoy: Boolean): AppDatabase {
        synchronized(initLock) {
            currentUseDecoy = useDecoy
            val dbName = if (useDecoy) "decoy.db" else "real.db"
            
            if (useDecoy) {
                if (decoyDatabase == null) {
                    Log.d(TAG, "Initializing decoy database: decoy.db")
                    copyDatabaseFromAssetsIfExist(context, dbName)
                    decoyDatabase = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        dbName
                    ).build()
                }
                return decoyDatabase!!
            } else {
                if (realDatabase == null) {
                    Log.d(TAG, "Initializing real database: real.db")
                    copyDatabaseFromAssetsIfExist(context, dbName)
                    realDatabase = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        dbName
                    ).build()
                }
                return realDatabase!!
            }
        }
    }

    fun closeDatabase() {
        // Safe no-op because we do not close active databases during runtime
        // to prevent IllegalStateExceptions inside other active collectors.
        Log.d(TAG, "closeDatabase requested (ignored during active runtime to prevent crash)")
    }

    private fun copyDatabaseFromAssetsIfExist(context: Context, dbName: String) {
        try {
            val dbFile = context.getDatabasePath(dbName)
            if (!dbFile.exists()) {
                // Ensure parent directory exists
                dbFile.parentFile?.mkdirs()

                // Try copying main db file
                val assetName = "databases/$dbName"
                if (assetExists(context, assetName)) {
                    context.assets.open(assetName).use { inputStream ->
                        FileOutputStream(dbFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    Log.d(TAG, "Copied asset $assetName to database path ${dbFile.absolutePath}")
                } else {
                    Log.d(TAG, "Database asset $assetName not found, letting Room build it programmatically.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying database from assets", e)
        }
    }

    private fun assetExists(context: Context, path: String): Boolean {
        return try {
            context.assets.open(path).use { }
            true
        } catch (e: Exception) {
            false
        }
    }
}
