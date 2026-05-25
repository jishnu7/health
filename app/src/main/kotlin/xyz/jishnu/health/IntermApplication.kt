package xyz.jishnu.health

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import xyz.jishnu.health.data.seed.MockSeeder
import javax.inject.Inject

@HiltAndroidApp
class IntermApplication : Application() {

    @Inject lateinit var seeder: MockSeeder

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) seeder.seedIfEmpty()
    }
}
