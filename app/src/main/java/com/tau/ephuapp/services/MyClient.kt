package com.tau.ephuapp.services

import android.content.Context
import retrofit2.Retrofit

class MyClient {
    companion object {
        var retrofit: Retrofit? = null

        fun getInstance(context: Context): Retrofit {
            if (retrofit == null) {
                val okHttpClient: okhttp3.OkHttpClient = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(2, java.util.concurrent.TimeUnit.MINUTES)
                    .readTimeout(2, java.util.concurrent.TimeUnit.MINUTES)
                    .writeTimeout(2, java.util.concurrent.TimeUnit.MINUTES)
                    .build()

                retrofit = Retrofit.Builder()
                    .baseUrl(MySettings.getInstance(context).baseUrl!!)
                    .client(okHttpClient)
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .build()
            }
            return retrofit!!
        }
    }
}