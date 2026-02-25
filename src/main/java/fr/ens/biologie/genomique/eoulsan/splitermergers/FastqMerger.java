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

package fr.ens.biologie.genomique.eoulsan.splitermergers;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.kenetre.bio.BadBioEntryException;
import fr.ens.biologie.genomique.kenetre.bio.ReadSequence;
import fr.ens.biologie.genomique.kenetre.bio.io.FastqReader;
import fr.ens.biologie.genomique.kenetre.bio.io.FastqWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * This class define a merger class for FASTQ files.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class FastqMerger implements Merger {

  @Override
  public DataFormat getFormat() {

    return DataFormats.READS_FASTQ;
  }

  @Override
  public void configure(final Set<Parameter> conf) throws EoulsanException {

    // The merge does not need any parameter
    for (Parameter p : conf) {
      throw new EoulsanException(
          "Unknown parameter for " + getFormat().getName() + " merger: " + p.getName());
    }
  }

  @Override
  public void merge(final Iterator<DataFile> inDataFileIterator, final DataFile outFile)
      throws IOException {

    final FastqWriter writer = new FastqWriter(outFile.create());

    while (inDataFileIterator.hasNext()) {

      // Get input file
      final DataFile inFile = inDataFileIterator.next();

      EoulsanLogger.getLogger().info("Merge " + inFile.getName() + " to " + outFile.getName());

      final FastqReader reader = new FastqReader(inFile.open());

      for (final ReadSequence read : reader) {
        writer.write(read);
      }
      reader.close();

      try {
        reader.throwException();
      } catch (BadBioEntryException e) {
        throw new IOException(e);
      }
    }

    writer.close();
  }
}
