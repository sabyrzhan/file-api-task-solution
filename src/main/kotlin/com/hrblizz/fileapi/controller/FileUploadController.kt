package com.hrblizz.fileapi.controller

import com.hrblizz.fileapi.controller.request.FileMetasRequest
import com.hrblizz.fileapi.controller.request.FileUploadRequest
import com.hrblizz.fileapi.controller.response.FileMetaResponse
import com.hrblizz.fileapi.facade.FileUploadFacade
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class FileUploadController(
    private val fileUploadFacade: FileUploadFacade
) {

    @PostMapping("/files")
    fun uploadFile(request: FileUploadRequest): ResponseEntity<Map<String, Any>> {
        request.validate()
        val metaEntity = fileUploadFacade.uploadFile(request.toFileUploadEntity(), request.content!!.inputStream)
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
}