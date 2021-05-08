package com.tau.ephuapp.database.daos

import androidx.room.*
import com.tau.ephuapp.models.ItemCount

@Dao
interface ItemCountDao {
    @Query("SELECT * FROM itemcount WHERE id = CAST(:id AS NUMERIC)")
    fun getById(id: Int): ItemCount?

    @Query("SELECT * FROM itemcount")
    fun getAll(): List<ItemCount>

    @Query("SELECT * FROM itemcount WHERE itemId = CAST(:itemId AS NUMERIC)")
    fun getAllByItem(itemId: Int): List<ItemCount>

    @Query("SELECT * FROM itemcount WHERE ephudeviceid = :deviceId AND uploaded = 0 AND sent = 0")
    fun getAllPendingToUploadByDevice(deviceId: String): List<ItemCount>

    @Query("SELECT * FROM itemcount WHERE ephudeviceid = :deviceId AND uploaded = 1 AND dirty = 1 AND sent = 0")
    fun getAllPendingToUpdateByDevice(deviceId: String): List<ItemCount>

    @Query("SELECT COUNT(*) FROM itemcount WHERE itemId = CAST(:itemId AS NUMERIC)")
    fun countAllByItem(itemId: Int): Int

    @Query("SELECT COUNT(*) FROM itemcount WHERE ephudeviceid = :deviceId AND uploaded = 0 AND dirty = 1 AND sent = 0")
    fun countAllPendingToUploadByDevice(deviceId: String): Int

    @Query("SELECT COUNT(*) FROM itemcount WHERE ephudeviceid = :deviceId AND uploaded = 1 AND dirty = 1 AND sent = 0")
    fun countAllPendingToUpdateByDevice(deviceId: String): Int

    @Query("SELECT COUNT(*) FROM itemcount WHERE ephudeviceid = :deviceId")
    fun countAllByDevice(deviceId: String): Int

    @Query("UPDATE itemcount SET sent = 0, uploaded = 1, dirty = 0, id = :remoteId WHERE localId = :localId")
    fun updateUploaded(localId: String?, remoteId: Int): Int

    @Query("UPDATE itemcount SET dirty = 0, sent = 0 WHERE localId = :localId")
    fun updateUpdated(localId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: ItemCount)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: ArrayList<ItemCount>)

    @Update()
    fun update(item: ItemCount)

    @Update()
    fun updateAll(items: List<ItemCount>)

    @Delete
    fun delete(item: ItemCount)

    @Query("DELETE FROM itemcount")
    fun deleteAll()
}