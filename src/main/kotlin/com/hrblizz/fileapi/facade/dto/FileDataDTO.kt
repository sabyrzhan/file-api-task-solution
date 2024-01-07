package com.hrblizz.fileapi.facade.dto

import com.hrblizz.fileapi.data.entities.FileEntity
import java.io.FileInputStream

data class FileDataDTO(val fileMeta: FileEntity, val fileData: FileInputStream)