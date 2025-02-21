package com.mytestwork2.models

class AdminDashboardResponse(
    val managedChildren: List<Child>?,    // children the admin manages
    val availableChildren: List<Child>?,    // children not managed
    val schoolName: String?
)
