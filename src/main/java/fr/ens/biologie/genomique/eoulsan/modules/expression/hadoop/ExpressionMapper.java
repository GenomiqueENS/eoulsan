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

package fr.ens.biologie.genomique.eoulsan.modules.expression.hadoop;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.modules.expression.ExpressionCounterCounter.INVALID_SAM_ENTRIES_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.modules.expression.hadoop.ExpressionHadoopModule.SAM_RECORD_PAIRED_END_SERPARATOR;

import com.google.common.base.Splitter;
import fr.ens.biologie.genomique.eoulsan.CommonHadoop;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.HadoopReporterIncrementer;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.PathUtils;
import fr.ens.biologie.genomique.kenetre.KenetreException;
import fr.ens.biologie.genomique.kenetre.bio.GenomeDescription;
import fr.ens.biologie.genomique.kenetre.bio.SAMUtils;
import fr.ens.biologie.genomique.kenetre.bio.expressioncounter.ExpressionCounter;
import fr.ens.biologie.genomique.kenetre.util.ReporterIncrementer;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFormatException;
import htsjdk.samtools.SAMLineParser;
import htsjdk.samtools.SAMRecord;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * Mapper for the expression estimation.
 *
 * @since 1.2
 * @author Claire Wallon
 */
public class ExpressionMapper extends Mapper<Text, Text, Text, LongWritable> {

  private ExpressionCounter counter;
  private String counterGroup;

  private final SAMLineParser parser = new SAMLineParser(new SAMFileHeader());
  private final Splitter recordSplitterPattern = Splitter.on(SAM_RECORD_PAIRED_END_SERPARATOR);

  private final List<SAMRecord> samRecords = new ArrayList<>();
  private ReporterIncrementer reporter;
  private final Text outKey = new Text();
  private final LongWritable outValue = new LongWritable(1L);

  @Override
  public void setup(final Context context) throws IOException, InterruptedException {

    EoulsanLogger.initConsoleHandler();
    getLogger().info("Start of setup()");

    final Configuration conf = context.getConfiguration();

    // Counter group
    this.counterGroup = conf.get(CommonHadoop.COUNTER_GROUP_KEY);
    if (this.counterGroup == null) {
      throw new IOException("No counter group defined");
    }

    // Define the reporter
    this.reporter = new HadoopReporterIncrementer(context);

    // Get the cache files
    final URI[] localCacheFiles = context.getCacheFiles();

    // Initialize counter and parser
    this.counter = initCounterAndParser(conf, this.parser, localCacheFiles);

    getLogger().info("End of setup()");
  }

  static ExpressionCounter initCounterAndParser(
      final Configuration conf, final SAMLineParser parser, final URI[] localCacheFiles)
      throws IOException {

    try {

      if (localCacheFiles == null || localCacheFiles.length == 0) {
        throw new IOException("Unable to retrieve genome index");
      }

      if (localCacheFiles.length > 1) {
        throw new IOException("Retrieve more than one file in distributed cache");
      }

      getLogger()
          .info("Genome index compressed file (from distributed cache): " + localCacheFiles[0]);

      if (localCacheFiles == null || localCacheFiles.length == 0) {
        throw new IOException("Unable to retrieve annotation index");
      }

      if (localCacheFiles.length > 1) {
        throw new IOException("Retrieve more than one file in distributed cache");
      }

      // Deserialize counter
      ExpressionCounter counter =
          loadSerializedCounter(PathUtils.createInputStream(new Path(localCacheFiles[0]), conf));

      // Get the genome description filename
      final String genomeDescFile = conf.get(ExpressionHadoopModule.GENOME_DESC_PATH_KEY);

      if (genomeDescFile == null) {
        throw new IOException("No genome desc file set");
      }

      // Load genome description object
      final GenomeDescription genomeDescription =
          GenomeDescription.load(PathUtils.createInputStream(new Path(genomeDescFile), conf));

      // Set the chromosomes sizes in the parser
      parser
          .getFileHeader()
          .setSequenceDictionary(SAMUtils.newSAMSequenceDictionary(genomeDescription));

      return counter;

    } catch (IOException e) {
      getLogger().severe("Error while loading annotation data in Mapper: " + e.getMessage());
      throw new IOException(e);
    }
  }

  /**
   * 'key': offset of the beginning of the line from the beginning of the alignment file. 'value':
   * the SAM record, if data are in paired-end mode, 'value' contains the two paired alignments
   * separated by a '£' (TSAM format).
   */
  @Override
  public void map(final Text key, final Text value, final Context context)
      throws IOException, InterruptedException {

    final String line = value.toString();

    // Discard SAM headers
    if (line.length() > 0 && line.charAt(0) == '@') {
      return;
    }

    // Clean samRecords
    this.samRecords.clear();

    try {

      // Split the entry (handle paired-end reads)
      for (String field : this.recordSplitterPattern.split(line)) {
        this.samRecords.add(this.parser.parseLine(field));
      }

      // Check if there is only one or two entries in the line
      if (samRecords.isEmpty() || samRecords.size() > 2) {
        throw new EoulsanException(
            "Invalid number of SAM record(s) found in the entry: " + samRecords.size());
      }

      // Count
      final Map<String, Integer> counts =
          this.counter.count(samRecords, this.reporter, this.counterGroup);

      // Write the results
      for (Map.Entry<String, Integer> e : counts.entrySet()) {
        this.outKey.set(e.getKey());
        this.outValue.set(e.getValue());
        context.write(this.outKey, this.outValue);
      }

    } catch (SAMFormatException | KenetreException | EoulsanException e) {

      context.getCounter(this.counterGroup, INVALID_SAM_ENTRIES_COUNTER.counterName()).increment(1);

      getLogger().info("Invalid SAM output entry: " + e.getMessage() + " line='" + line + "'");
    }
  }

  @Override
  public void cleanup(final Context context) throws IOException {}

  //
  // Other methods
  //

  private static ExpressionCounter loadSerializedCounter(final InputStream in) throws IOException {

    if (in == null) {
      throw new NullPointerException("is argument cannot be null");
    }

    try (ObjectInputStream ois = new ObjectInputStream(in)) {
      return (ExpressionCounter) ois.readObject();

    } catch (ClassNotFoundException e) {
      throw new IOException("Unable to load data.");
    }
  }
}
