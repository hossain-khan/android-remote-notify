package app.example.data

import android.content.Context
import app.example.di.ApplicationContext
import javax.inject.Inject

// Example service class that does not need DI module or binding
class ExampleAppVersionService
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val versionName: String = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"

        fun getApplicationVersion(): String = versionName
    }
