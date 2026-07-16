package com.example.textez.models

data class Version(
    val versionNumber: Int,
    val versionName: String,
    val fileName: String,
    val snapshotFileName: String,
    val createdAt: Long
)