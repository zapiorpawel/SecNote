package com.example.secnote
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import java.io.File

import java.util.concurrent.Executor
import java.util.concurrent.Executors
class UserActivity : AppCompatActivity() {
    // Deklaracja zmiennych biometrycznych
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var executor: Executor
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
        executor = Executors.newSingleThreadExecutor()
        // Inicjalizacja biometrycznych promptów
        biometricPrompt = createBiometricPrompt()
        promptInfo = createPromptInfo()

        val intents: Intent = intent
        val str = intents.getStringExtra(USER_NAME_EXTRA)
        USER_NAME_EXTRA = str

        onAddButtonClick()
        onBackupButtonClick()
        refresh()
    }

    // Metoda do odświeżania listy notatek
    private fun refresh(){
        var directory = applicationContext.getDir("$USER_NAME_EXTRA", Context.MODE_PRIVATE)
        var files = File(directory, "data").listFiles()
        val notesSaved = findViewById<LinearLayout>(R.id.notesSaved)
        notesSaved.removeAllViews()
        var j = 1
        if(files != null) {
            for (i in files) {
                var button = Button(this)
                button.text = i.name
                button.id = j
                val id = button.id
                notesSaved.addView(button)
                var but = findViewById<Button>(id)
                but.setOnClickListener {
                    val intent = Intent(this, SecureNoteActivity::class.java)
                    intent.putExtra(USER_EXTRA, USER_NAME_EXTRA)
                    intent.putExtra(DESCRIPTION_EXTRA, but.text)
                    startActivity(intent)
                }
                j++
            }
        }
    }

    // Metoda obsługująca dodawanie nowej notatki
    private fun onAddButtonClick() {
        val addButton = findViewById<View>(R.id.addButton) as Button
        val descriptionInput = findViewById<View>(R.id.noteDescription) as EditText
        val noteInput = findViewById<View>(R.id.note) as EditText
        val message = findViewById<View>(R.id.message) as TextView

        addButton.setOnClickListener{
            val description = descriptionInput.text.toString()
            val note = noteInput.text.toString()
            if(description == ""){
                message.text = "Please add a title"
            }
            else if(note == ""){
                message.text = "Note field cannot be empty"
            }
            else {
                // Wymaganie biometrii/pinu
                biometricPrompt.authenticate(promptInfo)

                // Czyszczenie pól description i note po skutecznym dodaniu notatki
                descriptionInput.text.clear()
                noteInput.text.clear()

                // Zapisanie notatki
                var directory = applicationContext.getDir("$USER_NAME_EXTRA", Context.MODE_PRIVATE)
                val file = File(File(directory, "data"), description)
                val crypto = Crypto()
                crypto.keyGen(USER_NAME_EXTRA.toString() + description)
                val toWrite = crypto.encrypt(USER_NAME_EXTRA + description, note)
                file.outputStream().use {
                    it.write(toWrite)
                }
                refresh()
            }
        }
    }


    // Metoda obsługująca tworzenie kopii zapasowej notatek
    private fun onBackupButtonClick() {
        val backupButton = findViewById<View>(R.id.backupButton) as Button
        val message = findViewById<View>(R.id.message) as TextView

        backupButton.setOnClickListener{
            // Kod tworzenia kopii zapasowej notatek
        }
    }

    // Metoda tworząca biometryczny prompt
    private fun createBiometricPrompt(): BiometricPrompt {
        return BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int, errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    // Obsługa błędu autentykacji
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    // Obsługa sukcesu autentykacji
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Obsługa nieudanej autentykacji
                }
            })
    }

    // Metoda tworząca informacje o biometrycznym promptcie
    private fun createPromptInfo(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use password")
            .build()
    }

    companion object {
        var USER_NAME_EXTRA: String? = null
        const val USER_EXTRA = "USER"
        const val DESCRIPTION_EXTRA = "DESCRIPTION"
    }
}
