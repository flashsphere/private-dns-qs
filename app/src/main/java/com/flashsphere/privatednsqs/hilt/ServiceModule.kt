package com.flashsphere.privatednsqs.hilt

import android.app.Service
import com.flashsphere.privatednsqs.service.DefaultTileInfoUpdater
import com.flashsphere.privatednsqs.service.SamsungTileInfoUpdater
import com.flashsphere.privatednsqs.service.TileInfoUpdater
import com.flashsphere.privatednsqs.util.FileOperations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped

@Module
@InstallIn(ServiceComponent::class)
internal object ServiceModule {
    @Provides
    @ServiceScoped
    fun provideTileInfoUpdater(
        context: Service,
        fileOperations: FileOperations,
    ): TileInfoUpdater {
        val defaultTileInfoUpdater = DefaultTileInfoUpdater(context, fileOperations)
        return when {
            SamsungTileInfoUpdater.isApplicable() -> SamsungTileInfoUpdater(defaultTileInfoUpdater)
            else -> defaultTileInfoUpdater
        }
    }
}
