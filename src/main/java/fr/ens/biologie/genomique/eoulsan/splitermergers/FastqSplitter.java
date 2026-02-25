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
 * This class define a splitter class for FASTQ files.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class FastqSplitter implements Splitter {

  private static final int DEFAULT_SPLIT_MAX_ENTRIES = 1000000;

  private int splitMaxEntries = DEFAULT_SPLIT_MAX_ENTRIES;

  @Override
  public DataFormat getFormat() {

    return DataFormats.READS_FASTQ;
  }

  @Override
  public void configure(final Set<Parameter> conf) throws EoulsanException {

    for (Parameter p : conf) {

      switch (p.getName()) {
        case "max.entries":
          this.splitMaxEntries = p.getIntValueGreaterOrEqualsTo(1);
          break;

        default:
          throw new EoulsanException(
              "Unknown parameter for " + getFormat().getName() + " splitter: " + p.getName());
      }
    }
  }

  @Override
  public void split(final DataFile inFile, final Iterator<DataFile> outFileIterator)
      throws IOException {

    final FastqReader reader = new FastqReader(inFile.open());

    final int max = this.splitMaxEntries;
    int entryCount = 0;
    FastqWriter writer = null;

    for (final ReadSequence read : reader) {

      if (entryCount % max == 0) {

        // Close previous writer
        if (writer != null) {
          writer.close();
        }

        // Create new writer
        writer = new FastqWriter(outFileIterator.next().create());
      }

      writer.write(read);
      entryCount++;
    }

    // Close reader and writer
    reader.close();
    if (writer != null) {
      writer.close();
    }

    try {
      reader.throwException();
    } catch (BadBioEntryException e) {
      throw new IOException(e);
    }
  }
}
