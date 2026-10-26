package com.gumah.nooran.data

import android.util.Log

enum class OperationType {
    GET, LIST, CREATE, UPDATE, DELETE
}

fun handleFirestoreError(error: Exception, operationType: OperationType, path: String) {
    val logData = mapOf(
        "error" to (error.message ?: "Unknown error"),
        "operation" to operationType.name,
        "path" to path,
        "type" to error.javaClass.simpleName
    )
    Log.e("NooranFirestore", "Firestore error: $logData", error)
}
