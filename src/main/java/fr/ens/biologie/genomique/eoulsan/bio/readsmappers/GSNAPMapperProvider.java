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

package fr.ens.biologie.genomique.eoulsan.bio.readsmappers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import fr.ens.biologie.genomique.eoulsan.bio.FastqFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;

/**
 * This class define a wrapper on the GSNAP mapper.
 * @since 1.2
 * @author Claire Wallon
 */
public class GSNAPMapperProvider extends AbstractMapperProvider {

  public static final String MAPPER_NAME = "GSNAP";
  private static final String DEFAULT_PACKAGE_VERSION = "2012-07-20";
  private static final String GSNAP_MAPPER_EXECUTABLE = "gsnap";
  private static final String GMAP_MAPPER_EXECUTABLE = "gmap";
  private static final String[] INDEXER_EXECUTABLES =
      new String[] {"fa_coords", "gmap_process", "gmapindex", "gmap_build"};

  public static final String DEFAULT_ARGUMENTS = "-N 1";

  private static final String SYNC = GSNAPMapperProvider.class.getName();

  @Override
  public String getName() {

    return MAPPER_NAME;
  }

  @Override
  public String getDefaultVersion() {

    return DEFAULT_PACKAGE_VERSION;
  }

  @Override
  public String getDefaultFlavor() {
    return GSNAP_MAPPER_EXECUTABLE;
  }

  @Override
  public DataFormat getArchiveFormat() {

    return DataFormats.GSNAP_INDEX_ZIP;
  }

  @Override
  public String getDefaultMapperArguments() {

    return DEFAULT_ARGUMENTS;
  }

  @Override
  public String readBinaryVersion(final MapperInstance mapperInstance) {

    try {
      final String gsnapPath;

      synchronized (SYNC) {
        gsnapPath = mapperInstance.getExecutor()
            .install(flavoredBinary(mapperInstance.getFlavor()));
      }

      final List<String> cmd = Lists.newArrayList(gsnapPath, " --version");

      final String s =
          MapperUtils.executeToString(mapperInstance.getExecutor(), cmd);

      final String[] lines = s.split("\n");

      if (lines.length == 0) {
        return null;
      }

      final String[] tokens = lines[0].split(" version ");
      if (tokens.length == 2) {
        return tokens[1];
      }

      return null;

    } catch (IOException e) {

      return null;
    }
  }

  @Override
  public List<String> getIndexerExecutables(
      final MapperInstance mapperInstance) {

    return Arrays.asList(INDEXER_EXECUTABLES);
  }

  @Override
  public String getMapperExecutableName(final MapperInstance mapperInstance) {
    return flavoredBinary(mapperInstance.getFlavor());
  }

  @Override
  public boolean checkIfFlavorExists(final MapperInstance mapperInstance) {

    switch (mapperInstance.getFlavor().trim().toLowerCase()) {

    case GSNAP_MAPPER_EXECUTABLE:
    case GMAP_MAPPER_EXECUTABLE:
      return true;

    default:
      return false;

    }
  }

  @Override
  public List<String> getIndexerCommand(final File indexerFile,
      final File genomeFile, final List<String> indexerArguments,
      final int threads) {

    List<String> cmd = new ArrayList<>();
    final String binariesDirectory =
        indexerFile.getParentFile().getAbsolutePath();
    final String genomeDirectory = genomeFile.getParentFile().getAbsolutePath();

    cmd.add(indexerFile.getAbsolutePath());
    cmd.add("-B");
    cmd.add(binariesDirectory);
    cmd.add("-D");
    cmd.add(genomeDirectory);
    cmd.add("-d");
    cmd.add("genome");
    cmd.add(genomeFile.getAbsolutePath());

    return cmd;
  }

  /**
   * Get the name of the flavored binary.
   * @return the flavored binary name
   */
  private String flavoredBinary(final String flavor) {

    if (flavor != null && "gmap".equals(flavor.trim().toLowerCase())) {
      return GMAP_MAPPER_EXECUTABLE;
    }
    return GSNAP_MAPPER_EXECUTABLE;

  }

  @Override
  public MapperProcess mapSE(final EntryMapping mapping, final File inputFile,
      final File errorFile, final File logFile) throws IOException {

    final String gsnapPath;

    synchronized (SYNC) {
      gsnapPath =
          mapping.getExecutor().install(flavoredBinary(mapping.getFlavor()));
    }

    return createMapperProcessSE(mapping, gsnapPath,
        getGSNAPQualityArgument(mapping.getFastqFormat()), inputFile,
        errorFile);
  }

  @Override
  public MapperProcess mapPE(final EntryMapping mapping, final File inputFile1,
      final File inputFile2, final File errorFile, final File logFile)
      throws IOException {
    final String gsnapPath;

    synchronized (SYNC) {
      gsnapPath =
          mapping.getExecutor().install(flavoredBinary(mapping.getFlavor()));
    }

    return createMapperProcessPE(mapping, gsnapPath,
        getGSNAPQualityArgument(mapping.getFastqFormat()), inputFile1,
        inputFile2, errorFile);
  }

  private MapperProcess createMapperProcessSE(final EntryMapping mapping,
      final String gsnapPath, final String fastqFormat, final File inputFile,
      final File errorFile) throws IOException {

    return new MapperProcess(mapping.getName(), mapping.getExecutor(),
        mapping.getTemporaryDirectory(), errorFile, false, inputFile) {

      @Override
      protected List<List<String>> createCommandLines() {

        // Build the command line
        final List<String> cmd = new ArrayList<>();
        cmd.add(gsnapPath);

        if (GSNAP_MAPPER_EXECUTABLE
            .equals(flavoredBinary(mapping.getFlavor()))) {
          cmd.add("-A");
          cmd.add("sam");
        } else {
          cmd.add("-f");
          cmd.add("samse");
        }

        cmd.add(fastqFormat);
        cmd.add("-t");
        cmd.add(mapping.getThreadNumber() + "");
        cmd.add("-D");
        cmd.add(mapping.getIndexDirectory().getAbsolutePath());
        cmd.add("-d");
        cmd.add("genome");

        // Set the user options
        cmd.addAll(mapping.getMapperArguments());

        cmd.add(getNamedPipeFile1().getAbsolutePath());

        return Collections.singletonList(cmd);
      }

    };
  }

  private MapperProcess createMapperProcessPE(final EntryMapping mapping,
      final String gsnapPath, final String fastqFormat, final File inputFile1,
      final File inputFile2, final File errorFile) throws IOException {

    return new MapperProcess(mapping.getName(), mapping.getExecutor(),
        mapping.getTemporaryDirectory(), errorFile, true, inputFile1,
        inputFile2) {

      @Override
      protected List<List<String>> createCommandLines() {

        // Build the command line
        final List<String> cmd = new ArrayList<>();
        cmd.add(gsnapPath);

        if (GSNAP_MAPPER_EXECUTABLE
            .equals(flavoredBinary(mapping.getFlavor()))) {
          cmd.add("-A");
          cmd.add("sam");
        } else {
          cmd.add("-f");
          cmd.add("sampe");
        }

        cmd.add(fastqFormat);
        cmd.add("-t");
        cmd.add(mapping.getThreadNumber() + "");
        cmd.add("-D");
        cmd.add(mapping.getIndexDirectory().getAbsolutePath());
        cmd.add("-d");
        cmd.add("genome");

        // Set the user options
        cmd.addAll(mapping.getMapperArguments());

        cmd.add(getNamedPipeFile1().getAbsolutePath());
        cmd.add(getNamedPipeFile2().getAbsolutePath());

        return Collections.singletonList(cmd);
      }

    };
  }

  private static String getGSNAPQualityArgument(final FastqFormat format)
      throws IOException {

    switch (format) {

    case FASTQ_ILLUMINA:
      return "--quality-protocol=illumina";

    case FASTQ_ILLUMINA_1_5:
      return "--quality-protocol=illumina";

    case FASTQ_SOLEXA:
      throw new IOException("GSNAP not handle the Solexa FASTQ format.");

    case FASTQ_SANGER:
    default:
      return "--quality-protocol=sanger";
    }
  }

}
