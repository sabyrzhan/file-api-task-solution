package com.hrblizz.fileapi.facade

import com.hrblizz.fileapi.library.log.LogItem
import com.hrblizz.fileapi.library.log.Logger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

@Component
@Qualifier(FileSystemFileManager.QUALIFIER)
class FileSystemFileManager(
    private val logger: Logger,
    @Value("\${app.upload.base_dir}")
    private val uploadBaseDir: String
) : FileManager {
    companion object {
        const val QUALIFIER = "FileSystemFileManager"
    }

    override fun readFile(filePath: String?): InputStream {
        val fileData = newFileForPath(filePath)
        require(fileData.exists()) { "File not found at path $filePath" }

        return FileInputStream(fileData)
    }

    override fun storeFile(fileInputStream: InputStream, filename: String): String {
        logger.info(LogItem("start: storeFile(fileInputStream={}, filename=$filename)"))

        val startTime = System.currentTimeMillis()
        try {
            val fileName = filename.replace("-", "")

            val baseDirectory = File(uploadBaseDir)
            var rootDirectory = File("/")

            val subdirectories = fileName.split("").filter { it.isNotEmpty() }
            var currentDirectory = baseDirectory

            for (subdirectory in subdirectories) {
                currentDirectory = File(currentDirectory, subdirectory)
                rootDirectory = File(rootDirectory, subdirectory)
                currentDirectory.mkdir()
            }

            val file = File(currentDirectory, fileName)

            FileOutputStream(file).use { output ->
                fileInputStream.use { input ->
                    input.copyTo(output)
                }
            }

            val result = rootDirectory.absolutePath + "/" + fileName

            val totalTime = System.currentTimeMillis() - startTime
            logger.info(LogItem("end: storeFile(fileInputStream={}, filename=$filename): Total time taken to save the file: $totalTime ms"))

            return result
        } catch (e: Exception) {
            logger.error(LogItem("Error while saving the file=$filename: $e"))
            throw RuntimeException(e)
        }
    }

    override fun deleteFile(filePath: String?) {
        try {
            val file = newFileForPath(filePath)
            if (file.exists()) {
                file.delete()
                logger.info(LogItem("Deleted the file with path $filePath"))
            } else {
                logger.warning(LogItem("No file found to delete at path $filePath"))
            }
        } catch (e: Exception) {
            logger.error(LogItem("Error while deleting the file at path $filePath: $e"))
        }
    }

    private fun newFileForPath(filePath: String?): File = File(uploadBaseDir + filePath)
}

