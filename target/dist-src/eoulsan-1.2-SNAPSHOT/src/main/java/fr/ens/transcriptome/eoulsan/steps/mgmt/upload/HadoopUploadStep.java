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
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.steps.mgmt.upload;

import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_FASTQ;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_TFQ;
import static fr.ens.transcriptome.eoulsan.io.CompressionType.BZIP2;
import static fr.ens.transcriptome.eoulsan.io.CompressionType.removeCompressionExtension;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.compressionExtension;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.core.ContextUtils;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatConverter;
import fr.ens.transcriptome.eoulsan.data.protocols.DataProtocol;
import fr.ens.transcriptome.eoulsan.data.protocols.DataProtocolService;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.PathUtils;

/**
 * This class define a Step for Hadoop file uploading.
 * @since 1.0
 * @author Laurent Jourdren
 */
@HadoopOnly
public class HadoopUploadStep extends UploadStep {

  private Configuration conf;

  @Override
  protected DataFile getUploadedDataFile(final DataFile file)
      throws IOException {

    return new DataFile(getDest(), file.getName());
  }

  @Override
  protected DataFile getUploadedDataFile(final DataFile file,
      final Sample sample, final DataFormat df, final int fileIndex)
      throws IOException {

    final String filename;

    if (sample == null || df == null) {

      if (file == null)
        throw new IOException("Input file is null.");

      filename = file.getName();
    } else {

//    	final DataFormat format = df == READS_FASTQ ? READS_TFQ : df;
      final DataFormat format = df;

      if (fileIndex == -1 || format.getMaxFilesCount() == 1)
        filename = ContextUtils.getNewDataFilename(format, sample);
      else
        filename = ContextUtils.getNewDataFilename(format, sample, fileIndex);
    }

    return new DataFile(getDest(), filename);
  }

  @Override
  protected void copy(final Map<DataFile, DataFile> files) throws IOException {

    if (files == null)
      throw new NullPointerException("The files argument is null.");

    // Copy local files

    final Set<DataFile> stdCopyFiles = new HashSet<DataFile>();
    final DataProtocol fileProtocol =
        DataProtocolService.getInstance().getProtocol("file");

    for (Map.Entry<DataFile, DataFile> e : files.entrySet()) {

      final DataFile src = e.getKey();
      final DataFile dest = e.getValue();

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

      DataFileDistCp distCp = new DataFileDistCp(this.conf, jobPath);
      distCp.copy(files);
    }
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
