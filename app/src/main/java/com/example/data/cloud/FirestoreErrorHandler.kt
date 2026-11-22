package com.example.data.cloud

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

object FirestoreErrorHandler {
    private const val TAG = "FirestoreError"

    fun handleFirestoreError(
        error: Throwable,
        operationType: OperationType,
        path: String?
    ): String {
        val currentUser = try {
            FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) {
            null
        }
        val payload = JSONObject().apply {
            put("error", error.message ?: error.toString())
            put("operationType", operationType.value)
            put("path", path ?: JSONObject.NULL)
            put(
                "authInfo",
                JSONObject().apply {
                    put("userId", currentUser?.uid ?: JSONObject.NULL)
                    put("email", currentUser?.email ?: JSONObject.NULL)
                    put("emailVerified", currentUser?.isEmailVerified ?: false)
                }
            )
        }
        val json = payload.toString()
        Log.e(TAG, json)
        return json
    }
}
