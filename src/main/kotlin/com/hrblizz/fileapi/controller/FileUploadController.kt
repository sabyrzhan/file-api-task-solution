package com.hrblizz.fileapi.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*

@RestController
class FileUploadController {
    @Value("\${app.upload.base_dir}")
    lateinit var uploadBaseDir: String

    @PostMapping("/files")
    fun uploadFile(@RequestParam file: MultipartFile): ResponseEntity<Map<String, Any>> {
        validateRequest(file)
        val originalFileName = file.originalFilename.trim()
        val token = UUID.randomUUID().toString()
        val storedFileName = storeFile(file.inputStream)
        val response = mapOf(
            "originalFileName" to originalFileName,
            "token" to token,
            "storedFileName" to storedFileName
        )

        return ResponseEntity(response, HttpStatus.OK)
    }

    fun validateRequest(file: MultipartFile) {
        val fileName = file.originalFilename ?: ""
        if (file.isEmpty || fileName.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty")
        }
    }

    fun storeFile(fileInputStream: InputStream) : String {
        val fileName = UUID.randomUUID().toString().replace("-", "");

        val baseDirectory = File(uploadBaseDir)

        val subdirectories = fileName.split("").filter { it.isNotEmpty() }
        var currentDirectory = baseDirectory

        for (subdirectory in subdirectories) {
            currentDirectory = File(currentDirectory, subdirectory)
            currentDirectory.mkdir()
        }

        FileOutputStream(File(currentDirectory, fileName)).use {output ->
            fileInputStream.use { input ->
                input.copyTo(output)
            }
        }

        return fileName;
    }
}