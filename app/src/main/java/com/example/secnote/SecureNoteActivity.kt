package com.example.secnote

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import java.io.File
import java.io.IOException
import java.util.concurrent.Executor
import androidx.core.content.ContextCompat
import android.app.Activity
import android.widget.Toast

class SecureNoteActivity : AppCompatActivity() {

    private lateinit var crypto: Crypto
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var noteOutput: EditText


    private var isNoteVisible: Boolean = false

    companion object {
        const val EXPORT_REQUEST_CODE = 1
        const val IMPORT_REQUEST_CODE = 2
        var PASS = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secure_note)

        noteOutput = findViewById(R.id.note)
        val message = findViewById<TextView>(R.id.message)
        val intents: Intent = intent
        val user = intents.getStringExtra("USER")
        val description = intents.getStringExtra("DESCRIPTION")

        crypto = Crypto()
        executor = ContextCompat.getMainExecutor(applicationContext)
        biometricPrompt = createBiometricPrompt()
        promptInfo = createPromptInfo()

        setOutput(description)
        loadData(user, description, message)
        onEditButtonClick(user, description, message)
        onDeleteButtonClick(user, description)
        hideNote()
        onShowButtonClick()
    }

    private fun loadData(user: String?, description: String?, errors: TextView) {
        val directory = applicationContext.getDir(user, Context.MODE_PRIVATE)
        val file = File(File(directory, "data"), description)
        try {
            val readPass = file.readBytes()
            val edit = crypto.decrypt(user + description, readPass)
            noteOutput.setText(edit)
        } catch (e: IOException) {
            errors.text = "Error during reading"
        } catch (e: NullPointerException) {
            errors.text = "No data stored"
        }
    }

    private fun setOutput(description: String?) {
        val descriptionOutput = findViewById<TextView>(R.id.description)
        descriptionOutput.text = description
    }

    private fun onEditButtonClick(
        user: String?,
        description: String?,
        message: TextView
    ) {
        val editButton = findViewById<Button>(R.id.editButton)

        editButton.setOnClickListener {
            val note = noteOutput.text.toString()
            if (note.isEmpty()) {
                message.text = "Note field cannot be empty"
            } else {
                val directory = applicationContext.getDir(user, Context.MODE_PRIVATE)
                val file = File(File(directory, "data"), description)
                val toWrite = crypto.encrypt(user + description, note)
                file.writeBytes(toWrite)
                loadData(user, description, message)
            }
        }
    }

    private fun onDeleteButtonClick(user: String?, description: String?) {
        val deleteButton = findViewById<Button>(R.id.deleteButton)
        deleteButton.setOnClickListener {
            val directory = applicationContext.getDir(user, Context.MODE_PRIVATE)
            val file = File(File(directory, "data"), description)
            file.delete()
            finish()
        }
    }

    private fun hideNote() {
        noteOutput.visibility = View.INVISIBLE
    }

    private fun onShowButtonClick() {
        val showButton = findViewById<Button>(R.id.showButton)

        showButton.setOnClickListener {
            authenticateBiometric() // Uwierzytelnianie przed wyświetleniem notatki
        }
    }

    private fun authenticateBiometric() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate()) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricPrompt.authenticate(promptInfo)
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                // Biometric hardware not available
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                // Biometric features are currently unavailable
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // Biometric not enrolled
            }
        }
    }

    private fun createBiometricPrompt(): BiometricPrompt {
        return BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int, errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    // Handle authentication error
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    toggleNoteVisibility() // Po poprawnym uwierzytelnieniu odkryj notatkę
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Handle authentication failed
                }
            })
    }

    private fun createPromptInfo(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use password")
            .build()
    }

    private fun toggleNoteVisibility() {
        if (isNoteVisible) {
            noteOutput.visibility = View.INVISIBLE
        } else {
            noteOutput.visibility = View.VISIBLE
        }
        isNoteVisible = !isNoteVisible
        val showButton = findViewById<View>(R.id.showButton) as Button
        val passwordOutput = findViewById<View>(R.id.note) as EditText
        showButton.setOnClickListener {
            if (PASS) {
                passwordOutput.transformationMethod = PasswordTransformationMethod.getInstance()
                PASS = false
            } else {
                passwordOutput.transformationMethod = HideReturnsTransformationMethod.getInstance()
                PASS = true
            }
        }
    }


}