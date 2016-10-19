package com.ridi.books.helper.io

import com.ridi.books.helper.Log
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import java.io.*
import java.nio.charset.MalformedInputException

object ZipHelper {
    private val bufferSize = 8192
    private val progressUpdateStepSize = bufferSize / 16

    interface Listener {
        fun onProgress(unzippedBytes: Long)
    }

    @JvmStatic
    @JvmOverloads
    fun unzip(zipFile: File, destDir: File,
                            deleteZip: Boolean, listener: Listener? = null): Boolean {
        if (!zipFile.exists()) {
            return false
        }
        try {
            return unzip(BufferedInputStream(FileInputStream(zipFile)), destDir, listener)
        } catch (e: Exception) {
            Log.e(javaClass, "error while unzip", e)
            return false
        } finally {
            if (deleteZip) {
                zipFile.delete()
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun unzip(inputStream: InputStream, destDir: File, listener: Listener? = null): Boolean {
        destDir.mkdirs()

        try {
            val zipInputStream = ZipArchiveInputStream(inputStream, "UTF8", true, true)
            var entry: ZipArchiveEntry?

            while (true) {
                try {
                    entry = zipInputStream.nextZipEntry
                } catch (e: MalformedInputException) {
                    Log.e(javaClass, "Filename encoding error", e)
                    // 파일명에서 인코딩 오류 발생 시에, 해당 파일 무시
                    continue
                }

                entry ?: break
                if (entry.isDirectory) {
                    continue
                }

                val path = File(destDir, entry.name).path
                if (path.lastIndexOf('/') != -1) {
                    val d = File(path.substring(0, path.lastIndexOf('/')))
                    d.mkdirs()
                }

                val out = BufferedOutputStream(FileOutputStream(path))
                val buf = ByteArray(bufferSize)
                var readSize: Int
                val prevBytesRead = zipInputStream.bytesRead
                do {
                    readSize = zipInputStream.read(buf)
                    if (readSize <= 0) {
                        break
                    }
                    out.write(buf, 0, readSize)
                    if (listener != null) {
                        val bytesRead = zipInputStream.bytesRead - prevBytesRead
                        if ((bytesRead / bufferSize) % progressUpdateStepSize == 0L) {
                            listener.onProgress(bytesRead + prevBytesRead)
                        }
                    }
                } while(true)
                out.close()
            }
            zipInputStream.close()

            return true
        } catch (e: Exception) {
            Log.e(javaClass, "error while unzip", e)
            return false
        }
    }
}