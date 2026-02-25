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

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.MappingCounters.ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.MappingCounters.OUTPUT_FILTERED_ALIGNMENTS_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop.HadoopMappingUtils.jobConfToParameters;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop.SAMHeaderHadoopUtils.createSAMSequenceDictionaryFromSAMHeader;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop.SAMHeaderHadoopUtils.loadSAMHeaders;

import com.google.common.base.Joiner;
import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.HadoopEoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.HadoopReporterIncrementer;
import fr.ens.biologie.genomique.kenetre.KenetreException;
import fr.ens.biologie.genomique.kenetre.bio.SAMComparator;
import fr.ens.biologie.genomique.kenetre.bio.alignmentfilter.MultiReadAlignmentFilter;
import fr.ens.biologie.genomique.kenetre.bio.alignmentfilter.MultiReadAlignmentFilterBuilder;
import fr.ens.biologie.genomique.kenetre.bio.alignmentfilter.ReadAlignmentFilterBuffer;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMLineParser;
import htsjdk.samtools.SAMRecord;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * This class define a reducer for alignments filtering.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public class SAMFilterReducer extends Reducer<Text, Text, Text, Text> {

  static final String GENOME_DESC_PATH_KEY =
      Globals.PARAMETER_PREFIX + ".samfilter.genome.desc.file";
  static final String MAP_FILTER_PARAMETER_KEY_PREFIX =
      Globals.PARAMETER_PREFIX + ".filter.alignments.parameter.";

  private final SAMLineParser parser = new SAMLineParser(new SAMFileHeader());
  private String counterGroup;
  private MultiReadAlignmentFilter filter;

  private final Text outKey = new Text();
  private final Text outValue = new Text();
  private final List<SAMRecord> records = new ArrayList<>();

  @Override
  protected void setup(final Context context) throws IOException, InterruptedException {

    EoulsanLogger.initConsoleHandler();
    getLogger().info("Start of setup()");

    // Get configuration object
    final Configuration conf = context.getConfiguration();

    // Initialize Eoulsan DataProtocols
    if (!EoulsanRuntime.isRuntime()) {
      HadoopEoulsanRuntime.newEoulsanRuntime(conf);
    }

    // Counter group
    this.counterGroup = conf.get(Globals.PARAMETER_PREFIX + ".counter.group");
    if (this.counterGroup == null) {
      throw new IOException("No counter group defined");
    }

    // Set the filters
    try {
      final MultiReadAlignmentFilterBuilder mrafb = new MultiReadAlignmentFilterBuilder();

      // Add the parameters from the job configuration to the builder
      mrafb.addParameters(jobConfToParameters(conf, MAP_FILTER_PARAMETER_KEY_PREFIX));

      this.filter =
          mrafb.getAlignmentFilter(new HadoopReporterIncrementer(context), this.counterGroup);
      getLogger()
          .info(
              "Read alignments filters to apply: "
                  + Joiner.on(", ").join(this.filter.getFilterNames()));

    } catch (KenetreException e) {
      throw new IOException(e);
    }

    // Write SAM header

    final List<String> samHeader = loadSAMHeaders(context);
    this.outKey.set("");
    for (String line : samHeader) {
      outValue.set(line);
      context.write(this.outKey, this.outValue);
    }

    // Set the sequences sizes in the parser
    this.parser
        .getFileHeader()
        .setSequenceDictionary(createSAMSequenceDictionaryFromSAMHeader(samHeader));

    getLogger().info("End of setup()");
  }

  /**
   * 'key': identifier of the aligned read, without the integer indicating the pair member if data
   * are in paired-end mode. 'value': alignments without the identifier part of the SAM line.
   */
  @Override
  protected void reduce(final Text key, final Iterable<Text> values, final Context context)
      throws IOException, InterruptedException {

    // Creation of a buffer object to store alignments with the same read name
    final ReadAlignmentFilterBuffer rafb = new ReadAlignmentFilterBuffer(this.filter);

    int cptRecords = 0;
    String strRecord = null;
    this.records.clear();

    for (Text val : values) {

      cptRecords++;
      strRecord = key.toString() + val.toString();
      rafb.addAlignment(this.parser.parseLine(strRecord));
    }

    this.records.addAll(rafb.getFilteredAlignments());
    context
        .getCounter(this.counterGroup, ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER.counterName())
        .increment(cptRecords - this.records.size());

    // sort alignments of the current read
    this.records.sort(new SAMComparator());

    // Writing records
    for (SAMRecord r : this.records) {

      strRecord = r.getSAMString().replaceAll("\n", "");

      // Set output key
      final int indexOfFirstTab = strRecord.indexOf("\t");
      this.outKey.set(strRecord.substring(0, indexOfFirstTab));

      // Set output value
      this.outValue.set(strRecord);

      // Write the entry
      context.write(this.outKey, this.outValue);

      // Increment the counter
      context
          .getCounter(this.counterGroup, OUTPUT_FILTERED_ALIGNMENTS_COUNTER.counterName())
          .increment(1);
    }
  }
}
