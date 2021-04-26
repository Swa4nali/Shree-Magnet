package com.s2labs.shreemagnetfota.api

data class BuildModel (
	var id: Long = 0,
	var name: String? = null,
	var desc: String? = null
)

data class VersionModel(
	var versionId: Long = 0,
	var version: Int = 0,
	var filePath: String? = null
)