package com.tau.ephuapp.database.daos

import androidx.room.*
import com.tau.ephuapp.models.ItemCount
import com.tau.ephuapp.models.Task

@Dao
interface ItemCountDao {
    @Query("SELECT * FROM itemcount WHERE id = CAST(:id AS NUMERIC)")
    fun getById(id: Int): ItemCount?

    @Query("SELECT * FROM itemcount WHERE tasklineid = CAST(:id AS NUMERIC)")
    fun getByTaskLineId(id: Int): ItemCount?

    @Query("SELECT * FROM itemcount WHERE tasklineid = CAST(:taskLineId AS NUMERIC) AND itemid = CAST(:itemId AS NUMERIC)")
    fun getByTaskLineAndItem(taskLineId: Int, itemId: Int): ItemCount?

    @Query("SELECT * FROM itemcount WHERE localid = :id")
    fun getByLocalId(id: String): ItemCount?

    @Query("SELECT * FROM itemcount ORDER BY readTimestamp DESC")
    fun getAll(): List<ItemCount>

    @Query("SELECT * FROM itemcount WHERE recount = 1 AND taskid = CAST(:taskId AS NUMERIC) AND locationid = CAST(:locationId AS NUMERIC) ORDER BY readTimestamp DESC")
    fun getAllRecountByTaskAndLocation(taskId: Int, locationId: Int): List<ItemCount>

    @Query("SELECT * FROM itemcount WHERE itemId = CAST(:itemId AS NUMERIC) ORDER BY readTimestamp DESC")
    fun getAllByItem(itemId: Int): List<ItemCount>

    @Query("SELECT * FROM itemcount WHERE taskid = CAST(:taskId AS NUMERIC) ORDER BY readTimestamp DESC")
    fun getAllByTask(taskId: Int): List<ItemCount>

    @Query("SELECT * FROM itemcount WHERE uploaded = 0 ORDER BY readTimestamp DESC")
    fun getAllPendingCountsAndRecountsToUpload(): List<ItemCount>

    @Query("SELECT * FROM itemcount WHERE ephudeviceid = :deviceId AND uploaded = 0 ORDER BY readTimestamp DESC")
    fun getAllPendingToUploadByDevice(deviceId: String): List<ItemCount>

    @Query("SELECT * FROM itemcount WHERE taskid = :taskId AND uploaded = 0 ORDER BY readTimestamp DESC")
    fun getAllPendingToUploadByTask(taskId: Int): List<ItemCount>

    @Query("SELECT * FROM itemcount WHERE taskid = :taskId AND uploaded = 0 AND recount = 1 ORDER BY readTimestamp DESC")
    fun getAllPendingRecountToUploadByTask(taskId: Int): List<ItemCount>

    @Query("SELECT * FROM itemcount WHERE ephudeviceid = :deviceId AND uploaded = 1 AND dirty = 1 ORDER BY readTimestamp DESC")
    fun getAllPendingToUpdateByDevice(deviceId: String): List<ItemCount>

    @Query("SELECT * FROM itemcount WHERE ephudeviceid = :deviceId AND uploaded = 1 AND dirty = 1 AND recount = 1 ORDER BY readTimestamp DESC")
    fun getAllPendingRecountToUpdateByDevice(deviceId: String): List<ItemCount>

    @Query("SELECT * FROM itemcount WHERE uploaded = 1 AND dirty = 1 ORDER BY readTimestamp DESC")
    fun getAllPendingCountsAndRecountsToUpdate(): List<ItemCount>

    @Query("SELECT * FROM itemcount WHERE taskid = :taskId AND uploaded = 1 AND dirty = 1 ORDER BY readTimestamp DESC")
    fun getAllPendingToUpdateByTask(taskId: Int): List<ItemCount>

    @Query("SELECT * FROM itemcount WHERE taskid = :taskId AND uploaded = 1 AND dirty = 1 AND recount = 1 ORDER BY readTimestamp DESC")
    fun getAllPendingRecountToUpdateByTask(taskId: Int): List<ItemCount>

    @Query("SELECT * FROM itemcount WHERE ephudeviceid = :deviceId AND uploaded = 0 OR (uploaded = 1 AND dirty = 1) ORDER BY readTimestamp DESC")
    fun getAllNotUploadedByDevice(deviceId: String): List<ItemCount>

    @Query("SELECT COUNT(*) FROM itemcount WHERE itemId = CAST(:itemId AS NUMERIC)")
    fun countAllByItem(itemId: Int): Int

    @Query("SELECT COUNT(*) FROM itemcount WHERE ephudeviceid = :deviceId AND uploaded = 0 OR (uploaded = 1 AND dirty = 1)")
    fun countAllNotUploadedByDevice(deviceId: String): Int

    @Query("SELECT COUNT(*) FROM itemcount WHERE ephuDeviceId = :deviceId AND (uploaded = 0 OR (uploaded = 1 AND dirty = 1)) AND hasError = 1")
    fun countAllNotUploadedWithError(deviceId: String): Int

    @Query("SELECT COUNT(*) FROM itemcount WHERE ephudeviceid = :deviceId AND uploaded = 0 AND dirty = 1")
    fun countAllPendingToUploadByDevice(deviceId: String): Int

    @Query("SELECT COUNT(*) FROM itemcount WHERE ephudeviceid = :deviceId AND uploaded = 0 AND dirty = 1 AND recount = 1")
    fun countAllPendingRecountToUploadByDevice(deviceId: String): Int

    @Query("SELECT COUNT(*) FROM itemcount WHERE taskid = :taskId AND uploaded = 0 AND dirty = 1")
    fun countAllPendingToUploadByTask(taskId: Int): Int

    @Query("SELECT COUNT(*) FROM itemcount WHERE taskid = :taskId AND uploaded = 0 AND dirty = 1 AND recount = 1")
    fun countAllPendingRecountToUploadByTask(taskId: Int): Int

    @Query("SELECT COUNT(*) FROM itemcount WHERE ephudeviceid = :deviceId AND uploaded = 1 AND dirty = 1")
    fun countAllPendingToUpdateByDevice(deviceId: String): Int

    @Query("SELECT COUNT(*) FROM itemcount WHERE ephudeviceid = :deviceId AND uploaded = 1 AND dirty = 1 AND recount = 1")
    fun countAllPendingRecountToUpdateByDevice(deviceId: String): Int

    @Query("SELECT COUNT(*) FROM itemcount WHERE taskid = :taskId AND uploaded = 1 AND dirty = 1")
    fun countAllPendingToUpdateByTask(taskId: Int): Int

    @Query("SELECT COUNT(*) FROM itemcount WHERE taskid = :taskId AND uploaded = 1 AND dirty = 1 AND recount = 1")
    fun countAllPendingRecountToUpdateByTask(taskId: Int): Int

    @Query("SELECT COUNT(*) FROM itemcount WHERE ephudeviceid = :deviceId")
    fun countAllByDevice(deviceId: String): Int

    @Query("SELECT COUNT(*) FROM itemcount WHERE ephudeviceid = :deviceId AND recount = 1")
    fun countAllRecountByDevice(deviceId: String): Int

    @Query("SELECT COUNT(*) FROM itemcount WHERE taskid = :taskId")
    fun countAllByTask(taskId: Int): Int

    @Query("SELECT COUNT(*) FROM itemcount WHERE taskid = :taskId AND recount = 1")
    fun countAllRecountByTask(taskId: Int): Int

    @Query("UPDATE itemcount SET sent = 0, haserror = 0, errormessage = '', uploaded = 1, dirty = 0, id = :remoteId WHERE localId = :localId")
    fun updateUploaded(localId: String?, remoteId: Int): Int

    @Query("UPDATE itemcount SET sent = 0, haserror = 1, errormessage = :errorMessage, dirty = 0 WHERE localId = :localId")
    fun updateWithError(errorMessage: String, localId: String?): Int

    @Query("UPDATE itemcount SET sent = 0, uploaded = 1, dirty = 0, id = :remoteId WHERE localId = :localId AND recount = 1")
    fun updateUploadedRecount(localId: String?, remoteId: Int): Int

    @Query("UPDATE itemcount SET dirty = 0, sent = 0 WHERE localId = :localId")
    fun updateUpdated(localId: String): Int

    @Query("UPDATE itemcount SET dirty = 0, sent = 0, haserror = 1, errormessage = :errorMessage WHERE localId = :localId")
    fun updateUpdatedWithError(errorMessage: String, localId: String): Int

    @Query("UPDATE itemcount SET dirty = 0, sent = 0 WHERE localId = :localId AND recount = 1")
    fun updateUpdatedRecount(localId: String): Int

    @Query("SELECT * FROM itemcount WHERE localid IN (:ids) ORDER BY readTimestamp DESC")
    fun loadAllByLocalIds(ids: Array<String>): List<ItemCount>

    @Query("SELECT * FROM itemcount WHERE localid IN (:ids) AND recount = 1 ORDER BY readTimestamp DESC")
    fun loadAllRecountsByLocalIds(ids: Array<String>): List<ItemCount>

    @Query("DELETE FROM itemcount WHERE taskId = :taskId")
    fun deleteAllByTaskId(taskId: Int)

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