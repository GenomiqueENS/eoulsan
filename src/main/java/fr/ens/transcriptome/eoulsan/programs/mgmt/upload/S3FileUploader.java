/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.programs.mgmt.upload;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.datasources.DataSource;
import fr.ens.transcriptome.eoulsan.datasources.FileDataSource;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define a FileUploader to upload files to S3.
 * @author Laurent Jourdren
 */
public class S3FileUploader implements FileUploader {

  /** Logger */
  private static final Logger logger = Logger.getLogger(Globals.APP_NAME);

  private static final String S3_PROTOCOL_PREFIX = Common.S3_PROTOCOL + "://";

  private AmazonS3 s3;
  private File tempDirectory;
  private FileToUpload fileToUpload;
  private boolean outputStreamMode;

  /**
   * This class is the core class for upload file to S3.
   * @author Laurent Jourdren
   */
  protected class FileToUpload {

    private DataSource src;
    private File file;
    private boolean temporaryFile;
    private String dest;
    private String contentType;
    private String contentEncoding;

    /**
     * The the content type of the file to upload
     * @param contentType the content type
     */
    public void setContentType(final String contentType) {

      this.contentType = contentType;
    }

    /**
     * Get the local file object of the file to upload
     * @return the file object
     * @throws IOException if an error occurs while creating a temporary file
     */
    public File getFile() throws IOException {

      if (file == null)
        this.file = createTemporaryFile();

      return file;
    }

    /**
     * Get the data source of the file.
     * @return the data source of the file
     */
    public DataSource getDataSource() {

      return this.src;
    }

    /**
     * Get the destination of the file.
     * @return the destination of the file
     */
    public String getDest() {

      return dest;
    }

    /**
     * Test if the the file to upload is a temporary file.
     * @return true if the file to upload is a temporary file
     */
    public boolean isTemporaryFile() {

      return temporaryFile;
    }

    /**
     * Create a temporary file.
     * @return a temporary file object
     * @throws IOException if an error occurs while creating the file
     */
    private File createTemporaryFile() throws IOException {

      File tmpFile;

      if (getTempDirectory() != null)
        tmpFile = File.createTempFile("aws-upload-", ".data", tempDirectory);
      else
        tmpFile = File.createTempFile("aws-upload-", ".data");

      return tmpFile;
    }

    /**
     * Upload the file.
     */
    public void upload() {

      logger.info("Upload data to " + dest);
      final ObjectMetadata md = new ObjectMetadata();

      if (this.contentType != null)
        md.setContentType(this.contentType);

      if (this.contentEncoding != null)
        md.setContentEncoding(this.contentEncoding);

      final PutObjectRequest or =
          new PutObjectRequest(getBucket(), getS3FilePath(), this.file);
      or.setMetadata(md);

      logger.info("Upload: "
          + this.file + " (" + md.getContentType() + ", "
          + md.getContentEncoding() + " " + this.file.length() + " bytes) in "
          + getBucket() + " bucket in " + getS3FilePath());

      int tryCount = 0;
      boolean uploadOk = false;
      final long start = System.currentTimeMillis();
      AmazonClientException ace = null;

      do {

        tryCount++;

        try {
          s3.putObject(or);
          uploadOk = true;
        } catch (AmazonClientException e) {
          ace = e;
          logger.info("Error while uploading "
              + this.file + " (Attempt " + tryCount + ")");
          
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
          (int) (this.file.length() / (duration / 1000.0) / 1024.0);

      logger.info("Upload of "
          + this.dest + " (" + this.file.length() + " bytes) in "
          + StringUtils.toTimeHumanReadable(duration) + " ms. (" + speedKiB
          + " KiB/s)");

      if (this.temporaryFile)
        this.file.delete();
    }

    /**
     * Get the the bucket from the url of the destination.
     * @return a String with the bucket name
     */
    private final String getBucket() {

      if (!this.dest.startsWith(S3_PROTOCOL_PREFIX))
        throw new EoulsanRuntimeException("Invalid S3 URL: " + this.dest);

      final int indexPos = this.dest.indexOf('/', S3_PROTOCOL_PREFIX.length());

      return this.dest.substring(S3_PROTOCOL_PREFIX.length(), indexPos);
    }

    /**
     * Get path of the file in the bucket on S3.
     * @return a String with the path
     */
    private final String getS3FilePath() {

      if (!this.dest.startsWith(S3_PROTOCOL_PREFIX))
        throw new EoulsanRuntimeException("Invalid S3 URL: " + this.dest);

      final int indexPos = this.dest.indexOf('/', S3_PROTOCOL_PREFIX.length());

      return this.dest.substring(indexPos + 1);
    }

    @Override
    public String toString() {

      return src + ", " + dest + ", " + contentType + ", " + contentEncoding;
    }

    //
    // Constructors
    //

    /**
     * Constructor.
     * @param src the datasource.
     * @param file the file object to upload. Can't be null
     * @param dest the destination
     * @param contentType the content type of the file
     * @param contentEncoding the content encoding
     */
    public FileToUpload(final DataSource src, final File file,
        final String dest, final String contentType,
        final String contentEncoding) {

      if (file == null)
        throw new NullPointerException("file to upload cannot be null.");

      this.src = src;
      this.file = file;
      this.dest = dest;
      this.contentType = contentType;
      this.contentEncoding = contentEncoding;
      this.temporaryFile = false;
    }

    /**
     * Constructor.
     * @param src the datasource.
     * @param dest the destination
     * @param contentType the content type of the file
     * @param contentEncoding the content encoding
     */
    public FileToUpload(final DataSource src, final String dest,
        final String contentType, final String contentEncoding) {

      this.src = src;
      this.dest = dest;
      this.contentType = contentType;
      this.contentEncoding = contentEncoding;
      this.temporaryFile = true;
    }

  }

  /**
   * This private class define an OutputStream used to create file that then
   * will be uploaded to S3.
   * @author Laurent Jourdren
   */
  private class S3OutputStream extends OutputStream {

    private OutputStream os;

    @Override
    public void write(int b) throws IOException {

      os.write(b);
    }

    @Override
    public void flush() throws IOException {

      os.flush();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {

      os.write(b, off, len);
    }

    @Override
    public void write(byte[] b) throws IOException {

      os.write(b);
    }

    @Override
    public void close() throws IOException {

      os.close();
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param fileToUpload the FileToUpload object
     */
    public S3OutputStream(final FileToUpload fileToUpload) throws IOException {

      this.os = new FileOutputStream(fileToUpload.getFile());
    }

  }

  /**
   * Get the temporary directory
   * @return the path to the temporary directory
   */
  public File getTempDirectory() {
    return tempDirectory;
  }

  /**
   * Set the FileToUpload object
   * @param fileToUpload
   */
  protected void setFileToUpload(final FileToUpload fileToUpload) {

    this.fileToUpload = fileToUpload;
  }

  /**
   * Get the FileTOUpload object
   * @return the FileToObject
   */
  protected FileToUpload getFileToUpload() {

    return this.fileToUpload;
  }

  /**
   * Set the content type.
   * @param contentType the content type to set
   */
  public void setContentType(final String contentType) {

    if (this.fileToUpload != null)
      this.fileToUpload.setContentType(contentType);
  }

  /**
   * Set the temporary directory
   * @param tempDirectory the temporary directory
   */
  public void setTempDirectory(final File tempDirectory) {
    this.tempDirectory = tempDirectory;
  }

  @Override
  public void init(final DataSource src, final String dest) {

    if (src == null) {
      this.fileToUpload =
          new FileToUpload(new FileDataSource("undefined"), dest, null, null);
      return;
    }

    final String contentType = findContentType(src.toString());

    if ("File".equals(src.getSourceType()))
      this.fileToUpload =
          new FileToUpload(src, new File(src.toString()), dest, contentType,
              null);
    else
      this.fileToUpload = new FileToUpload(src, dest, contentType, null);

  }

  @Override
  public void prepare() throws IOException {

    if (this.outputStreamMode)
      return;

    if (this.fileToUpload.isTemporaryFile())
      uploadInputStream(this.fileToUpload.getDataSource().getInputStream());

  }

  @Override
  public void upload() {

    this.fileToUpload.upload();
  }

  /**
   * Upload an input stream to AWS S3. As AWS S3 need the length of the stream,
   * the stream is save as a file before sending.
   * @param is input stream to upload
   * @param dest destination path in the bucket
   * @param contentType content type. Can be null
   * @param contentEncoding content encoding. Can be null
   */
  protected void uploadInputStream(final InputStream is) throws IOException {

    FileUtils.copy(is, new FileOutputStream(this.fileToUpload.getFile()));
  }

  @Override
  public OutputStream createUploadOutputStream() throws FileNotFoundException,
      IOException {

    this.outputStreamMode = true;
    return new S3OutputStream(this.fileToUpload);
  }

  /**
   * Identify the content type of a file from its extension
   * @param filename the name of the file
   * @return the content type of the file if found else null
   */
  protected String findContentType(String filename) {

    if (filename == null)
      return null;

    final String ext =
        StringUtils.compressionExtension(StringUtils
            .removeCompressedExtensionFromFilename(filename));

    if (".txt".equals(ext)
        || ".fasta".equals(ext) || ".fa".equals(ext) || ".fq".equals(ext)
        || ".fastq".equals(ext) || ".fq".equals(ext) || ".gff".equals(ext))
      return "text/plain";

    return null;
  }

  @Override
  public String getDest() {

    return this.fileToUpload.dest;
  }

  @Override
  public String getSrc() {

    return this.fileToUpload.src == null ? "[new file]" : this.fileToUpload.src
        .toString();
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   * @param credentialsFile AWS credentials file
   * @param bucketName bucket name
   */
  public S3FileUploader(final AWSCredentials awsCredentials) throws IOException {

    this.s3 = new AmazonS3Client(awsCredentials);
  }

}
