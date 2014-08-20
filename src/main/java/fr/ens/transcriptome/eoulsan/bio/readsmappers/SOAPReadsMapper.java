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
 * This class define a wrapper on the SOAP2 mapper.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class SOAPReadsMapper extends AbstractSequenceReadsMapper {

  private static final String DEFAULT_PACKAGE_VERSION = "2.20";
  private static final String MAPPER_EXECUTABLE = "soap";
  private static final String INDEXER_EXECUTABLE = "2bwt-builder";

  public static final String DEFAULT_ARGUMENTS = "-r 2 -l 28";

  private static final String SYNC = SOAPReadsMapper.class.getName();

  private File outputFile;
  private File unmapFile;
  private File unpairedFile;

  @Override
  public String getMapperName() {

    return "SOAP";
  }

  @Override
  protected String getPackageVersion() {

    return DEFAULT_PACKAGE_VERSION;
  }

  @Override
  public boolean isSplitsAllowed() {

    return true;
  }

  @Override
  public String getMapperVersion() {

    try {
      final String execPath;

      synchronized (SYNC) {
        execPath = install(MAPPER_EXECUTABLE);
      }

      final String cmd = execPath;

      final String s = ProcessUtils.execToString(cmd, true, false);
      final String[] lines = s.split("\n");

      for (int i = 0; i < lines.length; i++)
        if (lines[i].startsWith("Version:")) {

          final String[] tokens = lines[i].split(":");
          if (tokens.length > 1)
            return tokens[1].trim();
        }

      return null;

    } catch (IOException e) {
      return null;
    }
  }

  @Override
  public DataFormat getArchiveFormat() {

    return DataFormats.SOAP_INDEX_ZIP;
  }

  @Override
  protected String getIndexerExecutable() {

    return INDEXER_EXECUTABLE;
  }

  @Override
  protected List<String> getIndexerCommand(String indexerPathname,
      String genomePathname) {
    List<String> cmd = new ArrayList<String>();
    cmd.add(indexerPathname);
    cmd.add(genomePathname);
    return cmd;
  }

  @Override
  protected void internalMap(final File readsFile, final File archiveIndexDir)
      throws IOException {

    final String soapPath;

    synchronized (SYNC) {
      soapPath = install(MAPPER_EXECUTABLE);
    }

    this.outputFile =
        FileUtils.createTempFile(readsFile.getParentFile(), "soap-outputFile-",
            ".soap");
    this.unmapFile =
        FileUtils.createTempFile(readsFile.getParentFile(), "soap-outputFile-",
            ".unmap");

    // Build the command line
    final List<String> cmd = new ArrayList<String>();

    cmd.add(soapPath);
    if (getListMapperArguments() != null)
      cmd.addAll(getListMapperArguments());
    cmd.add("-p");
    cmd.add(getThreadsNumber() + "");
    cmd.add("-a");
    cmd.add(readsFile.getAbsolutePath());
    cmd.add("-D");
    cmd.add(getIndexPath(archiveIndexDir, ".index.amb", 4));
    cmd.add("-o");
    cmd.add(outputFile.getAbsolutePath());
    cmd.add("-u");
    cmd.add(unmapFile.getAbsolutePath());

    getLogger().info(cmd.toString());

    final int exitValue = sh(cmd);

    if (exitValue != 0) {
      throw new IOException("Bad error result for SOAP execution: " + exitValue);
    }

  }

  @Override
  protected void internalMap(File readsFile1, File readsFile2,
      File archiveIndexDir) throws IOException {

    final String soapPath;

    synchronized (SYNC) {
      soapPath = install(MAPPER_EXECUTABLE);
    }

    this.outputFile =
        FileUtils.createTempFile(readsFile1.getParentFile(), "soap-output-",
            ".soap");
    this.unmapFile =
        FileUtils.createTempFile(readsFile1.getParentFile(), "soap-output-",
            ".unmap");
    this.unpairedFile =
        FileUtils.createTempFile(readsFile1.getParentFile(), "soap-output-",
            ".unpaired");

    // Build the command line
    final List<String> cmd = new ArrayList<String>();

    cmd.add(soapPath);
    if (getListMapperArguments() != null)
      cmd.addAll(getListMapperArguments());
    cmd.add("-p");
    cmd.add(getThreadsNumber() + "");
    cmd.add("-a");
    cmd.add(readsFile1.getAbsolutePath());
    cmd.add("-b");
    cmd.add(readsFile2.getAbsolutePath());
    cmd.add("-D");
    cmd.add(getIndexPath(archiveIndexDir, ".index.amb", 4));
    cmd.add("-o");
    cmd.add(outputFile.getAbsolutePath());
    cmd.add("-u");
    cmd.add(unmapFile.getAbsolutePath());
    cmd.add("-2");
    cmd.add(unpairedFile.getAbsolutePath());

    getLogger().info(cmd.toString());

    final int exitValue = sh(cmd);

    if (exitValue != 0) {
      throw new IOException("Bad error result for SOAP execution: " + exitValue);
    }

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

  @Override
  public File getSAMFile(final GenomeDescription gd) throws IOException {

    final File resultFile =
        FileUtils.createTempFile(this.outputFile.getParentFile(),
            "soap-output-", ".sam");

    final SOAP2SAM convert =
        new SOAP2SAM(this.outputFile, this.unmapFile, gd, resultFile);
    convert.convert(isPairEnd());

    return resultFile;
  }

  @Override
  public void clean() {

    deleteFile(this.outputFile);
    deleteFile(this.unmapFile);
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
