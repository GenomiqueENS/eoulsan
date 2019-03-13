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
 * This class define a wrapper on the Minimap2 mapper.
 * @since 2.1
 * @author Laurent Jourdren
 */
public class Minimap2MapperProvider extends AbstractMapperProvider {

  public static final String MAPPER_NAME = "minimap2";
  private static final String DEFAULT_PACKAGE_VERSION = "2.12";

  private static final String MAPPER_STANDARD_EXECUTABLE = "minimap2";

  private static final String DEFAULT_FLAVOR = "standard";
  private static final String DEFAULT_ARGUMENTS = "";

  private static final String SYNC = Minimap2MapperProvider.class.getName();

  private static final String INDEX_FILENAME = "genome.idx";

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
    return DEFAULT_FLAVOR;
  }

  @Override
  public DataFormat getArchiveFormat() {
    return DataFormats.MINIMAP2_INDEX_ZIP;
  }

  @Override
  public String readBinaryVersion(final MapperInstance mapperInstance) {

    try {
      final String execPath;

      synchronized (SYNC) {
        execPath =
            mapperInstance.getExecutor().install(MAPPER_STANDARD_EXECUTABLE);
      }

      final List<String> cmd = Lists.newArrayList(execPath, "--version");

      final String s =
          MapperUtils.executeToString(mapperInstance.getExecutor(), cmd);
      final String[] lines = s.split("\n");
      if (lines.length == 0) {
        return null;
      }

      final String[] tokens = lines[0].split("-");
      if (tokens.length > 1) {
        return tokens[1].trim();
      }

      return null;

    } catch (IOException e) {

      return null;
    }
  }

  @Override
  public String getDefaultMapperArguments() {
    return DEFAULT_ARGUMENTS;
  }

  @Override
  public List<String> getIndexerExecutables(
      final MapperInstance mapperInstance) {
    return Collections.singletonList(MAPPER_STANDARD_EXECUTABLE);
  }

  @Override
  public String getMapperExecutableName(final MapperInstance mapperInstance) {
    return MAPPER_STANDARD_EXECUTABLE;
  }

  @Override
  public List<String> getIndexerCommand(final File indexerFile,
      final File genomeFile, final List<String> indexerArguments,
      final int threads) {

    List<String> cmd = new ArrayList<>();
    cmd.add(indexerFile.getAbsolutePath());
    cmd.addAll(indexerArguments);
    cmd.add(genomeFile.getAbsolutePath());
    cmd.add("-d");
    cmd.add(INDEX_FILENAME);

    return cmd;
  }

  @Override
  public boolean checkIfFlavorExists(final MapperInstance mapperInstance) {
    return true;
  }

  @Override
  public MapperProcess mapSE(final EntryMapping mapping, final File inputFile,
      final File errorFile, final File logFile) throws IOException {

    final String minimap2Path;

    synchronized (SYNC) {
      minimap2Path = mapping.getExecutor().install(MAPPER_STANDARD_EXECUTABLE);
    }

    return new MapperProcess(mapping.getName(), mapping.getExecutor(),
        mapping.getTemporaryDirectory(), errorFile, false, inputFile) {

      @Override
      protected List<List<String>> createCommandLines() {

        // Build the command line
        final List<String> cmd = new ArrayList<>();
        cmd.add(minimap2Path);
        cmd.add("-a");
        cmd.add("-t");
        cmd.add("" + mapping.getThreadNumber());
        cmd.addAll(mapping.getMapperArguments());
        cmd.add(mapping.getIndexDirectory().getAbsolutePath()
            + "/" + INDEX_FILENAME);

        cmd.add(getNamedPipeFile1().getAbsolutePath());

        return Collections.singletonList(cmd);
      }

    };
  }

  @Override
  public MapperProcess mapPE(final EntryMapping mapping, final File inputFile1,
      final File inputFile2, final File errorFile, final File logFile)
      throws IOException {
    final String minimap2Path;

    synchronized (SYNC) {
      minimap2Path = mapping.getExecutor().install(MAPPER_STANDARD_EXECUTABLE);
    }

    return new MapperProcess(mapping.getName(), mapping.getExecutor(),
        mapping.getTemporaryDirectory(), errorFile, false, inputFile1,
        inputFile2) {

      @Override
      protected List<List<String>> createCommandLines() {

        // Build the command line
        final List<String> cmd = new ArrayList<>();
        cmd.add(minimap2Path);
        cmd.add("-a");
        cmd.add("-t");
        cmd.add("" + mapping.getThreadNumber());
        cmd.addAll(mapping.getMapperArguments());
        cmd.add(mapping.getIndexDirectory().getAbsolutePath()
            + "/" + INDEX_FILENAME);

        cmd.add(getNamedPipeFile1().getAbsolutePath());
        cmd.add(getNamedPipeFile2().getAbsolutePath());

        return Collections.singletonList(cmd);
      }

    };
  }

}
