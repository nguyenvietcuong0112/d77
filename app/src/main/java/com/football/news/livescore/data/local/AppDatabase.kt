package com.football.news.livescore.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.football.news.livescore.data.local.dao.FavoriteDao
import com.football.news.livescore.data.local.dao.MatchDao
import com.football.news.livescore.data.local.entity.CachedMatchEntity
import com.football.news.livescore.data.local.entity.FavoriteLeagueEntity
import com.football.news.livescore.data.local.entity.FavoriteTeamEntity

@Database(
    entities = [
        FavoriteTeamEntity::class,
        FavoriteLeagueEntity::class,
        CachedMatchEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun matchDao(): MatchDao
}
