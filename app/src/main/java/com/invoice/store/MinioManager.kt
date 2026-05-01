package com.invoice.store

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.S3ClientOptions
import com.amazonaws.services.s3.model.PutObjectRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Date

object MinioManager {
    // 10.0.2.2 is the correct bridge for the Android Emulator to see your computer
    private const val ENDPOINT = "http://10.0.2.2:9000"
    private const val ACCESS_KEY = "minioadmin"
    private const val SECRET_KEY = "minioadmin"
    private const val BUCKET_NAME = "invoices"

    private val s3Client: AmazonS3Client by lazy {
        val credentials = BasicAWSCredentials(ACCESS_KEY, SECRET_KEY)
        val client = AmazonS3Client(credentials, Region.getRegion(Regions.US_EAST_1))
        client.setEndpoint(ENDPOINT)
        // PathStyleAccess is required for MinIO
        client.setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).build())
        client
    }

    suspend fun uploadFile(uri: Uri, context: Context): String {
        return withContext(Dispatchers.IO) {
            val fileName = getFileName(uri, context) ?: "file_${System.currentTimeMillis()}"
            val tempFile = copyUriToTempFile(uri, context, fileName)
            
            try {
                if (!s3Client.doesBucketExist(BUCKET_NAME)) {
                    s3Client.createBucket(BUCKET_NAME)
                }

                // 1. Upload the file
                val putRequest = PutObjectRequest(BUCKET_NAME, fileName, tempFile)
                s3Client.putObject(putRequest)
                
                // 2. Generate a Presigned URL (Valid for 7 days)
                // This URL includes a temporary "key" that lets you download 
                // the file even if the bucket is PRIVATE.
                val expiration = Date()
                var msec = expiration.time
                msec += 1000 * 60 * 60 * 24 * 7 // 7 days
                expiration.time = msec

                val presignedUrl = s3Client.generatePresignedUrl(BUCKET_NAME, fileName, expiration)
                
                // Return the full URL with the security signature
                presignedUrl.toString()
            } finally {
                tempFile.delete()
            }
        }
    }

    private fun getFileName(uri: Uri, context: Context): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) name = it.getString(index)
                }
            }
        }
        return name ?: uri.path?.let { File(it).name }
    }

    private fun copyUriToTempFile(uri: Uri, context: Context, fileName: String): File {
        val tempFile = File(context.cacheDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }
}
