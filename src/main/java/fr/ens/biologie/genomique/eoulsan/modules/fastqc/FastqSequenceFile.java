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

package fr.ens.biologie.genomique.eoulsan.modules.fastqc;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;

import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.kenetre.bio.ReadSequence;
import fr.ens.biologie.genomique.kenetre.bio.io.FastqReader;
import uk.ac.babraham.FastQC.Sequence.Sequence;
import uk.ac.babraham.FastQC.Sequence.SequenceFormatException;

/**
 * This class define a SequenceFile for FASTQ files.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class FastqSequenceFile implements CounterSequenceFile {

  private final DataFile file;
  private final FastqReader reader;
  private long count;

  @Override
  public File getFile() {

    return new File(this.file.getName());
  }

  @Override
  public int getPercentComplete() {

    return 0;
  }

  @Override
  public boolean hasNext() {

    return this.reader.hasNext();
  }

  @Override
  public boolean isColorspace() {

    return false;
  }

  @Override
  public String name() {

    return file.getName();
  }

  @Override
  public Sequence next() throws SequenceFormatException {

    final ReadSequence read = this.reader.next();
    this.count++;

    return new Sequence(this, read.getSequence(), read.getQuality(),
        read.getName());
  }

  @Override
  public long getCount() {

    return count;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param fastqFile FASTQ input file
   * @throws IOException if an error occurs when opening the file
   */
  public FastqSequenceFile(final DataFile fastqFile) throws IOException {

    requireNonNull(fastqFile, "file argument cannot be null");

    this.file = fastqFile;
    this.reader = new FastqReader(fastqFile.open());
  }

}
