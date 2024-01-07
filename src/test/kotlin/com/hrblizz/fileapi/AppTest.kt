package com.hrblizz.fileapi

import com.hrblizz.fileapi.facade.FileSystemFileManager
import com.hrblizz.fileapi.library.log.Logger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.util.LinkedMultiValueMap
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.nio.file.Files
import kotlin.io.path.toPath


/**
 * Unit test for simple App.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AppTest {
    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Value("\${app.test.basic_username}")
    lateinit var basicUsername: String

    @Value("\${app.test_basic_password}")
    lateinit var basicPassword: String

    companion object {
        private const val UPLOAD_ROOT_DIR = "test_uploads/"

        @Container
        private val mongoContainer = MongoDBContainer("mongo:latest")

        @DynamicPropertySource
        @JvmStatic
        fun uploadFolderProperties(registry: DynamicPropertyRegistry) {
            mongoContainer.startupAttempts = 3
            mongoContainer.start()
            File("test_uploads/").mkdirs()
            registry.add("app.upload.base_dir") { -> UPLOAD_ROOT_DIR }
            registry.add("spring.data.mongodb.uri") { -> "mongodb://localhost:${mongoContainer.getMappedPort(27017)}/files" }
        }
    }

    @Test
    fun `successfully upload file and return token - success`() {
        val response = uploadTestFile()
        val token = getDataFromResponse(response)["token"]!!.toString()
        val file = buildFilePathFromToken(token)

        assertTrue(file.exists() && file.isFile)
    }

    @Test
    fun `get uploaded file metadata - success`() {
        val uploadResponse = uploadTestFile()
        val token = getDataFromResponse(uploadResponse)["token"]
        val metaBody = mapOf("tokens" to listOf(token))

        val metaRequest = RequestEntity.post("/files/metas")
            .contentType(MediaType.APPLICATION_JSON)
            .body(metaBody)

        val metadataResponse = restTemplateWithBasic().exchange(metaRequest, Map::class.java)

        assertTrue(metadataResponse.statusCode.is2xxSuccessful)
        val filesMetadata = getDataFromResponse(metadataResponse)["files"]!! as Map<*, *>
        val singleFileMetadata = filesMetadata[token]!! as Map<*, *>
        assertEquals(token, singleFileMetadata["token"])
        assertEquals("test_file.txt", singleFileMetadata["filename"])
        assertEquals(getTestFileSize(), singleFileMetadata["size"].toString().toLong())
        assertEquals("text/plain", singleFileMetadata["contentType"])
        assertTrue(singleFileMetadata.containsKey("createTime"))
        assertTrue(singleFileMetadata.containsKey("meta"))
    }

    @Test
    fun `get non-existing file metadata - returns empty list`() {
        val metaBody = mapOf("tokens" to listOf("invalid-token"))

        val metaRequest = RequestEntity.post("/files/metas")
            .contentType(MediaType.APPLICATION_JSON)
            .body(metaBody)

        val metadataResponse = restTemplateWithBasic().exchange(metaRequest, Map::class.java)

        assertTrue(metadataResponse.statusCode.is2xxSuccessful)
        val filesMetadata = getDataFromResponse(metadataResponse)["files"]!! as Map<*, *>
        assertEquals(0, filesMetadata.size)
    }

    @Test
    fun `get uploaded file data - success`() {
        val uploadResponse = uploadTestFile()
        val token = getDataFromResponse(uploadResponse)["token"]
        val metaRequest = RequestEntity.get("/file/$token").build()

        val fileDataResponse = restTemplateWithBasic().exchange(metaRequest, String::class.java)

        assertTrue(fileDataResponse.statusCode.is2xxSuccessful)
        val expectedFileData = String(Files.readAllBytes(getTestFile().toURI().toPath()))
        val responseFileData = fileDataResponse.body!!
        assertEquals(expectedFileData, responseFileData)
        val headers = fileDataResponse.headers
        assertEquals("test_file.txt", headers["X-Filename"]!![0])
        assertEquals(getTestFileSize(), headers["X-Filesize"]!![0].toString().toLong())
        assertEquals("text/plain", headers["Content-Type"]!![0])
        assertTrue(headers.containsKey("X-CreateTime"))
    }

    @Test
    fun `get non existing uploaded file data - returns non found`() {
        val metaRequest = RequestEntity.get("/file/invalid-token").build()

        val fileDataResponse = restTemplateWithBasic().exchange(metaRequest, String::class.java)

        assertEquals(HttpStatus.NOT_FOUND, fileDataResponse.statusCode)
    }

    @Test
    fun `delete file - success`() {
        val uploadResponse = uploadTestFile()
        val token = getDataFromResponse(uploadResponse)["token"]
        val metaRequest = RequestEntity.delete("/file/$token").build()

        val deleteResponse = restTemplateWithBasic().exchange(metaRequest, String::class.java)

        assertTrue(deleteResponse.statusCode.is2xxSuccessful)
        val file = buildFilePathFromToken(token!!.toString())
        assertTrue(!file.exists())
    }

    @Test
    fun `delete non existing file - idempotent success`() {
        val metaRequest = RequestEntity.delete("/file/invalid-token").build()

        val deleteResponse = restTemplateWithBasic().exchange(metaRequest, String::class.java)

        assertTrue(deleteResponse.statusCode.is2xxSuccessful)
    }

    // Utility methods

    private fun uploadTestFile(): ResponseEntity<Map<*, *>> {
        val body = createUploadBody()
        val request = RequestEntity.post("/files")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)

        val response = restTemplateWithBasic().exchange(request, Map::class.java)
        assertTrue(response.statusCode.is2xxSuccessful)
        assertTrue(getDataFromResponse(response).containsKey("token"))

        return response
    }

    private fun restTemplateWithBasic(): TestRestTemplate {
        return restTemplate.withBasicAuth(basicUsername, basicPassword)
    }

    private fun createUploadBody(): LinkedMultiValueMap<String, Any> {
        val body = LinkedMultiValueMap<String, Any>()
        body.add("name", "test_file.txt")
        body.add("contentType", "text/plain")
        body.add("meta", """{"description": "this is the test file"}""")
        body.add("source", "WebBrowser")
        body.add("content", FileSystemResource(getTestFile()))

        return body
    }

    private fun getTestFile(): File = File(javaClass.getResource("/test_file.txt")!!.toURI())

    private fun getTestFileSize(): Long = getTestFile().length()

    private fun getDataFromResponse(responseEntity: ResponseEntity<Map<*, *>>): Map<*, *> =
        responseEntity.body["data"]!! as Map<*, *>

    private fun buildFilePathFromToken(token: String): File {
        val fileManager = FileSystemFileManager(Logger(), "/")
        val dirPath = UPLOAD_ROOT_DIR + fileManager.createDirPathFromUUID(token)
        return File("$dirPath/$token")
    }
}
