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

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatConverter;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.data.protocols.DataProtocol;
import fr.ens.transcriptome.eoulsan.data.protocols.DataProtocolService;
import fr.ens.transcriptome.eoulsan.util.PathUtils;

/**
 * This class define a Step for Hadoop file uploading.
 * @author Laurent Jourdren
 */
@HadoopOnly
public class HadoopUploadStep extends UploadStep {

  private Configuration conf;

  @Override
  protected DataFile getUploadedDataFile(final DataFile file, final int id)
      throws IOException {

    if (file == null || !file.exists())
      return null;

    final DataFile dest = getDest();

    DataFile result = null;

    DataFormat df = file.getMetaData().getDataFormat();

    if (df == DataFormats.READS_FASTQ || df == DataFormats.READS_TFQ)
      result =
          new DataFile(dest, DataFormats.READS_TFQ.getType().getPrefix()
              + id + DataFormats.READS_TFQ.getDefaultExtention());

    if (df == DataFormats.GENOME_FASTA)
      result =
          new DataFile(dest, DataFormats.GENOME_FASTA.getType().getPrefix()
              + id + DataFormats.GENOME_FASTA.getDefaultExtention());

    if (df == DataFormats.ANNOTATION_GFF)
      result =
          new DataFile(dest, DataFormats.ANNOTATION_GFF.getType().getPrefix()
              + id + DataFormats.ANNOTATION_GFF.getDefaultExtention());

    if (result == null)
      result = new DataFile(dest, file.getName());

    return result;
  }

  @Override
  protected void copy(final Map<DataFile, DataFile> files) throws IOException {

    if (files == null)
      throw new NullPointerException("The files argument is null.");

    // Copy local files

    final Set<DataFile> stdCopyFiles = new HashSet<DataFile>();
    final DataProtocol fileProtocol =
        DataProtocolService.getInstance().getProtocol("file");

    // Sort the file according file size
    final List<DataFile> inFiles = Lists.newArrayList(files.keySet());
    sortInFilesByDescSize(inFiles);

    for (DataFile src : inFiles) {

      final DataFile dest = files.get(src);

      if (src == null || dest == null) {
        continue;
      }

      if (src.getProtocol() == fileProtocol) {

        new DataFormatConverter(src, dest).convert();
        stdCopyFiles.add(src);
      }
    }

    // Remove already copied files from list files for distributed copy
    for (DataFile file : stdCopyFiles)
      files.remove(file);

    if (files.size() > 0) {
      final Path jobPath =
          PathUtils.createTempPath(new Path(getDest().getSource()), "distcp-",
              "", this.conf);

      DataSourceDistCp distCp = new DataSourceDistCp(this.conf, jobPath);
      distCp.copy(files);
    }
  }

  /**
   * Sort a list of DataFile by dissident order.
   * @param inFiles list of DataFile to sort
   */
  private void sortInFilesByDescSize(final List<DataFile> inFiles) {

    Collections.sort(inFiles, new Comparator<DataFile>() {

      @Override
      public int compare(final DataFile f1, DataFile f2) {

        long size1;

        try {
          size1 = f1.getMetaData().getContentLength();
        } catch (IOException e) {
          size1 = -1;
        }

        long size2;
        try {
          size2 = f2.getMetaData().getContentLength();
        } catch (IOException e) {
          size2 = -1;
        }

        return ((Long) size1).compareTo(size2) * -1;
      }

    });

  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param dest destination of the files to upload
   * @param conf Hadoop configuration
   */
  public HadoopUploadStep(final DataFile dest, final Configuration conf) {

    super(dest);

    if (conf == null)
      throw new NullPointerException("The configuration object is null");

    this.conf = conf;
  }

}
