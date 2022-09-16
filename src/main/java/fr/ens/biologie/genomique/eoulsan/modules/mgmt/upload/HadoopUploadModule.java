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

package fr.ens.biologie.genomique.eoulsan.modules.mgmt.upload;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import fr.ens.biologie.genomique.eoulsan.annotations.HadoopOnly;
import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.core.workflow.StepOutputDataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatConverter;
import fr.ens.biologie.genomique.eoulsan.data.protocols.StorageDataProtocol;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import fr.ens.biologie.genomique.kenetre.io.CompressionType;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.PathUtils;

/**
 * This class define a module for Hadoop file uploading.
 * @since 1.0
 * @author Laurent Jourdren
 */
@HadoopOnly
public class HadoopUploadModule extends UploadModule {

  private final Configuration conf;

  @Override
  protected DataFile getUploadedDataFile(final DataFile file)
      throws IOException {

    return new DataFile(getDest(), file.getName());
  }

  @Override
  protected DataFile getUploadedDataFile(final DataFile file, final Step step,
      final Sample sample, final String portName, final DataFormat format,
      final int fileIndex) throws IOException {

    final String filename;

    if (sample == null || portName == null) {

      if (file == null) {
        throw new IOException("Input file is null.");
      }

      filename = file.getName();
    } else {

      filename = StepOutputDataFile.newStandardFilename(step, portName, format,
          sample, fileIndex, CompressionType.NONE);
    }

    return new DataFile(getDest(), filename);
  }

  @Override
  protected void copy(final Map<DataFile, DataFile> files) throws IOException {

    if (files == null) {
      throw new NullPointerException("The files argument is null.");
    }

    // Process to local copies
    for (Map.Entry<DataFile, DataFile> e : new HashMap<>(files).entrySet()) {

      final DataFile src = e.getKey();
      final DataFile dest = e.getValue();

      if (src == null || dest == null) {
        continue;
      }

      // Test if the file exists
      if (!src.exists()) {
        throw new IOException("The file does not exists: " + src);
      }

      // If the file is local file to a local copy/conversion
      if (src.toFile() != null) {

        // Process to copy now
        new DataFormatConverter(new DataFile(src.toFile()), dest).convert();

        // Remove the file from the list of files to copy
        files.remove(src);

      } else
      // If the file comes from a storage
      if (src.getProtocol() instanceof StorageDataProtocol) {

        final DataFile newSrc =
            ((StorageDataProtocol) src.getProtocol()).getUnderLyingData(src);

        // Update the map of files to copy
        if (src != null) {
          files.remove(src);
          files.put(newSrc, dest);
        }
      }
    }

    // Process to distributed copies
    if (files.size() > 0) {
      final Path jobPath = PathUtils.createTempPath(
          new Path(getDest().getSource()), "distcp-", "", this.conf);

      new DataFileDistCp(this.conf, jobPath).copy(files);
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
  public HadoopUploadModule(final DataFile dest, final Configuration conf) {

    super(dest);

    if (conf == null) {
      throw new NullPointerException("The configuration object is null");
    }

    this.conf = conf;
  }

}
