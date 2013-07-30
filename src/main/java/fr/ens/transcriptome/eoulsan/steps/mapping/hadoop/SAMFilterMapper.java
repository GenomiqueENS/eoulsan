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
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.INPUT_ALIGNMENTS_COUNTER;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.HadoopEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.util.hadoop.PathUtils;

/**
 * This class defines a mapper for alignment filtering.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class SAMFilterMapper extends Mapper<LongWritable, Text, Text, Text> {

  /** Logger */
  private static final Logger LOGGER = EoulsanLogger.getLogger();

  // Parameters keys
  static final String MAPPING_QUALITY_THRESOLD_KEY = Globals.PARAMETER_PREFIX
      + ".samfilter.mapping.quality.threshold";
  static final String GENOME_DESC_PATH_KEY = Globals.PARAMETER_PREFIX
      + ".samfilter.genome.desc.file";

  static final String SAM_HEADER_FILE_PREFIX = "_samheader_";

  private static final Splitter ID_SPLITTER = Splitter.on(':').trimResults();
  private List<String> idFields = newArrayList();

  private String counterGroup;

  private final Text outKey = new Text();
  private final Text outValue = new Text();

  @Override
  protected void setup(final Context context) throws IOException,
      InterruptedException {

    LOGGER.info("Start of configure()");

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

    LOGGER.info("End of setup()");
  }

  /**
   * 'key': offset of the beginning of the line from the beginning of the SAM
   * file if data are in single-end mode or in TSAM file if data are in
   * paired-end mode. 'value': the SAM or TSAM line.
   */
  @Override
  protected void map(final LongWritable key, final Text value,
      final Context context) throws IOException, InterruptedException {

    final String line = value.toString();

    // Avoid empty and header lines
    if (!isValidLineAndSaveSAMHeader(line, context))
      return;

    context.getCounter(this.counterGroup,
        INPUT_ALIGNMENTS_COUNTER.counterName()).increment(1);

    final int indexOfFirstTab = line.indexOf("\t");
    String completeId = line.substring(0, indexOfFirstTab);
    int endReadId;

    idFields.clear();
    for (String e : ID_SPLITTER.split(completeId)) {
      idFields.add(e);
    }

    // Read identifiant format : before Casava 1.8 or other technologies that
    // Illumina
    if (idFields.size() < 7) {
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

    // Read identifiant format : Illumina - Casava 1.8
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
  protected void cleanup(Context context) throws IOException,
      InterruptedException {
  }

  private List<String> headers;

  private final boolean isValidLineAndSaveSAMHeader(final String line,
      final Context context) throws IOException {

    // Test empty line
    if (line.length() == 0)
      return false;

    if (line.charAt(0) != '@') {

      // If headers previously found write it in a file
      if (this.headers != null) {

        // Save headers

        // TODO change for Hadoop 2.0
        final Path outputPath =
            new Path(context.getConfiguration().get("mapred.output.dir"));

        final Path headerPath =
            new Path(outputPath, SAM_HEADER_FILE_PREFIX
                + context.getTaskAttemptID().toString());
        final Writer writer =
            new OutputStreamWriter(PathUtils.createOutputStream(headerPath,
                context.getConfiguration()));

        for (String l : this.headers)
          writer.write(l + "\n");

        writer.close();

        this.headers = null;
      }

      // The line is an alignment
      return true;
    }

    if (this.headers == null)
      this.headers = Lists.newArrayList();

    this.headers.add(line);

    // The line is an header
    return false;
  }
}