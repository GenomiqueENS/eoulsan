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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.SAMParserLine;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

/**
 * This class define a mapper for Gmap. <b>Warning:</b> This is a fake mapper
 * that can be only use to create index for gmap/gsnap as gmap does not read
 * fastq file and not generate SAM files.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class GmapReadsMapper extends AbstractSequenceReadsMapper {

  private static final String DEFAULT_PACKAGE_VERSION = "2012-07-20";
  private static final String MAPPER_EXECUTABLE = "gmap";
  private static final String[] INDEXER_EXECUTABLES = new String[] {
      "fa_coords", "gmap_process", "gmapindex", "gmap_build" };

  public static final String DEFAULT_ARGUMENTS = "";

  private static final String SYNC = GmapReadsMapper.class.getName();
  private static final String MAPPER_NAME = "Gmap";

  @Override
  public String getMapperName() {

    return MAPPER_NAME;
  }

  @Override
  protected String getPackageVersion() {

    return DEFAULT_PACKAGE_VERSION;
  }

  @Override
  public boolean isSplitsAllowed() {

    return false;
  }

  @Override
  public boolean isIndexGeneratorOnly() {
    return true;
  }

  @Override
  public String getMapperVersion() {

    try {
      final String gmapPath;

      synchronized (SYNC) {
        gmapPath = install(MAPPER_EXECUTABLE);
      }

      final String cmd = gmapPath + " --version";

      final String s = ProcessUtils.execToString(cmd);
      final String[] lines = s.split("\n");
      if (lines.length == 0)
        return null;

      final String[] tokens1 = lines[0].split(" version ");
      if (tokens1.length > 1) {
        final String[] tokens2 = tokens1[1].trim().split(" called ");
        if (tokens2.length > 1)
          return tokens2[0];
      }

      return null;

    } catch (IOException e) {

      return null;
    }
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
  public File getSAMFile(GenomeDescription gd) throws IOException {

    return null;
  }

  @Override
  public void clean() {
    // Do nothing
  }

  @Override
  protected void internalMap(File readsFile1, File readsFile2, File archiveIndex)
      throws IOException {
    // Do Nothing
  }

  @Override
  protected void internalMap(File readsFile, File archiveIndex)
      throws IOException {
    // Do Nothing
  }

  @Override
  protected void internalMap(final File readsFile1, final File readsFile2,
      final File archiveIndex, final SAMParserLine parserLine)
      throws IOException {
    new UnsupportedOperationException();
  }

  @Override
  protected void internalMap(final File readsFile, final File archiveIndex,
      final SAMParserLine parserLine) throws IOException {
    new UnsupportedOperationException();
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
