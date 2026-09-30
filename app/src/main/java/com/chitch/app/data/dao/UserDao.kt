package com.chitch.app.data.dao

import androidx.room.Dao
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.chitch.app.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Upsert
    suspend fun upsert(user: UserEntity)

    @Query("SELECT * FROM user WHERE id = ${UserEntity.SINGLETON_ID} LIMIT 1")
    fun observe(): Flow<UserEntity?>

    @Query("SELECT * FROM user WHERE id = ${UserEntity.SINGLETON_ID} LIMIT 1")
    suspend fun get(): UserEntity?
}
