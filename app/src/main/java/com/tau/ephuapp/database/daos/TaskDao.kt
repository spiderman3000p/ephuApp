package com.tau.ephuapp.database.daos

import androidx.room.*
import com.tau.ephuapp.models.Task
@Dao
interface TaskDao {
    @Query("SELECT * FROM task")
    fun getAll(): List<Task>

    @Query("SELECT * FROM task WHERE deviceCode = :deviceId")
    fun getAllByDevice(deviceId: String): List<Task>

    @Query("SELECT COUNT(*) FROM task WHERE deviceCode = :deviceId")
    fun countAllByDevice(deviceId: String): Int

    @Query("SELECT * FROM task WHERE id IN (:ids)")
    fun loadAllByIds(ids: IntArray): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(tasks: ArrayList<Task>)

    @Update()
    fun update(task: Task)

    @Delete
    fun delete(task: Task)

    @Query("DELETE FROM task")
    fun deleteAll()

    @Query("SELECT * FROM task WHERE id = CAST(:id AS NUMERIC)")
    fun getById(id: Int): Task
}