package com.football.news.livescore.di

import android.content.Context
import androidx.room.Room
import com.football.news.livescore.data.local.AppDatabase
import com.football.news.livescore.data.local.dao.FavoriteDao
import com.football.news.livescore.data.local.dao.MatchDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "livescore_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    fun provideMatchDao(database: AppDatabase): MatchDao {
        return database.matchDao()
    }
}
