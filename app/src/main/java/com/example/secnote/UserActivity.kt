package com.example.secnote

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.io.ByteArrayOutputStream
import java.io.File
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class UserActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        val intents: Intent = intent
        val str = intents.getStringExtra(UserActivity.USER_NAME_EXTRA)
        USER_NAME_EXTRA = str

        onAddButtonClick()
        onBackupButtonClick()
        refresh()
    }

    override fun onRestart(){
        super.onRestart()
        refresh()
    }

    private val SALT:String = "secret"
    private val IV:String = "IV_VALUE_16_BYTE"

    companion object {
        var USER_NAME_EXTRA: String? = null
    }

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
                    intent.putExtra("USER", USER_NAME_EXTRA)
                    intent.putExtra("DESCRIPTION", but.text)
                    startActivity(intent)
                }
                j++
            }
        }
    }

    private fun onAddButtonClick() {
        val addButton = findViewById<View>(R.id.addButton) as Button
        val descriptionInput = findViewById<View>(R.id.noteDescription) as EditText
        val noteInput = findViewById<View>(R.id.password) as EditText
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

        backupButton.setOnClickListener{
            var directory = applicationContext.getDir(USER_NAME_EXTRA, Context.MODE_PRIVATE)
            val file = File(directory, "note")
            var readPass:ByteArray = ByteArray(0)
            file.inputStream().use {
                while(true) {
                    var next = it.read()
                    if (next == -1)
                        break
                    readPass += next.toByte()
                }
            }
            val crypto = Crypto()
            val pass = crypto.decrypt(USER_NAME_EXTRA.toString(), readPass)
            val direct = File(applicationContext.filesDir, "backup")
            if(!direct.exists()){
                direct.mkdir()
            }
            var backup = File(direct, USER_NAME_EXTRA)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            val note = pass.toCharArray();
            val salt = SALT.toByteArray();

            val spec = PBEKeySpec(note, salt, 65536, 128);
            val tmp = factory.generateSecret(spec);
            val encoded = tmp.encoded;
            val key =  SecretKeySpec(encoded, "AES")
            val c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            val iv = IV.toByteArray();
            c.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
            val data = File(directory, "data")
            var toEncrypt = ""
            for(i in data.listFiles()){
                var read:ByteArray = ByteArray(0)
                i.inputStream().use {
                    while(true) {
                        var next = it.read()
                        if (next == -1)
                            break
                        read += next.toByte()
                    }
                }
                toEncrypt += i.name + ":" + String(read, Charsets.UTF_8) + ";"
            }

            val encryptedBytes: ByteArray = c.doFinal(toEncrypt.toByteArray(Charsets.UTF_8))
            val iv2: ByteArray = c.iv
            var outputStream = ByteArrayOutputStream( )
            outputStream.write( iv2 )
            outputStream.write( encryptedBytes )

            backup.outputStream().use{
                it.write(outputStream.toByteArray( ))
            }

            val files = direct.listFiles()
            var names = ""

            for(i in files)
                names += i.name + "\n"

            message.text = "Saved backups for " + names
        }
    }
}