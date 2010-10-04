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

package fr.ens.transcriptome.eoulsan.programs.mgmt.local;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.core.SOAPWrapper;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.io.DesignReader;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.io.SimpleDesignReader;

/**
 * Main class for creating soap index.
 * @author Laurent Jourdren
 */
public class CreateSoapIndexLocalMain {

  public static String PROGRAM_NAME = "createsoapindex";

  public static void makeIndex(final String designFilename)
      throws EoulsanIOException, IOException {

    DesignReader dr = new SimpleDesignReader(designFilename);
    makeIndex(dr.read());
  }

  private static void makeIndex(final Design design) throws IOException {

    if (design == null)
      return;

    final Map<String, Integer> genomesIndex = new HashMap<String, Integer>();
    int count = 0;

    for (Sample sample : design.getSamples()) {

      final String genomeFilename = sample.getMetadata().getGenome();
      if (genomeFilename == null)
        return;

      final String genomeFilenameTrimed = genomeFilename.trim();

      if (!genomesIndex.containsKey(genomeFilenameTrimed)) {

        count++;

        SOAPWrapper.makeIndex(new File(genomeFilenameTrimed), new File(
            Common.GENOME_SOAP_INDEX_DIR_PREFIX + count));

        genomesIndex.put(genomeFilenameTrimed, count);
      }

    }

  }

}
