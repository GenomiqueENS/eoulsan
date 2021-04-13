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
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;

/**
 * This class define a wrapper on the STAR mapper.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class STARMapperProvider extends AbstractMapperProvider {

  public static final String MAPPER_NAME = "STAR";
  private static final String DEFAULT_VERSION = "2.7.2d";

  private static final String MAPPER_STANDARD_EXECUTABLE = "STAR";
  private static final String MAPPER_LARGE_INDEX_EXECUTABLE = "STARlong";

  private static final String SHORT_INDEX_FLAVOR = "standard";
  private static final String LARGE_INDEX_FLAVOR = "large-index";

  public static final String DEFAULT_ARGUMENTS = "--outSAMunmapped Within";

  private static final String SYNC = STARMapperProvider.class.getName();

  @Override
  public String getName() {
    return MAPPER_NAME;
  }

  @Override
  public String getDefaultVersion() {
    return DEFAULT_VERSION;
  }

  @Override
  public String getDefaultFlavor() {
    return SHORT_INDEX_FLAVOR;
  }

  @Override
  public DataFormat getArchiveFormat() {

    return DataFormats.STAR_INDEX_ZIP;
  }

  @Override
  public String getDefaultMapperArguments() {

    return DEFAULT_ARGUMENTS;
  }

  @Override
  public String readBinaryVersion(final MapperInstance mapperInstance) {

    try {
      final String execPath;

      synchronized (SYNC) {
        execPath = mapperInstance.getExecutor()
            .install(flavoredBinary(mapperInstance.getFlavor()));
      }

      final List<String> cmd = Lists.newArrayList(execPath, "--version");

      final String s =
          MapperUtils.executeToString(mapperInstance.getExecutor(), cmd);
      final String[] lines = s.split("\n");
      if (lines.length == 0) {
        return null;
      }

      final String[] tokens = lines[0].split("_");
      if (tokens.length > 1) {
        return tokens[1].trim();
      }

      return null;

    } catch (IOException e) {

      return null;
    }
  }

  @Override
  public List<String> getIndexerExecutables(
      final MapperInstance mapperInstance) {

    return Collections
        .singletonList(flavoredBinary(mapperInstance.getFlavor()));
  }

  @Override
  public String getMapperExecutableName(final MapperInstance mapperInstance) {
    return flavoredBinary(mapperInstance.getFlavor());
  }

  @Override
  public boolean checkIfFlavorExists(final MapperInstance mapperInstance) {

    switch (mapperInstance.getFlavor().trim().toLowerCase()) {
    case SHORT_INDEX_FLAVOR:
    case LARGE_INDEX_FLAVOR:
      return true;

    default:
      return false;
    }
  }

  /**
   * Get the name of the flavored binary.
   * @return the flavored binary name
   */
  private String flavoredBinary(final String flavor) {

    if (flavor != null
        && LARGE_INDEX_FLAVOR.equals(flavor.trim().toLowerCase())) {
      return MAPPER_LARGE_INDEX_EXECUTABLE;
    }
    return MAPPER_STANDARD_EXECUTABLE;

  }

  @Override
  public List<String> getIndexerCommand(final File indexerFile,
      final File genomeFile, final List<String> indexerArguments,
      final int threads) {

    List<String> cmd = new ArrayList<>();
    cmd.add(indexerFile.getAbsolutePath());
    cmd.add("--runThreadN");
    cmd.add("" + threads);
    cmd.add("--runMode");
    cmd.add("genomeGenerate");
    cmd.add("--genomeDir");
    cmd.add(genomeFile.getParentFile().getAbsolutePath());
    cmd.add("--genomeFastaFiles");
    cmd.add(genomeFile.getAbsolutePath());

    cmd.addAll(indexerArguments);

    return cmd;
  }

  @Override
  public MapperProcess mapSE(final EntryMapping mapping, final File inputFile,
      final File errorFile, final File logFile) throws IOException {

    final String starPath;

    synchronized (SYNC) {
      starPath =
          mapping.getExecutor().install(flavoredBinary(mapping.getFlavor()));
    }

    return createMapperProcessSE(mapping, starPath, inputFile, errorFile,
        logFile);
  }

  @Override
  public MapperProcess mapPE(final EntryMapping mapping, final File inputFile1,
      final File inputFile2, final File errorFile, final File logFile)
      throws IOException {

    final String starPath;

    synchronized (SYNC) {
      starPath =
          mapping.getExecutor().install(flavoredBinary(mapping.getFlavor()));
    }

    return createMapperProcessPE(mapping, starPath, inputFile1, inputFile2,
        errorFile, logFile);
  }

  private MapperProcess createMapperProcessSE(final EntryMapping mapping,
      final String starPath, final File inputFile, final File errorFile,
      final File logFile) throws IOException {

    return new MapperProcess(mapping.getName(), mapping.getExecutor(),
        mapping.getTemporaryDirectory(), errorFile, false, inputFile) {

      @Override
      protected List<List<String>> createCommandLines() {

        // Build the command line
        final List<String> cmd = new ArrayList<>();
        cmd.add(starPath);
        cmd.add("--runThreadN");
        cmd.add("" + mapping.getThreadNumber());
        cmd.add("--genomeDir");
        cmd.add(mapping.getIndexDirectory().getAbsolutePath());

        if (logFile != null) {
          cmd.add("--outFileNamePrefix");
          cmd.add(logFile.getAbsolutePath());
        }

        cmd.add("--outStd");
        cmd.add("SAM");

        cmd.addAll(mapping.getMapperArguments());

        cmd.add("--readFilesIn");
        cmd.add(getNamedPipeFile1().getAbsolutePath());

        return Collections.singletonList(cmd);
      }

    };
  }

  private MapperProcess createMapperProcessPE(final EntryMapping mapping,
      final String starPath, final File inputFile1, final File inputFile2,
      final File errorFile, final File logFile) throws IOException {

    return new MapperProcess(mapping.getName(), mapping.getExecutor(),
        mapping.getTemporaryDirectory(), errorFile, true, true, inputFile1,
        inputFile2) {

      @Override
      protected List<List<String>> createCommandLines() {

        // Build the command line
        final List<String> cmd = new ArrayList<>();
        cmd.add(starPath);
        cmd.add("--runThreadN");
        cmd.add("" + mapping.getThreadNumber());
        cmd.add("--genomeDir");
        cmd.add(mapping.getIndexDirectory().getAbsolutePath());

        if (logFile != null) {
          cmd.add("--outFileNamePrefix");
          cmd.add(logFile.getAbsolutePath());
        }

        cmd.add("--outStd");
        cmd.add("SAM");

        cmd.addAll(mapping.getMapperArguments());

        cmd.add("--readFilesIn");
        cmd.add(getNamedPipeFile1().getAbsolutePath());
        cmd.add(getNamedPipeFile2().getAbsolutePath());

        return Collections.singletonList(cmd);
      }

    };
  }

}
