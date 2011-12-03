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

import static fr.ens.transcriptome.eoulsan.io.CompressionType.BZIP2;
import static fr.ens.transcriptome.eoulsan.io.CompressionType.removeCompressionExtension;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.core.ContextUtils;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatConverter;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.io.CompressionType;

/**
 * This class define a Step for local mode file uploading.
 * @author Laurent Jourdren
 */
@LocalOnly
public class LocalUploadStep extends UploadStep {

  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  @Override
  protected DataFile getUploadedDataFile(final DataFile file)
      throws IOException {

    final String filename;

    if (file.getName().endsWith(".zip")
        || file.getName().endsWith(".jar") || file.getName().endsWith(".xml")
        || file.getName().endsWith(".txt"))
      filename = file.getName();
    else
      filename =
          CompressionType.removeCompressionExtension(file.getName())
              + CompressionType.BZIP2.getExtension();

    return new DataFile(getDest(), filename);
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

      if (fileIndex == -1)
        filename = ContextUtils.getNewDataFilename(df, sample);
      else
        filename = ContextUtils.getNewDataFilename(df, sample, fileIndex);
    }

    return new DataFile(getDest(), removeCompressionExtension(filename)
        + BZIP2.getExtension());
  }

  @Override
  protected void copy(final Map<DataFile, DataFile> files) throws IOException {

    if (files == null)
      throw new NullPointerException("The files argument is null.");

    for (Map.Entry<DataFile, DataFile> e : files.entrySet()) {

      final DataFile src = e.getKey();
      final DataFile dest = e.getValue();

      if (src == null || dest == null) {
        continue;
      }

      LOGGER.info("Convert " + src + " to " + dest);
      new DataFormatConverter(src, dest).convert();
    }

  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param dest destination of the files to upload
   */
  public LocalUploadStep(final DataFile dest) {

    super(dest);
  }

}
