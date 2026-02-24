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
import java.nio.file.Path;

import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import uk.ac.babraham.FastQC.Sequence.Sequence;
import uk.ac.babraham.FastQC.Sequence.SequenceFormatException;

/**
 * This class define a SequenceFile for SAM files.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class SAMSequenceFile implements CounterSequenceFile {

  private final DataFile file;
  private final SamReader reader;
  private final SAMRecordIterator iterator;
  private long count;

  @Override
  public File getFile() {

    return Path.of(this.file.getName()).toFile();
  }

  @Override
  public int getPercentComplete() {

    return 0;
  }

  @Override
  public boolean hasNext() {

    final boolean result = this.iterator.hasNext();

    if (!result) {
      try {
        reader.close();
      } catch (IOException e) {
        // Do not handle exception
      }
    }

    return result;
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

    final SAMRecord record = this.iterator.next();
    this.count++;

    return new Sequence(this, record.getReadString(),
        record.getBaseQualityString(), record.getReadName());
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
   * @param inFile SAM input file
   * @throws IOException if an error occurs when opening the file
   */
  public SAMSequenceFile(final DataFile inFile) throws IOException {

    requireNonNull(inFile, "file argument cannot be null");

    this.file = inFile;
    this.reader =
        SamReaderFactory.makeDefault().open(SamInputResource.of(inFile.open()));
    this.iterator = reader.iterator();
  }

}
