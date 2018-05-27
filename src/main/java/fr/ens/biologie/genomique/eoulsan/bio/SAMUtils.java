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

package fr.ens.biologie.genomique.eoulsan.bio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SAMTextHeaderCodec;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

/**
 * This class define utility methods for SAM objects
 * @since 1.2
 * @author Laurent Jourdren
 */
public class SAMUtils {

  /**
   * Read the SAM header of a SAM file.
   * @param file file to read
   * @return a String with the SAM header
   * @throws FileNotFoundException if the file cannot be found
   */
  public static String readSAMHeader(final File file)
      throws FileNotFoundException {

    if (file == null) {
      throw new NullPointerException("The file is null");
    }

    return readSAMHeader(new FileInputStream(file));
  }

  /**
   * Read the SAM header of a SAM file.
   * @param dataFile file to read
   * @return a String with the SAM header
   * @throws IOException if an error occurs while reading the file
   */
  public static String readSAMHeader(final DataFile dataFile)
      throws IOException {

    if (dataFile == null) {
      throw new NullPointerException("The data file is null");
    }

    return readSAMHeader(dataFile.open());
  }

  /**
   * Read the SAM header of a SAM file.
   * @param is input stream
   * @return a String with the SAM header
   */
  public static String readSAMHeader(final InputStream is) {

    if (is == null) {
      throw new NullPointerException("The input stream is null.");
    }

    // Read SAM file header
    final SamReader reader =
        SamReaderFactory.makeDefault().open(SamInputResource.of(is));
    final SAMFileHeader header = reader.getFileHeader();

    // Close reader
    // reader.close();

    final StringWriter headerTextBuffer = new StringWriter();
    new SAMTextHeaderCodec().encode(headerTextBuffer, header);

    return headerTextBuffer.toString();
  }

  /**
   * Create a GenomeDescription object from a SAM header.
   * @param file SAM file witch header must be read
   * @return a new GenomeDescription object with the name and chromosomes length
   *         defined in the SAM header
   * @throws FileNotFoundException if the file cannot be found
   */
  public static GenomeDescription createGenomeDescriptionFromSAM(
      final File file) throws FileNotFoundException {

    if (file == null) {
      throw new NullPointerException("The file is null");
    }

    return createGenomeDescriptionFromSAM(new FileInputStream(file));
  }

  /**
   * Create a GenomeDescription object from a SAM header.
   * @param dataFile SAM file witch header must be read
   * @return a new GenomeDescription object with the name and chromosomes length
   *         defined in the SAM header
   * @throws IOException if an error occurs while reading the file
   */
  public static GenomeDescription createGenomeDescriptionFromSAM(
      final DataFile dataFile) throws IOException {

    if (dataFile == null) {
      throw new NullPointerException("The data file is null");
    }

    return createGenomeDescriptionFromSAM(dataFile.open());
  }

  /**
   * Create a GenomeDescription object from a SAM header.
   * @param is InputStream of the SAM file witch header must be read
   * @return a new GenomeDescription object with the name and chromosomes length
   *         defined in the SAM header
   */
  public static GenomeDescription createGenomeDescriptionFromSAM(
      final InputStream is) {

    return createGenomeDescriptionFromSAM(readSAMHeader(is));
  }

  /**
   * Create a GenomeDescription object from a SAM header.
   * @param header SAM header in a String
   * @return a new GenomeDescription object with the name and chromosomes length
   *         defined in the SAM header
   */
  public static GenomeDescription createGenomeDescriptionFromSAM(
      final String header) {

    if (header == null) {
      return null;
    }

    final GenomeDescription desc = new GenomeDescription();

    final String prefix = "@SQ\tSN:";

    for (String line : header.split("\n")) {

      if (!line.startsWith(prefix)) {
        continue;
      }

      final String[] fields = line.substring(prefix.length()).split("\tLN:");

      if (fields.length == 2) {
        desc.addSequence(fields[0], Integer.parseInt(fields[1]));
      }

    }

    return desc;
  }

  /**
   * Create a GenomeDescription object from a SAMFileHeader object.
   * @param header SAM header object
   * @return a new GenomeDescription object with the name and chromosomes length
   *         defined in the SAM header
   */
  public static GenomeDescription createGenomeDescriptionFromSAM(
      final SAMFileHeader header) {

    if (header == null) {
      return null;
    }

    final GenomeDescription desc = new GenomeDescription();

    if (header.getSequenceDictionary() == null) {
      return desc;
    }

    for (SAMSequenceRecord seq : header.getSequenceDictionary()
        .getSequences()) {
      desc.addSequence(seq.getSequenceName(), seq.getSequenceLength());
    }

    return desc;
  }

  /**
   * Create a GenomeDescription object from a SAMFileHeader object.
   * @param samRecord header SAM header object
   * @return a new GenomeDescription object with the name and chromosomes length
   *         defined in the SAM header
   */
  public static GenomeDescription createGenomeDescriptionFromSAM(
      final SAMRecord samRecord) {

    if (samRecord == null) {
      return null;
    }

    return createGenomeDescriptionFromSAM(samRecord.getHeader());
  }

  /**
   * Convert a GenomeDescription object to a SAMSequenceDictionary object.
   * @param genomeDescription genomeDescription object to convert
   * @return a new SAMSequenceDictionary object with chromosomes name and size
   *         from the GenomeDescription object
   */
  public static SAMSequenceDictionary newSAMSequenceDictionary(
      final GenomeDescription genomeDescription) {

    if (genomeDescription == null) {
      throw new NullPointerException("The genome description is null.");
    }

    final List<SAMSequenceRecord> sequences = new ArrayList<>();

    for (String sequenceName : genomeDescription.getSequencesNames()) {

      final SAMSequenceRecord sequenceRecord =
          new SAMSequenceRecord(sequenceName,
              (int) genomeDescription.getSequenceLength(sequenceName));
      sequences.add(sequenceRecord);
    }

    return new SAMSequenceDictionary(sequences);
  }

  /**
   * Convert a GenomeDescription object to a SAMFileHeader object.
   * @param genomeDescription genomeDescription object to convert
   * @return a new SAMFileHeader object with chromosomes name and size from the
   *         GenomeDescription object
   */
  public static SAMFileHeader newSAMFileHeader(
      final GenomeDescription genomeDescription) {

    final SAMFileHeader header = new SAMFileHeader();

    header.setSequenceDictionary(newSAMSequenceDictionary(genomeDescription));

    return header;
  }

}
