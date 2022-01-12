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
package fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getGenericLogger;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.MappingCounters.OUTPUT_MAPPING_ALIGNMENTS_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.util.StringUtils.unDoubleQuotes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.CommonHadoop;
import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.HadoopEoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.bio.FastqFormat;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.EntryMapping;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.MapperIndex;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.MapperInstance;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.MapperProcess;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.util.ProcessUtils;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.HadoopReporter;
import fr.ens.biologie.genomique.eoulsan.util.locker.DistributedLocker;
import fr.ens.biologie.genomique.eoulsan.util.locker.Locker;

/**
 * This class defines a generic mapper for reads mapping.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class ReadsMapperMapper extends Mapper<Text, Text, Text, Text> {

  // Parameter keys
  static final String MAPPER_NAME_KEY =
      Globals.PARAMETER_PREFIX + ".mapper.name";
  static final String MAPPER_VERSION_KEY =
      Globals.PARAMETER_PREFIX + ".mapper.version";
  static final String MAPPER_FLAVOR_KEY =
      Globals.PARAMETER_PREFIX + ".mapper.flavor";
  static final String PAIR_END_KEY =
      Globals.PARAMETER_PREFIX + ".mapper.pairend";
  static final String MAPPER_ARGS_KEY =
      Globals.PARAMETER_PREFIX + ".mapper.args";
  static final String MAPPER_THREADS_KEY =
      Globals.PARAMETER_PREFIX + ".mapper.nb.threads";
  static final String FASTQ_FORMAT_KEY =
      Globals.PARAMETER_PREFIX + ".mapper.fastq.format";
  static final String INDEX_CHECKSUM_KEY =
      Globals.PARAMETER_PREFIX + ".mapper.index.checksum";
  static final String ZOOKEEPER_CONNECT_STRING_KEY =
      Globals.PARAMETER_PREFIX + ".mapper.zookeeper.connect.string";
  static final String ZOOKEEPER_SESSION_TIMEOUT_KEY =
      Globals.PARAMETER_PREFIX + ".mapper.zookeeper.session.timeout";

  private static final Splitter TAB_SPLITTER = Splitter.on('\t').trimResults();
  private static final String MAPPER_INDEX_DIR_PREFIX =
      Globals.APP_NAME + "-mapper-index-";
  private static final String MAPPER_LAST_USED_FILENAME =
      Globals.APP_NAME.toUpperCase() + "_LAST_USED";
  private static final long DEFAULT_AGE_OF_UNUSED_MAPPER_INDEXES = 7;
  private static final String LOCK_SUFFIX = ".lock";

  private String counterGroup = this.getClass().getName();
  private File mapperIndexDir;

  private Locker lock;

  private EntryMapping mapping;
  private MapperProcess process;
  private Thread samResultsParserThread;
  private final BlockingDeque<String> queue = new LinkedBlockingDeque<>();
  private final ExceptionWrapper exception = new ExceptionWrapper();
  private int entriesParsed;
  private boolean writeHeaders;

  private final List<String> fields = new ArrayList<>();

  private final Text outKey = new Text();
  private final Text outValue = new Text();

  private static final class ExceptionWrapper {
    private IOException exception;
  }

  /**
   * 'key': offset of the beginning of the line from the beginning of the TFQ
   * file. 'value': the TFQ line (3 fields if data are in single-end mode, 6
   * fields if data are in paired-end mode).
   */
  @Override
  protected void map(final Text key, final Text value, final Context context)
      throws IOException, InterruptedException {

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

    writeResults(context, this.writeHeaders);
  }

  @Override
  protected void setup(final Context context) throws IOException {

    EoulsanLogger.initConsoleHandler();
    getLogger().info("Start of setup()");

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
    final fr.ens.biologie.genomique.eoulsan.bio.readsmappers.Mapper mapper =
        fr.ens.biologie.genomique.eoulsan.bio.readsmappers.Mapper
            .newMapper(mapperName, getGenericLogger());

    // Create temporary directory if not exists
    final File tempDir = EoulsanRuntime.getRuntime().getTempDirectory();
    if (!tempDir.exists()) {
      getLogger()
          .fine("Create temporary directory: " + tempDir.getAbsolutePath());
      if (!tempDir.mkdirs()) {
        throw new IOException(
            "Unable to create local Hadoop temporary directory: " + tempDir);
      }
    }

    // Set mapper temporary directory
    mapper.setTempDirectory(tempDir);

    // Set mapper executable temporary directory
    mapper.setExecutablesTempDirectory(tempDir);

    final MapperInstance mapperInstance = mapper.newMapperInstance(
        conf.get(MAPPER_VERSION_KEY), conf.get(MAPPER_FLAVOR_KEY));

    // Get counter group
    final String counterGroup = conf.get(CommonHadoop.COUNTER_GROUP_KEY);
    if (counterGroup != null) {
      this.counterGroup = counterGroup;
    }

    final boolean pairedEnd = Boolean.parseBoolean(conf.get(PAIR_END_KEY));
    final FastqFormat fastqFormat =
        FastqFormat.getFormatFromName(conf.get(FASTQ_FORMAT_KEY,
            "" + EoulsanRuntime.getSettings().getDefaultFastqFormat()));

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

    getLogger().info("Genome index compressed file (from distributed cache): "
        + archiveIndexFile);

    // Set index directory
    this.mapperIndexDir = new File(
        EoulsanRuntime.getRuntime().getTempDirectory(), MAPPER_INDEX_DIR_PREFIX
            + mapper.getName() + "-index-" + conf.get(INDEX_CHECKSUM_KEY));

    getLogger()
        .info("Genome index directory where decompressed: " + mapperIndexDir);

    // Create the MapperIndex object
    final MapperIndex mapperIndex =
        mapperInstance.newMapperIndex(archiveIndexFile.open(), mapperIndexDir);

    getLogger().info("Fastq format: " + fastqFormat);

    this.lock = new DistributedLocker(conf.get(ZOOKEEPER_CONNECT_STRING_KEY),
        Integer.parseInt(conf.get(ZOOKEEPER_SESSION_TIMEOUT_KEY)),
        "/eoulsan-locks-" + InetAddress.getLocalHost().getHostName(),
        "eoulsan-mapper-lock");

    // Get Mapper arguments
    final String mapperArguments = unDoubleQuotes(conf.get(MAPPER_ARGS_KEY));

    // Get the number of threads to use
    int mapperThreads = Integer.parseInt(conf.get(MAPPER_THREADS_KEY,
        "" + Runtime.getRuntime().availableProcessors()));

    if (mapperThreads > Runtime.getRuntime().availableProcessors()
        || mapperThreads < 1) {
      mapperThreads = Runtime.getRuntime().availableProcessors();
    }

    getLogger().info("Use "
        + mapper.getName() + " with " + mapperThreads + " threads option");

    // Update last used file timestamp for the mapper indexes clean up
    updateLastUsedMapperIndex(this.mapperIndexDir);

    context.setStatus("Wait lock");

    // Lock if mapper
    ProcessUtils.waitRandom(5000);
    this.lock.lock();

    // Initialize mapping
    this.mapping = mapperIndex.newEntryMapping(fastqFormat, mapperArguments,
        mapperThreads, true, new HadoopReporter(context), this.counterGroup);

    // Lock if no multiple instances enabled
    if (this.mapping.isMultipleInstancesEnabled()) {

      // Unlock
      this.lock.unlock();
    } else {

      context.setStatus("Wait free JVM for running " + this.mapping.getName());

      // Wait free JVM
      waitFreeJVM(context);
    }

    if (pairedEnd) {
      this.process = this.mapping.mapPE();
    } else {
      this.process = this.mapping.mapSE();
    }

    this.writeHeaders = context.getTaskAttemptID().getTaskID().getId() == 0;
    this.samResultsParserThread = startParseSAMResultsThread(this.process);

    context.setStatus("Run " + this.mapping.getName());

    getLogger().info("End of setup()");
  }

  @Override
  protected void cleanup(final Context context)
      throws IOException, InterruptedException {

    getLogger().info("Start of cleanup() of the mapper.");

    // Close the writers
    this.process.closeEntriesWriter();

    // Wait the end of the SAM parsing
    this.samResultsParserThread.join();

    this.process.waitFor();

    // Unlock if no multiple instances enabled
    if (!this.mapping.isMultipleInstancesEnabled()) {
      this.lock.unlock();
    }

    // Write headers
    writeResults(context, this.writeHeaders);

    getLogger().info(this.entriesParsed
        + " entries parsed in " + this.mapping.getName() + " output file");

    // Clear old mapper indexes
    removeUnusedMapperIndexes(context.getConfiguration());

    getLogger().info("End of close() of the mapper.");
  }

  //
  // Other mapping methods
  //

  /**
   * Wait a free JVM.
   * @param context the Hadoop context
   */
  private void waitFreeJVM(final Context context) {

    final long waitStartTime = System.currentTimeMillis();

    ProcessUtils.waitUntilExecutableRunning(
        this.mapping.getMapperInstance().getMapper().getProvider()
            .getMapperExecutableName(this.mapping.getMapperInstance()));

    getLogger().info("Wait "
        + StringUtils
            .toTimeHumanReadable(System.currentTimeMillis() - waitStartTime)
        + " before running " + this.mapping.getName());

    context.setStatus("Run " + this.mapping.getName());
  }

  /**
   * Start SAM parser result thread.
   * @param mp the mapper process
   * @return the created thread
   */
  private Thread startParseSAMResultsThread(final MapperProcess mp) {

    final Thread t = new Thread(() -> {

      // Parse SAM result file

      String line;
      try (BufferedReader readerResults =
          new BufferedReader(new InputStreamReader(mp.getStout()))) {
        while ((line = readerResults.readLine()) != null) {

          queue.add(line);
        }
      } catch (IOException e) {
        exception.exception = e;
      }
    });

    t.start();

    return t;
  }

  /**
   * Write results.
   * @param context the Hadoop context
   * @param writeHeader true if SAM header must be written
   * @throws InterruptedException if an error occurs while writing data
   * @throws IOException if an error occurs while writing data
   */
  private void writeResults(final Context context, boolean writeHeader)
      throws InterruptedException, IOException {

    while (!this.queue.isEmpty()) {

      final String line = this.queue.take().trim();

      if (line.length() == 0) {
        continue;
      }

      // Test if line is an header line
      final boolean headerLine = line.charAt(0) == '@';

      // Only write header lines once (on the first output file)
      if (headerLine && !writeHeader) {
        continue;
      }

      if (!headerLine) {

        // Set the output key as the read id
        final int tabPos = line.indexOf('\t');
        if (tabPos == -1) {
          outKey.set("");
        } else {
          outKey.set(line.substring(0, tabPos));
        }

        // Increment counters if not header
        this.entriesParsed++;
        context.getCounter(this.counterGroup,
            OUTPUT_MAPPING_ALIGNMENTS_COUNTER.counterName()).increment(1);

      } else {

        // Set empty key for headers
        this.outKey.set("");
      }

      // Set the output value
      this.outValue.set(line);

      // Write the result
      context.write(this.outKey, this.outValue);

    }

    // Throw reader exception if exists
    if (this.exception.exception != null) {
      throw this.exception.exception;
    }

  }

  //
  // Old mappers indexes cleanup methods
  //

  /**
   * Update the last usage of the current mapper index.
   * @param mapperIndexDir the mapper index directory
   */
  private void updateLastUsedMapperIndex(final File mapperIndexDir) {

    final File lockFile = new File(mapperIndexDir.getParentFile(),
        mapperIndexDir.getName() + LOCK_SUFFIX);

    try (FileOutputStream out = new FileOutputStream(lockFile)) {

      // Lock the mapper directory
      FileLock lock = out.getChannel().lock();

      final File lastMapperUsedFile =
          new File(mapperIndexDir, MAPPER_LAST_USED_FILENAME);

      if (lastMapperUsedFile.exists()) {
        if (!lastMapperUsedFile.setLastModified(System.currentTimeMillis())) {
          getLogger()
              .warning("Unable to set the modification time of the file: "
                  + lastMapperUsedFile);
        }
      }

      // Unlock the mapper directory
      lock.release();
    } catch (IOException e) {
      getLogger().warning(
          "Cannot update the timestamp of the last usage of the current mapper index: "
              + e.getMessage());
    }
  }

  /**
   * Remove unused mapper indexes.
   * @param conf Hadoop configuration
   */
  private void removeUnusedMapperIndexes(final Configuration conf) {

    final File mapperIndexesDir = this.mapperIndexDir.getParentFile();

    for (File dir : mapperIndexesDir.listFiles((dir, name) -> {

      final File f = new File(dir, name);

      return f.isDirectory() && name.startsWith(MAPPER_INDEX_DIR_PREFIX);
    })) {

      // First check without lock on the mapper index directory
      if (isMapperIndexMustBeRemoved(mapperIndexesDir)) {
        removeUnusedMapperIndex(dir, conf);
      }
    }
  }

  /**
   * Check if a mapper index directory must be removed.
   * @param mapperIndexDir the mapper index directory
   * @return true if the mapper index directory must be removed
   */
  private boolean isMapperIndexMustBeRemoved(final File mapperIndexDir) {

    final File lastModifiedFile =
        new File(mapperIndexDir, MAPPER_LAST_USED_FILENAME);

    if (!lastModifiedFile.exists())
      return false;

    final long duration =
        System.currentTimeMillis() - lastModifiedFile.lastModified();

    return duration > (DEFAULT_AGE_OF_UNUSED_MAPPER_INDEXES * 24 * 3600 * 1000);
  }

  /**
   * Remove an unused mapper index directory.
   * @param mapperIndexDir the mapper index directory to remove
   * @param conf Hadoop configuration
   */
  private void removeUnusedMapperIndex(final File mapperIndexDir,
      final Configuration conf) {

    final File lockFile = new File(mapperIndexDir.getParentFile(),
        mapperIndexDir.getName() + LOCK_SUFFIX);

    try (FileOutputStream out = new FileOutputStream(lockFile)) {

      // Lock the mapper directory
      FileLock lock = out.getChannel().lock();

      // Second check with lock on the mapper index directory
      if (isMapperIndexMustBeRemoved(mapperIndexDir)) {

        getLogger()
            .info("Remove  unused mapper index directory: " + mapperIndexDir);

        // Remove the mapper index
        // TODO use Datafile.delete(true)
        final Path mapperIndexPath = new Path(mapperIndexDir.toURI());
        final FileSystem fs = FileSystem.get(mapperIndexDir.toURI(), conf);
        fs.delete(mapperIndexPath, true);
      }

      // Unlock the mapper directory
      lock.release();
    } catch (IOException e) {
      getLogger().warning("Cannot remove unused mapper index directory ("
          + mapperIndexDir + "): " + e.getMessage());
    }
  }

}
