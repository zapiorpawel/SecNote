package com.example.secnote

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        onLoginButtonClick()
        onRegisterButtonClick()
    }

    private fun onRegisterButtonClick() {
        val usernameInput = findViewById<View>(R.id.username) as EditText
        val passwordInput = findViewById<View>(R.id.note) as EditText
        val registerButton = findViewById<View>(R.id.registerButton) as Button
        val messageOutput = findViewById<View>(R.id.message) as TextView

        registerButton.setOnClickListener{
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()
            if(username == "" || password == ""){
                messageOutput.text = "Please enter the credentials to register"
            }
            else if(password.length < 12){
                messageOutput.text = "The master password should have at least 12 characters"
            }
            else{
                val direct = File(applicationContext.filesDir, username)
                if(!direct.exists()) {
                    if(direct.mkdir()){
                        var directory = applicationContext.getDir("$username", Context.MODE_PRIVATE)
                        val file = File(directory, "password")
                        val dir = File(directory, "data")
                        val crypto = Crypto()
                        crypto.keyGen(username)
                        val toWrite = crypto.encrypt(username, password)
                        file.outputStream().use {
                            it.write(toWrite)
                        }
                        dir.mkdir()
                        passwordInput.text = SpannableStringBuilder("")
                        usernameInput.text = SpannableStringBuilder("")
                        val intent = Intent(this, UserActivity::class.java)
                        intent.putExtra(UserActivity.USER_NAME_EXTRA, username)
                        startActivity(intent)
                    }
                }
                else {
                    messageOutput.text = "This user is already registered"
                }
            }
        }
    }

    private fun onLoginButtonClick() {
        val usernameInput = findViewById<View>(R.id.username) as EditText
        val passwordInput = findViewById<View>(R.id.note) as EditText
        val loginButton = findViewById<View>(R.id.loginButton) as Button
        val messageOutput = findViewById<View>(R.id.message) as TextView

        loginButton.setOnClickListener{
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()
            if(username == "" || password == ""){
                messageOutput.text = "Please enter your credentials"
            }
            else{
                val direct = File(applicationContext.filesDir, username)
                if(direct.exists()){
                    var directory = applicationContext.getDir("$username", Context.MODE_PRIVATE)
                    val file = File(directory, "password")
                    try {
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
                        if(password == crypto.decrypt(username, readPass)){
                            passwordInput.text = SpannableStringBuilder("")
                            usernameInput.text = SpannableStringBuilder("")
                            val intent = Intent(this, UserActivity::class.java)
                            intent.putExtra(UserActivity.USER_NAME_EXTRA, username)
                            startActivity(intent)
                        }
                        else{
                            messageOutput.text = "You entered wrong password for this user"
                        }
                    }
                    catch (e: IOException) {
                        e.printStackTrace();
                    }
                    catch(e: java.lang.NullPointerException){
                        messageOutput.text = "No password stored"
                    }
                }
                else{
                    messageOutput.text = "There is no such user - you need to register"
                }
            }
        }
    }
}