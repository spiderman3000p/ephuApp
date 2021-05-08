package com.tau.ephuapp.database.daos

import androidx.room.*
import com.tau.ephuapp.models.Item
@Dao
interface ItemDao {
    @Query("SELECT * FROM item")
    fun getAll(): List<Item>

    @Query("SELECT COUNT(*) FROM item")
    fun countAll(): Int

    @Query("SELECT COUNT(*) FROM item WHERE ownerid = CAST(:ownerId AS NUMERIC)")
    fun countAllByOwner(ownerId: Int): Int

    @Query("SELECT * FROM item WHERE id IN (:ids)")
    fun loadAllByIds(ids: IntArray): List<Item>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: Item)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(lines: ArrayList<Item>)

    @Update()
    fun update(item: Item)

    @Delete
    fun delete(item: Item)

    @Query("DELETE FROM item")
    fun deleteAll()

    @Query("DELETE FROM item WHERE ownerId = CAST(:ownerId AS NUMERIC)")
    fun deleteAllByOwner(ownerId: Int)

    @Query("SELECT * FROM item WHERE id = CAST(:id AS NUMERIC)")
    fun getById(id: Int): Item

    @Query("SELECT * FROM item WHERE sku = :str OR ean13 = :str OR ean14 = :str LIMIT 1")
    fun search(str: String): Item?
}