package com.tau.ephuapp.database.daos

import androidx.room.*
import com.tau.ephuapp.models.ItemCount
import com.tau.ephuapp.models.Location
@Dao
interface TaskLocationDao {
    @Query("SELECT * FROM location")
    fun getAll(): List<Location>

    @Query("SELECT * FROM location WHERE taskId = CAST(:taskId AS NUMERIC) ORDER BY lane, columnAt, height ASC")
    fun getAllByTask(taskId: Int): List<Location>

    @Query("SELECT COUNT(*) FROM location WHERE taskId = CAST(:taskId AS NUMERIC)")
    fun countAllByTask(taskId: Int): Int

    @Query("SELECT * FROM location WHERE id IN (:ids)")
    fun loadAllByIds(ids: IntArray): List<Location>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(location: Location)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(locations: ArrayList<Location>)

    @Update()
    fun update(location: Location)

    @Delete
    fun delete(location: Location)

    @Query("DELETE FROM location")
    fun deleteAll()

    @Query("DELETE FROM location WHERE taskId = CAST(:taskId AS NUMERIC)")
    fun deleteAllByTask(taskId: Int)

    @Query("SELECT * FROM location WHERE id = CAST(:id AS NUMERIC)")
    fun getById(id: Int): Location

    @Query("SELECT * FROM itemcount WHERE locationId = CAST(:locationId AS NUMERIC) AND taskId = CAST(:taskId AS NUMERIC)")
    fun getLocationCounts(locationId: Int, taskId: Int): List<ItemCount>
}