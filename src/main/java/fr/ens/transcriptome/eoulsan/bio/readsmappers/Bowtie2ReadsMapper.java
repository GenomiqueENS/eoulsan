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

package fr.ens.transcriptome.eoulsan.bio.readsmappers;

import java.util.ArrayList;
import java.util.List;

import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class define a wrapper on the Bowtie mapper. Includes only specific
 * methods of bowtie2
 * @since 1.2
 * @author Laurent Jourdren
 */
public class Bowtie2ReadsMapper extends AbstractBowtieReadsMapper {

  public static final String MAPPER_NAME = "Bowtie2";
  private static final String DEFAULT_PACKAGE_VERSION = "2.0.6";
  public static final String DEFAULT_ARGUMENTS = "";

  private static final Version FIRST_FLAVORED_VERSION = new Version(2, 2, 0);
  private static final String MAPPER_EXECUTABLE = "bowtie2-align";
  private static final String INDEXER_EXECUTABLE = "bowtie2-build";

  private static final String EXTENSION_INDEX_FILE = ".rev.1.bt2";

  @Override
  public String getMapperName() {

    return MAPPER_NAME;
  }

  @Override
  protected String getDefaultPackageVersion() {

    return DEFAULT_PACKAGE_VERSION;
  }

  @Override
  protected String getExtensionIndexFile() {

    return EXTENSION_INDEX_FILE
        + (isLongIndexFlavor(FIRST_FLAVORED_VERSION) ? "l" : "");
  }

  @Override
  public DataFormat getArchiveFormat() {

    return DataFormats.BOWTIE2_INDEX_ZIP;
  }

  @Override
  protected String getIndexerExecutable() {

    return flavoredBinary(INDEXER_EXECUTABLE, FIRST_FLAVORED_VERSION);
  }

  @Override
  protected String[] getMapperExecutables() {

    return new String[] { flavoredBinary(MAPPER_EXECUTABLE,
        FIRST_FLAVORED_VERSION) };
  }

  @Override
  public String getDefaultMapperArguments() {
    return DEFAULT_ARGUMENTS;
  }

  protected static final String getBowtieQualityArgument(
      final FastqFormat format) throws Exception {

    switch (format) {

    case FASTQ_SOLEXA:
      // TODO BOWTIE do not support solexa quality scores
      // return "--solexa-quals";
      throw new Exception("Format "
          + format.getName() + " not available with bowtie2");

    case FASTQ_ILLUMINA:
    case FASTQ_ILLUMINA_1_5:
      return "--phred64";

    case FASTQ_SANGER:
    default:
      return "--phred33";
    }
  }

  @Override
  protected List<String> createCommonArgs(final String bowtiePath,
      final String index, final boolean inputCrossbowFormat,
      final boolean memoryMappedIndex) {

    final List<String> result = new ArrayList<>();

    // Bowtie Executable path
    result.add(bowtiePath);

    // Set the user options
    result.addAll(getListMapperArguments());

    // Set the number of threads to use
    result.add("-p");
    result.add(getThreadsNumber() + "");

    // Enable memory mapped index
    if (memoryMappedIndex) {
      result.add("--mm");
    }

    // Input Format in FASTQ or Crossbow format
    if (inputCrossbowFormat) {
      result.add(("-r"));
    } else {
      result.add(("-q"));
    }

    // Set the quality format
    result.add(bowtieQualityArgument());

    // TODO option used for specify sam file
    // Output in SAM format
    // result.add("-S");

    // Quiet mode
    result.add("--quiet");

    // Genome index name
    result.add("-x");
    result.add(index);

    return result;
  }

}
