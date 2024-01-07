package com.hrblizz.fileapi.controller

import com.hrblizz.fileapi.controller.exception.NotFoundException
import com.hrblizz.fileapi.controller.request.FileMetasRequest
import com.hrblizz.fileapi.controller.request.FileUploadRequest
import com.hrblizz.fileapi.controller.response.FileMetaResponse
import com.hrblizz.fileapi.facade.FileUploadFacade
import com.hrblizz.fileapi.library.log.LogItem
import com.hrblizz.fileapi.library.log.Logger
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class FileUploadController(
    private val fileUploadFacade: FileUploadFacade,
    private val logger: Logger
) {

    @PostMapping("/files")
    suspend fun uploadFile(request: FileUploadRequest): ResponseEntity<Map<String, Any>> {
        logger.info(LogItem("start: uploadFile(request=$request)"))
        request.validate()
        val metaEntity = fileUploadFacade.uploadFile(
            request.toFileUploadEntity(),
            request.content!!.inputStream
        )
        val response = mapOf(
            "token" to metaEntity.token,
        )

        logger.info(LogItem("end: uploadFile()"))

        return ResponseEntity(response, HttpStatus.OK)
    }

    @PostMapping("/files/metas")
    fun getFilesMetas(@RequestBody request: FileMetasRequest): ResponseEntity<FileMetaResponse> {
        logger.info(LogItem("start: getFilesMetas(request=$request)"))
        request.validate()
        val fileEntities = fileUploadFacade.findAllByIds(request.tokens!!.toList())
        logger.info(LogItem("end: getFilesMetas(fileEntities.size=${fileEntities.size})"))
        return ResponseEntity(FileMetaResponse(fileEntities), HttpStatus.OK)
    }

    @GetMapping("/file/{token}")
    suspend fun getFileContent(@PathVariable token: String): ResponseEntity<InputStreamResource> {
        logger.info(LogItem("start: getFileContent(token)"))
        try {
            val fileData = fileUploadFacade.getFileData(token)
            val headers = HttpHeaders()

            headers.contentDisposition = ContentDisposition
                .builder("attachment")
                .filename(fileData.fileMeta.filename).build()
            headers.contentLength = fileData.fileMeta.size.toLong()

            headers.add("X-Filename", fileData.fileMeta.filename)
            headers.add("X-Filesize", fileData.fileMeta.size.toString())
            headers.add("X-CreateTime", fileData.fileMeta.createTime.toString())
            headers.add("Content-Type", fileData.fileMeta.contentType)

            logger.info(LogItem("end: getFileContent()"))

            return ResponseEntity(InputStreamResource(fileData.fileData), headers, HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            logger.error(LogItem("Error while reading the file: ${e.message}"))
            throw NotFoundException("File not found for specified token")
        }
    }

    @DeleteMapping("/file/{token}")
    fun deleteFile(@PathVariable token: String): ResponseEntity<Unit> {
        logger.info(LogItem("start: deleteFile(token)"))
        fileUploadFacade.deleteFile(token)
        logger.info(LogItem("end: deleteFile(token)"))
        return ResponseEntity(HttpStatus.OK)
    }
}