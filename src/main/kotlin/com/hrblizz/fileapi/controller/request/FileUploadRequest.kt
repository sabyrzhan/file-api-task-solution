package com.hrblizz.fileapi.controller.request

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hrblizz.fileapi.controller.exception.BadRequestException
import com.hrblizz.fileapi.data.entities.FileEntity
import org.springframework.web.multipart.MultipartFile
import java.time.Instant

data class FileUploadRequest(val name: String?,
                             val contentType: String?,
                             val meta: String?,
                             val source: String?,
                             val expireTime: Instant?,
                             val content: MultipartFile?) {
    fun validate() {
        if (name.isNullOrBlank()) {
            throw BadRequestException("Name is blank")
        }

        if (contentType.isNullOrBlank()) {
            throw BadRequestException("Content type is blank")
        }

        if (meta.isNullOrBlank() || metaToMap() == null) {
            throw BadRequestException("Meta is blank")
        }

        if (expireTime != null && expireTime.isBefore(Instant.now())) {
            throw BadRequestException("Expire time is less than current time (expireTime must be in UTC)")
        }

        if (content == null || content.originalFilename.isNullOrBlank() || content.size.toInt() == 0) {
            throw BadRequestException("File is empty")
        }
    }

    fun toFileUploadEntity(): FileEntity {
        val entity = FileEntity()
        entity.filename = name!!
        entity.size = content!!.size
        entity.contentType = contentType!!
        entity.expireTime = expireTime
        entity.meta = metaToMap()!!
        entity.source = source!!

        return entity
    }

    private fun metaToMap(): Map<String,Any>? {
        try {
            val result = ObjectMapper().readValue(meta, object: TypeReference<Map<String,Any>>(){})
            if (result.isEmpty()) {
                return null
            }

            return result
        } catch (e: Exception) {
            return null
        }
    }
}