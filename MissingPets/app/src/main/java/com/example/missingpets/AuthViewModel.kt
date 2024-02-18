package com.example.missingpets

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private var auth = FirebaseAuth.getInstance()
    private var isAuthenticated by mutableStateOf(false)

    init {
        viewModelScope.launch {
            auth.currentUser?.let {
                isAuthenticated = true
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    fun currentUser(): FirebaseUser? {
        return auth.currentUser
    }

    //----------------------------------------------------------------------------------------------

    fun isLogged(): Boolean {
        return isAuthenticated
    }

    //----------------------------------------------------------------------------------------------

    fun signIn(email: String, password: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    val success = task.isSuccessful
                    if (success) {
                        isAuthenticated = true
                    }
                    onComplete(success)
                }
        }
    }

    //----------------------------------------------------------------------------------------------

    fun signUp(username: String, email: String, password: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    val success = task.isSuccessful
                    if (success) {

                        val user = auth.currentUser
                        if (user != null) {
                            // Set the username
                            val userProfileChangeRequest = UserProfileChangeRequest.Builder()
                                .setDisplayName(username)
                                .build()
                            user.updateProfile(userProfileChangeRequest)
                        }
                    }
                    else {
                        // Handle authentication failure
                        isAuthenticated = false
                        val exception = task.exception
                        Log.e("AuthViewModel", "Authentication failed: ${exception?.message}")
                    }
                    onComplete(success)
                }
        }
    }

    //----------------------------------------------------------------------------------------------

    fun signOut() {
        viewModelScope.launch {
            auth.signOut()
            isAuthenticated = false
        }
    }
}
