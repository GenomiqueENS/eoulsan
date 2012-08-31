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

import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.OUTPUT_FILTERED_ALIGNMENTS_COUNTER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.sf.samtools.SAMComparator;
import net.sf.samtools.SAMParser;
import net.sf.samtools.SAMRecord;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.HadoopEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.MultiReadAlignmentsFilterBuilder;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilter;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilterBuffer;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.util.hadoop.HadoopReporter;

/**
 * This class define a reducer for alignments filtering.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class SAMFilterReducer extends Reducer<Text, Text, Text, Text> {

  static final String GENOME_DESC_PATH_KEY = Globals.PARAMETER_PREFIX
      + ".samfilter.genome.desc.file";
  static final String MAP_FILTER_PARAMETER_KEY_PREFIX =
      Globals.PARAMETER_PREFIX + ".filter.alignments.parameter.";

  private final SAMParser parser = new SAMParser();
  private String counterGroup;
  private ReadAlignmentsFilter filter;

  private Text outKey = new Text();
  private Text outValue = new Text();
  private List<SAMRecord> records = new ArrayList<SAMRecord>();

  @Override
  protected void setup(final Context context) throws IOException,
      InterruptedException {

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

    // Get the genome description filename
    final String genomeDescFile = conf.get(GENOME_DESC_PATH_KEY);

    if (genomeDescFile == null) {
      throw new IOException("No genome desc file set");
    }

    // Load genome description object
    final GenomeDescription genomeDescription =
        GenomeDescription.load(new DataFile(genomeDescFile).open());

    // Set the chromosomes sizes in the parser
    this.parser.setGenomeDescription(genomeDescription);

    // Set the filters
    try {
      final MultiReadAlignmentsFilterBuilder mrafb =
          new MultiReadAlignmentsFilterBuilder();

      for (Map.Entry<String, String> e : conf) {

        if (e.getKey().startsWith(MAP_FILTER_PARAMETER_KEY_PREFIX))
          mrafb.addParameter(
              e.getKey().substring(MAP_FILTER_PARAMETER_KEY_PREFIX.length()),
              e.getValue());
      }

      this.filter =
          mrafb.getAlignmentsFilter(new HadoopReporter(context),
              this.counterGroup);

    } catch (EoulsanException e) {
      throw new IOException(e.getMessage());
    }

  }

  /**
   * 'key': identifier of the aligned read, without the integer indicating the
   * pair member if data are in paired-end mode. 'value': alignments without the
   * identifier part of the SAM line. 
   */
  @Override
  protected void reduce(final Text key, final Iterable<Text> values,
      final Context context) throws IOException, InterruptedException {

    // Creation of a buffer object to store alignments with the same read name
    final ReadAlignmentsFilterBuffer rafb =
        new ReadAlignmentsFilterBuffer(this.filter);

    int cptRecords = 0;
    String strRecord = null;
    records.clear();

    for (Text val : values) {

      cptRecords++;
      strRecord = key.toString() + val.toString();
      rafb.addAlignment(this.parser.parseLine(strRecord));

    }

    records.addAll(rafb.getFilteredAlignments());
    context.getCounter(this.counterGroup,
        ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER.counterName()).increment(
        cptRecords - records.size());

    // sort alignments of the current read
    Collections.sort(records, new SAMComparator());

    // Writing records
    for (SAMRecord r : records) {
      strRecord = r.getSAMString().replaceAll("\n", "");

      final int indexOfFirstTab = strRecord.indexOf("\t");
      this.outKey.set(strRecord.substring(0, indexOfFirstTab));
      this.outValue.set(strRecord.substring(indexOfFirstTab + 1));

      context.write(this.outKey, this.outValue);
      context.getCounter(this.counterGroup,
          OUTPUT_FILTERED_ALIGNMENTS_COUNTER.counterName()).increment(1);
    }

  }
}
