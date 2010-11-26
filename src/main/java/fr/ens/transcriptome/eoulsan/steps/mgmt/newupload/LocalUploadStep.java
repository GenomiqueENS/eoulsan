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

package fr.ens.transcriptome.eoulsan.steps.mgmt.newupload;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormatConverter;

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

    return new DataFile(getDest(), file.getName());
  }

  @Override
  protected void copy(final Map<DataFile, DataFile> files) throws IOException {

    if (files == null)
      throw new NullPointerException("The files argument is null.");

    for (Map.Entry<DataFile, DataFile> e : files.entrySet()) {
      LOGGER.info("Convert "+e.getKey()+" to "+ e.getValue());
      new DataFormatConverter(e.getKey(), e.getValue()).convert();
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
