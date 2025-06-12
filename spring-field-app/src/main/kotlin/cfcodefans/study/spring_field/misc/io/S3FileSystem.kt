package cfcodefans.study.spring_field.misc.io

import cfcodefans.study.spring_field.commons.Jsons
import cfcodefans.study.spring_field.misc.io.S3Constants.S3_SCHEME
import com.amazonaws.services.s3.Headers.ETAG
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION
import com.amazonaws.services.s3.model.S3ObjectSummary
import org.apache.commons.vfs2.*
import org.apache.commons.vfs2.FileName.ROOT_PATH
import org.apache.commons.vfs2.FileType.FILE
import org.apache.commons.vfs2.FileType.FOLDER
import org.apache.commons.vfs2.operations.FileOperation
import org.apache.commons.vfs2.provider.AbstractFileName
import org.apache.commons.vfs2.provider.AbstractFileObject
import org.apache.commons.vfs2.provider.AbstractFileSystem
import org.apache.commons.vfs2.provider.url.UrlFileNameParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 *  For study purpose
 *  https://github.com/abashev/vfs-s3/blob/branch-4.x.x/src/main/java/com/github/vfss3/S3FileName.java
 */
object S3Constants {
    const val S3_SCHEME: String = "s3"
}

open class S3FileName(val endpoint: String,
                      val urlPrefix: String?,
                      val pathPrefix: String?,
                      val bucket: String,
                      val signingRegion: String,
                      val kind: FileType,
                      val accessKey: String,
                      val secretKey: String,
                      val platformFeatures: FileOperation,
                      path: String) : AbstractFileName(S3_SCHEME, path, kind) {

    private val path: String = path
    override fun getPath(): String = path

    init {
        require(bucket.contains("/") || bucket.isBlank()) { "Bucket name [$bucket] has to be valid bucket name" }
    }

    override fun appendRootUri(buffer: StringBuilder, addPassword: Boolean) {
        buffer.append(scheme)
        buffer.append("://")

        urlPrefix?.let { buffer.append(it).append('.') }
        buffer.append(endpoint)
        pathPrefix?.let { buffer.append('/').append(it) }
    }

    override fun createName(absolutePath: String, fileType: FileType): S3FileName = S3FileName(endpoint = endpoint,
            urlPrefix = urlPrefix,
            pathPrefix = pathPrefix,
            bucket = bucket,
            signingRegion = signingRegion,
            path = absolutePath,
            kind = fileType,
            accessKey = accessKey,
            secretKey = secretKey,
            platformFeatures = platformFeatures)

    @Throws(FileSystemException::class)
    open fun getS3Key(): String? {
        if ((type != FILE) && (type != FOLDER)) throw FileSystemException("Not able to get key from imaginary file")
        if (getPathDecoded() == ROOT_PATH) return null
        val path: StringBuilder = StringBuilder(getPathDecoded())
        if ((path.indexOf(SEPARATOR) == 0) && (path.length > 1)) path.deleteCharAt(0)
        if (type == FOLDER) path.append(SEPARATOR_CHAR)

        return path.toString()
    }

    @Throws(FileSystemException::class)
    open fun getS3KeyAs(fileType: FileType?): String {
        val path: StringBuilder = StringBuilder(getPathDecoded())
        if ((path.indexOf(SEPARATOR) == 0) && (path.length > 1)) path.deleteCharAt(0)
        if (fileType === FOLDER) path.append(SEPARATOR_CHAR)
        return path.toString()
    }

    override fun toString(): String = Jsons.toString(this)
}

open class S3FileNameParser : UrlFileNameParser()

open class S3FileSystem(rootName: FileName,
                        parentLayer: FileObject,
                        fileSystemOptions: FileSystemOptions) : AbstractFileSystem(rootName, parentLayer, fileSystemOptions),
                                                                FileSystem {
    override fun addCapabilities(caps: Collection<Capability?>?) {
        TODO("Not yet implemented")
    }

    override fun createFile(name: AbstractFileName?): FileObject? {
        TODO("Not yet implemented")
    }
}

open class ObjMetaHolder(private val metadata: ObjectMetadata, val virtual: Boolean) {
    constructor() : this(metadata = ObjectMetadata(), virtual = true)
    constructor(objMetadata: ObjectMetadata) : this(metadata = objMetadata, virtual = false)
    constructor(summary: S3ObjectSummary) : this(metadata = ObjectMetadata().apply {
        contentLength = summary.size
        lastModified = summary.lastModified
        setHeader(ETAG, summary.eTag)
    }, virtual = true)

    open fun withContentLen(length: Long): ObjMetaHolder = metadata
        .clone()
        .also { it.contentLength = length }
        .let { ObjMetaHolder(it, virtual) }

    open fun withContentType(kind: String): ObjMetaHolder = metadata
        .clone()
        .also { it.contentType = kind }
        .let { ObjMetaHolder(it, virtual) }

    open fun withLastModifiedNow(): ObjMetaHolder = metadata
        .clone()
        .also { it.lastModified = Date() }
        .let { ObjMetaHolder(it, virtual) }

    open fun withServerSideEncryption(useEncryption: Boolean): ObjMetaHolder = metadata
        .clone()
        .also { if (useEncryption) it.sseAlgorithm = AES_256_SERVER_SIDE_ENCRYPTION }
        .let { ObjMetaHolder(it, virtual) }
}

open class S3FileObj(name: S3FileName, val fileSystem: S3FileSystem) : AbstractFileObject<S3FileSystem>(name, fileSystem) {
    companion object {
        var log: Logger = LoggerFactory.getLogger(S3FileObj::class.java)
    }

    override fun getName(): S3FileName = super.name as S3FileName

    private var objMetaHolder: ObjMetaHolder? = null

    override fun isAttached(): Boolean = objMetaHolder != null
    override fun doAttach() {
        if (name.path == ROOT_PATH) {
            if (log.isDebugEnabled) log.debug("Attach S3FileObject to the bucket $name")
            doAttachVirtualFolder()
            return
        }
        try {
            name.getS3KeyAs(FILE)
        } catch (e: AmazonS3Exception) {
            if (e.statusCode == 403) {
                doAttach(kind = FILE, metadata = ObjMetaHolder())
                log.debug("Attach to forbidden S3 obj: $name")
                return
            }
        }
    }

    @Throws(FileSystemException::class)
    protected fun doAttachVirtualFolder(): S3FileObj = apply {
        doAttach(FOLDER, ObjMetaHolder().withContentLen(0).withContentType(""))
    }

    @Throws(FileSystemException::class)
    protected fun doAttach(kind: FileType?, metadata: ObjMetaHolder): S3FileObj = apply {
        if (objMetaHolder != null) throw FileSystemException("""Try to reattach file $name without detach""")
        this.objMetaHolder = metadata
        if (kind != null) injectType(kind)
    }

    override fun doGetContentSize(): Long {
        TODO("Not yet implemented")
    }

    override fun doGetType(): FileType? {
        TODO("Not yet implemented")
    }

    override fun doListChildren(): Array<out String?>? {
        TODO("Not yet implemented")
    }

}