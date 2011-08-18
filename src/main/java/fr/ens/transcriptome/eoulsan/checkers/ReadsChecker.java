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

package fr.ens.transcriptome.eoulsan.checkers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.IlluminaReadId;
import fr.ens.transcriptome.eoulsan.bio.io.FastqReader;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;

public class ReadsChecker implements Checker {

  public static final int MAX_READS_TO_CHECK = 1000;

  @Override
  public String getName() {

    return "reads_checker";
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {
  }

  @Override
  public boolean check(final Design design, final Context context,
      final CheckStore checkInfo) throws EoulsanException {

    if (design == null)
      throw new NullPointerException("The design is null");

    if (context == null)
      throw new NullPointerException("The execution context is null");

    if (checkInfo == null)
      throw new NullPointerException("The check info info is null");

    for (Sample s : design.getSamples()) {

      // get input file count for the sample
      final int inFileCount =
          context.getDataFileCount(DataFormats.READS_FASTQ, s);

      if (inFileCount < 1)
        throw new EoulsanException("No reads file found.");

      if (inFileCount > 2)
        throw new EoulsanException(
            "Cannot handle more than 2 reads files at the same time.");

      final FastqFormat format = s.getMetadata().getFastqFormat();

      // Single end mode
      if (inFileCount == 1) {

        final DataFile file =
            context.getDataFile(DataFormats.READS_FASTQ, s, 0);

        checkReadFile(file, format);
      }

      // Paired end mode
      if (inFileCount == 2) {

        final DataFile file1 =
            context.getDataFile(DataFormats.READS_FASTQ, s, 0);

        final DataFile file2 =
            context.getDataFile(DataFormats.READS_FASTQ, s, 1);

        checkReadFile(file1, format, true, 1);
        checkReadFile(file2, format, true, 2);
      }

    }

    return true;
  }

  private void checkReadFile(final DataFile file, final FastqFormat format)
      throws EoulsanException {

    checkReadFile(file, format, false, -1);
  }

  private void checkReadFile(final DataFile file, FastqFormat format,
      final boolean checkPairMember, final int pairMember)
      throws EoulsanException {

    final InputStream is;

    try {

      is = file.open();
      checkReadsFile(is, MAX_READS_TO_CHECK, format, checkPairMember,
          pairMember);

    } catch (IOException e) {
      throw new EoulsanException("Error while reading reads of sample "
          + file.getSource() + " for checking: " + e.getMessage());
    } catch (BadBioEntryException e) {
      throw new EoulsanException("Found bad read entry in sample "
          + file.getSource() + " (cause: " + e.getMessage()
          + ") when checking: " + e.getEntry());
    }

  }

  private boolean checkReadsFile(final InputStream is,
      final int maxReadTocheck, final FastqFormat format,
      final boolean checkPairMember, final int pairMember) throws IOException,
      BadBioEntryException {

    final FastqReader reader = new FastqReader(is);

    int count = 0;

    while (reader.readEntry() && count < maxReadTocheck) {

      // For the first read check the id
      if (checkPairMember && count == 0) {

        final String readId = reader.getName();
        int readPairMember = -1;
        try {
          final IlluminaReadId irid = new IlluminaReadId(readId);

          readPairMember = irid.getPairMember();

        } catch (EoulsanException e) {

          // Not an Illumina id
          if (readId.endsWith("/1"))
            readPairMember = 1;
          else if (readId.endsWith("/2"))
            readPairMember = 2;
        }

        if (readPairMember > 0 && readPairMember != pairMember)
          throw new BadBioEntryException("Invalid pair member number, "
              + pairMember + " was excepted", reader.getName());
      }

      // check the quality string
      if (format != null) {

        final int invalidChar = format.isStringValid(reader.getQuality());

        if (invalidChar != -1)
          throw new BadBioEntryException("Invalid quality character found for "
              + format.getName() + " format: " + (char) invalidChar,
              reader.getQuality());
      }

      count++;
    }
    is.close();
    return true;
  }
}
