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
import java.util.Map;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormatConverter;
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
  protected DataFile getUploadedDataFile(final DataFile file, final int id)
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
