package com.hrblizz.fileapi.data.repository

import com.hrblizz.fileapi.data.entities.FileEntity
import org.springframework.data.mongodb.repository.MongoRepository
import java.time.Instant

interface FileRepository: MongoRepository<FileEntity, String> {
    fun findAllByTokenInAndExpireTimeNullOrExpireTimeGreaterThan(ids: List<String>, currentDate: Instant): List<FileEntity>
}