package fr.ens.biologie.genomique.eoulsan.modules.expression.hadoop;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.modules.expression.ExpressionCounterCounter.INVALID_SAM_ENTRIES_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.modules.expression.hadoop.ExpressionHadoopModule.SAM_RECORD_PAIRED_END_SERPARATOR;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import fr.ens.biologie.genomique.eoulsan.CommonHadoop;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.kenetre.KenetreException;
import fr.ens.biologie.genomique.kenetre.bio.expressioncounter.ExpressionCounter;
import fr.ens.biologie.genomique.kenetre.util.ReporterIncrementer;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.HadoopReporterIncrementer;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFormatException;
import htsjdk.samtools.SAMLineParser;
import htsjdk.samtools.SAMRecord;

/**
 * Mapper for the expression estimation with a SAM output.
 * @since 2.3
 * @author Laurent Jourdren
 */
public class ExpressionSAMOutputMapper extends Mapper<Text, Text, Text, Text> {

  private ExpressionCounter counter;
  private String counterGroup;

  private final SAMLineParser parser = new SAMLineParser(new SAMFileHeader());
  private final Pattern recordSplitterPattern =
      Pattern.compile("" + SAM_RECORD_PAIRED_END_SERPARATOR);

  private final List<SAMRecord> samRecords = new ArrayList<>();
  private ReporterIncrementer reporter;
  private final Text outKey = new Text("");
  private final Text outValue = new Text();

  @Override
  public void setup(final Context context)
      throws IOException, InterruptedException {

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
    this.counter = ExpressionMapper.initCounterAndParser(conf, this.parser,
        localCacheFiles);

    getLogger().info("End of setup()");
  }

  /**
   * 'key': offset of the beginning of the line from the beginning of the
   * alignment file. 'value': the SAM record, if data are in paired-end mode,
   * 'value' contains the two paired alignments separated by a '£' (TSAM
   * format).
   */
  @Override
  public void map(final Text key, final Text value, final Context context)
      throws IOException, InterruptedException {

    final String line = value.toString();

    // Write SAM headers
    if (!line.isEmpty() && line.charAt(0) == '@') {
      this.outKey.set("");
      this.outValue.set(line);
      context.write(this.outKey, this.outValue);
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
            "Invalid number of SAM record(s) found in the entry: "
                + samRecords.size());
      }

      // Count
      this.counter.count(this.samRecords, this.reporter, this.counterGroup);

      // Write the results
      for (SAMRecord samRecord : this.samRecords) {

        // Get the SAM string
        final String samString = samRecord.getSAMString().replaceAll("\n", "");

        // Set the output key as the read id
        final int tabPos = samString.indexOf('\t');
        if (tabPos == -1) {
          outKey.set("");
        } else {
          outKey.set(samString.substring(0, tabPos));
        }

        this.outValue.set(samString);
        context.write(this.outKey, this.outValue);
      }

    } catch (SAMFormatException | KenetreException | EoulsanException e) {

      context.getCounter(this.counterGroup,
          INVALID_SAM_ENTRIES_COUNTER.counterName()).increment(1);

      getLogger().info("Invalid SAM output entry: "
          + e.getMessage() + " line='" + line + "'");
    }

  }

  @Override
  public void cleanup(final Context context) throws IOException {
  }

}
