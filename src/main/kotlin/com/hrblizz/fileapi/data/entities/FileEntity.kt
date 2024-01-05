package com.hrblizz.fileapi.data.entities

import org.springframework.data.annotation.Id
import java.time.Instant

class FileEntity {
    @Id
    lateinit var token : String
    lateinit var filename: String
    lateinit var size: Number
    lateinit var contentType: String
    var createTime: Instant = Instant.now()
    var expireTime: Instant? = null
    lateinit var meta: Map<String, Any>
    lateinit var source: String
    lateinit var filePath: String
}