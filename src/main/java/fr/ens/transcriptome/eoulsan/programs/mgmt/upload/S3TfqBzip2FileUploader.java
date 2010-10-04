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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;

import com.amazonaws.auth.AWSCredentials;

import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.io.FastQReader;
import fr.ens.transcriptome.eoulsan.datasources.DataSource;

/**
 * This class define an uploader for bzip2 TFQ files. *
 * @author Laurent Jourdren
 */
public class S3TfqBzip2FileUploader extends S3Bzip2FileUploader {

  @Override
  public void init(final DataSource src, final String dest) {

    super.init(src, dest);
  }

  @Override
  public void prepare() throws IOException {

    final BufferedWriter bw =
        new BufferedWriter(new OutputStreamWriter(super
            .createUploadOutputStream()));

    final FastQReader fqr =
        new FastQReader(getFileToUpload().getDataSource().getInputStream());

    try {
      while (fqr.readEntry())
        bw.write(fqr.toTFQ(false));
    } catch (BadBioEntryException e) {

      throw new IOException("Invalid fastq entry in "
          + getFileToUpload().getDataSource());
    }

    bw.close();
    fqr.close();
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   * @param credentialsFile AWS credentials file
   * @param bucketName bucket name
   */
  public S3TfqBzip2FileUploader(final AWSCredentials awsCredentials)
      throws FileNotFoundException, IllegalArgumentException, IOException {

    super(awsCredentials);
  }

}
