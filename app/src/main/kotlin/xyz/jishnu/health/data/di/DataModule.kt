package xyz.jishnu.health.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import xyz.jishnu.health.data.local.FastingSessionDao
import xyz.jishnu.health.data.local.IntermDatabase
import xyz.jishnu.health.data.local.MIGRATION_3_4
import xyz.jishnu.health.data.local.ProfileDataStore
import xyz.jishnu.health.data.local.SettingsDataStore
import xyz.jishnu.health.data.local.WaterEntryDao
import xyz.jishnu.health.data.local.WeightEntryDao
import xyz.jishnu.health.data.repo.DataStoreProfileRepository
import xyz.jishnu.health.data.repo.DataStoreSettingsRepository
import xyz.jishnu.health.data.repo.FastingRepository
import xyz.jishnu.health.data.repo.LocalFastingRepository
import xyz.jishnu.health.data.repo.LocalWaterRepository
import xyz.jishnu.health.data.repo.LocalWeightRepository
import xyz.jishnu.health.data.repo.ProfileRepository
import xyz.jishnu.health.data.repo.SettingsRepository
import xyz.jishnu.health.data.repo.WaterRepository
import xyz.jishnu.health.data.repo.WeightRepository
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): IntermDatabase =
        Room.databaseBuilder(ctx, IntermDatabase::class.java, "interm.db")
            .addMigrations(MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideFastingSessionDao(db: IntermDatabase): FastingSessionDao = db.fastingSessionDao()
    @Provides fun provideWeightEntryDao(db: IntermDatabase): WeightEntryDao = db.weightEntryDao()
    @Provides fun provideWaterEntryDao(db: IntermDatabase): WaterEntryDao = db.waterEntryDao()

    @Provides @Singleton @Named("settings")
    fun provideSettingsDataStorePreferences(@ApplicationContext ctx: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { ctx.preferencesDataStoreFile("interm_settings") },
        )

    @Provides @Singleton
    fun provideSettingsDataStore(@Named("settings") store: DataStore<Preferences>): SettingsDataStore =
        SettingsDataStore(store)

    @Provides @Singleton @Named("profile")
    fun provideProfileDataStorePreferences(@ApplicationContext ctx: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { ctx.preferencesDataStoreFile("interm_profile") },
        )

    @Provides @Singleton
    fun provideProfileDataStore(@Named("profile") store: DataStore<Preferences>): ProfileDataStore =
        ProfileDataStore(store)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepoModule {
    @Binds @Singleton abstract fun bindFastingRepository(impl: LocalFastingRepository): FastingRepository
    @Binds @Singleton abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository
    @Binds @Singleton abstract fun bindProfileRepository(impl: DataStoreProfileRepository): ProfileRepository
    @Binds @Singleton abstract fun bindWeightRepository(impl: LocalWeightRepository): WeightRepository
    @Binds @Singleton abstract fun bindWaterRepository(impl: LocalWaterRepository): WaterRepository
}
