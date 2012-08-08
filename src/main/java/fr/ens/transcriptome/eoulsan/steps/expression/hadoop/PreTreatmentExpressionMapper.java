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

package fr.ens.transcriptome.eoulsan.steps.expression.hadoop;

import static com.google.common.collect.Lists.newArrayList;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import com.google.common.base.Splitter;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.HadoopEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;

/**
 * This class define a mapper for the pretreatment of paired-end data before
 * the expression estimation step.
 * @since 1.2
 * @author Claire Wallon
 */
public class PreTreatmentExpressionMapper extends Mapper<LongWritable, Text, Text, Text> {
  
  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private String counterGroup;
  
  private static final Splitter ID_SPLITTER = Splitter.on(':').trimResults();
  private List<String> idFields = newArrayList();

  private Text outKey = new Text();
  private Text outValue = new Text();

  //
  // Setup
  //

  @Override
  protected void setup(final Context context) throws IOException,
      InterruptedException {

    LOGGER.info("Start of configure()");

    // Get configuration object
    final Configuration conf = context.getConfiguration();

    // Initialize Eoulsan Settings
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

  //
  // Map
  //

  @Override
  protected void map(final LongWritable key, final Text value,
      final Context context) throws IOException, InterruptedException {
    
    final String line = value.toString();
    
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
  
}
