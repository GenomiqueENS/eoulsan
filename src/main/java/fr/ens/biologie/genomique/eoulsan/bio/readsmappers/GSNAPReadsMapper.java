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

import fr.ens.biologie.genomique.eoulsan.bio.FastqFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.eoulsan.util.ReporterIncrementer;

/**
 * This class define a wrapper on the GSNAP mapper.
 * @since 1.2
 * @author Claire Wallon
 */
public class GSNAPReadsMapper extends AbstractSequenceReadsMapper {

  public static final String MAPPER_NAME = "GSNAP";
  private static final String DEFAULT_PACKAGE_VERSION = "2012-07-20";
  private static final String GSNAP_MAPPER_EXECUTABLE = "gsnap";
  private static final String GMAP_MAPPER_EXECUTABLE = "gmap";
  private static final String[] INDEXER_EXECUTABLES =
      new String[] {"fa_coords", "gmap_process", "gmapindex", "gmap_build"};

  public static final String DEFAULT_ARGUMENTS = "-N 1";

  private static final String SYNC = GSNAPReadsMapper.class.getName();

  @Override
  public String getMapperName() {

    return MAPPER_NAME;
  }

  @Override
  protected String getDefaultPackageVersion() {

    return DEFAULT_PACKAGE_VERSION;
  }

  @Override
  public String internalGetMapperVersion() {

    try {
      final String gsnapPath;

      synchronized (SYNC) {
        gsnapPath = install(flavoredBinary());
      }

      final List<String> cmd = Lists.newArrayList(gsnapPath, " --version");

      final String s = executeToString(cmd);

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
  public boolean isSplitsAllowed() {

    return true;
  }

  @Override
  public DataFormat getArchiveFormat() {

    return DataFormats.GSNAP_INDEX_ZIP;
  }

  @Override
  protected String getDefaultMapperArguments() {

    return DEFAULT_ARGUMENTS;
  }

  @Override
  protected String getIndexerExecutable() {

    return null;
  }

  @Override
  protected String[] getIndexerExecutables() {

    return INDEXER_EXECUTABLES.clone();
  }

  @Override
  public String getMapperExecutableName() {
    return flavoredBinary();
  }

  @Override
  protected List<String> getIndexerCommand(final String indexerPathname,
      final String genomePathname) {
    List<String> cmd = new ArrayList<>();
    final String binariesDirectory =
        new File(indexerPathname).getParentFile().getAbsolutePath();
    final String genomeDirectory =
        new File(genomePathname).getParentFile().getAbsolutePath();

    cmd.add(indexerPathname);
    cmd.add("-B");
    cmd.add(binariesDirectory);
    cmd.add("-D");
    cmd.add(genomeDirectory);
    cmd.add("-d");
    cmd.add("genome");
    cmd.add(genomePathname);

    return cmd;
  }

  /**
   * Get the name of the flavored binary.
   * @return the flavored binary name
   */
  private String flavoredBinary() {

    final String flavor = getMapperFlavorToUse();

    if (flavor != null
            && "gmap".equals(flavor.trim().toLowerCase())) {
      return GMAP_MAPPER_EXECUTABLE;
    }
    return GSNAP_MAPPER_EXECUTABLE;

  }

  @Override
  protected MapperProcess internalMapSE(final File archiveIndexDir)
      throws IOException {

    final String gsnapPath;

    synchronized (SYNC) {
      gsnapPath = install(flavoredBinary());
    }

    return createMapperProcessSE(gsnapPath,
        getGSNAPQualityArgument(getFastqFormat()),
        archiveIndexDir.getAbsolutePath());
  }

  @Override
  protected MapperProcess internalMapPE(final File archiveIndexDir)
      throws IOException {
    final String gsnapPath;

    synchronized (SYNC) {
      gsnapPath = install(flavoredBinary());
    }

    return createMapperProcessPE(gsnapPath,
        getGSNAPQualityArgument(getFastqFormat()),
        archiveIndexDir.getAbsolutePath());
  }

  private MapperProcess createMapperProcessSE(final String gsnapPath,
      final String fastqFormat, final String archivePath) throws IOException {

    return new MapperProcess(this, false) {

      @Override
      protected List<List<String>> createCommandLines() {

        // Build the command line
        final List<String> cmd = new ArrayList<>();
        cmd.add(gsnapPath);

        if (GSNAP_MAPPER_EXECUTABLE.equals(flavoredBinary())) {
          cmd.add("-A");
          cmd.add("sam");
        } else {
          cmd.add("-f");
          cmd.add("samse");
        }

        cmd.add(fastqFormat);
        cmd.add("-t");
        cmd.add(getThreadsNumber() + "");
        cmd.add("-D");
        cmd.add(archivePath);
        cmd.add("-d");
        cmd.add("genome");

        // Set the user options
        cmd.addAll(getListMapperArguments());

        cmd.add(getNamedPipeFile1().getAbsolutePath());

        return Collections.singletonList(cmd);
      }

    };
  }

  private MapperProcess createMapperProcessPE(final String gsnapPath,
      final String fastqFormat, final String archivePath) throws IOException {

    return new MapperProcess(this, true) {

      @Override
      protected List<List<String>> createCommandLines() {

        // Build the command line
        final List<String> cmd = new ArrayList<>();
        cmd.add(gsnapPath);

        if (GSNAP_MAPPER_EXECUTABLE.equals(flavoredBinary())) {
          cmd.add("-A");
          cmd.add("sam");
        } else {
          cmd.add("-f");
          cmd.add("sampe");
        }

        cmd.add(fastqFormat);
        cmd.add("-t");
        cmd.add(getThreadsNumber() + "");
        cmd.add("-D");
        cmd.add(archivePath);
        cmd.add("-d");
        cmd.add("genome");

        // Set the user options
        cmd.addAll(getListMapperArguments());

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
