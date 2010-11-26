package fr.ens.transcriptome.eoulsan.data.protocols;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFileMetadata;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define the s3 protocol in local mode.
 * @author Laurent Jourdren
 */
@LocalOnly
public class S3DataProtocol implements DataProtocol {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String S3_PROTOCOL_PREFIX = Common.S3_PROTOCOL + "://";

  private AmazonS3 s3;

  private class S3URL {

    private String source;
    private String bucket;
    private String filePath;

    public String getSource() {

      return source;
    }

    public String getBucket() {

      return this.bucket;
    }

    public String getFilePath() {

      return filePath;
    }

    /**
     * Get the the bucket from the URL of the destination.
     * @param source the source URL
     * @return a String with the bucket name
     * @throws IOException if the s3 URL is invalid
     */
    private final String getBucket(final String source) throws IOException {

      if (!source.startsWith(S3_PROTOCOL_PREFIX))
        throw new IOException("Invalid S3 URL: " + source);

      final int indexPos = source.indexOf('/', S3_PROTOCOL_PREFIX.length());

      return source.substring(S3_PROTOCOL_PREFIX.length(), indexPos);
    }

    /**
     * Get path of the file in the bucket on S3.
     * @param source the source URL
     * @return a String with the path
     * @throws IOException if the s3 URL is invalid
     */
    private final String getS3FilePath(final String source) throws IOException {

      if (!source.startsWith(S3_PROTOCOL_PREFIX))
        throw new IOException("Invalid S3 URL: " + source);

      final int indexPos = source.indexOf('/', S3_PROTOCOL_PREFIX.length());

      return source.substring(indexPos + 1);
    }

    private ObjectMetadata getMetaData() throws FileNotFoundException {

      final AmazonS3 s3 = getS3();

      S3Object s3Obj =
          s3.getObject(new GetObjectRequest(getBucket(), getFilePath()));

      if (s3Obj == null)
        throw new FileNotFoundException("No file found: " + this.source);

      return s3Obj.getObjectMetadata();
    }

    private S3Object getS3Object() throws IOException {

      final AmazonS3 s3 = getS3();

      return s3.getObject(new GetObjectRequest(getBucket(), getFilePath()));
    }

    @Override
    public String toString() {

      return this.source;
    }

    //
    // Constructor
    //

    /**
     * Create an S3 URL
     * @param src the S3 URL in a String
     */
    public S3URL(final DataFile src) throws IOException {

      this.source = src.getSource();
      this.bucket = getBucket(this.source);
      this.filePath = getS3FilePath(this.source);
    }

  }

  private class FileToUpload {

    private InputStream is;
    private S3URL s3url;
    private DataFileMetadata metadata;

    /**
     * Upload the file.
     */
    public void upload() {

      LOGGER.info("Upload data to " + s3url.getSource());
      final ObjectMetadata md = new ObjectMetadata();

      if (this.metadata.getContentType() != null)
        md.setContentType(this.metadata.getContentType());

      if (this.metadata.getContentEncoding() != null)
        md.setContentEncoding(this.metadata.getContentEncoding());

      md.setContentLength(this.metadata.getContentLength());

      final PutObjectRequest or =
          new PutObjectRequest(s3url.bucket, s3url.getFilePath(), this.is, md);

      LOGGER.fine("Upload: "
          + this.s3url + " (" + md.getContentType() + ", "
          + md.getContentEncoding() + " " + this.metadata.getContentLength()
          + " bytes)");

      int tryCount = 0;
      boolean uploadOk = false;
      final long start = System.currentTimeMillis();
      AmazonClientException ace = null;

      do {

        tryCount++;

        try {

          getS3().putObject(or);
          uploadOk = true;
        } catch (AmazonClientException e) {
          ace = e;
          LOGGER.warning("Error while uploading "
              + this.s3url + " (Attempt " + tryCount + ")");

          try {
            Thread.sleep(10000);
          } catch (InterruptedException e1) {

            e1.printStackTrace();
          }
        }

      } while (!uploadOk && tryCount < 3);

      if (!uploadOk)
        throw ace;

      final long end = System.currentTimeMillis();
      final long duration = end - start;
      final int speedKiB =
          (int) (this.metadata.getContentLength() / (duration / 1000.0) / 1024.0);

      LOGGER.info("Upload of "
          + this.s3url + " (" + this.metadata.getContentLength()
          + " bytes) in " + StringUtils.toTimeHumanReadable(duration)
          + " ms. (" + speedKiB + " KiB/s)");

    }

    //
    // Constructor
    //

    public FileToUpload(final DataFile dest, final InputStream is,
        final DataFileMetadata md) throws IOException {

      this.s3url = new S3URL(dest);
      this.is = is;
      this.metadata = md == null ? new SimpleDataFileMetadata() : md;

    }

  }

  //
  // Protocol methods
  //

  @Override
  public String getName() {

    return "s3";
  }

  @Override
  public InputStream getData(final DataFile src) throws IOException {

    return new S3URL(src).getS3Object().getObjectContent();
  }

  @Override
  public OutputStream putData(final DataFile dest) throws IOException {

    return putData(dest, (DataFileMetadata) null);
  }

  @Override
  public OutputStream putData(final DataFile dest, final DataFileMetadata md)
      throws IOException {

    final File f = FileUtils.createTempFile("", ".s3upload");

    return new FileOutputStream(f) {

      @Override
      public void close() throws IOException {

        super.close();

        final SimpleDataFileMetadata md2 = new SimpleDataFileMetadata(md);
        if (md2.getContentLength() < 0)
          md2.setContentLength(f.length());

        new FileToUpload(dest, FileUtils.createInputStream(f), md2).upload();

        if (!f.delete())
          LOGGER.severe("Can not delete temporarry file: "
              + f.getAbsolutePath());
      }

    };

  }

  @Override
  public DataFileMetadata getMetadata(final DataFile src) throws IOException {

    if (!exists(src))
      throw new FileNotFoundException("File not found: " + src);

    final ObjectMetadata md = new S3URL(src).getMetaData();

    final SimpleDataFileMetadata result = new SimpleDataFileMetadata();
    result.setContentLength(md.getContentLength());
    result.setLastModified(md.getLastModified().getTime());
    result.setContentType(md.getContentType());
    result.setContentEncoding(md.getContentEncoding());
    result.setDataFormat(DataFormatRegistry.getInstance().getDataFormat(
        src.getName()));

    return result;
  }

  @Override
  public void putData(final DataFile src, final DataFile dest)
      throws IOException {

    if (src == null)
      throw new NullPointerException("The source of the data to put is null");

    if (dest == null)
      throw new NullPointerException(
          "The destination of the data to put is null");

    final DataFileMetadata mdSrc = src.getMetaData();

    new FileToUpload(dest, src.open(), mdSrc).upload();
  }

  @Override
  public boolean exists(final DataFile src) {

    try {
      return new S3URL(src).getS3Object() != null;
    } catch (IOException e) {
      return false;
    }

  }

  @Override
  public boolean isReadable() {

    return true;
  }

  @Override
  public boolean isWritable() {

    return true;
  }

  //
  // Other methods
  //

  /**
   * Get the AmazonS3 object.
   * @return an AmazonS3
   */
  private AmazonS3 getS3() {

    if (this.s3 == null) {

      final Settings settings = EoulsanRuntime.getSettings();

      this.s3 =
          new AmazonS3Client(new BasicAWSCredentials(
              settings.getAWSAccessKey(), settings.getAWSSecretKey()));
    }

    return this.s3;
  }

}
