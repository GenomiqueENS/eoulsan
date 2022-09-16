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
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.MappingCounters.INPUT_RAW_READS_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.MappingCounters.OUTPUT_FILTERED_READS_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.MappingCounters.READS_REJECTED_BY_FILTERS_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop.HadoopMappingUtils.jobConfToParameters;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop.ReadsFilterHadoopModule.OUTPUT_FILE1_KEY;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop.ReadsFilterHadoopModule.OUTPUT_FILE2_KEY;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.CommonHadoop;
import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.HadoopEoulsanRuntime;
import fr.ens.biologie.genomique.kenetre.bio.FastqFormat;
import fr.ens.biologie.genomique.kenetre.bio.ReadSequence;
import fr.ens.biologie.genomique.kenetre.KenetreException;
import fr.ens.biologie.genomique.kenetre.bio.readfilter.MultiReadFilter;
import fr.ens.biologie.genomique.kenetre.bio.readfilter.MultiReadFilterBuilder;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.HadoopReporterIncrementer;

/**
 * This class defines a read filter mapper.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class ReadsFilterMapper extends Mapper<Text, Text, Text, Text> {

  // Parameters keys
  static final String FASTQ_FORMAT_KEY =
      Globals.PARAMETER_PREFIX + ".filter.reads.fastq.format";

  static final String READ_FILTER_PARAMETER_KEY_PREFIX =
      Globals.PARAMETER_PREFIX + ".filter.reads.parameter.";

  private static final Splitter TAB_SPLITTER = Splitter.on('\t').trimResults();
  private final List<String> fields = new ArrayList<>();
  private MultiReadFilter filter;
  private String counterGroup;

  private final ReadSequence read1 = new ReadSequence();
  private final ReadSequence read2 = new ReadSequence();

  private final Text outValue = new Text();

  private MultipleOutputs<Text, Text> out;
  private String outputFilename1;
  private String outputFilename2;

  //
  // Setup
  //

  @Override
  protected void setup(final Context context)
      throws IOException, InterruptedException {

    EoulsanLogger.initConsoleHandler();
    getLogger().info("Start of setup()");

    // Get configuration object
    final Configuration conf = context.getConfiguration();

    // Initialize Eoulsan Settings
    if (!EoulsanRuntime.isRuntime()) {
      HadoopEoulsanRuntime.newEoulsanRuntime(conf);
    }

    // Set the FastqFormat
    final FastqFormat fastqFormat =
        FastqFormat.getFormatFromName(conf.get(FASTQ_FORMAT_KEY,
            "" + EoulsanRuntime.getSettings().getDefaultFastqFormat()));
    this.read1.setFastqFormat(fastqFormat);
    this.read2.setFastqFormat(fastqFormat);

    // Counter group
    this.counterGroup = conf.get(CommonHadoop.COUNTER_GROUP_KEY);
    if (this.counterGroup == null) {
      throw new IOException("No counter group defined");
    }

    getLogger().info("Fastq format: " + fastqFormat);

    // Set the filters
    try {
      final MultiReadFilterBuilder mrfb = new MultiReadFilterBuilder();

      // Add the parameters from the job configuration to the builder
      mrfb.addParameters(
          jobConfToParameters(conf, READ_FILTER_PARAMETER_KEY_PREFIX));

      this.filter = mrfb.getReadFilter(new HadoopReporterIncrementer(context),
          this.counterGroup);

      getLogger().info("Reads filters to apply: "
          + Joiner.on(", ").join(this.filter.getFilterNames()));

    } catch (KenetreException e) {
      throw new IOException(e);
    }

    // Set the output writers
    this.out = new MultipleOutputs<>(context);
    this.outputFilename1 = createOutputPath(conf, OUTPUT_FILE1_KEY);
    this.outputFilename2 = createOutputPath(conf, OUTPUT_FILE2_KEY);

    getLogger().info("End of setup()");
  }

  private static String createOutputPath(final Configuration conf,
      final String key) {

    if (conf == null || key == null) {
      return null;
    }

    final String value = conf.get(key);

    return value + "/part";
  }

  //
  // Map
  //

  /**
   * 'key': offset of the beginning of the line from the beginning of the TFQ
   * file. 'value': the TFQ line.
   */
  @Override
  protected void map(final Text key, final Text value, final Context context)
      throws IOException, InterruptedException {

    context.getCounter(this.counterGroup, INPUT_RAW_READS_COUNTER.counterName())
        .increment(1);

    final String line = value.toString();

    this.fields.clear();
    for (String e : TAB_SPLITTER.split(line)) {
      this.fields.add(e);
    }

    final int fieldsSize = this.fields.size();

    if (fieldsSize == 3) {

      // Single end
      this.read1.setName(this.fields.get(0));
      this.read1.setSequence(this.fields.get(1));
      this.read1.setQuality(this.fields.get(2));

      if (this.filter.accept(this.read1)) {

        this.outValue.set(this.read1.toTFQ());

        context.write(key, this.outValue);
        context.getCounter(this.counterGroup,
            OUTPUT_FILTERED_READS_COUNTER.counterName()).increment(1);
      } else {
        context.getCounter(this.counterGroup,
            READS_REJECTED_BY_FILTERS_COUNTER.counterName()).increment(1);
      }

    } else if (fieldsSize == 6) {

      // First end
      this.read1.setName(this.fields.get(0));
      this.read1.setSequence(this.fields.get(1));
      this.read1.setQuality(this.fields.get(2));

      // Second end
      this.read2.setName(this.fields.get(3));
      this.read2.setSequence(this.fields.get(4));
      this.read2.setQuality(this.fields.get(5));

      if (this.filter.accept(this.read1, this.read2)) {

        if (this.outputFilename1 == null) {

          // Output of the mapper is chained
          this.outValue.set(this.read1.toTFQ() + '\t' + this.read2.toTFQ());
          context.write(key, this.outValue);
        } else {

          // The output of the mapper is not reused by another mapper or reducer

          // Write read 1
          this.outValue.set(this.read1.toTFQ());
          this.out.write(key, this.outValue, this.outputFilename1);

          // Write read 2
          this.outValue.set(this.read2.toTFQ());
          this.out.write(key, this.outValue, this.outputFilename2);
        }

        context.getCounter(this.counterGroup,
            OUTPUT_FILTERED_READS_COUNTER.counterName()).increment(1);
      } else {
        context.getCounter(this.counterGroup,
            READS_REJECTED_BY_FILTERS_COUNTER.counterName()).increment(1);
      }
    }

  }

  @Override
  protected void cleanup(final Context context)
      throws IOException, InterruptedException {

    // Close the multiple output writer
    if (this.out != null) {
      this.out.close();
    }
  }

}
