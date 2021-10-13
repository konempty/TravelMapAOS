package kim.hanbin.gpstracker

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.*


class CountingRequestBody(val delegate: RequestBody, val listener: Listener) : RequestBody() {


    override fun contentType(): MediaType? {
        return delegate.contentType()
    }

    override fun contentLength(): Long {
        return delegate.contentLength()
    }

    override fun writeTo(sink: BufferedSink) {

        val countingSink = CountingSink(sink)
        val bufferedSink: BufferedSink = countingSink.buffer()

        delegate.writeTo(bufferedSink)

        bufferedSink.flush()
    }

    inner class CountingSink(delegate: Sink?) : ForwardingSink(delegate!!) {
        private var bytesWritten: Long = 0

        @Throws(IOException::class)
        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount
            listener.onRequestProgress(bytesWritten, contentLength())
        }
    }

    interface Listener {
        fun onRequestProgress(bytesWitten: Long, contentLength: Long)

    }
}