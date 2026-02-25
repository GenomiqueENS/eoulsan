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
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class define a splitter class for BAM files.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class BAMSplitter implements Splitter {

  private static final int DEFAULT_SPLIT_MAX_ENTRIES = 1000000;

  private int splitMaxEntries = DEFAULT_SPLIT_MAX_ENTRIES;
  private boolean splitByChromosomes;

  @Override
  public DataFormat getFormat() {

    return DataFormats.MAPPER_RESULTS_BAM;
  }

  @Override
  public void configure(final Set<Parameter> conf) throws EoulsanException {

    for (Parameter p : conf) {

      switch (p.getName()) {
        case "max.entries":
          this.splitMaxEntries = p.getIntValueGreaterOrEqualsTo(1);
          break;

        case "chromosomes":
          this.splitByChromosomes = p.getBooleanValue();
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

    if (this.splitByChromosomes) {
      splitByChromosomes(inFile, outFileIterator);
    } else {
      splitByLineCount(inFile, outFileIterator);
    }
  }

  /**
   * Split BAM file by line count.
   *
   * @param inFile input file
   * @param outFileIterator output files iterator
   * @throws IOException if an error occurs while reading or creating output files
   */
  private void splitByLineCount(final DataFile inFile, final Iterator<DataFile> outFileIterator)
      throws IOException {

    // Get temporary directory
    final File tmpDir = EoulsanRuntime.getRuntime().getTempDirectory();

    // Get reader
    final SamReader reader =
        SamReaderFactory.makeDefault().open(SamInputResource.of(inFile.open()));

    // Get SAM header
    final SAMFileHeader header = reader.getFileHeader();

    final int max = this.splitMaxEntries;
    int entryCount = 0;
    SAMFileWriter writer = null;

    for (final SAMRecord record : reader) {

      if (entryCount % max == 0) {

        // Close previous writer
        if (writer != null) {
          writer.close();
        }

        DataFile outFile = outFileIterator.next();

        // Create new writer
        writer =
            new SAMFileWriterFactory()
                .setTempDirectory(tmpDir)
                .makeBAMWriter(header, false, outFile.create());
      }

      writer.addAlignment(record);
      entryCount++;
    }

    // Close reader and writer
    reader.close();
    if (writer != null) {
      writer.close();
    }
  }

  /**
   * Split BAM file by chromosomes.
   *
   * @param inFile input file
   * @param outFileIterator output files iterator
   * @throws IOException if an error occurs while reading or creating output files
   */
  public void splitByChromosomes(final DataFile inFile, final Iterator<DataFile> outFileIterator)
      throws IOException {

    // Get temporary directory
    final File tmpDir = EoulsanRuntime.getRuntime().getTempDirectory();

    // Get reader
    final SamReader reader =
        SamReaderFactory.makeDefault().open(SamInputResource.of(inFile.open()));

    // Get SAM header
    final SAMFileHeader header = reader.getFileHeader();

    final Map<String, SAMFileWriter> writers = new HashMap<>();

    for (final SAMRecord record : reader) {

      final String chromosome = record.getReferenceName();

      final SAMFileWriter writer;

      // Test if the writer for the chromosome exists
      if (!writers.containsKey(chromosome)) {

        // Create the writer for the chromosome
        DataFile outFile = outFileIterator.next();
        writer =
            new SAMFileWriterFactory()
                .setTempDirectory(tmpDir)
                .makeBAMWriter(header, false, outFile.create());
        writers.put(chromosome, writer);
      } else {
        writer = writers.get(chromosome);
      }

      // Write the record
      writer.addAlignment(record);
    }

    // Close reader
    reader.close();

    // Close writers
    for (SAMFileWriter writer : writers.values()) {
      writer.close();
    }
  }
}
