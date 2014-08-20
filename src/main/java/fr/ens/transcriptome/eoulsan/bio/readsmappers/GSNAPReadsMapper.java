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
import java.util.List;

import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.SAMParserLine;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

/**
 * This class define a wrapper on the GSNAP mapper.
 * @since 1.2
 * @author Claire Wallon
 */
public class GSNAPReadsMapper extends AbstractSequenceReadsMapper {

  private static final String DEFAULT_PACKAGE_VERSION = "2012-07-20";
  private static final String MAPPER_EXECUTABLE = "gsnap";
  private static final String[] INDEXER_EXECUTABLES = new String[] {
      "fa_coords", "gmap_process", "gmapindex", "gmap_build"};

  public static final String DEFAULT_ARGUMENTS = "-N 1";

  private static final String SYNC = GSNAPReadsMapper.class.getName();
  private static final String MAPPER_NAME = "GSNAP";

  private File outputFile;

  @Override
  public String getMapperName() {

    return MAPPER_NAME;
  }

  @Override
  protected String getPackageVersion() {

    return DEFAULT_PACKAGE_VERSION;
  }

  @Override
  public String getMapperVersion() {

    try {
      final String gsnapPath;

      synchronized (SYNC) {
        gsnapPath = install(MAPPER_EXECUTABLE);
      }

      final String cmd = gsnapPath + " --version";

      final String s = ProcessUtils.execToString(cmd);

      if (s == null)
        return null;

      final String[] lines = s.split("\n");
      if (lines.length == 0)
        return null;

      final String[] tokens = lines[2].split(" version ");
      if (tokens.length == 2)
        return tokens[1];

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
  public File getSAMFile(GenomeDescription gd) throws IOException {

    return this.outputFile;
  }

  @Override
  public void clean() {

  }

  @Override
  public DataFormat getArchiveFormat() {

    return DataFormats.GSNAP_INDEX_ZIP;
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
  protected List<String> getIndexerCommand(String indexerPathname,
      String genomePathname) {
    List<String> cmd = new ArrayList<String>();
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

  // TODO to remove
  protected String getIndexerCommand_OLD(String indexerPathname,
      String genomePathname) {

    final String binariesDirectory =
        new File(indexerPathname).getParentFile().getAbsolutePath();
    final String genomeDirectory =
        new File(genomePathname).getParentFile().getAbsolutePath();

    return indexerPathname
        + " -B " + binariesDirectory + " -D " + genomeDirectory + " -d genome "
        + genomePathname;
  }

  @Override
  protected void internalMap(File readsFile1, File readsFile2,
      File archiveIndexDir) throws IOException {

    final String gsnapPath;

    synchronized (SYNC) {
      gsnapPath = install(MAPPER_EXECUTABLE);
    }

    final File outputFile =
        FileUtils.createTempFile(readsFile1.getParentFile(), getMapperName()
            .toLowerCase() + "-outputFile-", ".sam");

    // Build the command line
    final List<String> cmd = new ArrayList<String>();
    cmd.add(gsnapPath);
    cmd.add("-A");
    cmd.add("sam");
    cmd.add(getGSNAPQualityArgument(getFastqFormat()));
    cmd.add("-t");
    cmd.add(getThreadsNumber() + "");
    cmd.add("-D");
    cmd.add(archiveIndexDir.getAbsolutePath());
    cmd.add("-d");
    cmd.add("genome");
    if (getListMapperArguments() != null)
      cmd.addAll(getListMapperArguments());
    cmd.add(readsFile1.getAbsolutePath());
    cmd.add(readsFile2.getAbsolutePath());

    cmd.add(">");
    cmd.add(outputFile.getAbsolutePath());

    // Old version : cmd = gsnapPath
    // + " -A sam " + getGSNAPQualityArgument(getFastqFormat()) + " -t "
    // + getThreadsNumber() + " -D " + archiveIndexDir.getAbsolutePath()
    // + " -d genome " + getMapperArguments() + " "
    // + readsFile1.getAbsolutePath() + " " + readsFile2.getAbsolutePath()
    // + " > " + outputFile.getAbsolutePath() + " 2> /dev/null";

    getLogger().info(cmd.toString());

    final int exitValue = ProcessUtils.sh(cmd);

    if (exitValue != 0) {
      throw new IOException("Bad error result for "
          + MAPPER_NAME + " execution: " + exitValue);
    }

    this.outputFile = outputFile;

  }

  @Override
  protected void internalMap(File readsFile, File archiveIndexDir)
      throws IOException {

    final String gsnapPath;

    synchronized (SYNC) {
      gsnapPath = install(MAPPER_EXECUTABLE);
    }

    final File outputFile =
        FileUtils.createTempFile(readsFile.getParentFile(), getMapperName()
            .toLowerCase() + "-outputFile-", ".sam");

    // Build the command line
    final List<String> cmd = new ArrayList<String>();
    cmd.add(gsnapPath);
    cmd.add("-A");
    cmd.add("sam");
    cmd.add(getGSNAPQualityArgument(getFastqFormat()));
    cmd.add("-t");
    cmd.add(getThreadsNumber() + "");
    cmd.add("-D");
    cmd.add(archiveIndexDir.getAbsolutePath());
    cmd.add("-d");
    cmd.add("genome");
    if (getListMapperArguments() != null)
      cmd.addAll(getListMapperArguments());
    cmd.add(readsFile.getAbsolutePath());

    cmd.add(">");
    cmd.add(outputFile.getAbsolutePath());

    // Old version : cmd = gsnapPath
    // + " -A sam " + getGSNAPQualityArgument(getFastqFormat()) + " -t "
    // + getThreadsNumber() + " -D " + archiveIndexDir.getAbsolutePath()
    // + " -d genome " + getMapperArguments() + " "
    // + readsFile.getAbsolutePath() + " > "
    // + outputFile.getAbsolutePath() + " 2> /dev/null";

    getLogger().info(cmd.toString());

    final int exitValue = ProcessUtils.sh(cmd);

    if (exitValue != 0) {
      throw new IOException("Bad error result for "
          + MAPPER_NAME + " execution: " + exitValue);
    }

    this.outputFile = outputFile;

  }

  @Override
  protected void internalMap(final File readsFile1, final File readsFile2,
      final File archiveIndex, final SAMParserLine parserLine)
      throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void internalMap(final File readsFile, final File archiveIndex,
      final SAMParserLine parserLine) throws IOException {
    throw new UnsupportedOperationException();
  }

  private static final String getGSNAPQualityArgument(final FastqFormat format)
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
  public void init(final boolean pairEnd, final FastqFormat fastqFormat,
      final File archiveIndexFile, final File archiveIndexDir,
      final ReporterIncrementer incrementer, final String counterGroup)
      throws IOException {

    super.init(pairEnd, fastqFormat, archiveIndexFile, archiveIndexDir,
        incrementer, counterGroup);
    setMapperArguments(DEFAULT_ARGUMENTS);
  }

}
