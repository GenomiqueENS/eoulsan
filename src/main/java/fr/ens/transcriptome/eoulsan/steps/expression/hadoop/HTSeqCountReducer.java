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

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;

/**
 * Reducer for the expression estimation with htseq-count.
 * @since 1.2
 * @author Claire Wallon
 */
public class HTSeqCountReducer extends Reducer<Text, Text, Text, Long> {

  /** Logger */
  private static final Logger LOGGER = EoulsanLogger.getLogger();

  private String counterGroup;

  @Override
  protected void setup(final Context context) throws IOException,
      InterruptedException {

    LOGGER.info("Start of setup()");

    // Counter group
    this.counterGroup =
        context.getConfiguration().get(CommonHadoop.COUNTER_GROUP_KEY);
    if (this.counterGroup == null) {
      throw new IOException("No counter group defined");
    }

    LOGGER.info("End of setup()");
  }

  /**
   * 'key': annotation identifier of the feature (gene, mRNA, exon...).
   * 'values': a list of '1', the size of this list is the number of reads found
   * on the feature.
   */
  @Override
  protected void reduce(final Text key, final Iterable<Text> values,
      final Context context) throws IOException, InterruptedException {

    int counts = 0;

    for (Text val : values) {
      counts++;
    }

    // context.write(key, new Text(String.valueOf(counts)));
    context.write(key, new Long(counts));
  }

}
