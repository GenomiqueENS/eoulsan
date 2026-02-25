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
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.MappingCounters.INPUT_ALIGNMENTS_COUNTER;

import com.google.common.base.Splitter;
import fr.ens.biologie.genomique.eoulsan.CommonHadoop;
import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.HadoopEoulsanRuntime;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * This class defines a mapper for alignment filtering.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public class SAMFilterMapper extends Mapper<Text, Text, Text, Text> {

  // Parameters keys
  static final String MAPPING_QUALITY_THRESOLD_KEY =
      Globals.PARAMETER_PREFIX + ".samfilter.mapping.quality.threshold";

  private static final Splitter ID_SPLITTER = Splitter.on(':').trimResults();
  private final List<String> idFields = new ArrayList<>();

  private String counterGroup;
  private SAMHeaderHadoopUtils.SAMHeaderWriter samHeaderWriter;

  private final Text outKey = new Text();
  private final Text outValue = new Text();

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
    this.counterGroup = conf.get(CommonHadoop.COUNTER_GROUP_KEY);
    if (this.counterGroup == null) {
      throw new IOException("No counter group defined");
    }

    // SAM header writer
    this.samHeaderWriter =
        new SAMHeaderHadoopUtils.SAMHeaderWriter(context.getTaskAttemptID().toString());

    getLogger().info("End of setup()");
  }

  /**
   * 'key': offset of the beginning of the line from the beginning of the SAM file if data are in
   * single-end mode or in TSAM file if data are in paired-end mode. 'value': the SAM or TSAM line.
   */
  @Override
  protected void map(final Text key, final Text value, final Context context)
      throws IOException, InterruptedException {

    final String line = value.toString();

    // Avoid empty and header lines
    if (this.samHeaderWriter.writeIfHeaderLine(context, line)) {
      return;
    }

    context.getCounter(this.counterGroup, INPUT_ALIGNMENTS_COUNTER.counterName()).increment(1);

    final int indexOfFirstTab = line.indexOf("\t");
    String completeId = line.substring(0, indexOfFirstTab);
    int endReadId;

    this.idFields.clear();
    for (String e : ID_SPLITTER.split(completeId)) {
      this.idFields.add(e);
    }

    // Read identifier format : before Casava 1.8 or other technologies that
    // Illumina
    if (this.idFields.size() < 7) {
      endReadId = completeId.indexOf('/');
      // single-end mode
      if (endReadId == -1) {
        this.outKey.set(completeId);
        this.outValue.set(line.substring(indexOfFirstTab));
      }
      // paired-end mode
      else {
        this.outKey.set(line.substring(0, endReadId + 1));
        this.outValue.set(line.substring(endReadId + 1));
      }
    }

    // Read identifier format : Illumina - Casava 1.8
    else {
      endReadId = completeId.indexOf(' ');
      // mapped read
      if (endReadId == -1) {
        this.outKey.set(completeId);
        this.outValue.set(line.substring(indexOfFirstTab));
      }
      // unmapped read
      else {
        this.outKey.set(line.substring(0, endReadId));
        this.outValue.set(line.substring(endReadId));
      }
    }

    context.write(this.outKey, this.outValue);
  }

  @Override
  protected void cleanup(final Context context) throws IOException, InterruptedException {

    // Write SAM header if there is no SAM entries
    this.samHeaderWriter.close(context);
  }
}
