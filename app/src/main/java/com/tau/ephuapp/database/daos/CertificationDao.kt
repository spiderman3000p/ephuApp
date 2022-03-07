package com.tau.ephuapp.database.daos

import androidx.room.*
import com.tau.ephuapp.models.Certification
import com.tau.ephuapp.models.CertificationTaskItem
import com.tau.ephuapp.models.Task

@Dao
interface CertificationDao {
    @Query("SELECT * FROM certification WHERE itemId = CAST(:id AS NUMERIC)")
    fun getById(id: Int): Certification?
    
    @Query("UPDATE certification SET uploaded = 1, dirty = 0  WHERE itemId = CAST(:itemId AS NUMERIC) AND taskId = CAST(:taskId AS NUMERIC)")
    fun setAsUploaded(itemId: Int, taskId: Int): Int

    @Query("SELECT * FROM certification")
    fun getAll(): List<Certification>

    @Query("SELECT * FROM certification WHERE taskid = CAST(:taskId AS NUMERIC)")
    fun getAllByTask(taskId: Int): List<Certification>

    @Query("SELECT *, SUM(certification.quantity) as totalQuantity FROM certificationtaskitem A INNER JOIN certification ON certification.itemId = A.itemId  WHERE certification.taskid = CAST(:taskId AS NUMERIC) GROUP BY A.itemId")
    fun getAllByTaskGroupedByItemId(taskId: Int): List<CertificationTaskItem>?

    @Query("SELECT * FROM certification WHERE uploaded = 0")
    fun getAllPendingToUpload(): List<Certification>

    @Query("SELECT * FROM certification WHERE taskid = :taskId AND uploaded = 0")
    fun getAllPendingToUploadByTask(taskId: Int): List<Certification>

    @Query("SELECT * FROM certification WHERE uploaded = 1")
    fun getAllUploaded(): List<Certification>

    @Query("SELECT * FROM certification WHERE taskid = :taskId AND uploaded = 1")
    fun getAllUploadedByTask(taskId: Int): List<Certification>

    @Query("SELECT * FROM certification WHERE uploaded = 1 AND dirty = 1")
    fun getAllPendingToUpdate(): List<Certification>

    @Query("SELECT * FROM certification WHERE taskid = :taskId AND uploaded = 1 AND dirty = 1")
    fun getAllPendingToUpdateByTask(taskId: Int): List<Certification>

    @Query("SELECT COUNT(*) FROM certification WHERE itemId = CAST(:itemId AS NUMERIC)")
    fun countAllByItem(itemId: Int): Int

    @Query("SELECT COUNT(*) FROM certification WHERE taskId = CAST(:taskId AS NUMERIC)")
    fun countAllByTask(taskId: Int): Int

    @Query("SELECT COUNT(*) FROM certification WHERE uploaded = 0 OR (uploaded = 1 AND dirty = 1)")
    fun countAllPendingToUpload(): Int

    @Query("SELECT COUNT(*) FROM certification WHERE (uploaded = 0 OR (uploaded = 1 AND dirty = 1)) AND hasError = 1")
    fun countAllNotUploadedWithError(): Int

    @Query("SELECT COUNT(*) FROM certification WHERE taskid = :taskId AND uploaded = 0 AND dirty = 1")
    fun countAllPendingToUploadByTask(taskId: Int): Int

    @Query("DELETE FROM certification WHERE taskId = :taskId")
    fun deleteAllByTask(taskId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: Certification)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: ArrayList<Certification>)

    @Update()
    fun update(item: Certification)

    @Update()
    fun updateAll(items: List<Certification>)

    @Delete
    fun delete(item: Certification)

    @Query("DELETE FROM certification")
    fun deleteAll()
}