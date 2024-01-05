package com.hrblizz.fileapi.facade

import com.hrblizz.fileapi.data.entities.FileEntity
import com.hrblizz.fileapi.data.repository.FileRepository
import com.hrblizz.fileapi.facade.dto.FileDataDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
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
        return fileRepository.findAllById(ids).filter { !it.isExpired() }
    }

    fun getFileData(token: String): FileDataDTO {
        val fileMeta = findAllByIds(listOf(token))
        val fileData = File(uploadBaseDir + fileMeta.firstOrNull()?.filePath)
        require(fileMeta.isNotEmpty() && fileData.exists()) { "File with token not found" }
        return FileDataDTO(fileMeta.first(), FileInputStream(fileData))
    }

    fun deleteFile(token: String) {
        val fileMeta = findAllByIds(listOf(token))
        if (fileMeta.isNotEmpty()) {
            fileRepository.deleteById(fileMeta.first().token)
            runBlocking {
                launch(Dispatchers.IO) {
                    val file = File(uploadBaseDir + fileMeta.first().filePath)
                    println("deleting the file")
                    if (file.exists()) {
                        file.delete()
                        println("deleted the file")
                    }
                }
            }
        }
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