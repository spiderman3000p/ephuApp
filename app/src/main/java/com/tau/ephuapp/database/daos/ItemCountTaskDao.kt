package com.tau.ephuapp.database.daos

import androidx.room.*
import com.tau.ephuapp.models.ItemCount
import com.tau.ephuapp.models.ItemCountTask
import com.tau.ephuapp.models.Task

@Dao
interface ItemCountTaskDao {
    @Query("SELECT * FROM itemcounttask WHERE taskLineId = CAST(:id AS NUMERIC)")
    fun getById(id: Int): ItemCountTask?

    @Query("SELECT * FROM itemcounttask")
    fun getAll(): List<ItemCountTask>

    @Query("SELECT * FROM itemcounttask WHERE taskid = CAST(:taskId AS NUMERIC) AND locationid = CAST(:locationId AS NUMERIC)")
    fun getAllRecountByTaskAndLocation(taskId: Int, locationId: Int): List<ItemCountTask>

    @Query("SELECT * FROM itemcounttask WHERE itemId = CAST(:itemId AS NUMERIC)")
    fun getAllByItem(itemId: Int): List<ItemCountTask>

    @Query("DELETE FROM itemcounttask WHERE taskId = :taskId")
    fun deleteAllByTaskId(taskId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: ItemCountTask)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: ArrayList<ItemCountTask>)

    @Update()
    fun update(item: ItemCountTask)

    @Update()
    fun updateAll(items: List<ItemCountTask>)

    @Delete
    fun delete(item: ItemCountTask)

    @Query("DELETE FROM itemcounttask")
    fun deleteAll()
}