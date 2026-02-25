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

package fr.ens.biologie.genomique.eoulsan.checkers;

import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.READS_FASTQ;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.kenetre.KenetreException;
import fr.ens.biologie.genomique.kenetre.bio.BadBioEntryException;
import fr.ens.biologie.genomique.kenetre.bio.FastqFormat;
import fr.ens.biologie.genomique.kenetre.bio.IlluminaReadId;
import fr.ens.biologie.genomique.kenetre.bio.ReadSequence;
import fr.ens.biologie.genomique.kenetre.bio.io.FastqReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

/**
 * This class define a checker on FASTQ files.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public class ReadsChecker implements Checker {

  public static final int MAX_READS_TO_CHECK = 1000;

  @Override
  public String getName() {

    return "reads_checker";
  }

  @Override
  public boolean isDesignChecker() {
    return false;
  }

  @Override
  public DataFormat getFormat() {
    return READS_FASTQ;
  }

  @Override
  public Set<DataFormat> getCheckersRequired() {
    return Collections.emptySet();
  }

  @Override
  public void configure(final Set<Parameter> stepParameters) throws EoulsanException {}

  @Override
  public boolean check(final Data data, final CheckStore checkInfo) throws EoulsanException {

    if (data == null) {
      throw new NullPointerException("The sample is null");
    }

    if (checkInfo == null) {
      throw new NullPointerException("The check info info is null");
    }

    final int inFileCount = data.getDataFileCount();

    if (inFileCount < 1) {
      throw new EoulsanException("No reads file found.");
    }

    if (inFileCount > 2) {
      throw new EoulsanException("Cannot handle more than 2 reads files at the same time.");
    }

    // Get FASTQ format
    final FastqFormat format = data.getMetadata().getFastqFormat();

    // Single end mode
    if (inFileCount == 1) {

      checkReadFile(data.getDataFile(0), format);
    }

    // Paired end mode
    if (inFileCount == 2) {

      checkReadFile(data.getDataFile(0), format, true, 1);
      checkReadFile(data.getDataFile(1), format, true, 2);
    }

    return true;
  }

  private void checkReadFile(final DataFile file, final FastqFormat format)
      throws EoulsanException {

    checkReadFile(file, format, false, -1);
  }

  private void checkReadFile(
      final DataFile file,
      final FastqFormat format,
      final boolean checkPairMember,
      final int pairMember)
      throws EoulsanException {

    // If the file does not exists do nothing
    if (!file.exists()) {
      return;
    }

    final InputStream is;

    try {

      is = file.open();
      checkReadsFile(is, MAX_READS_TO_CHECK, format, checkPairMember, pairMember);

    } catch (IOException e) {
      throw new EoulsanException(
          "Error while reading reads of sample "
              + file.getSource()
              + " for checking: "
              + e.getMessage(),
          e);
    } catch (BadBioEntryException e) {
      throw new EoulsanException(
          "Found bad read entry in sample "
              + file.getSource()
              + " (cause: "
              + e.getMessage()
              + ") when checking: "
              + e.getEntry(),
          e);
    }
  }

  private boolean checkReadsFile(
      final InputStream is,
      final int maxReadToCheck,
      final FastqFormat format,
      final boolean checkPairMember,
      final int pairMember)
      throws IOException, BadBioEntryException {

    final FastqReader reader = new FastqReader(is);

    int count = 0;

    for (final ReadSequence read : reader) {

      if (count > maxReadToCheck) {
        break;
      }

      // For the first read check the id
      if (checkPairMember && count == 0) {

        final String readId = read.getName();
        int readPairMember = -1;
        try {

          final IlluminaReadId irid = new IlluminaReadId(readId);

          readPairMember = irid.getPairMember();
          if (readPairMember != pairMember) {
            throw new BadBioEntryException(
                "Invalid pair member number, " + pairMember + " was excepted", read.getName());
          }

          // check the quality string
          if (format != null) {

            final int invalidChar = format.findInvalidChar(read.getQuality());

            if (invalidChar != -1) {
              throw new BadBioEntryException(
                  "Invalid quality character found for "
                      + format.getName()
                      + " format: "
                      + (char) invalidChar,
                  read.getQuality());
            }
          }

          readPairMember = irid.getPairMember();

        } catch (KenetreException e) {

          // Not an Illumina id
          if (readId.endsWith("/1")) {
            readPairMember = 1;
          } else if (readId.endsWith("/2")) {
            readPairMember = 2;
          }
        }

        if (readPairMember > 0 && readPairMember != pairMember) {
          reader.close();
          throw new BadBioEntryException(
              "Invalid pair member number, " + pairMember + " was excepted", read.getName());
        }
      }

      // check the quality string
      if (format != null) {

        final int invalidChar = format.findInvalidChar(read.getQuality());

        if (invalidChar != -1) {
          reader.close();
          throw new BadBioEntryException(
              "Invalid quality character found for "
                  + format.getName()
                  + " format: "
                  + (char) invalidChar,
              read.getQuality());
        }
      }

      count++;
    }
    reader.throwException();

    reader.close();
    return true;
  }
}
