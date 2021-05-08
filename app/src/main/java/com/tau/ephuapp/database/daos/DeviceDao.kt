package com.tau.ephuapp.database.daos

import androidx.room.*
import com.tau.ephuapp.models.Device

@Dao
interface DeviceDao {
    @Query("SELECT * FROM device")
    fun getAll(): List<Device>

    @Query("SELECT * FROM device WHERE code = :deviceId")
    fun getByDevice(deviceId: String): Device?

    @Query("SELECT COUNT(*) FROM device WHERE code = :deviceId")
    fun countAllByDevice(deviceId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(device: Device)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(device: ArrayList<Device>)

    @Update()
    fun update(device: Device)

    @Delete
    fun delete(device: Device)

    @Query("DELETE FROM device")
    fun deleteAll()

    @Query("SELECT * FROM device WHERE id = CAST(:id AS NUMERIC)")
    fun getById(id: Int): Device
}