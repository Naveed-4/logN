package com.asloki.logn

data class AppInfo(
    val packageName: String,
    val name: String,
    val isSystemApp: Boolean,
    var isSelected: Boolean
)