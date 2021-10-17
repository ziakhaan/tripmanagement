package com.easemytrip.mytripmanagement.utils

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object ExportUtils {
    /**
     * this will write the shared preferences tripref objects to the file for export
     */
    fun writeStringAsFile(context: Context, fileContents: String?): String {
        lateinit var jsonFile: File
        try {
            jsonFile = getFile(context)
            val out = FileWriter(jsonFile)
            out.write(fileContents)
            out.close()
        } catch (e: IOException) {
        }
        return jsonFile.absolutePath
    }

    /**
     * to create the name of the json exported file
     */
    fun getFile(context: Context): File {
        val pattern = "MM-dd-yyyy HH:MM:SS"
        val simpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())
        val date = simpleDateFormat.format(Date())
        val fileName = "TEMP_$date.json"
        val dirPath: String = context.getExternalFilesDir("EXPORT_DATA")!!.absolutePath
        return File(dirPath, fileName)

    }
}