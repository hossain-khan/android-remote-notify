package dev.hossain.remotenotify.di

import android.content.Context
import androidx.room.Room
import dev.hossain.remotenotify.db.AlertCheckLogDao
import dev.hossain.remotenotify.db.AlertConfigDao
import dev.hossain.remotenotify.db.AppDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Module
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@Module
@ContributesTo(AppScope::class)
object DatabaseModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(context, AppDatabase::class.java, "notify_app.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideNotificationDao(database: AppDatabase): AlertConfigDao = database.notificationDao()

    @Provides
    fun provideAlertCheckLogDao(database: AppDatabase): AlertCheckLogDao = database.alertCheckLogDao()
}
