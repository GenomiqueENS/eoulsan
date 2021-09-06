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

import fr.ens.biologie.genomique.eoulsan.core.Version;

/**
 * This class define a wrapper on the Bowtie mapper.
 * @since 2.0
 * @author Laurent Jourdren
 */
public abstract class AbstractBowtieMapperProvider
    extends AbstractMapperProvider {

  protected static final String SYNC =
      AbstractBowtieMapperProvider.class.getName();

  private static final String SHORT_INDEX_FLAVOR = "standard";
  private static final String LARGE_INDEX_FLAVOR = "large-index";

  abstract protected String getExtensionIndexFile(final EntryMapping mapping);

  @Override
  public String getDefaultFlavor() {
    return SHORT_INDEX_FLAVOR;
  }

  @Override
  public boolean isMultipleInstancesAllowed() {
    return true;
  }

  @Override
  public boolean checkIfFlavorExists(final MapperInstance mapperInstance) {

    if (mapperInstance.getFlavor() == null) {
      return true;
    }

    switch (mapperInstance.getFlavor().trim().toLowerCase()) {
    case SHORT_INDEX_FLAVOR:
      return true;

    case LARGE_INDEX_FLAVOR:
      return true;

    default:
      return false;
    }
  }

  protected boolean isLongIndexFlavor(final EntryMapping mapping,
      final Version firstFlavoredVersion) {

    final Version currentVersion = new Version(mapping.getVersion());

    if (currentVersion.greaterThanOrEqualTo(firstFlavoredVersion)) {

      final String flavor = mapping.getFlavor();

      return flavor != null
          && LARGE_INDEX_FLAVOR.equals(flavor.trim().toLowerCase());
    }

    return false;
  }

  /**
   * Get the name of a bowtie flavored binary.
   * @param version mapper version
   * @param flavor mapper flavor
   * @param binary the binary
   * @param firstFlavoredVersion first version of Bowtie to be flavored
   * @return the flavored binary name
   */
  protected String flavoredBinary(final String version, final String flavor,
      final String binary, final Version firstFlavoredVersion) {

    return flavoredBinary(version, flavor, binary, binary,
        firstFlavoredVersion);
  }

  /**
   * Get the name of a bowtie flavored binary.
   * @param version mapper version
   * @param flavor mapper flavor
   * @param binary the binary
   * @param newBinary the binary name for the new versions
   * @param firstFlavoredVersion first version of Bowtie to be flavored
   * @return the flavored binary name
   */
  protected String flavoredBinary(final String version, final String flavor,
      final String binary, final String newBinary,
      final Version firstFlavoredVersion) {

    final Version currentVersion = new Version(version);

    if (currentVersion.greaterThanOrEqualTo(firstFlavoredVersion)) {

      if (flavor != null
          && LARGE_INDEX_FLAVOR.equals(flavor.trim().toLowerCase())) {
        return newBinary + "-l";
      } else {
        return newBinary + "-s";
      }
    }

    return binary;
  }

  @Override
  public String readBinaryVersion(final MapperInstance mapperInstance) {

    try {
      final String bowtiePath;

      synchronized (SYNC) {
        bowtiePath = mapperInstance.getExecutor()
            .install(getMapperExecutableName(mapperInstance));
      }

      final List<String> cmd = Lists.newArrayList(bowtiePath, "--version");

      final String s =
          MapperUtils.executeToString(mapperInstance.getExecutor(), cmd);
      final String[] lines = s.split("\n");
      if (lines.length == 0) {
        return null;
      }

      final String[] tokens = lines[0].split(" version ");
      if (tokens.length > 1) {
        return tokens[1].trim();
      }

      return null;

    } catch (IOException e) {

      return null;
    }
  }

  @Override
  public List<String> getIndexerCommand(final File indexerFile,
      final File genomeFile, final List<String> indexerArguments,
      final int threads) {

    List<String> cmd = new ArrayList<>();

    cmd.add(indexerFile.getAbsolutePath());
    cmd.add(genomeFile.getAbsolutePath());
    cmd.add("genome");

    return cmd;
  }

  protected String bowtieQualityArgument(final EntryMapping mapping) {
    return BowtieMapperProvider
        .getBowtieQualityArgument(mapping.getFastqFormat());
  }

  //
  // Map in streaming mode
  //

  @Override
  public MapperProcess mapSE(final EntryMapping mapping, final File inputFile,
      final File errorFile, final File logFile) throws IOException {

    final String bowtiePath;

    synchronized (SYNC) {
      bowtiePath = mapping.getExecutor()
          .install(getMapperExecutableName(mapping.getMapperInstance()));
    }

    // Get index argument
    final String index = getIndexArgument(mapping);

    return new MapperProcess(mapping.getName(), mapping.getExecutor(),
        mapping.getTemporaryDirectory(), errorFile, false, inputFile) {

      @Override
      protected List<List<String>> createCommandLines() {

        // Build the command line and add common arguments
        final List<String> cmd =
            new ArrayList<>(createCommonArgs(mapping, bowtiePath, index));

        // Enable Index memory mapped in streaming mode
        if (mapping.isMultipleInstancesEnabled()) {
          cmd.add("--mm");
        }

        // Input from temporary FASTQ file
        cmd.add(getNamedPipeFile1().getAbsolutePath());

        return Collections.singletonList(cmd);
      }

      @Override
      protected File executionDirectory() {

        return mapping.getIndexDirectory();
      }

    };
  }

  @Override
  public MapperProcess mapPE(final EntryMapping mapping, final File inputFile1,
      final File inputFile2, final File errorFile, final File logFile)
      throws IOException {

    final String bowtiePath;

    synchronized (SYNC) {
      bowtiePath = mapping.getExecutor()
          .install(getMapperExecutableName(mapping.getMapperInstance()));
    }

    // Get index argument
    final String index = getIndexArgument(mapping);

    return new MapperProcess(mapping.getName(), mapping.getExecutor(),
        mapping.getTemporaryDirectory(), errorFile, true, inputFile1,
        inputFile2) {

      @Override
      protected List<List<String>> createCommandLines() {

        // Build the command line and add common arguments
        final List<String> cmd =
            new ArrayList<>(createCommonArgs(mapping, bowtiePath, index));

        // Enable Index memory mapped in streaming mode
        if (mapping.isMultipleInstancesEnabled()) {
          cmd.add("--mm");
        }

        // First end input FASTQ file
        cmd.add("-1");
        cmd.add(getNamedPipeFile1().getAbsolutePath());

        // Second end input FASTQ file
        cmd.add("-2");
        cmd.add(getNamedPipeFile2().getAbsolutePath());

        return Collections.singletonList(cmd);
      }

      @Override
      protected File executionDirectory() {

        return mapping.getIndexDirectory();
      }

    };

  }

  /**
   * Get the index argument for bowtie from the archive index directory path
   * @param mapping mapping object
   * @return the Bowtie index argument
   * @throws IOException if an error occurs when getting directory path
   */
  private String getIndexArgument(final EntryMapping mapping)
      throws IOException {

    final String extensionIndexFile = getExtensionIndexFile(mapping);

    return MapperUtils.getIndexPath(getName(), mapping.getIndexDirectory(),
        extensionIndexFile, extensionIndexFile.length()).getName();
  }

  protected abstract List<String> createCommonArgs(final EntryMapping mapping,
      final String bowtiePath, final String index);

}
