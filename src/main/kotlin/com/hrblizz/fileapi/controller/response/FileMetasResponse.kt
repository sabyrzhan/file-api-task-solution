package com.hrblizz.fileapi.controller.response

import com.fasterxml.jackson.annotation.JsonIgnore
import com.hrblizz.fileapi.data.entities.FileEntity
import java.time.Instant

class FileSingleMetaResponse(@JsonIgnore private val fileUploadEntity: FileEntity) {
    val token: String = fileUploadEntity.token
    val filename: String = fileUploadEntity.filename
    val size: Int = fileUploadEntity.size.toInt()
    val contentType: String = fileUploadEntity.contentType
    val createTime: Instant = fileUploadEntity.createTime
    val meta: Map<String, Any> = fileUploadEntity.meta
}

class FileMetaResponse(@JsonIgnore private val metaList: List<FileEntity>) {
    val files: Map<String, FileSingleMetaResponse> = metaList
        .map { FileSingleMetaResponse(it) }
        .associateBy { it.token }
}

