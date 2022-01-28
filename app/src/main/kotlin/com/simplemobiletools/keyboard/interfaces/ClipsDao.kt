package com.simplemobiletools.keyboard.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simplemobiletools.keyboard.models.Clip

@Dao
interface ClipsDao {
    @Query("SELECT * FROM clips ORDER BY id")
    fun getClips(): List<Clip>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(clip: Clip): Long

    @Query("DELETE FROM clips WHERE id = :id")
    fun delete(id: Long)
}
