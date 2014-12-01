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
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.INPUT_RAW_READS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.OUTPUT_PRETREATMENT_READS_COUNTER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import com.google.common.base.Splitter;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.HadoopEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;

/**
 * This class defines a mapper for the pretreatment of paired-end data before
 * the reads filtering step.
 * @since 1.2
 * @author Claire Wallon
 */
public class PreTreatmentMapper extends Mapper<LongWritable, Text, Text, Text> {

  // Parameters keys
  static final String FASTQ_FORMAT_KEY = Globals.PARAMETER_PREFIX
      + ".pretreatment.fastq.format";

  private String counterGroup;

  private static final Splitter TAB_SPLITTER = Splitter.on('\t').trimResults();
  private List<String> fields = new ArrayList<>();

  private final ReadSequence read = new ReadSequence();

  private Text outKey = new Text();
  private Text outValue = new Text();

  //
  // Setup
  //

  @Override
  protected void setup(final Context context) throws IOException,
      InterruptedException {

    getLogger().info("Start of configure()");

    // Get configuration object
    final Configuration conf = context.getConfiguration();

    // Initialize Eoulsan Settings
    if (!EoulsanRuntime.isRuntime()) {
      HadoopEoulsanRuntime.newEoulsanRuntime(conf);
    }

    // Set the FastqFormat
    final FastqFormat fastqFormat =
        FastqFormat.getFormatFromName(conf.get(FASTQ_FORMAT_KEY, ""
            + EoulsanRuntime.getSettings().getDefaultFastqFormat()));
    this.read.setFastqFormat(fastqFormat);

    getLogger().info("Fastq format: " + fastqFormat);

    // Counter group
    this.counterGroup = conf.get(CommonHadoop.COUNTER_GROUP_KEY);
    if (this.counterGroup == null) {
      throw new IOException("No counter group defined");
    }

    getLogger().info("End of setup()");
  }

  //
  // Map
  //

  /**
   * 'key': offset of the beginning of the line from the beginning of the TFQ
   * file. 'value': the TFQ record.
   */
  @Override
  protected void map(final LongWritable key, final Text value,
      final Context context) throws IOException, InterruptedException {

    context
        .getCounter(this.counterGroup, INPUT_RAW_READS_COUNTER.counterName())
        .increment(1);

    final String line = value.toString();
    fields.clear();
    for (String e : TAB_SPLITTER.split(line)) {
      fields.add(e);
    }

    this.read.setName(fields.get(0));
    this.read.setSequence(fields.get(1));
    this.read.setQuality(fields.get(2));

    // Illumina technology and Casava 1.8 format for the '@' line
    if (!fields.get(0).contains("/")) {
      this.outKey = new Text(this.read.getName().split(" ")[0]);
      this.outValue =
          new Text(this.read.getName().split(" ")[1]
              + "\t" + this.read.getSequence() + "\t" + this.read.getQuality());
    }
    // Before Casava 1.8 or technology other than Illumina
    else {
      this.outKey = new Text(this.read.getName().split("/")[0] + "/");
      this.outValue =
          new Text(this.read.getName().split("/")[1]
              + "\t" + this.read.getSequence() + "\t" + this.read.getQuality());
    }

    context.write(this.outKey, this.outValue);
    context.getCounter(this.counterGroup,
        OUTPUT_PRETREATMENT_READS_COUNTER.counterName()).increment(1);

  }

  @Override
  protected void cleanup(Context context) throws IOException,
      InterruptedException {
  }

}
