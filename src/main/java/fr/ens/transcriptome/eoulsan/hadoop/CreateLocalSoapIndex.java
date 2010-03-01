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

package fr.ens.transcriptome.eoulsan.hadoop;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;

import fr.ens.transcriptome.eoulsan.core.SOAPWrapper;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class CreateLocalSoapIndex {

  public static void main(final String[] args) throws Exception {

    if (args.length == 0)
      throw new InvalidParameterException(
          "No parameter for create local soap index");

    final File genomeFile = new File(args[0]);
    FileUtils.checkExistingStandardFile(genomeFile, "genome file");

    // Make SOAP index
    final File genomeIndexFile =
        new File(genomeFile.getParentFile(), StringUtils.basename(genomeFile
            .getName())
            + Common.SOAP_INDEX_ZIP_FILE_EXTENSION);

    final File tempGenomeIndexFile = SOAPWrapper.makeIndex(genomeFile, true);
    System.out.println("Move "
        + tempGenomeIndexFile.getAbsolutePath() + " to "
        + genomeIndexFile.getAbsolutePath());

    if (!FileUtils.moveFile(tempGenomeIndexFile, genomeIndexFile))
      throw new IOException("Can't move "
          + tempGenomeIndexFile.getAbsolutePath() + " to "
          + genomeIndexFile.getAbsolutePath());

  }

}
