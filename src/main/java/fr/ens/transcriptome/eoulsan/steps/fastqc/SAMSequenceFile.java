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

package fr.ens.transcriptome.eoulsan.steps.fastqc;

import static org.python.google.common.base.Preconditions.checkNotNull;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

import java.io.File;
import java.io.IOException;

import uk.ac.babraham.FastQC.Sequence.Sequence;
import uk.ac.babraham.FastQC.Sequence.SequenceFile;
import uk.ac.babraham.FastQC.Sequence.SequenceFormatException;
import fr.ens.transcriptome.eoulsan.data.DataFile;

/**
 * This class define a SequenceFile for SAM files.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class SAMSequenceFile implements SequenceFile {

  private final DataFile file;
  private final SamReader reader;
  private final SAMRecordIterator iterator;

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

    final boolean result = this.iterator.hasNext();

    if (result == false) {
      try {
        reader.close();
      } catch (IOException e) {
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

    return new Sequence(this, record.getReadString(),
        record.getBaseQualityString(), record.getReadName());
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

    checkNotNull(inFile, "file argument cannot be null");

    this.file = inFile;
    this.reader =
        SamReaderFactory.makeDefault().open(SamInputResource.of(inFile.open()));
    this.iterator = reader.iterator();
  }
}
