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
        if (path != null && path.size == 4) {
            val directory = context?.getDir(path[1], Context.MODE_PRIVATE)
            val noteFile = File(directory, "note")
            try {
                val noteRead = noteFile.readBytes()
                val crypto = Crypto()
                if (p == crypto.decrypt(path[1], noteRead)) {
                    val file = File(File(directory, "data"), path[3])
                    response = try {
                        val dataRead = file.readBytes()
                        crypto.decrypt(path[1] + path[3], dataRead)
                    } catch (e: IOException) {
                        "Error with data reading occurred"
                    } catch (e: NullPointerException) {
                        "No such data"
                    }
                } else {
                    response = "Wrong text"
                }
            } catch (e: IOException) {
                response = "An error occurred"
            } catch (e: NullPointerException) {
                response = "No text stored"
            }
        } else {
            response = "Wrong path"
        }
        val cursor = MatrixCursor(arrayOf("response"))
        cursor.newRow().add("response", response)
        return cursor
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }
}
