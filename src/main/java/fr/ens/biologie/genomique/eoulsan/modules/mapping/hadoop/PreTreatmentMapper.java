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
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.MappingCounters.OUTPUT_PRETREATMENT_READS_COUNTER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.CommonHadoop;
import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.HadoopEoulsanRuntime;
import fr.ens.biologie.genomique.kenetre.bio.FastqFormat;
import fr.ens.biologie.genomique.kenetre.bio.ReadSequence;

/**
 * This class defines a mapper for the pretreatment of paired-end data before
 * the reads filtering step.
 * @since 1.2
 * @author Claire Wallon
 */
public class PreTreatmentMapper extends Mapper<LongWritable, Text, Text, Text> {

  // Parameters keys
  static final String FASTQ_FORMAT_KEY =
      Globals.PARAMETER_PREFIX + ".pretreatment.fastq.format";

  private String counterGroup;

  private static final Splitter TAB_SPLITTER = Splitter.on('\t').trimResults();
  private final List<String> fields = new ArrayList<>();

  private final ReadSequence read = new ReadSequence();

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

    context.getCounter(this.counterGroup, INPUT_RAW_READS_COUNTER.counterName())
        .increment(1);

    final String line = value.toString();
    this.fields.clear();
    for (String e : TAB_SPLITTER.split(line)) {
      this.fields.add(e);
    }

    this.read.setName(this.fields.get(0));
    this.read.setSequence(this.fields.get(1));
    this.read.setQuality(this.fields.get(2));

    final Text outKey;
    final Text outValue;

    // Illumina technology and Casava 1.8 format for the '@' line
    if (!this.fields.get(0).contains("/")) {
      outKey = new Text(this.read.getName().split(" ")[0]);
      outValue = new Text(this.read.getName().split(" ")[1]
          + "\t" + this.read.getSequence() + "\t" + this.read.getQuality());
    }
    // Before Casava 1.8 or technology other than Illumina
    else {
      outKey = new Text(this.read.getName().split("/")[0] + "/");
      outValue = new Text(this.read.getName().split("/")[1]
          + "\t" + this.read.getSequence() + "\t" + this.read.getQuality());
    }

    context.write(outKey, outValue);
    context.getCounter(this.counterGroup,
        OUTPUT_PRETREATMENT_READS_COUNTER.counterName()).increment(1);

  }

  @Override
  protected void cleanup(final Context context)
      throws IOException, InterruptedException {
  }

}
