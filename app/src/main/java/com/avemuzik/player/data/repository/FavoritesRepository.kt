package com.avemuzik.player.data.repository

import com.avemuzik.player.data.database.FavoriteDao
import com.avemuzik.player.data.database.FavoriteEntity
import kotlinx.coroutines.flow.Flow

class FavoritesRepository(private val dao: FavoriteDao) {

    /** Son eklenen en üstte. */
    val favoriteIds: Flow<List<Long>> = dao.observeIds()

    /** @return işlemden sonra şarkı favori mi */
    suspend fun toggle(songId: Long): Boolean =
        if (dao.isFavorite(songId)) {
            dao.delete(songId)
            false
        } else {
            dao.insert(FavoriteEntity(songId, System.currentTimeMillis()))
            true
        }
}
