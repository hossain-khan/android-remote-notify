package dev.hossain.remotenotify.di

import android.content.Context
import androidx.room.Room
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.optional.SingleIn
import dagger.Module
import dagger.Provides
import dev.hossain.remotenotify.db.AppDatabase
import dev.hossain.remotenotify.db.NotificationDao

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
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideNotificationDao(database: AppDatabase): NotificationDao = database.notificationDao()
}
