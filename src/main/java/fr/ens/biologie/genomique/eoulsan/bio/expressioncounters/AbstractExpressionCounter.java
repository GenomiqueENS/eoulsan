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

package fr.ens.biologie.genomique.eoulsan.bio.expressioncounters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.GenomeDescription;
import fr.ens.biologie.genomique.eoulsan.bio.io.GFFReader;
import fr.ens.biologie.genomique.eoulsan.bio.io.GTFReader;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.util.ReporterIncrementer;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

/**
 * This abstract class implements a generic Expression Counter.
 * @since 1.2
 * @author Claire Wallon
 */
public abstract class AbstractExpressionCounter implements ExpressionCounter {

  /**
   * This class allow to save the modified SAM entries after the counting.
   */
  private static class IteratorWriter
      implements Iterable<SAMRecord>, Iterator<SAMRecord> {

    private final SAMFileWriter writer;
    private final Iterator<SAMRecord> samRecords;
    private SAMRecord current;

    @Override
    public Iterator<SAMRecord> iterator() {
      return this;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("remove");
    }

    @Override
    public boolean hasNext() {

      boolean result = this.samRecords.hasNext();

      if (!result && this.current != null) {
        this.writer.addAlignment(this.current);
        this.writer.close();
        this.current = null;
      }

      return result;
    }

    @Override
    public SAMRecord next() {

      if (this.current != null) {
        this.writer.addAlignment(this.current);
        this.current = null;
      }

      this.current = this.samRecords.next();

      return this.current;
    }

    /**
     * Constructor.
     * @param writer SAM writer
     * @param samReader SAM reader
     */
    private IteratorWriter(final SAMFileWriter writer,
        final SamReader samReader) {

      this.writer = writer;
      this.samRecords = samReader.iterator();
    }
  }

  @Override
  public void init(final DataFile genomeDescFile, final DataFile annotationFile,
      final boolean gtfFormat) throws EoulsanException, IOException {

    init(genomeDescFile.open(), annotationFile.open(), gtfFormat);
  }

  @Override
  public void init(final InputStream descIs, final InputStream annotationIs,
      final boolean gtfFormat) throws EoulsanException, IOException {

    try (GFFReader gffReader =
        gtfFormat ? new GTFReader(annotationIs) : new GFFReader(annotationIs)) {

      init(GenomeDescription.load(descIs), gffReader);
    }
  }

  @Override
  public Map<String, Integer> count(final DataFile samFile,
      final ReporterIncrementer reporter, final String counterGroup)
      throws EoulsanException, IOException {

    if (samFile == null) {
      throw new NullPointerException("the samFile argument is null");
    }

    return count(samFile.open(), reporter, counterGroup);
  }

  @Override
  public Map<String, Integer> count(final InputStream inputSam,
      final ReporterIncrementer reporter, final String counterGroup)
      throws EoulsanException {

    if (inputSam == null) {
      throw new NullPointerException("the inputSam argument is null");
    }

    return count(
        SamReaderFactory.makeDefault().open(SamInputResource.of(inputSam)),
        reporter, counterGroup);
  }

  @Override
  public Map<String, Integer> count(final InputStream inputSam,
      final OutputStream outputSam, final File temporaryDirectory,
      final ReporterIncrementer reporter, final String counterGroup)
      throws EoulsanException {

    if (inputSam == null) {
      throw new NullPointerException("the inputSam argument is null");
    }

    if (outputSam == null) {
      throw new NullPointerException("the outputSam argument is null");
    }

    if (temporaryDirectory == null) {
      throw new NullPointerException("the temporaryDirectory argument is null");
    }

    // Define the reader
    SamReader reader =
        SamReaderFactory.makeDefault().open(SamInputResource.of(inputSam));

    // Define the writer
    SAMFileWriter writer =
        new SAMFileWriterFactory().setTempDirectory(temporaryDirectory)
            .makeSAMWriter(reader.getFileHeader(), false, outputSam);

    return count(new IteratorWriter(writer, reader), reporter, counterGroup);
  }

}
