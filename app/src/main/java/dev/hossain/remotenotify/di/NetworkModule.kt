package dev.hossain.remotenotify.di

import dev.hossain.remotenotify.BuildConfig
import me.tatarka.inject.annotations.ContributesTo
import me.tatarka.inject.annotations.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

@ContributesTo(AppScope::class)
interface NetworkModule {
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor =
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

        return OkHttpClient
            .Builder()
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(loggingInterceptor)
                }
            }.build()
    }
}
