package com.hrblizz.fileapi.controller

import com.hrblizz.fileapi.controller.exception.NotFoundException
import com.hrblizz.fileapi.controller.request.FileMetasRequest
import com.hrblizz.fileapi.controller.request.FileUploadRequest
import com.hrblizz.fileapi.controller.response.FileMetaResponse
import com.hrblizz.fileapi.facade.FileUploadFacade
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class FileUploadController(
    private val fileUploadFacade: FileUploadFacade
) {

    @PostMapping("/files")
    suspend fun uploadFile(request: FileUploadRequest): ResponseEntity<Map<String, Any>> {
        request.validate()
        val metaEntity = fileUploadFacade.uploadFile(
            request.toFileUploadEntity(),
            request.content!!.inputStream
        )
        val response = mapOf(
            "token" to metaEntity.token,
        )

        return ResponseEntity(response, HttpStatus.OK)
    }

    @PostMapping("/files/metas")
    fun getFilesMetas(@RequestBody request: FileMetasRequest): ResponseEntity<FileMetaResponse> {
        request.validate()
        val fileEntities = fileUploadFacade.findAllByIds(request.tokens!!.toList())
        return ResponseEntity(FileMetaResponse(fileEntities), HttpStatus.OK)
    }

    @GetMapping("/file/{token}")
    suspend fun getFileContent(@PathVariable token: String): ResponseEntity<InputStreamResource> {
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

            return ResponseEntity(InputStreamResource(fileData.fileData), headers, HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            throw NotFoundException(e.message!!)
        }
    }

    @DeleteMapping("/file/{token}")
    fun deleteFile(@PathVariable token: String): ResponseEntity<Unit> {
        fileUploadFacade.deleteFile(token)
        return ResponseEntity(HttpStatus.OK)
    }
}