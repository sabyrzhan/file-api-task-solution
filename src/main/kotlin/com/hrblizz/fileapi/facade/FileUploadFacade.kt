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
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.io.InputStream
import java.time.Instant
import java.util.*

@Service
class FileUploadFacade(
    private val fileRepository: FileRepository,
    @Qualifier(FileSystemFileManager.QUALIFIER) private val fileManager: FileManager,
    private val logger: Logger
) {
    suspend fun uploadFile(entity: FileEntity, fileInputStream: InputStream):
            FileEntity = withContext(Dispatchers.IO) {
        val token = UUID.randomUUID().toString()
        val filePath = fileManager.storeFile(fileInputStream, token)
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
        require(fileMeta.isNotEmpty()) { "FileMeta with token=$token not found" }
        FileDataDTO(fileMeta.first(), fileManager.readFile(fileMeta.firstOrNull()?.filePath))
    }

    fun deleteFile(token: String) {
        val fileMeta = findAllByIds(listOf(token))
        if (fileMeta.isNotEmpty()) {
            fileRepository.deleteById(fileMeta.first().token)
            CoroutineScope(Dispatchers.IO).launch { fileManager.deleteFile(fileMeta.first().filePath) }
        } else {
            logger.warning(LogItem("No fileMeta found to delete for token $token"))
        }
    }
}