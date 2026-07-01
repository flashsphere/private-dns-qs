package com.flashsphere.privatednsqs.hilt

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.svg.SvgDecoder
import com.flashsphere.privatednsqs.datastore.SettingsMigration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
internal object ApplicationModule {
    @Provides
    fun provideDataStore(@ApplicationContext context: Context, json: Json): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            ),
            migrations = listOf(
                SharedPreferencesMigration(context, "togglestates"),
                SettingsMigration(context, json),
            ),
            produceFile = { context.preferencesDataStoreFile("settings") }
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            exceptionsWithDebugInfo = true
        }
    }

    @Provides
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .diskCachePolicy(CachePolicy.DISABLED)
            .build()
    }

    @IoDispatcher
    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @ComputeDispatcher
    @Provides
    fun provideComputeDispatcher(): CoroutineDispatcher = Dispatchers.Default
}