package com.tau.ephuapp.database.daos

import androidx.room.*
import com.tau.ephuapp.models.CertificationTaskItem
import com.tau.ephuapp.models.Task

@Dao
interface CertificationTaskItemDao {
    @Query("SELECT * FROM certificationtaskitem WHERE itemId = CAST(:id AS NUMERIC)")
    fun getById(id: Int): CertificationTaskItem?

    @Query("UPDATE certification SET uploaded = 1, dirty = 0  WHERE itemId = CAST(:itemId AS NUMERIC) AND taskId = CAST(:taskId AS NUMERIC)")
    fun setAsUploaded(itemId: Int, taskId: Int): Int

    @Query("UPDATE certificationtaskitem SET taskQuantity = taskQuantity + CAST(:quantity AS NUMERIC)  WHERE itemId = CAST(:itemId AS NUMERIC) AND taskId = CAST(:taskId AS NUMERIC)")
    fun updateTaskQuantity(itemId: Int, taskId: Int, quantity: Int): Int

    @Query("SELECT * FROM certificationtaskitem")
    fun getAll(): List<CertificationTaskItem>

    @Query("SELECT * FROM certificationtaskitem WHERE itemId IN (:ids)")
    fun loadAllByIds(ids: IntArray): List<CertificationTaskItem>

    @Query("SELECT * FROM certificationtaskitem WHERE taskid = CAST(:taskId AS NUMERIC)")
    fun getAllByTask(taskId: Int): List<CertificationTaskItem>

    @Query("SELECT * FROM certificationtaskitem WHERE itemId NOT IN (SELECT itemId FROM certification)")
    fun getAllPendingToCertificate(): List<CertificationTaskItem>

    @Query("SELECT * FROM certificationtaskitem WHERE itemId IN (SELECT itemId FROM certification WHERE uploaded = 0)")
    fun getAllPendingToUpload(): List<CertificationTaskItem>

    @Query("SELECT * FROM certificationtaskitem WHERE taskid = :taskId AND itemId IN (SELECT itemId FROM certification WHERE uploaded = 0)")
    fun getAllPendingToUploadByTask(taskId: Int): List<CertificationTaskItem>

    @Query("SELECT * FROM certificationtaskitem WHERE itemId IN (SELECT itemId FROM certification WHERE uploaded = 1)")
    fun getAllUploaded(): List<CertificationTaskItem>

    @Query("SELECT * FROM certificationtaskitem WHERE taskid = :taskId AND itemId IN (SELECT itemId FROM certification WHERE uploaded = 1)")
    fun getAllUploadedByTask(taskId: Int): List<CertificationTaskItem>

    @Query("SELECT * FROM certificationtaskitem WHERE itemId IN (SELECT itemId FROM certification WHERE uploaded = 1 AND dirty = 1)")
    fun getAllPendingToUpdate(): List<CertificationTaskItem>

    @Query("SELECT * FROM certificationtaskitem WHERE taskid = :taskId AND itemId IN (SELECT itemId FROM certification WHERE uploaded = 1 AND dirty = 1)")
    fun getAllPendingToUpdateByTask(taskId: Int): List<CertificationTaskItem>

    @Query("SELECT COUNT(*) FROM certificationtaskitem WHERE itemId = CAST(:itemId AS NUMERIC)")
    fun countAllByItem(itemId: Int): Int

    @Query("SELECT COUNT(*) FROM certification WHERE uploaded = 0 OR (uploaded = 1 AND dirty = 1)")
    fun countAllPendingToUpload(): Int

    @Query("SELECT COUNT(*) FROM certification WHERE (uploaded = 0 OR (uploaded = 1 AND dirty = 1)) AND hasError = 1")
    fun countAllNotUploadedWithError(): Int

    @Query("SELECT COUNT(*) FROM certification WHERE taskid = :taskId AND uploaded = 0 AND dirty = 1")
    fun countAllPendingToUploadByTask(taskId: Int): Int

    @Query("DELETE FROM certificationtaskitem WHERE taskId = :taskId")
    fun deleteAllByTaskId(taskId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: CertificationTaskItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: ArrayList<CertificationTaskItem>)

    @Update()
    fun update(item: CertificationTaskItem)

    @Update()
    fun updateAll(items: List<CertificationTaskItem>)

    @Delete
    fun delete(item: CertificationTaskItem)

    @Query("DELETE FROM certificationtaskitem")
    fun deleteAll()
}