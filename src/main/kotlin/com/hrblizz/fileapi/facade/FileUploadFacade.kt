package com.hrblizz.fileapi.facade

import com.hrblizz.fileapi.data.entities.FileEntity
import com.hrblizz.fileapi.data.repository.FileRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*

@Service
class FileUploadFacade(private val fileRepository: FileRepository) {
    @Value("\${app.upload.base_dir}")
    lateinit var uploadBaseDir: String

    fun uploadFile(entity: FileEntity, fileInputStream: InputStream): FileEntity {
        val token = UUID.randomUUID().toString()
        val filePath = storeFile(fileInputStream, token)
        entity.token = token
        entity.filePath = filePath

        fileRepository.save(entity)

        return entity
    }

    fun findAllByIds(ids: List<String>): List<FileEntity> {
        return fileRepository.findAllById(ids).toList()
    }

    private fun storeFile(fileInputStream: InputStream, filename: String) : String {
        val fileName = filename.replace("-", "");

        val baseDirectory = File(uploadBaseDir)
        var rootDirectory = File("/")

        val subdirectories = fileName.split("").filter { it.isNotEmpty() }
        var currentDirectory = baseDirectory

        for (subdirectory in subdirectories) {
            currentDirectory = File(currentDirectory, subdirectory)
            rootDirectory = File(rootDirectory, subdirectory)
            currentDirectory.mkdir()
        }

        val file = File(currentDirectory, fileName)

        FileOutputStream(file).use { output ->
            fileInputStream.use { input ->
                input.copyTo(output)
            }
        }

        return rootDirectory.absolutePath + "/" + fileName;
    }
}