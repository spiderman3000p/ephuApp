package com.tau.ephuapp.database.daos

import androidx.room.*
import com.tau.ephuapp.models.FetchedDataHistory

@Dao
interface FetchedDataHistoryDao {
    @Query("SELECT * FROM fetcheddatahistory")
    fun getAll(): List<FetchedDataHistory>

    @Query("SELECT * FROM fetcheddatahistory WHERE tag = :tag")
    fun getByTag(tag: String): FetchedDataHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(history: FetchedDataHistory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(historyRecords: ArrayList<FetchedDataHistory>)

    @Update()
    fun update(history: FetchedDataHistory)

    @Delete
    fun delete(history: FetchedDataHistory)

    @Query("DELETE FROM fetcheddatahistory")
    fun deleteAll()
}