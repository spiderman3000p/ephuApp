package com.tau.ephuapp.database.daos

import androidx.room.*
import com.tau.ephuapp.models.Task
import com.tau.ephuapp.models.TaskParameter

@Dao
interface TaskParameterDao {
    @Query("SELECT * FROM taskparameter WHERE taskId = CAST(:taskId AS NUMERIC)")
    fun getAllByTask(taskId: Int): List<TaskParameter>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(parameters: TaskParameter)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(params: List<TaskParameter>)

    @Update()
    fun update(param: TaskParameter)

    @Delete
    fun delete(param: TaskParameter)

    @Query("DELETE FROM taskparameter")
    fun deleteAll()

    @Query("DELETE FROM taskparameter WHERE taskId = CAST(:taskId AS NUMERIC)")
    fun deleteAllByTask(taskId: Int)
}