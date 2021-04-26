package com.s2labs.shreemagnetfota.api

import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
	@GET("build.php")
	suspend fun buildList(): List<BuildModel>

	@GET("versions.php")
	suspend fun versionList(@Query("buildId") buildId: Long): List<VersionModel>

	@GET("uploads/{fileName}")
	suspend fun downloadOtaFile(@Path("fileName") fileName: String): ResponseBody

	companion object {
		private const val BASE_URL = "https://magnetdoctor.com/shree-magnets/server/"
		private var client: ApiService? = null

		fun getInstance(): ApiService {
			if (client == null) {
				val rf = Retrofit.Builder()
					.baseUrl(BASE_URL)
					.addConverterFactory(GsonConverterFactory.create()).build()
				client = rf.create(ApiService::class.java)
			}

			return client!!
		}
	}
}