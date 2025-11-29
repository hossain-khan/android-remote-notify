package dev.hossain.remotenotify.di

import com.squareup.moshi.Moshi
import dev.hossain.remotenotify.BuildConfig
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

@BindingContainer
object NetworkBindings {
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

    @Provides
    fun provideMoshi(): Moshi =
        Moshi
            .Builder()
            .build()
}
