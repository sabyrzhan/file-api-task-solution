package com.hrblizz.fileapi.facade

import com.hrblizz.fileapi.data.entities.FileEntity
import com.hrblizz.fileapi.data.repository.FileRepository
import com.hrblizz.fileapi.facade.dto.FileDataDTO
import com.hrblizz.fileapi.library.log.LogItem
import com.hrblizz.fileapi.library.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.time.Instant
import java.util.*

@Service
class FileUploadFacade(
    private val fileRepository: FileRepository,
    private val logger: Logger
) {

    @Value("\${app.upload.base_dir}")
    lateinit var uploadBaseDir: String

    suspend fun uploadFile(entity: FileEntity, fileInputStream: InputStream):
            FileEntity = withContext(Dispatchers.IO) {
        val token = UUID.randomUUID().toString()
        val filePath = storeFile(fileInputStream, token)
        entity.token = token
        entity.filePath = filePath

        fileRepository.save(entity)

        entity
    }

    fun findAllByIds(ids: List<String>): List<FileEntity> {
        return fileRepository.findAllByTokenInAndExpireTimeNullOrExpireTimeGreaterThan(ids, Instant.now())
    }

    suspend fun getFileData(token: String): FileDataDTO = withContext(Dispatchers.IO) {
        val fileMeta = findAllByIds(listOf(token))
        val fileData = File(uploadBaseDir + fileMeta.firstOrNull()?.filePath)
        require(fileMeta.isNotEmpty() && fileData.exists()) { "File with token not found" }
        FileDataDTO(fileMeta.first(), FileInputStream(fileData))
    }

    fun deleteFile(token: String) {
        val fileMeta = findAllByIds(listOf(token))
        if (fileMeta.isNotEmpty()) {
            fileRepository.deleteById(fileMeta.first().token)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val file = File(uploadBaseDir + fileMeta.first().filePath)
                    logger.info(LogItem("Deleted fileMeta for token: $token. Now deleting file..."))
                    if (file.exists()) {
                        file.delete()
                        logger.info(LogItem("Deleted the file for token: $token"))
                    } else {
                        logger.warning(LogItem("No file found to delete for token $token"))
                    }
                } catch (e: Exception) {
                    logger.error(LogItem("Error while deleting the file for token=$token: $e"))
                }
            }
        } else {
            logger.warning(LogItem("No fileMeta found to delete for token $token"))
        }
    }

    private fun storeFile(fileInputStream: InputStream, filename: String): String {
        logger.info(LogItem("start: storeFile(fileInputStream={}, filename=$filename)"))

        val startTime = System.currentTimeMillis()
        try {
            val fileName = filename.replace("-", "")

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

            val result = rootDirectory.absolutePath + "/" + fileName

            val totalTime = System.currentTimeMillis() - startTime
            logger.info(LogItem("end: storeFile(fileInputStream={}, filename=$filename): Total time taken to save the file: $totalTime ms"))

            return result
        } catch (e: Exception) {
            logger.error(LogItem("Error while saving the file=$filename: $e"))
            throw RuntimeException(e)
        }
    }
}