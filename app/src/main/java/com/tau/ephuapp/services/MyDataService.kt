package com.tau.ephuapp.services

import com.tau.ephuapp.models.*
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface MyDataService {
    @GET
    @Headers("Content-Type: application/json")
    fun getTasks(
        @Url url: String
    ): Call<ArrayList<Task>>

    @GET
    @Headers("Content-Type: application/json")
    fun getTaskLines(
        @Url url: String
    ): Call<ArrayList<Location>>

    @GET
    @Headers("Content-Type: application/json")
    fun getItems(
        @Url url: String
    ): Call<ArrayList<Item>>

    @GET
    @Headers("Content-Type: application/json")
    fun getDevice(
        @Url url: String
    ): Call<Device>

    @POST
    @Multipart
    fun uploadFile(
        @Url url: String,
        @Part filePart: MultipartBody.Part
    ): Call<Void>

    @POST
    @Headers("Content-Type: application/json")
    fun saveCount(
        @Body item: ItemCount,
        @Url url: String = "contarItemApp"
    ): Call<Void>

    @POST
    @Headers("Content-Type: application/json")
    fun editTaskState(
            @Url url: String
    ): Call<Void>

    @POST
    @Headers("Content-Type: application/json")
    fun editCount(
            @Url url: String,
            @Body count: ItemCount,
    ): Call<ItemCount>

    @POST
    @Headers("Content-Type: application/json")
    fun editCountWithVoidResponse(
            @Url url: String,
            @Body count: ItemCount,
    ): Call<Void>

    @DELETE
    @Headers("Content-Type: application/json")
    fun deleteCount(
            @Url url: String
    ): Call<Void>

    @POST
    @Headers("Content-Type: application/json")
    fun saveCounts(
        @Body counts: List<ItemCount>,
        @Url url: String = "contarItemApp"
    ): Call<List<ItemCount>>
}