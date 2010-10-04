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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import com.amazonaws.auth.AWSCredentials;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.datasources.DataSource;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define an uploader for bzip2 files.
 * @author Laurent Jourdren
 */
public class S3Bzip2FileUploader extends S3FileUploader {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private boolean outputStreamMode;

  @Override
  public void init(final DataSource src, final String dest) {

    final String contentType = findContentType(src.toString());

    // setFileToUpload(new FileToUpload(src, StringUtils
    // .removeCompressedExtensionFromFilename(dest), contentType, "bzip2"));
    setFileToUpload(new FileToUpload(src, dest, contentType, "bzip2"));
  }

  @Override
  public void prepare() throws IOException {

    if (outputStreamMode)
      return;

    uploadInputStream(getFileToUpload().getDataSource().getInputStream());
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

    logger.info("Temporary bz2 file: " + getFileToUpload().getFile());

    final OutputStream os =
        new BZip2CompressorOutputStream(new FileOutputStream(getFileToUpload()
            .getFile()));
    FileUtils.copy(is, os);
  }

  @Override
  public OutputStream createUploadOutputStream() throws FileNotFoundException,
      IOException {

    this.outputStreamMode = true;
    return new BZip2CompressorOutputStream(super.createUploadOutputStream());
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   * @param credentialsFile AWS credentials file
   */
  public S3Bzip2FileUploader(final AWSCredentials awsCredentials)
      throws FileNotFoundException, IllegalArgumentException, IOException {

    super(awsCredentials);
  }

}
