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
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop.SAMHeaderHadoopUtils.createSAMSequenceDictionaryFromSAMHeader;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop.SAMHeaderHadoopUtils.loadSAMHeaders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.kenetre.bio.SAMComparator;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFormatException;
import htsjdk.samtools.SAMLineParser;
import htsjdk.samtools.SAMRecord;

/**
 * This class define a reducer for the pretreatment of paired-end data before
 * the expression estimation step.
 * @since 1.2
 * @author Claire Wallon
 */
public class PreTreatmentExpressionReducer
    extends Reducer<Text, Text, Text, Text> {

  private String counterGroup;
  private final Text outKey = new Text();
  private final Text outValue = new Text();

  private final SAMLineParser parser = new SAMLineParser(new SAMFileHeader());
  private final List<SAMRecord> records = new ArrayList<>();

  @Override
  protected void setup(final Context context)
      throws IOException, InterruptedException {

    EoulsanLogger.initConsoleHandler();
    getLogger().info("Start of setup()");

    final Configuration conf = context.getConfiguration();

    // Set the chromosomes sizes in the parser
    final List<String> samHeader = loadSAMHeaders(context);
    this.parser.getFileHeader().setSequenceDictionary(
        createSAMSequenceDictionaryFromSAMHeader(samHeader));

    // Counter group
    this.counterGroup = conf.get(Globals.PARAMETER_PREFIX + ".counter.group");
    if (this.counterGroup == null) {
      throw new IOException("No counter group defined");
    }

    getLogger().info("End of setup()");
  }

  /**
   * 'key': the identifier of the aligned read without the integer indicating
   * the member of the pair. 'values': the rest of the paired alignments, i.e
   * the SAM line of the first paired alignment and the SAM line of the second
   * paired alignment.
   */
  @Override
  protected void reduce(final Text key, final Iterable<Text> values,
      final Context context) throws IOException, InterruptedException {

    String stringVal;
    final String strOutKey;
    StringBuilder strOutValue = new StringBuilder();
    SAMRecord samRecord;
    String stringRecord;

    this.records.clear();

    for (Text val : values) {

      stringVal = val.toString();
      stringRecord = key.toString() + stringVal;

      try {
        samRecord = this.parser.parseLine(stringRecord);
        this.records.add(samRecord);

      } catch (SAMFormatException e) {
        context.getCounter(this.counterGroup,
            INVALID_SAM_ENTRIES_COUNTER.counterName()).increment(1);
        getLogger().info("Invalid SAM output entry: "
            + e.getMessage() + " line='" + stringRecord + "'");
        return;
      }

    }

    // sort alignments of the current read
    this.records.sort(new SAMComparator());

    // Writing records
    int indexOfFirstTab = this.records.get(0).getSAMString().indexOf("\t");
    strOutKey =
        this.records.get(0).getSAMString().substring(0, indexOfFirstTab);
    strOutValue.append(this.records.get(0).getSAMString()
        .substring(indexOfFirstTab + 1).replaceAll("\n", ""));

    this.records.remove(0);

    for (SAMRecord r : this.records) {
      if (r.getFirstOfPairFlag()) {
        strOutValue.append('\n');
      } else {
        strOutValue.append(SAM_RECORD_PAIRED_END_SERPARATOR);
      }
      strOutValue.append(r.getSAMString().replaceAll("\n", ""));
    }

    this.outKey.set(strOutKey);
    this.outValue.set(strOutValue.toString());
    context.write(this.outKey, this.outValue);
  }
}
