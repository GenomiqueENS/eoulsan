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

package fr.ens.transcriptome.eoulsan.splitermergers;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.bio.io.FastqReader;
import fr.ens.transcriptome.eoulsan.bio.io.FastqWriter;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;

/**
 * This class define a splitter class for FASTQ files.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class FastqSplitter implements Splitter {

  private static final int DEFAULT_SPLIT_MAX_LINES = 1000000;

  private int splitMaxLines = DEFAULT_SPLIT_MAX_LINES;

  @Override
  public DataFormat getFormat() {

    return DataFormats.READS_FASTQ;
  }

  @Override
  public void configure(final Set<Parameter> conf) throws EoulsanException {

    for (Parameter p : conf) {

      if ("max.lines".equals(p.getName())) {
        this.splitMaxLines = p.getIntValue();

        if (this.splitMaxLines < 1) {
          throw new EoulsanException("Invalid "
              + p.getName() + " parameter value: " + p.getIntValue());
        }

      } else {
        throw new EoulsanException("Unknown parameter for "
            + getFormat().getName() + " splitter: " + p.getName());
      }
    }
  }

  @Override
  public void split(final DataFile inFile,
      final Iterator<DataFile> outFileIterator) throws IOException {

    final FastqReader reader = new FastqReader(inFile.open());

    final int max = this.splitMaxLines;
    int readCount = 0;
    FastqWriter writer = null;

    for (final ReadSequence read : reader) {

      if (readCount % max == 0) {

        // Close previous writer
        if (writer != null) {
          writer.close();
        }

        // Create new writer
        writer = new FastqWriter(outFileIterator.next().create());
      }

      writer.write(read);
      readCount++;
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
