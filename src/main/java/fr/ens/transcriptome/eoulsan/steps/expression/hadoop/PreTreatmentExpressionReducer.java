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

import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.INVALID_SAM_ENTRIES_COUNTER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import net.sf.samtools.SAMComparator;
import net.sf.samtools.SAMFormatException;
import net.sf.samtools.SAMParser;
import net.sf.samtools.SAMRecord;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.util.hadoop.PathUtils;

/**
 * This class define a reducer for the pretreatment of paired-end data before
 * the expression estimation step.
 * @since 1.2
 * @author Claire Wallon
 */
public class PreTreatmentExpressionReducer extends
    Reducer<Text, Text, Text, Text> {

  /** Logger */
  private static Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  // Parameters keys
  // static final String GENOME_DESC_PATH_KEY = Globals.PARAMETER_PREFIX
  // + ".pretreatment.expression.genome.desc.file";

  private String counterGroup;
  private Text outKey = new Text();
  private Text outValue = new Text();

  private final SAMParser parser = new SAMParser();
  private List<SAMRecord> records = new ArrayList<SAMRecord>();

  @Override
  protected void setup(final Context context) throws IOException,
      InterruptedException {

    final Configuration conf = context.getConfiguration();

    // Get the genome description filename
    // System.err
    // .println("genome desc : " + ExpressionMapper.GENOME_DESC_PATH_KEY);
    final String genomeDescFile =
        conf.get(ExpressionMapper.GENOME_DESC_PATH_KEY);

    if (genomeDescFile == null) {
      throw new IOException("No genome desc file set");
    }

    // Load genome description object
    final GenomeDescription genomeDescription =
        GenomeDescription.load(PathUtils.createInputStream(new Path(
            genomeDescFile), conf));

    // Set the chromosomes sizes in the parser
    this.parser.setGenomeDescription(genomeDescription);

    // Counter group
    this.counterGroup = conf.get(Globals.PARAMETER_PREFIX + ".counter.group");
    if (this.counterGroup == null) {
      throw new IOException("No counter group defined");
    }

  }

  @Override
  protected void reduce(final Text key, final Iterable<Text> values,
      final Context context) throws IOException, InterruptedException {

    String stringVal;
    String strOutKey = "";
    String strOutValue = "";
    int firstIndexOfTab;
    SAMRecord samRecord;
    String stringRecord;

    records.clear();

    for (Text val : values) {

      stringVal = val.toString();
      stringRecord = key.toString() + stringVal;

      try {
        samRecord = this.parser.parseLine(stringRecord);
        records.add(samRecord);

        // //////////////////////////////////////////////////////////
        // the current alignment is the first one for the read (in unique or
        // multiple alignments)
        // if (strOutKey == "") {
        // firstIndexOfTab = stringVal.indexOf("\t");
        // strOutKey += key.toString() + stringVal.substring(0,
        // firstIndexOfTab);
        // }

        // the current alignment is the second one for the read or the read has
        // multiple alignments
        // else {
        // //
        // if (strOutValue == "") {
        //
        // }
        // }

        // collection des alignements puis Collection.sort() puis écriture

        // //////////////////////////////////////////////////////////
        // the current alignment is the first one for the read
        // if (samRecord.getFirstOfPairFlag()) {
        // firstIndexOfTab = stringVal.indexOf("\t");
        // strOutKey += key.toString() + stringVal.substring(0,
        // firstIndexOfTab);
        //
        // // unique alignment or first one in a multiple alignment, and the
        // // second alignment of the read has not already been read
        // if (strOutValue == "") {
        // strOutValue += stringVal.substring(firstIndexOfTab + 1);
        // strOutValue += "£";
        // }
        // // the second alignment of the read has already been read
        // else {
        // strOutValue =
        // stringVal.substring(firstIndexOfTab + 1) + "£" + strOutValue;
        // }
        // }
        //
        // // the current alignment is the second one for the read
        // else {
        // strOutValue += stringVal;
        // }

      } catch (SAMFormatException e) {
        context.getCounter(this.counterGroup,
            INVALID_SAM_ENTRIES_COUNTER.counterName()).increment(1);
        LOGGER.info("Invalid SAM output entry: "
            + e.getMessage() + " line='" + stringRecord + "'");
        return;
      }

    }

    Collections.sort(records, new SAMComparator());

    int indexOfFirstTab = records.get(0).getSAMString().indexOf("\t");
    strOutKey = records.get(0).getSAMString().substring(0, indexOfFirstTab);
    strOutValue =
        records.get(0).getSAMString().substring(indexOfFirstTab + 1)
            .replaceAll("\n", "");

    records.remove(0);
    for (SAMRecord r : records) {
      if (r.getFirstOfPairFlag()) {
        strOutValue += "\n" + r.getSAMString().replaceAll("\n", "");
      } else {
        strOutValue += "£" + r.getSAMString().replaceAll("\n", "");
      }
    }

    this.outKey.set(strOutKey);
    this.outValue.set(strOutValue);
    context.write(this.outKey, this.outValue);
  }
}
