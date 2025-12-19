package com.yhm.mail_client.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yhm.mail_client.data.model.Email
import com.yhm.mail_client.data.model.EmailAccount

@Database(
    entities = [Email::class, EmailAccount::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun emailDao(): EmailDao
    abstract fun accountDao(): AccountDao
    
    companion object {
        /**
         * Migration from version 1 to 2: Add SMTP fields to accounts table
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add SMTP configuration fields to accounts table
                database.execSQL("ALTER TABLE accounts ADD COLUMN smtpHost TEXT NOT NULL DEFAULT 'localhost'")
                database.execSQL("ALTER TABLE accounts ADD COLUMN smtpPort INTEGER NOT NULL DEFAULT 25")
                database.execSQL("ALTER TABLE accounts ADD COLUMN smtpUseSsl INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        /**
         * Migration from version 2 to 3: Add mailbox type, starred, and draft fields to emails table
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add mailbox categorization fields to emails table
                database.execSQL("ALTER TABLE emails ADD COLUMN mailboxType TEXT NOT NULL DEFAULT 'INBOX'")
                database.execSQL("ALTER TABLE emails ADD COLUMN isStarred INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE emails ADD COLUMN isDraft INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
