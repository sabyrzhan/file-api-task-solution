package com.hrblizz.fileapi.facade

import java.io.InputStream

interface FileManager {
    fun readFile(filePath: String?): InputStream

    fun storeFile(fileInputStream: InputStream, filename: String): String

    fun deleteFile(filePath: String?)
}