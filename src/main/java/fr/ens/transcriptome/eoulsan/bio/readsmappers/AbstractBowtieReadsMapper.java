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

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class define a wrapper on the Bowtie mapper.
 * @since 2.0
 * @author Laurent Jourdren
 */
public abstract class AbstractBowtieReadsMapper extends
    AbstractSequenceReadsMapper {

  protected static final String SYNC = AbstractBowtieReadsMapper.class
      .getName();

  abstract protected String getExtensionIndexFile();

  abstract protected String[] getMapperExecutables();

  @Override
  abstract protected String getIndexerExecutable();

  @Override
  public boolean isSplitsAllowed() {
    return true;
  }

  @Override
  public boolean isMultipleInstancesAllowed() {
    return true;
  };

  @Override
  protected boolean checkIfFlavorExists() {

    final String flavor = getMapperFlavorToUse();

    if (flavor == null) {
      return true;
    }

    switch (flavor.trim().toLowerCase()) {
    case "":
    case SHORT_INDEX_FLAVOR:
      setFlavor(SHORT_INDEX_FLAVOR);
      return true;

    case LARGE_INDEX_FLAVOR:
      setFlavor(LARGE_INDEX_FLAVOR);
      return true;

    default:
      return false;
    }
  }

  protected boolean isLongIndexFlavor(final Version firstFlavoredVersion) {

    final Version currentVersion = new Version(getMapperVersionToUse());

    if (currentVersion.greaterThanOrEqualTo(firstFlavoredVersion)) {

      final String flavor = getMapperFlavorToUse();

      return flavor != null
          && LARGE_INDEX_FLAVOR.equals(flavor.trim().toLowerCase());
    }

    return false;
  }

  /**
   * Get the name of a bowtie flavored binary.
   * @param binary the binary
   * @param firstFlavoredVersion first version of Bowtie to be flavored
   * @return the flavored binary name
   */
  protected String flavoredBinary(final String binary,
      final Version firstFlavoredVersion) {

    return flavoredBinary(binary, binary, firstFlavoredVersion);
  }

  /**
   * Get the name of a bowtie flavored binary.
   * @param binary the binary
   * @param newBinary the binary name for the new versions
   * @param firstFlavoredVersion first version of Bowtie to be flavored
   * @return the flavored binary name
   */
  protected String flavoredBinary(final String binary, final String newBinary,
      final Version firstFlavoredVersion) {

    final Version currentVersion = new Version(getMapperVersionToUse());

    if (currentVersion.greaterThanOrEqualTo(firstFlavoredVersion)) {

      final String flavor = getMapperFlavorToUse();

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
  protected String internalGetMapperVersion() {

    try {
      final String bowtiePath;

      synchronized (SYNC) {
        bowtiePath = install(getMapperExecutables());
      }

      final List<String> cmd = Lists.newArrayList(bowtiePath, " --version");

      final String s = executeToString(cmd);
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
  protected List<String> getIndexerCommand(final String indexerPathname,
      final String genomePathname) {

    List<String> cmd = new ArrayList<>();

    cmd.add(indexerPathname);
    cmd.add(genomePathname);
    cmd.add("genome");

    return cmd;
  }

  protected String bowtieQualityArgument() {
    return BowtieReadsMapper.getBowtieQualityArgument(getFastqFormat());
  }

  //
  // Map in streaming mode
  //

  @Override
  protected MapperProcess internalMapSE(final File archiveIndexDir)
      throws IOException {

    final String bowtiePath;

    synchronized (SYNC) {
      bowtiePath = install(getMapperExecutables());
    }

    // Get index argument
    final String index = getIndexArgument(archiveIndexDir);

    return new MapperProcess(this, false) {

      @Override
      protected List<List<String>> createCommandLines() {

        // Build the command line
        final List<String> cmd = new ArrayList<>();

        // Add common arguments
        cmd.addAll(createCommonArgs(bowtiePath, index, false, false));

        // Enable Index memory mapped in streaming mode
        if (isMultipleInstancesEnabled()) {
          cmd.add("--mm");
        }

        // Input from temporary FASTQ file
        cmd.add(getNamedPipeFile1().getAbsolutePath());

        getLogger().info(cmd.toString());

        return Collections.singletonList(cmd);
      }

      @Override
      protected File executionDirectory() {

        return archiveIndexDir;
      }

    };
  }

  @Override
  protected MapperProcess internalMapPE(final File archiveIndexDir)
      throws IOException {

    final String bowtiePath;

    synchronized (SYNC) {
      bowtiePath = install(getMapperExecutables());
    }

    // Get index argument
    final String index = getIndexArgument(archiveIndexDir);

    return new MapperProcess(this, true) {

      @Override
      protected List<List<String>> createCommandLines() {

        // Build the command line
        final List<String> cmd = new ArrayList<>();

        // Add common arguments
        cmd.addAll(createCommonArgs(bowtiePath, index, false, false));

        // Enable Index memory mapped in streaming mode
        if (isMultipleInstancesEnabled()) {
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

        return archiveIndexDir;
      }

    };

  }

  /**
   * Get the index argument for bowtie from the archive index directory path
   * @param archiveIndexDir archive index directory
   * @return the Bowtie index argument
   * @throws IOException if an error occurs when getting directory path
   */
  private String getIndexArgument(final File archiveIndexDir)
      throws IOException {

    final String extensionIndexFile = getExtensionIndexFile();

    return new File(getIndexPath(archiveIndexDir, extensionIndexFile,
        extensionIndexFile.length())).getName();
  }

  protected List<String> createCommonArgs(final String bowtiePath,
      final String index) {

    return createCommonArgs(bowtiePath, index, false, false);
  }

  protected abstract List<String> createCommonArgs(final String bowtiePath,
      final String index, final boolean inputCrossbowFormat,
      final boolean memoryMappedIndex);

  //
  // Init
  //

  @Override
  public void init(final File archiveIndexFile, final File archiveIndexDir,
      final ReporterIncrementer incrementer, final String counterGroup)
      throws IOException {

    super.init(archiveIndexFile, archiveIndexDir, incrementer, counterGroup);
  }

}
