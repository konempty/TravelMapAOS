package kim.hanbin.gpstracker

interface OnAttachmentDownloadListener {
    fun onAttachmentDownloadedSuccess()
    fun onAttachmentDownloadedError()
    fun onAttachmentDownloadedFinished()
    fun onAttachmentDownloadUpdate(percent: Int)
}