package com.example.secnote


import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import java.io.File
import java.io.IOException

class SecNoteProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {
        var response: String? = null
        val p = uri.getQueryParameter("p")
        val path = uri.path?.split("/")
        if (path != null) {
            if (path.size == 4) {
                var directory = context?.getDir(path[1], Context.MODE_PRIVATE)
                val noteFile = File(directory, "note")
                try {
                    var noteRead:ByteArray = ByteArray(0)
                    noteFile.inputStream().use {
                        while(true) {
                            var next = it.read()
                            if (next == -1)
                                break
                            noteRead += next.toByte()
                        }
                    }
                    val crypto = Crypto()
                    if(p == crypto.decrypt(path[1], noteRead)){
                        val file = (path?.get(3) ?:null)?.let { File(File(directory, "data"), it) }
                        try {
                            noteRead= ByteArray(0)
                            file?.inputStream()?.use {
                                while(true) {
                                    var next = it.read()
                                    if (next == -1)
                                        break
                                    noteRead += next.toByte()
                                }
                            }
                            response = crypto.decrypt(path[1]+path[3], noteRead)
                        } catch (e: IOException) {
                            response = "Error with data reading occured"
                        } catch(e: java.lang.NullPointerException){
                            response = "No such data"
                        }
                    } else {
                        response = "Wrong text"
                    }
                } catch (e: IOException) {
                    response = "An error occurred"
                } catch (e: java.lang.NullPointerException) {
                    response = "No text stored"
                }
            }
            else{
                response = "Wrong path"
            }
        }
        else{
            response = "No text provided"
        }
        var cursor = MatrixCursor(arrayOf<String>("response"))
        cursor.newRow().add("response", response)
        cursor.close()
        return cursor
    }

    override fun getType(p0: Uri): String? {
        TODO("Not yet implemented")
    }

    override fun insert(p0: Uri, p1: ContentValues?): Uri? {
        TODO("Not yet implemented")
    }

    override fun delete(p0: Uri, p1: String?, p2: Array<out String>?): Int {
        TODO("Not yet implemented")
    }

    override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int {
        TODO("Not yet implemented")
    }
}