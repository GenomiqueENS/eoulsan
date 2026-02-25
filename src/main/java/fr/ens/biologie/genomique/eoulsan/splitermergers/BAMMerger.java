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
import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * This class define a merger class for SAM files.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class BAMMerger implements Merger {

  @Override
  public DataFormat getFormat() {

    return DataFormats.MAPPER_RESULTS_BAM;
  }

  @Override
  public void configure(final Set<Parameter> conf) throws EoulsanException {

    // The merge does not need any parameter
    for (Parameter p : conf) {
      throw new EoulsanException(
          "Unknown parameter for " + getFormat().getName() + " merger: " + p.getName());
    }
  }

  @Override
  public void merge(final Iterator<DataFile> inFileIterator, final DataFile outFile)
      throws IOException {

    // Get temporary directory
    final File tmpDir = EoulsanRuntime.getRuntime().getTempDirectory();

    SAMFileWriter outputSam = null;

    while (inFileIterator.hasNext()) {

      // Get input file
      final DataFile inFile = inFileIterator.next();

      EoulsanLogger.getLogger().info("Merge " + inFile.getName() + " to " + outFile.getName());

      // Get reader
      final SamReader inputSam =
          SamReaderFactory.makeDefault().open(SamInputResource.of(inFile.open()));

      // Get Writer
      if (outputSam == null) {

        outputSam =
            new SAMFileWriterFactory()
                .setTempDirectory(tmpDir)
                .makeBAMWriter(inputSam.getFileHeader(), false, outFile.create());
      }

      // Write all the entries of the input file to the output file
      for (SAMRecord samRecord : inputSam) {
        outputSam.addAlignment(samRecord);
      }

      inputSam.close();
    }

    outputSam.close();
  }
}
