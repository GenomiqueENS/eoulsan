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

package fr.ens.transcriptome.eoulsan.steps.mgmt.upload;

import static fr.ens.transcriptome.eoulsan.datatypes.DataFormats.READS_FASTQ;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.bio.SOAPWrapper;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.datasources.DataSourceUtils;
import fr.ens.transcriptome.eoulsan.datasources.FileDataSource;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class allow to upload a file on S3.
 * @author Laurent Jourdren
 */
public class S3DataUploadStep extends DataUploadStep {

  private AWSCredentials awsCredentials;

  //
  // Step method
  //

  @Override
  public ExecutionMode getExecutionMode() {

    return Step.ExecutionMode.LOCAL;
  }

  @Override
  public String getDescription() {

    return "Upload data to S3 filesystem";
  }

  //
  // Overriding methods
  //

  @Override
  protected FileUploader getDesignUploader(final String src,
      final String filename) throws IOException {

    final S3FileUploader fu = new S3FileUploader(this.awsCredentials);
    fu.init(DataSourceUtils.identifyDataSource(src), getDestURI()
        + "/" + filename);
    fu.setContentType("text/plain");

    return fu;
  }

  @Override
  protected FileUploader getParameterUploader(final String src,
      final String filename) throws IOException {

    final S3FileUploader fu = new S3FileUploader(this.awsCredentials);
    fu.init(DataSourceUtils.identifyDataSource(src), getDestURI()
        + "/" + filename);
    fu.setContentType("text/plain");

    return fu;
  }

  @Override
  protected FileUploader getFastaUploader(final String src,
      final String filename) throws IOException {

    if (!this.uploadGemome)
      return new FakeFileUploader(src, getDestURI() + "/" + filename);

    final String extension = StringUtils.extension(src);
    final S3FileUploader fu;

    if (CompressionType.BZIP2.getExtension().equals(extension)) {

      fu = new S3FileUploader(this.awsCredentials);
      fu.init(new FileDataSource(src), getDestURI() + "/" + filename);
    } else {

      fu = new S3Bzip2FileUploader(this.awsCredentials);
      fu.init(DataSourceUtils.identifyDataSource(src), getDestURI()
          + "/" + filename + CompressionType.BZIP2.getExtension());
      fu.setContentType("text/plain");

    }
    return fu;
  }

  @Override
  protected FileUploader getFastqUploader(final String src,
      final String filename) throws IOException {

    final String extension = StringUtils.extension(src);
    final S3FileUploader fu;

    if (CompressionType.BZIP2.getExtension().equals(extension)) {

      fu = new S3FileUploader(this.awsCredentials);
      fu.init(new FileDataSource(src), getDestURI()
          + "/" + filename + CompressionType.BZIP2.getExtension());
    } else {

      fu = new S3Bzip2FileUploader(this.awsCredentials);
      fu.init(DataSourceUtils.identifyDataSource(src), getDestURI()
          + "/" + StringUtils.filenameWithoutExtension(filename)
          + READS_FASTQ.getDefaultExtention()
          + CompressionType.BZIP2.getExtension());
      fu.setContentType("text/plain");
    }

    return fu;
  }

  @Override
  protected FileUploader getGFFUploader(final String src, final String filename)
      throws IOException {

    final S3FileUploader fu = new S3Bzip2FileUploader(this.awsCredentials);
    fu.init(DataSourceUtils.identifyDataSource(src), getDestURI()
        + "/" + filename + CompressionType.BZIP2.getExtension());
    fu.setContentType("text/plain");

    return fu;
  }

  @Override
  protected FileUploader getIndexUploader(final String src,
      final String filename) throws IOException {

    final File srcFile = new File(src);
    final File indexFile = new File(srcFile.getParent(), filename);

    if (indexFile.exists()) {

      final FileUploader fu = new S3FileUploader(this.awsCredentials);
      fu.init(DataSourceUtils.identifyDataSource(src), getDestURI()
          + "/" + filename);

      return fu;
    }

    final File newIndexFile = SOAPWrapper.makeIndexInZipFile(srcFile);

    final FileUploader fu = new S3FileUploader(this.awsCredentials);
    fu.init(new FileDataSource(newIndexFile), getDestURI() + "/" + filename);

    return fu;

  }

  // @Override
  // public Design upload(final URI paramURI, final URI designURI,
  // final URI destURI) throws EoulsanException, IOException {
  //
  // final Design design =
  // DesignUtils.readAndCheckDesign(FileUtils.createInputStream(new File(
  // designURI)));
  // setDest(destURI);
  // upload(paramURI, design);
  //
  // return design;
  // }

  private IOException exp = null;

  protected void uploadFiles(final List<FileUploader> files) throws IOException {

    int done = 0;
    final List<FileUploader> filesPreprosseced = new ArrayList<FileUploader>();

    final Thread t = new Thread(new Runnable() {

      @Override
      public void run() {

        for (FileUploader f : files) {

          try {
            f.prepare();
            filesPreprosseced.add(f);

          } catch (final IOException e) {
            exp = e;
            return;
          }

        }

      }

    });

    t.start();

    while (done != files.size()) {

      if (exp != null)
        throw this.exp;

      if (filesPreprosseced.size() != 0) {

        final FileUploader f = filesPreprosseced.get(0);
        f.upload();
        filesPreprosseced.remove(0);
        done++;
      }

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new IOException("Unexpected error.");
      }
    }

  }

  protected void writeLog(final URI destURI, final long startTime,
      final String msg) throws IOException {

    Common.writeLog(new File("upload.log"), startTime, msg);
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   * @param credentialsFile credential file
   */
  public S3DataUploadStep(final File credentialsFile) throws IOException {

    if (credentialsFile == null)
      throw new NullPointerException("The credential file is null");

    this.awsCredentials = new PropertiesCredentials(credentialsFile);
  }

}
