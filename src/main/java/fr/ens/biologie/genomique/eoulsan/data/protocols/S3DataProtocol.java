/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.data.protocols;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.Transfer.TransferState;
import com.amazonaws.services.s3.transfer.TransferManager;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Settings;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFileMetadata;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;

/**
 * This class define the s3 protocol in local mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
@LocalOnly
public class S3DataProtocol implements DataProtocol {

  /** Protocol name. */
  public static final String PROTOCOL_NAME = "s3";

  private AmazonS3 s3;
  private TransferManager tx;

  @Override
  public String getSourceFilename(final String source) {

    final int lastSlashPos = source.lastIndexOf(DataFile.separatorChar);

    if (lastSlashPos == -1) {
      return source;
    }

    return source.substring(lastSlashPos + 1);
  }

  @Override
  public DataFile getDataFileParent(final DataFile src) {

    final String source = src.getSource();

    final int parentSrcLen = source.length() - getName().length() - 1;

    return new DataFile(
        source.substring(0, parentSrcLen < 0 ? 0 : parentSrcLen));
  }

  protected String getProtocolPrefix() {

    return "s3://";
  }

  private class S3URL {

    private final String source;
    private final String bucket;
    private final String filePath;

    public String getSource() {

      return this.source;
    }

    public String getBucket() {

      return this.bucket;
    }

    public String getFilePath() {

      return this.filePath;
    }

    /**
     * Get the the bucket from the URL of the destination.
     * @param source the source URL
     * @return a String with the bucket name
     * @throws IOException if the s3 URL is invalid
     */
    private String getBucket(final String source) throws IOException {

      final String protocolPrefix = getProtocolPrefix();

      if (!source.startsWith(protocolPrefix)) {
        throw new IOException("Invalid S3 URL: " + source);
      }

      final int indexPos = source.indexOf('/', protocolPrefix.length());

      return source.substring(protocolPrefix.length(), indexPos);
    }

    /**
     * Get path of the file in the bucket on S3.
     * @param source the source URL
     * @return a String with the path
     * @throws IOException if the s3 URL is invalid
     */
    private String getS3FilePath(final String source) throws IOException {

      final String protocolPrefix = getProtocolPrefix();

      if (!source.startsWith(protocolPrefix)) {
        throw new IOException("Invalid S3 URL: " + source);
      }

      final int indexPos = source.indexOf('/', protocolPrefix.length());

      return source.substring(indexPos + 1);
    }

    private ObjectMetadata getMetaData() throws FileNotFoundException {

      final AmazonS3 s3 = getS3();

      S3Object s3Obj =
          s3.getObject(new GetObjectRequest(getBucket(), getFilePath()));

      if (s3Obj == null) {
        throw new FileNotFoundException("No file found: " + this.source);
      }

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

    private final InputStream is;
    private final File file;
    private final S3URL s3url;
    private final DataFileMetadata metadata;

    /**
     * Upload the file.
     */
    public void upload() throws IOException {

      getLogger().info("Upload data to " + this.s3url.getSource());
      final ObjectMetadata md = new ObjectMetadata();

      if (this.metadata.getContentType() != null) {
        md.setContentType(this.metadata.getContentType());
      }

      if (this.metadata.getContentEncoding() != null) {
        md.setContentEncoding(this.metadata.getContentEncoding());
      }

      if (this.file == null) {
        md.setContentLength(this.metadata.getContentLength());
      }

      final long fileLength = this.file == null
          ? this.metadata.getContentLength() : this.file.length();

      getLogger().info("Try to upload: "
          + this.s3url + " (" + md.getContentType() + ", "
          + md.getContentEncoding() + " " + fileLength + " bytes)");

      int tryCount = 0;
      boolean uploadOk = false;
      final long start = System.currentTimeMillis();
      AmazonClientException ace = null;

      do {

        tryCount++;

        try {

          multipartUpload(md);
          uploadOk = true;
        } catch (AmazonClientException e) {
          ace = e;
          getLogger().warning("Error while uploading "
              + this.s3url + " (Attempt " + tryCount + "): " + e.getMessage());

          try {
            Thread.sleep(10000);
          } catch (InterruptedException e1) {

            e1.printStackTrace();
          }
        }

      } while (!uploadOk && tryCount < 3);

      if (!uploadOk) {
        throw new IOException(ace.getMessage());
      }

      final long end = System.currentTimeMillis();
      final long duration = end - start;
      final int speedKiB = (int) (fileLength / (duration / 1000.0) / 1024.0);

      getLogger().info("Upload of "
          + this.s3url + " (" + fileLength + " bytes) in "
          + StringUtils.toTimeHumanReadable(duration) + " ms. (" + speedKiB
          + " KiB/s)");

    }

    private void multipartUpload(final ObjectMetadata md) {

      getLogger().info("Use multipart upload");

      final Transfer myUpload;

      if (this.file != null) {
        myUpload = getTransferManager().upload(this.s3url.bucket,
            this.s3url.getFilePath(), this.file);
      } else {
        myUpload = getTransferManager().upload(this.s3url.bucket,
            this.s3url.getFilePath(), this.is, md);
      }

      try {

        while (!myUpload.isDone()) {

          Thread.sleep(500);
        }
        if (myUpload.getState() != TransferState.Completed) {
          throw new AmazonClientException(
              "Transfer not completed correctly. Status: "
                  + myUpload.getState());
        }

      } catch (InterruptedException e) {
        getLogger().warning(e.getMessage());
        throw new AmazonClientException(e.getMessage());
      } finally {

        try {
          if (this.is != null) {
            this.is.close();
          }
        } catch (IOException e) {
          throw new AmazonClientException(e.getMessage());
        }

      }
    }

    //
    // Constructor
    //

    public FileToUpload(final DataFile dest, final InputStream is,
        final DataFileMetadata md) throws IOException {

      this.s3url = new S3URL(dest);
      this.is = is;
      this.file = null;
      this.metadata = md == null ? new SimpleDataFileMetadata() : md;
    }

    public FileToUpload(final DataFile dest, final File file)
        throws IOException {

      this.s3url = new S3URL(dest);
      this.is = null;
      this.file = file;
      this.metadata = new SimpleDataFileMetadata();
    }

  }

  //
  // Protocol methods
  //

  @Override
  public String getName() {

    return PROTOCOL_NAME;
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

    final File f = EoulsanRuntime.getRuntime().createTempFile("", ".s3upload");

    return new FileOutputStream(f) {

      @Override
      public void close() throws IOException {

        super.close();

        final SimpleDataFileMetadata md2 = new SimpleDataFileMetadata(md);
        if (md2.getContentLength() < 0) {
          md2.setContentLength(f.length());
        }

        getLogger().finest("Upload temporary file: " + f.getAbsolutePath());
        new FileToUpload(dest, FileUtils.createInputStream(f), md2).upload();

        if (!f.delete()) {
          getLogger()
              .severe("Can not delete temporary file: " + f.getAbsolutePath());
        }
      }

    };

  }

  @Override
  public DataFileMetadata getMetadata(final DataFile src) throws IOException {

    if (!exists(src, true)) {
      throw new FileNotFoundException("File not found: " + src);
    }

    final ObjectMetadata md = new S3URL(src).getMetaData();

    final SimpleDataFileMetadata result = new SimpleDataFileMetadata();
    result.setContentLength(md.getContentLength());
    result.setLastModified(md.getLastModified().getTime());
    result.setContentType(md.getContentType());
    result.setContentEncoding(md.getContentEncoding());
    result.setDataFormat(DataFormatRegistry.getInstance()
        .getDataFormatFromFilename(src.getName()));

    return result;
  }

  @Override
  public void putData(final DataFile src, final DataFile dest)
      throws IOException {

    if (src == null) {
      throw new NullPointerException("The source of the data to put is null");
    }

    if (dest == null) {
      throw new NullPointerException(
          "The destination of the data to put is null");
    }

    final DataFileMetadata mdSrc = src.getMetaData();

    getLogger().finest("Upload existing source: " + dest);

    final File file = src.toFile();
    final FileToUpload toUpload;

    if (file != null) {
      toUpload = new FileToUpload(dest, file);
    } else {
      toUpload = new FileToUpload(dest, src.rawOpen(), mdSrc);
    }

    // Upload
    toUpload.upload();
  }

  @Override
  public boolean exists(final DataFile src, final boolean followLink) {

    try {
      return new S3URL(src).getS3Object() != null;
    } catch (AmazonS3Exception | IOException e) {
      return false;
    }

  }

  @Override
  public boolean canRead() {

    return true;
  }

  @Override
  public boolean canWrite() {

    return true;
  }

  @Override
  public File getSourceAsFile(final DataFile src) {

    if (src == null || src.getSource() == null) {
      throw new NullPointerException("The source is null.");
    }

    return null;
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
          new AmazonS3Client(new BasicAWSCredentials(settings.getAWSAccessKey(),
              settings.getAWSSecretKey()));

      getLogger().info("AWS S3 account owner: " + this.s3.getS3AccountOwner());

      this.tx = new TransferManager(this.s3);
    }

    return this.s3;
  }

  /**
   * Get transfer manager.
   * @return the transfer manager
   */
  private TransferManager getTransferManager() {

    if (this.tx == null) {
      this.tx = new TransferManager(getS3());
    }

    return this.tx;
  }

  @Override
  public void mkdir(final DataFile dir) throws IOException {

    throw new IOException("The mkdir() method is not supported by the "
        + getName() + " protocol");

  }

  @Override
  public void mkdirs(final DataFile dir) throws IOException {

    throw new IOException("The mkdir() method is not supported by the "
        + getName() + " protocol");
  }

  @Override
  public boolean canMkdir() {

    return false;
  }

  @Override
  public void symlink(final DataFile target, final DataFile link)
      throws IOException {

    throw new IOException("The symlink() method is not supported by the "
        + getName() + " protocol");
  }

  @Override
  public boolean canSymlink() {

    return false;
  }

  @Override
  public void delete(final DataFile file, final boolean recursive)
      throws IOException {

    throw new IOException("The delete() method is not supported by the "
        + getName() + " protocol");
  }

  @Override
  public boolean canDelete() {

    return false;
  }

  @Override
  public List<DataFile> list(final DataFile file) throws IOException {

    throw new IOException(
        "The list() method is not supported by the " + getName() + " protocol");
  }

  @Override
  public boolean canList() {

    return false;
  }

  @Override
  public void rename(final DataFile oldName, final DataFile newName)
      throws IOException {

    throw new IOException("The rename() method is not supported by the "
        + getName() + " protocol");
  }

  @Override
  public boolean canRename() {

    return false;
  }

}
