package com.tau.ephuapp.database.daos

import androidx.room.*
import com.tau.ephuapp.models.DeliveryLine

@Dao
interface DeliveryLineDao {
    @Query("SELECT * FROM deliveryline")
    fun getAll(): List<DeliveryLine>

    @Query("SELECT * FROM deliveryline WHERE deliveryId = CAST(:deliveryId AS NUMERIC)")
    fun getByDelivery(deliveryId: Long): List<DeliveryLine>

    @Query("SELECT * FROM deliveryline where uploaded = 0")
    fun getAllToUpload(): List<DeliveryLine>

    @Query("SELECT * FROM deliveryline WHERE id IN (:ids)")
    fun loadAllByIds(ids: LongArray): List<DeliveryLine>

    @Query("SELECT COUNT(*) > 0 FROM deliveryline WHERE id = CAST(:id AS NUMERIC) AND `index` = CAST(:index AS NUMERIC)")
    fun exists(id: Long, index: Long): Boolean

    @Query("SELECT * FROM deliveryline WHERE id = CAST(:id AS NUMERIC)")
    fun get(id: Long): List<DeliveryLine>

    @Query("SELECT * FROM deliveryline WHERE id = CAST(:id AS NUMERIC) AND `index` = CAST(:index AS NUMERIC) AND planificationId = CAST(:planificationId AS NUMERIC) LIMIT 1")
    fun getByIdAndIndex(id: Long, index: Int, planificationId: Long): DeliveryLine

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(deliveryLine: DeliveryLine)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(deliveryLines: MutableList<DeliveryLine>)

    @Update
    fun update(deliveryLine: DeliveryLine)

    @Delete
    fun delete(deliveryLine: DeliveryLine)

    @Query("DELETE FROM deliveryLine")
    fun deleteAll()

    @Query("SELECT * FROM deliveryline WHERE planificationId = CAST(:planificationId AS NUMERIC)")
    fun getAllByPlanification(planificationId: Long): List<DeliveryLine>

    @Query("SELECT * FROM deliveryline WHERE id IN (:ids)")
    fun getAllByIds(ids: LongArray): List<DeliveryLine>
}