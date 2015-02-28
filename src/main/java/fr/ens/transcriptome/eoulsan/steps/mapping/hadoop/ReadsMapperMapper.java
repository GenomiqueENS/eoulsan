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

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.INPUT_MAPPING_READS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.OUTPUT_MAPPING_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.util.Utils.checkNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import com.google.common.base.Splitter;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.HadoopEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.MapperProcess;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapperService;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.hadoop.HadoopReporter;
import fr.ens.transcriptome.eoulsan.util.locker.Locker;
import fr.ens.transcriptome.eoulsan.util.locker.ZooKeeperLocker;

/**
 * This class defines a generic mapper for reads mapping.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class ReadsMapperMapper extends Mapper<LongWritable, Text, Text, Text> {

  private static final String HADOOP_TEMP_DIR = "hadoop.tmp.dir";

  // Parameter keys
  static final String MAPPER_NAME_KEY = Globals.PARAMETER_PREFIX
      + ".mapper.name";
  static final String MAPPER_VERSION_KEY = Globals.PARAMETER_PREFIX
      + ".mapper.version";
  static final String MAPPER_FLAVOR_KEY = Globals.PARAMETER_PREFIX
      + ".mapper.flavor";
  static final String PAIR_END_KEY = Globals.PARAMETER_PREFIX
      + ".mapper.pairend";
  static final String MAPPER_ARGS_KEY = Globals.PARAMETER_PREFIX
      + ".mapper.args";
  static final String MAPPER_THREADS_KEY = Globals.PARAMETER_PREFIX
      + ".mapper.nb.threads";
  static final String FASTQ_FORMAT_KEY = Globals.PARAMETER_PREFIX
      + ".mapper.fastq.format";
  static final String ZOOKEEPER_CONNECT_STRING_KEY = Globals.PARAMETER_PREFIX
      + ".mapper.zookeeper.connect.string";
  static final String ZOOKEEPER_SESSION_TIMEOUT_KEY = Globals.PARAMETER_PREFIX
      + ".mapper.zookeeper.session.timeout";

  private static final Splitter TAB_SPLITTER = Splitter.on('\t').trimResults();

  private String counterGroup = this.getClass().getName();

  // private File archiveIndexFile;

  private Locker lock;

  private SequenceReadsMapper mapper;
  private MapperProcess process;
  private final List<String> fields = new ArrayList<>();

  /**
   * 'key': offset of the beginning of the line from the beginning of the TFQ
   * file. 'value': the TFQ line (3 fields if data are in single-end mode, 6
   * fields if data are in paired-end mode).
   */
  @Override
  protected void map(final LongWritable key, final Text value,
      final Context context) throws IOException {

    context.getCounter(this.counterGroup,
        INPUT_MAPPING_READS_COUNTER.counterName()).increment(1);

    this.fields.clear();
    for (String e : TAB_SPLITTER.split(value.toString())) {
      this.fields.add(e);
    }

    final int fieldsSize = this.fields.size();

    if (fieldsSize == 3) {

      // Single end
      this.process.writeEntry(this.fields.get(0), this.fields.get(1),
          this.fields.get(2));

    } else if (fieldsSize == 6) {

      // Pair end
      this.process.writeEntry(this.fields.get(0), this.fields.get(1),
          this.fields.get(2), this.fields.get(3), this.fields.get(4),
          this.fields.get(5));
    }

  }

  @Override
  protected void setup(final Context context) throws IOException {

    getLogger().info("Start of configure()");

    final Configuration conf = context.getConfiguration();

    // Initialize Eoulsan Settings
    if (!EoulsanRuntime.isRuntime()) {
      HadoopEoulsanRuntime.newEoulsanRuntime(conf);
    }

    // Get mapper name
    final String mapperName = conf.get(MAPPER_NAME_KEY);

    if (mapperName == null) {
      throw new IOException("No mapper set");
    }

    // Set the mapper
    this.mapper =
        SequenceReadsMapperService.getInstance().newService(mapperName);

    // Set the mapper version
    this.mapper.setMapperVersionToUse(conf.get(MAPPER_VERSION_KEY));

    // Set the mapper flavor
    this.mapper.setMapperFlavorToUse(conf.get(MAPPER_FLAVOR_KEY));

    // Get counter group
    final String counterGroup = conf.get(CommonHadoop.COUNTER_GROUP_KEY);
    if (counterGroup != null) {
      this.counterGroup = counterGroup;
    }

    final boolean pairedEnd = Boolean.parseBoolean(conf.get(PAIR_END_KEY));
    final FastqFormat fastqFormat =
        FastqFormat.getFormatFromName(conf.get(FASTQ_FORMAT_KEY, ""
            + EoulsanRuntime.getSettings().getDefaultFastqFormat()));

    // DistributedCache.purgeCache(conf);

    // Download genome reference
    final URI[] localCacheFiles = context.getCacheFiles();

    if (localCacheFiles == null || localCacheFiles.length == 0) {
      throw new IOException("Unable to retrieve genome index");
    }

    if (localCacheFiles.length > 1) {
      throw new IOException("Retrieve more than one file in distributed cache");
    }

    // Get the local genome index zip file
    getLogger().info("localCacheFiles[0]: " + localCacheFiles[0]);
    final DataFile archiveIndexFile =
        new DataFile(localCacheFiles[0].toString());

    getLogger().info(
        "Genome index compressed file (from distributed cache): "
            + archiveIndexFile);

    // Set index directory
    final File archiveIndexDir =
        new File(context.getConfiguration().get(HADOOP_TEMP_DIR)
            + "/" + getIndexLocalName(archiveIndexFile));

    getLogger().info(
        "Genome index directory where decompressed: " + archiveIndexDir);

    // Set FASTQ format
    this.mapper.setFastqFormat(fastqFormat);

    getLogger().info("Fastq format: " + fastqFormat);

    this.lock =
        new ZooKeeperLocker(conf.get(ZOOKEEPER_CONNECT_STRING_KEY),
            Integer.parseInt(conf.get(ZOOKEEPER_SESSION_TIMEOUT_KEY)),
            "/eoulsan-locks-" + InetAddress.getLocalHost().getHostName(),
            "mapper-lock-");

    // Get Mapper arguments
    final String mapperArguments = conf.get(MAPPER_ARGS_KEY);
    if (mapperArguments != null) {
      this.mapper.setMapperArguments(mapperArguments);
    }

    // Get the number of threads to use
    int mapperThreads =
        Integer.parseInt(conf.get(MAPPER_THREADS_KEY, ""
            + Runtime.getRuntime().availableProcessors()));

    if (mapperThreads > Runtime.getRuntime().availableProcessors()
        || mapperThreads < 1) {
      mapperThreads = Runtime.getRuntime().availableProcessors();
    }

    this.mapper.setThreadsNumber(mapperThreads);
    getLogger().info(
        "Use "
            + this.mapper.getMapperName() + " with " + mapperThreads
            + " threads option");

    // Create temporary directory if not exists
    final File tempDir = new File(conf.get(HADOOP_TEMP_DIR));
    if (!tempDir.exists()) {
      getLogger().fine(
          "Create temporary directory: " + tempDir.getAbsolutePath());
      if (!tempDir.mkdirs()) {
        throw new IOException(
            "Unable to create local Hadoop temporary directory: " + tempDir);
      }
    }

    // Set mapper temporary directory
    this.mapper.setTempDirectory(tempDir);

    // Init mapper
    this.mapper.init(archiveIndexFile.open(), archiveIndexDir,
        new HadoopReporter(context), this.counterGroup);

    // TODO Handle genome description
    if (pairedEnd) {
      this.process = this.mapper.mapPE(null);
    } else {
      this.process = this.mapper.mapSE(null);
    }

    getLogger().info("End of setup()");
  }

  private String getIndexLocalName(final DataFile archiveIndexFile)
      throws IOException {

    checkNotNull(archiveIndexFile, "Index Zip file is null");

    final ZipInputStream zis = new ZipInputStream(archiveIndexFile.open());

    final HashFunction hf = Hashing.md5();
    final Hasher hs = hf.newHasher();

    ZipEntry e;

    while ((e = zis.getNextEntry()) != null) {

      hs.putString(e.getName(), StandardCharsets.UTF_8);
      hs.putLong(e.getSize());
      hs.putLong(e.getTime());
      hs.putLong(e.getCrc());
    }

    zis.close();

    return this.mapper.getMapperName() + "-index-" + hs.hash().toString();
  }

  @Override
  protected void cleanup(final Context context) throws IOException,
      InterruptedException {

    getLogger().info("Start of cleanup() of the mapper.");

    context.setStatus("Wait free JVM for running "
        + this.mapper.getMapperName());
    final long waitStartTime = System.currentTimeMillis();

    ProcessUtils.waitRandom(5000);
    this.lock.lock();

    try {
      ProcessUtils.waitUntilExecutableRunning(this.mapper.getMapperName()
          .toLowerCase());

      getLogger().info(
          "Wait "
              + StringUtils.toTimeHumanReadable(System.currentTimeMillis()
                  - waitStartTime) + " before running "
              + this.mapper.getMapperName());

      // Close the data file
      this.process.closeEntriesWriter();

      context.setStatus("Run " + this.mapper.getMapperName());

      // Process to mapping
      parseSAMResults(this.process.getStout(), context);
      this.process.waitFor();

    } catch (IOException e) {

      getLogger().severe(
          "Error while running "
              + this.mapper.getMapperName() + ": " + e.getMessage());
      throw e;

    } finally {
      this.lock.unlock();
    }

    getLogger().info("End of close() of the mapper.");
  }

  private final void parseSAMResults(final InputStream resultFileInputStream,
      final Context context) throws IOException, InterruptedException {

    String line;

    final Text outKey = new Text();
    final Text outValue = new Text();

    // Parse SAM result file
    final BufferedReader readerResults =
        FileUtils.createBufferedReader(resultFileInputStream);

    final int taskId = context.getTaskAttemptID().getTaskID().getId();

    int entriesParsed = 0;

    while ((line = readerResults.readLine()) != null) {

      final String trimmedLine = line.trim();
      if ("".equals(trimmedLine)) {
        continue;
      }

      // Test if line is an header line
      final boolean headerLine = trimmedLine.charAt(0) == '@';

      // Only write header lines once (on the first output file)
      if (headerLine && taskId > 0) {
        continue;
      }

      final int tabPos = line.indexOf('\t');

      if (tabPos != -1) {

        outKey.set(line.substring(0, tabPos));
        outValue.set(line.substring(tabPos + 1));

        context.write(outKey, outValue);

        // Increment counters if not header
        if (!headerLine) {

          entriesParsed++;
          context.getCounter(this.counterGroup,
              OUTPUT_MAPPING_ALIGNMENTS_COUNTER.counterName()).increment(1);
        }
      }

    }

    readerResults.close();

    getLogger().info(
        entriesParsed
            + " entries parsed in " + this.mapper.getMapperName()
            + " output file");

    // TODO clear mapper temporary files

  }

}
