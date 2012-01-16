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
package fr.ens.transcriptome.eoulsan.steps.mapping.hadoop;

import static com.google.common.collect.Lists.newArrayList;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.INPUT_MAPPING_READS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.OUTPUT_MAPPING_ALIGNMENTS_COUNTER;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import com.google.common.base.Splitter;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapperService;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.HadoopReporter;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.locker.ExecLock;

/**
 * This class define a generic mapper for reads mapping.
 * @author Laurent Jourdren
 */
public class ReadsMapperMapper extends Mapper<LongWritable, Text, Text, Text> {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String HADOOP_TEMP_DIR = "hadoop.tmp.dir";

  // Parameter keys
  static final String MAPPER_NAME_KEY = Globals.PARAMETER_PREFIX
      + ".mapper.name";
  static final String PAIR_END_KEY = Globals.PARAMETER_PREFIX
      + ".mapper.pairend";
  static final String MAPPER_ARGS_KEY = Globals.PARAMETER_PREFIX
      + ".mapper.args";
  static final String MAPPER_THREADS_KEY = Globals.PARAMETER_PREFIX
      + ".mapper.nb.threads";
  static final String FASTQ_FORMAT_KEY = Globals.PARAMETER_PREFIX
      + ".mapper.fastq.format";

  private static final Splitter TAB_SPLITTER = Splitter.on('\t').trimResults();

  private String counterGroup = this.getClass().getName();

  private File archiveIndexFile;

  private ExecLock lock;

  private SequenceReadsMapper mapper;
  private List<String> fields = newArrayList();

  @Override
  protected void map(final LongWritable key, final Text value,
      final Context context) throws IOException {

    context.getCounter(this.counterGroup,
        INPUT_MAPPING_READS_COUNTER.counterName()).increment(1);

    fields.clear();
    for (String e : TAB_SPLITTER.split(value.toString())) {
      fields.add(e);
    }

    final int fieldsSize = fields.size();

    if (fieldsSize == 3) {

      // Single end
      mapper.writeInputEntry(fields.get(0), fields.get(1), fields.get(2));

    } else if (fieldsSize == 6) {

      // Pair end
      mapper.writeInputEntry(fields.get(0), fields.get(1), fields.get(2),
          fields.get(3), fields.get(4), fields.get(5));
    }

  }

  @Override
  protected void setup(final Context context) throws IOException {

    LOGGER.info("Start of configure()");

    final Configuration conf = context.getConfiguration();

    // Get mapper name
    final String mapperName =
        conf.get(Globals.PARAMETER_PREFIX + ".mapper.name");

    if (mapperName == null) {
      throw new IOException("No mapper set");
    }

    // Set the mapper
    this.mapper =
        SequenceReadsMapperService.getInstance().getMapper(mapperName);

    // Get counter group
    final String counterGroup = conf.get(CommonHadoop.COUNTER_GROUP_KEY);
    if (counterGroup != null) {
      this.counterGroup = counterGroup;
    }

    final boolean pairEnd = Boolean.parseBoolean(conf.get(PAIR_END_KEY));
    final FastqFormat fastqFormat =
        FastqFormat.getFormatFromName(conf.get(FASTQ_FORMAT_KEY, ""
            + EoulsanRuntime.getSettings().getDefaultFastqFormat()));

    // Init mapper
    mapper.init(pairEnd, fastqFormat, new HadoopReporter(context),
        this.counterGroup);
    LOGGER.info("Fastq format: " + fastqFormat);

    // Set lock
    this.lock = new ExecLock(this.mapper.getMapperName().toLowerCase());

    // Get Mapper arguments
    final String mapperArguments = conf.get(MAPPER_ARGS_KEY);
    if (mapperArguments != null) {
      mapper.setMapperArguments(mapperArguments);
    }

    // Get the number of threads to use
    int mapperThreads =
        Integer.parseInt(conf.get(MAPPER_THREADS_KEY, ""
            + Runtime.getRuntime().availableProcessors()));

    if (mapperThreads > Runtime.getRuntime().availableProcessors()
        || mapperThreads < 1) {
      mapperThreads = Runtime.getRuntime().availableProcessors();
    }

    mapper.setThreadsNumber(mapperThreads);
    LOGGER.info("Use "
        + this.mapper.getMapperName() + " with " + mapperThreads
        + " threads option");

    // Set mapper temporary directory
    mapper.setTempDirectory(new File(conf.get(HADOOP_TEMP_DIR)));

    // Download genome reference
    final Path[] localCacheFiles = DistributedCache.getLocalCacheFiles(conf);

    if (localCacheFiles == null || localCacheFiles.length == 0)
      throw new IOException("Unable to retrieve genome index");

    if (localCacheFiles.length > 1)
      throw new IOException("Retrieve more than one file in distributed cache");

    // Get the local genome index zip file
    this.archiveIndexFile = new File(localCacheFiles[0].toString());

    LOGGER.info("Genome index compressed file (from distributed cache): "
        + archiveIndexFile);

    LOGGER.info("End of setup()");
  }

  private String getIndexLocalName(final File archiveIndexFile)
      throws IOException {

    return this.mapper.getMapperName()
        + "-index-" + archiveIndexFile.length() + "-"
        + archiveIndexFile.lastModified();
  }

  @Override
  protected void cleanup(final Context context) throws IOException,
      InterruptedException {

    LOGGER.info("Start of cleanup() of the mapper.");

    context.setStatus("Wait free JVM for running "
        + this.mapper.getMapperName());
    final long waitStartTime = System.currentTimeMillis();

    ProcessUtils.waitRandom(5000);
    LOGGER.info(lock.getProcessesWaiting() + " process(es) waiting.");
    lock.lock();
    ProcessUtils.waitUntilExecutableRunning(mapper.getMapperName()
        .toLowerCase());

    LOGGER
        .info("Wait "
            + StringUtils.toTimeHumanReadable(System.currentTimeMillis()
                - waitStartTime) + " before running "
            + this.mapper.getMapperName());

    // Close the data file
    this.mapper.closeInput();

    context.setStatus("Run " + this.mapper.getMapperName());

    try {

      // Set index directory
      final File archiveIndexDir =
          new File(context.getConfiguration().get(HADOOP_TEMP_DIR)
              + "/" + getIndexLocalName(this.archiveIndexFile));

      LOGGER.info("Genome index directory where decompressed: "
          + archiveIndexDir);

      // Process to mapping
      mapper.map(this.archiveIndexFile, archiveIndexDir);

    } catch (IOException e) {

      LOGGER.severe("Error while running "
          + this.mapper.getMapperName() + ": " + e.getMessage());
      throw e;

    } finally {
      lock.unlock();
    }

    // Parse result file
    context.setStatus("Parse " + this.mapper.getMapperName() + " results");
    final File samOutputFile = this.mapper.getSAMFile(null);
    parseSAMResults(samOutputFile, context);

    // Remove temporary files
    if (!samOutputFile.delete())
      LOGGER.warning("Can not delete "
          + this.mapper.getMapperName() + " output file: "
          + samOutputFile.getAbsolutePath());

    // Clean mapper input files
    this.mapper.clean();

    LOGGER.info("End of close() of the mapper.");
  }

  private final void parseSAMResults(final File resultFile,
      final Context context) throws IOException, InterruptedException {

    String line;

    final Text outKey = new Text();
    final Text outValue = new Text();

    // Parse SAM result file
    final BufferedReader readerResults =
        FileUtils.createBufferedReader(resultFile);

    int entriesParsed = 0;

    while ((line = readerResults.readLine()) != null) {

      final String trimmedLine = line.trim();
      if ("".equals(trimmedLine) || trimmedLine.startsWith("@"))
        continue;

      final int tabPos = line.indexOf('\t');

      if (tabPos != -1) {

        outKey.set(line.substring(0, tabPos));
        outValue.set(line.substring(tabPos + 1));

        entriesParsed++;

        context.write(outKey, outValue);
        context.getCounter(this.counterGroup,
            OUTPUT_MAPPING_ALIGNMENTS_COUNTER.counterName()).increment(1);
      }

    }

    readerResults.close();

    LOGGER.info(entriesParsed
        + " entries parsed in " + this.mapper.getMapperName() + " output file");
  }

}
