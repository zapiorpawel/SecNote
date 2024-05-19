package com.example.secnote

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import java.io.File
import android.app.Activity
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import com.google.gson.Gson
import android.net.Uri
import com.google.gson.reflect.TypeToken

class UserActivity : AppCompatActivity() {
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var executor: Executor
    private lateinit var passwordInput: EditText
    private lateinit var exportButton: Button
    private lateinit var importButton: Button
    private lateinit var exportPassword: String
    private lateinit var message: TextView
    private var description: String? = null
    private var user: String? = null
    private var encryptedData: ByteArray? = null
    private lateinit var crypto: Crypto

    companion object {
        var USER_NAME_EXTRA: String? = null
        const val USER_EXTRA = "USER"
        const val DESCRIPTION_EXTRA = "DESCRIPTION"
        const val EXPORT_REQUEST_CODE = 1
        const val IMPORT_REQUEST_CODE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
        executor = Executors.newSingleThreadExecutor()
        biometricPrompt = createBiometricPrompt()
        promptInfo = createPromptInfo()

        val intents: Intent = intent
        USER_NAME_EXTRA = intents.getStringExtra(USER_NAME_EXTRA)

        onAddButtonClick()
        onBackupButtonClick()
        onExportButtonClick()
        onImportButtonClick()
        refresh()
    }


    private fun refresh() {
        val directory = applicationContext.getDir("$USER_NAME_EXTRA", Context.MODE_PRIVATE)
        val files = File(directory, "data").listFiles()
        val notesSaved = findViewById<LinearLayout>(R.id.notesSaved)
        notesSaved.removeAllViews()
        var j = 1
        if (files != null) {
            for (i in files) {
                val button = Button(this)
                button.text = i.name
                button.id = j
                val id = button.id
                notesSaved.addView(button)
                val but = findViewById<Button>(id)
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

    private fun onAddButtonClick() {
        val addButton = findViewById<View>(R.id.addButton) as Button
        val descriptionInput = findViewById<View>(R.id.noteDescription) as EditText
        val noteInput = findViewById<View>(R.id.note) as EditText
        val message = findViewById<View>(R.id.message) as TextView

        addButton.setOnClickListener {
            val description = descriptionInput.text.toString()
            val note = noteInput.text.toString()
            if (description == "") {
                message.text = "Please add a title"
            } else if (note == "") {
                message.text = "Note field cannot be empty"
            } else {
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
    private fun onBackupButtonClick() {
        val backupButton = findViewById<View>(R.id.backupButton) as Button
        val message = findViewById<View>(R.id.message) as TextView

        backupButton.setOnClickListener {
            // Kod tworzenia kopii zapasowej notatek
        }
    }

    private fun createBiometricPrompt(): BiometricPrompt {
        return BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int, errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
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

    private fun onExportButtonClick() {
        exportButton = findViewById<Button>(R.id.exportButton)
        passwordInput = findViewById<EditText>(R.id.passwordIExport)

        exportButton.setOnClickListener {
            val exportPassword = passwordInput.text.toString()

            if (exportPassword.isNotEmpty()) {
                val directory = applicationContext.getDir("$USER_NAME_EXTRA", Context.MODE_PRIVATE)
                val files = File(directory, "data").listFiles()

                if (files != null && files.isNotEmpty()) {
                    val notesList = mutableListOf<Map<String, String>>()
                    val crypto = Crypto()

                    for (file in files) {
                        try {
                            val noteBytes = file.readBytes()
                            val noteContent = crypto.decryptWithPassword(noteBytes, USER_NAME_EXTRA + file.name)
                            val noteMap = mapOf("title" to file.name, "description" to noteContent)
                            notesList.add(noteMap)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    val notesJson = notesList.joinToString(",\n", "[\n", "\n]") {
                        """
                {
                    "title": "${it["title"]}",
                    "description": "${it["description"]}"
                }
                """.trimIndent()
                    }

                    val encryptedNotes = crypto.encryptWithPassword(notesJson, exportPassword)

                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "text/plain"  // Changed type to "text/plain"
                        putExtra(Intent.EXTRA_TITLE, "notes_export.txt") // Changed file extension to ".txt"
                    }


                    startActivityForResult(intent, EXPORT_REQUEST_CODE)
                } else {
                    Toast.makeText(this, "No notes to export", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter the export password", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun importNotes(importPassword: String, uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        if (inputStream != null) {
            val encryptedData = inputStream.readBytes()

            val crypto = Crypto()
            try {
                // Dekodowanie danych
                val decryptedData = crypto.decryptWithPassword(encryptedData, importPassword)

                // Poprawienie parsowania danych
                val jsonObject = Gson().fromJson<Map<String, Any>>(decryptedData, Map::class.java)
                val notesJson = jsonObject["notes"] as String
                val notesListType = object : TypeToken<List<Map<String, String>>>() {}.type
                val notesList = Gson().fromJson<List<Map<String, String>>>(notesJson, notesListType)

                // Zapisanie notatek
                for (note in notesList) {
                    val title = note["title"]
                    val description = note["description"]

                    if (title != null && description != null) {
                        val directory = applicationContext.getDir(USER_NAME_EXTRA, Context.MODE_PRIVATE)
                        val file = File(File(directory, "data"), title)
                        val toWrite = crypto.encryptWithPassword(description, USER_NAME_EXTRA + title)
                        file.outputStream().use {
                            it.write(toWrite)
                        }
                    }
                }
                refresh()
                Toast.makeText(this, "Import successful", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to import notes: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "Input stream is null", Toast.LENGTH_SHORT).show()
        }
    }




    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                EXPORT_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        val outputStream = contentResolver.openOutputStream(uri)
                        if (outputStream != null) {
                            val exportPassword = passwordInput.text.toString()
                            val directory =
                                applicationContext.getDir(USER_NAME_EXTRA, Context.MODE_PRIVATE)
                            val files = File(directory, "data").listFiles()

                            if (files != null && files.isNotEmpty()) {
                                val notesList = mutableListOf<Map<String, String>>()
                                val crypto = Crypto()

                                for (file in files) {
                                    try {
                                        val noteBytes = file.readBytes()
                                        val noteContent = crypto.decryptWithPassword(
                                            noteBytes,
                                            USER_NAME_EXTRA + file.name
                                        )
                                        val noteMap = mapOf(
                                            "title" to file.name,
                                            "description" to noteContent
                                        )
                                        notesList.add(noteMap)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }

                                val notesJson = notesList.joinToString(",\n", "[\n", "\n]") {
                                    """
{
    "title": "${it["title"]}",
    "description": "${it["description"]}"
}
""".trimIndent()
                                }

                                val dataToExport = mapOf("password" to exportPassword, "notes" to notesJson)
                                val exportDataJson = Gson().toJson(dataToExport)
                                val encryptedExportData = crypto.encryptWithPassword(exportDataJson, exportPassword)


                                outputStream.use { output ->
                                    output.write(encryptedExportData) // Zapisz zaszyfrowane dane eksportowe
                                }
                                Toast.makeText(this, "Export successful", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "No notes to export", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        } else {
                            Toast.makeText(this, "Output stream is null", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                IMPORT_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        val importPassword = passwordInput.text.toString()
                        importNotes(importPassword, uri)
                    }
                }
            }
        }
    }



    private fun parseNotesJson(notesJson: String): List<Map<String, String>> {
        val notesList = mutableListOf<Map<String, String>>()
        val notesArray = notesJson.trim().removeSurrounding("[", "]").split("},").map { it.trim().plus("}") }

        for (note in notesArray) {
            val title = note.substringAfter("\"title\": \"").substringBefore("\",")
            val description = note.substringAfter("\"description\": \"").substringBefore("\"")
            notesList.add(mapOf("title" to title, "description" to description))
        }

        return notesList
    }

    private fun onImportButtonClick() {
        importButton = findViewById<Button>(R.id.importButton)

        importButton.setOnClickListener {
            val importPassword = passwordInput.text.toString()

            if (importPassword.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/plain"  // zmieniono typ na "text/plain"
                }
                startActivityForResult(intent, IMPORT_REQUEST_CODE)
            } else {
                Toast.makeText(this, "Please enter the import password", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


private fun parseNotesJson(notesJson: String): List<Map<String, String>> {
        val notesList = mutableListOf<Map<String, String>>()
        val notesArray = notesJson.trim().removeSurrounding("[", "]").split("},").map { it.trim().plus("}") }

        for (note in notesArray) {
            val title = note.substringAfter("\"title\": \"").substringBefore("\",")
            val description = note.substringAfter("\"description\": \"").substringBefore("\"")
            notesList.add(mapOf("title" to title, "description" to description))
        }

        return notesList
    }

