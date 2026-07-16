package com.example.textez.models

data class Version(
    val versionNumber: Int,
    val versionName: String,
    val fileName: String,
    val storageFileName: String,
    val createdAt: Long,
    val isBaseVersion: Boolean,
    val previousVersionNumber: Int?
)