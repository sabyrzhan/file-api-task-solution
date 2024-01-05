package com.hrblizz.fileapi.controller.request

import com.hrblizz.fileapi.controller.exception.BadRequestException

data class FileMetasRequest(val tokens: List<String>?) {
    fun validate() {
        if (tokens == null || tokens.isEmpty()) {
            throw BadRequestException("Tokens param is empty")
        }
    }
}