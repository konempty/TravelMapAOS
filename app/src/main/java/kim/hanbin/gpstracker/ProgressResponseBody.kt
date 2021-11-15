import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.*


class ProgressResponseBody(
    val responseBody: ResponseBody,
    val onAttachmentDownloadUpdate: (Float) -> Unit
) : ResponseBody() {
    private var bufferedSource = source(responseBody.source()).buffer()
    override fun contentLength(): Long {
        return responseBody.contentLength()
    }

    override fun contentType(): MediaType? {
        return responseBody.contentType()
    }

    override fun source(): BufferedSource {
        return bufferedSource
    }

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead = 0L
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(
                    sink,
                    byteCount
                )
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                val progress =
                    if (bytesRead == -1L) 1f else totalBytesRead.toFloat() / responseBody.contentLength()
                        .toFloat()
                onAttachmentDownloadUpdate(progress)
                return bytesRead
            }
        }
    }
}
