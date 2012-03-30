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

package fr.ens.transcriptome.eoulsan.steps.mapping.local;

import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.ALIGNMENTS_WITH_INVALID_SAM_FORMAT;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.ALIGNMENTS_WITH_MORE_ONE_HIT_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.INPUT_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.OUTPUT_FILTERED_ALIGNMENTS_COUNTER;

import java.util.List;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMFormatException;
import net.sf.samtools.SAMParser;
import net.sf.samtools.SAMRecord;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilter;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilterBuffer;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.ProcessSample;
import fr.ens.transcriptome.eoulsan.steps.ProcessSampleExecutor;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractSAMFilterStep;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This class define a Step for alignements filtering.
 * @since 1.0
 * @author Laurent Jourdren
 */
@LocalOnly
public class SAMFilterLocalStep extends AbstractSAMFilterStep {

  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  /**
   * My version...
   */
  @Override
  public StepResult execute(final Design design, final Context context) {
    
    final GenomeDescription genomeDescription;

    // Load genome description object
    try {

      if (design.getSampleCount() > 0)
        genomeDescription =
            GenomeDescription.load(context.getInputDataFile(
                DataFormats.GENOME_DESC_TXT, design.getSample(0)).open());
      else
        genomeDescription = null;

    } catch (FileNotFoundException e) {

      return new StepResult(context, e, "File not found: " + e.getMessage());
    } catch (IOException e) {

      return new StepResult(context, e, "error while filtering: "
          + e.getMessage());
    }
    
    // Get threshold
    //final int mappingQualityThreshold = getMappingQualityThreshold();

    // Process all samples
    return ProcessSampleExecutor.processAllSamples(context, design,
        new ProcessSample() {

          @Override
          public String processSample(Context context, Sample sample)
              throws ProcessSampleException {
            
            // Define Result
            String resultString = null;
            
            // Create the reporter
            final Reporter reporter = new Reporter();

            try {
              
              // Create parser object
              final SAMParser parser = new SAMParser();
              parser.setGenomeDescription(genomeDescription);

              // get input file count for the sample
              final int inFileCount =
                  context.getDataFileCount(DataFormats.READS_FASTQ, sample);
              
              // Get the read filter
              final ReadAlignmentsFilter filter = 
                  getAlignmentsFilter(reporter, COUNTER_GROUP);

              if (inFileCount == 1) {
                
                // Filter alignments in single end mode
                //return filterSAMFileSingleEnd(context, sample, parser,
                    //mappingQualityThreshold);
                resultString = filterSample(context, sample, reporter, 
                    parser, filter, false);
                
              } else if (inFileCount == 2) {
                
                // Filter alignments in paired-end mode
                resultString = filterSample(context, sample, reporter, 
                    parser, filter, true);
                
              }
            } /*catch (FileNotFoundException e) {
              throwException(e, "File not found: " + e.getMessage());
            }*/ catch (IOException e) {
              throwException(e, "Error while filtering: " + e.getMessage());
            } catch (EoulsanException e) {
              throwException(e, "Error while initializing filter: " + e.getMessage());
            }
            
            return resultString;
          }

        });
  }
  
  /**
   * Filter a sample data in single-end mode and in paired-end mode.
   * @param context Eoulsan context
   * @param sample sample to process
   * @param reporter reporter to use
   * @param parser SAM parser to use
   * @param filter alignments filter to use
   * @param pairedEnd true if data are in paired-end mode
   * @return a string with information to log
   * @throws IOException if an error occurs while filtering reads
   */
  private static String filterSample(final Context context, final Sample sample,
    final Reporter reporter, final SAMParser parser, 
    final ReadAlignmentsFilter filter, final boolean pairedEnd) throws IOException {
    
 // Get the source
  final DataFile inFile =
      context.getInputDataFile(DataFormats.MAPPER_RESULTS_SAM, sample);

  // Get the dest
  final DataFile outFile =
      context.getOutputDataFile(DataFormats.FILTERED_MAPPER_RESULTS_SAM, sample);
  
  
  // filter alignments in single-end mode or in paired-end mode
  filterFile(inFile, outFile, reporter, filter, pairedEnd);
  
  // Add counters for this sample to log file
  return reporter.countersValuesToString(COUNTER_GROUP, "Filter SAM file ("
      + sample.getName() + ", " + inFile + ")");
  }
  
//  /**
//   * Filter a sample data in single end mode.
//   * @param context Eoulsan context
//   * @param sample sample to process
//   * @param reporter reporter to use
//   * @param parser SAM parser to use
//   * @param filter alignments filter to use
//   * @return a string with information to log
//   * @throws IOException if an error occurs while filtering reads
//   */
//  private static String singleEnd(final Context context, final Sample sample,
//      final Reporter reporter, final SAMParser parser, 
//      final ReadAlignmentsFilter filter) throws IOException {
//    
//    // Get the source
//    final DataFile inFile =
//        context.getInputDataFile(DataFormats.MAPPER_RESULTS_SAM, sample, 0);
//
//    // Get the dest
//    final DataFile outFile =
//        context.getOutputDataFile(DataFormats.FILTERED_MAPPER_RESULTS_SAM, sample, 0);
//    
//    
//    // filter alignments
//    filterFile(inFile, outFile, reporter, filter);
//    
//    // Add counters for this sample to log file
//    return reporter.countersValuesToString(COUNTER_GROUP, "Filter SAM file ("
//        + sample.getName() + ", " + inFile + ")");
//  }
  
//  /**
//   * Filter a sample data in paired-end mode.
//   * @param context Eoulsan context
//   * @param sample sample to process
//   * @param reporter reporter to use
//   * @param parser SAM parser to use
//   * @param filter alignments filter to use
//   * @return a string with information to log
//   * @throws IOException if an error occurs while filtering reads
//   */
//  private static String pairedEnd(final Context context, final Sample sample,
//      final Reporter reporter, final SAMParser parser, 
//      final ReadAlignmentsFilter filter) throws IOException {
//    
//    // Get the source
//    final DataFile inFile1 =
//        context.getInputDataFile(DataFormats.MAPPER_RESULTS_SAM, sample, 0);
//    final DataFile inFile2 =
//        context.getInputDataFile(DataFormats.MAPPER_RESULTS_SAM, sample, 1);
//
//    // Get the dest
//    final DataFile outFile1 =
//        context.getOutputDataFile(DataFormats.FILTERED_MAPPER_RESULTS_SAM, 
//            sample, 0);
//    final DataFile outFile2 =
//        context.getOutputDataFile(DataFormats.FILTERED_MAPPER_RESULTS_SAM, 
//            sample, 1);
//    
//    
//    // filter alignments
//    filterFile(inFile1, inFile2, outFile1, outFile2, reporter, filter);
//    
//    // Add counters for this sample to log file
//    return reporter.countersValuesToString(COUNTER_GROUP, "Filter SAM files ("
//        + sample.getName() + ", " + inFile1 + ", " + inFile2 + ")");
//  }
  
  /**
   * Filter a file in single-end mode or paired-end mode.
   * @param inFile input file
   * @param outFile output file
   * @param reporter reporter to use
   * @param filter alignments filter to use
   * @param pairedEnd true if data are in paired-end mode
   * @throws IOException if an error occurs while filtering data
   */
  private static void filterFile(final DataFile inFile, final DataFile outFile,
      final Reporter reporter, final ReadAlignmentsFilter filter, 
      final boolean pairedEnd) throws IOException {
    
    final List<SAMRecord> records = new ArrayList<SAMRecord>();
    int counterRecord = 0;
    
    // Creation of a buffer object to store alignments with the same read name 
    final ReadAlignmentsFilterBuffer rafb = 
        new ReadAlignmentsFilterBuffer(filter);
    
    LOGGER.info("Filter SAM file: " + inFile);
    
    // Get reader
    final SAMFileReader inputSam =
        new SAMFileReader(inFile.open());

    // Get Writer
    final SAMFileWriter outputSam =
        new SAMFileWriterFactory().makeSAMWriter(
            inputSam.getFileHeader(), false, outFile.create());
    
    try {
      
      for (SAMRecord samRecord : inputSam) {
        
        reporter.incrCounter(COUNTER_GROUP,
            INPUT_ALIGNMENTS_COUNTER.counterName(), 1);
        
        if (!rafb.addAlignment(samRecord)) {
          if (pairedEnd && counterRecord > 2) {
            reporter.incrCounter(COUNTER_GROUP, 
                ALIGNMENTS_WITH_MORE_ONE_HIT_COUNTER.counterName(), counterRecord);
          }
          else {
            if (counterRecord > 1) {
              reporter.incrCounter(COUNTER_GROUP, 
                  ALIGNMENTS_WITH_MORE_ONE_HIT_COUNTER.counterName(), 
                  counterRecord);
            }
          }
          counterRecord = 0;
          
          records.clear();
          records.addAll(rafb.getFilteredAlignments(pairedEnd));
          
          // writing records
          for (SAMRecord r : records) {
            outputSam.addAlignment(r);
            reporter.incrCounter(COUNTER_GROUP, 
                OUTPUT_FILTERED_ALIGNMENTS_COUNTER.counterName(), 1);
          }
        }
        counterRecord++;
      }
      
      // treatment of the last record
      rafb.checkBuffer();
      if (pairedEnd && counterRecord > 2) {
        reporter.incrCounter(COUNTER_GROUP, 
            ALIGNMENTS_WITH_MORE_ONE_HIT_COUNTER.counterName(), counterRecord);
      }
      else {
        if (counterRecord > 1) {
          reporter.incrCounter(COUNTER_GROUP, 
              ALIGNMENTS_WITH_MORE_ONE_HIT_COUNTER.counterName(), counterRecord);
        }
      }
      
      records.clear();
      records.addAll(rafb.getFilteredAlignments(pairedEnd));
      
      // writing records
      for (SAMRecord r : records) {
        outputSam.addAlignment(r);
        reporter.incrCounter(COUNTER_GROUP, 
            OUTPUT_FILTERED_ALIGNMENTS_COUNTER.counterName(), 1);
      }
    } catch (SAMFormatException e) {

      reporter.incrCounter(COUNTER_GROUP,
          ALIGNMENTS_WITH_INVALID_SAM_FORMAT.counterName(), 1);
    }
    
    // Close files
    inputSam.close();
    outputSam.close();
  }
  
  /**
   * Filter a file in paired-end mode.
   * @param inFile input file
   * @param outFile output file
   * @param reporter reporter to use
   * @param filter alignments filter to use
   * @throws IOException if an error occurs while filtering data
   */
//  private static void filterFilePairedEnd(final DataFile inFile, 
//      final DataFile outFile, final Reporter reporter, 
//      final ReadAlignmentsFilter filter) throws IOException {
//    
//    final List<SAMRecord> records = new ArrayList<SAMRecord>();
//    
//    // Creation of a buffer object to store alignments with the same read name 
//    final ReadAlignmentsFilterBuffer rafb = 
//        new ReadAlignmentsFilterBuffer(filter);
//    
//    LOGGER.info("Filter SAM files: " + inFile);
//    
//    // Get readers
//    final SAMFileReader inputSam =
//        new SAMFileReader(inFile.open());
//
//    // Get writers
//    final SAMFileWriter outputSam =
//        new SAMFileWriterFactory().makeSAMWriter(
//            inputSam.getFileHeader(), false, outFile.create());
//    
//    try {
//      
//      for (SAMRecord samRecord : inputSam) {
//        
//        reporter.incrCounter(COUNTER_GROUP,
//            INPUT_ALIGNMENTS_COUNTER.counterName(), 1);
//        
//        if (!rafb.addAlignment(samRecord, reporter, COUNTER_GROUP)) {
//          records.clear();
//          records.addAll(rafb.getFilteredAlignmentsPairedEnd());
//          
//          // writing records
//          for (SAMRecord r : records) {
//            outputSam.addAlignment(r);
//            reporter.incrCounter(COUNTER_GROUP, 
//                OUTPUT_FILTERED_ALIGNMENTS_COUNTER.counterName(), 1);
//          }
//        }
//      }
//      
//      // treatment of the last record
//      if (!rafb.addAlignment(null, reporter, COUNTER_GROUP)) {
//        records.clear();
//        records.addAll(rafb.getFilteredAlignmentsPairedEnd());
//        
//        // writing records
//        for (SAMRecord r : records) {
//          outputSam.addAlignment(r);
//          reporter.incrCounter(COUNTER_GROUP, 
//              OUTPUT_FILTERED_ALIGNMENTS_COUNTER.counterName(), 1);
//        }
//      }
//      
//    } catch (SAMFormatException e) {
//
//      reporter.incrCounter(COUNTER_GROUP,
//          ALIGNMENTS_WITH_INVALID_SAM_FORMAT.counterName(), 1);
//    }
//    
//    // Close files
//    inputSam.close();
//    outputSam.close();
//  }
  
  
  /**
   * Version originale...
   */
  /*
  @Override
  public StepResult execute(final Design design, final Context context) {

    final GenomeDescription genomeDescription;

    // Load genome description object
    try {

      if (design.getSampleCount() > 0)
        genomeDescription =
            GenomeDescription.load(context.getInputDataFile(
                DataFormats.GENOME_DESC_TXT, design.getSample(0)).open());
      else
        genomeDescription = null;

    } catch (FileNotFoundException e) {

      return new StepResult(context, e, "File not found: " + e.getMessage());
    } catch (IOException e) {

      return new StepResult(context, e, "error while filtering: "
          + e.getMessage());
    }

    // Get threshold
    final int mappingQualityThreshold = getMappingQualityThreshold();

    // Process all samples
    return ProcessSampleExecutor.processAllSamples(context, design,
        new ProcessSample() {

          @Override
          public String processSample(Context context, Sample sample)
              throws ProcessSampleException {

            try {

              // Create parser object
              final SAMParser parser = new SAMParser();
              parser.setGenomeDescription(genomeDescription);

              // get input file count for the sample
              final int inFileCount =
                  context.getDataFileCount(DataFormats.READS_FASTQ, sample);

              if (inFileCount == 1) {
                
                // Filter alignments in single end mode
                return filterSAMFileSingleEnd(context, sample, parser,
                    mappingQualityThreshold);
                
              } else if (inFileCount == 2) {
                
                // Filter alignments in paired-end mode
                return filterSAMFilePairedEnd(context, sample, parser,
                    mappingQualityThreshold);
                
              }
            } catch (FileNotFoundException e) {

              throwException(e, "File not found: " + e.getMessage());
            } catch (IOException e) {

              throwException(e, "Error while filtering: " + e.getMessage());
            } catch (EoulsanRuntimeException e) {

              throwException(e, "Error while filtering: " + e.getMessage());
            }
            return null;
          }

        });
  }*/

  /**
   * Parse a sam file in single-end mode.
   * @param context context object
   * @param sample sample to use
   * @param parser parse with genome description
   * @param mappingQualityThreshold mapping quality threshold
   * @return a String with log information about the filtering of alignments of
   *         the sample
   * @throws IOException if an error occurs while reading SAM input file or
   *           writing filtered SAM file
   */
  /*private String filterSAMFileSingleEnd(final Context context, final Sample sample,
      final SAMParser parser, final int mappingQualityThreshold)
      throws IOException {

    // Create the reporter
    final Reporter reporter = new Reporter();

    // Get reader
    final SAMFileReader inputSam =
        new SAMFileReader(context.getInputDataFile(
            DataFormats.MAPPER_RESULTS_SAM, sample).open());

    // Get Writer
    final SAMFileWriter outputSam =
        new SAMFileWriterFactory().makeSAMWriter(
            inputSam.getFileHeader(),
            false,
            context.getOutputDataFile(DataFormats.FILTERED_MAPPER_RESULTS_SAM,
                sample).create());

    String lastId = null;
    SAMRecord lastRecord = null;
    int lastIdCount = 1;
    int cpt = 0;

    for (SAMRecord samRecord : inputSam) {
      
      if (cpt < 100) {
        LOGGER.info(samRecord.getReadName());
        cpt ++;
      }

      try {
        reporter.incrCounter(COUNTER_GROUP,
            INPUT_ALIGNMENTS_COUNTER.counterName(), 1);

        if (samRecord.getReadUnmappedFlag()) {
          reporter.incrCounter(COUNTER_GROUP,
              UNMAP_READS_COUNTER.counterName(), 1);
        } else {

          if (samRecord.getMappingQuality() >= mappingQualityThreshold) {

            reporter.incrCounter(COUNTER_GROUP,
                GOOD_QUALITY_ALIGNMENTS_COUNTER.counterName(), 1);

            final String id = samRecord.getReadName();

            if (id.equals(lastId)) {
              lastIdCount++;
            } else {

              if (lastIdCount == 1) {

                outputSam.addAlignment(samRecord);
                reporter.incrCounter(COUNTER_GROUP,
                    OUTPUT_FILTERED_ALIGNMENTS_COUNTER.counterName(), 1);

              } else if (lastIdCount > 1) {

                reporter.incrCounter(COUNTER_GROUP,
                    ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER.counterName(),
                    lastIdCount);
                reporter.incrCounter(COUNTER_GROUP,
                    ALIGNMENTS_WITH_MORE_ONE_HIT_COUNTER.counterName(),
                    lastIdCount);
              }

              lastIdCount = 1;
              lastId = id;
            }

          } else {

            reporter.incrCounter(COUNTER_GROUP,
                ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER.counterName(), 1);
          }

          lastRecord = samRecord;
        }

      } catch (SAMFormatException e) {

        reporter.incrCounter(COUNTER_GROUP,
            ALIGNMENTS_WITH_INVALID_SAM_FORMAT.counterName(), 1);
      }

    }

    if (lastIdCount == 1) {

      outputSam.addAlignment(lastRecord);
      reporter.incrCounter(COUNTER_GROUP,
          OUTPUT_FILTERED_ALIGNMENTS_COUNTER.counterName(), 1);

    } else if (lastIdCount > 1) {

      reporter.incrCounter(COUNTER_GROUP,
          ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER.counterName(), 1);
      reporter.incrCounter(COUNTER_GROUP,
          ALIGNMENTS_WITH_MORE_ONE_HIT_COUNTER.counterName(), 1);
    }

    // Close files
    inputSam.close();
    outputSam.close();

    return reporter.countersValuesToString(
        COUNTER_GROUP,
        "Filter SAM files ("
            + sample.getName()
            + ", "
            + context.getInputDataFile(DataFormats.MAPPER_RESULTS_SAM, sample)
                .getName() + ")");
  }*/

  /**
   * Parse a sam file in paired-end mode.
   * @param context context object
   * @param sample sample to use
   * @param parser parse with genome description
   * @param mappingQualityThreshold mapping quality threshold
   * @return a String with log information about the filtering of alignments of
   *         the sample
   * @throws IOException if an error occurs while reading SAM input file or
   *           writing filtered SAM file
   */
  /*private String filterSAMFilePairedEnd(final Context context, final Sample sample,
      final SAMParser parser, final int mappingQualityThreshold)
      throws IOException {
    
    // Create the reporter
    final Reporter reporter = new Reporter();

    // Get reader
    final SAMFileReader inputSam =
        new SAMFileReader(context.getInputDataFile(
            DataFormats.MAPPER_RESULTS_SAM, sample).open());

    // Get Writer
    final SAMFileWriter outputSam =
        new SAMFileWriterFactory().makeSAMWriter(
            inputSam.getFileHeader(),
            false,
            context.getOutputDataFile(DataFormats.FILTERED_MAPPER_RESULTS_SAM,
                sample).create());

    String lastId = null;
    SAMRecord lastRecord = null;
    int lastIdCount = 0;

    for (SAMRecord samRecord : inputSam) {

      try {
        reporter.incrCounter(COUNTER_GROUP,
            INPUT_ALIGNMENTS_COUNTER.counterName(), 1);

        if (samRecord.getReadUnmappedFlag()) {
          reporter.incrCounter(COUNTER_GROUP,
              UNMAP_READS_COUNTER.counterName(), 1);
        } else {

          if (samRecord.getMappingQuality() >= mappingQualityThreshold) {

            reporter.incrCounter(COUNTER_GROUP,
                GOOD_QUALITY_ALIGNMENTS_COUNTER.counterName(), 1);

            final String id = samRecord.getReadName();

            if (id.equals(lastId)) {
              lastIdCount++;
            } else {

              if (lastIdCount == 1) {

                outputSam.addAlignment(samRecord);
                reporter.incrCounter(COUNTER_GROUP,
                    OUTPUT_FILTERED_ALIGNMENTS_COUNTER.counterName(), 1);

              } else if (lastIdCount > 1) {

                reporter.incrCounter(COUNTER_GROUP,
                    ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER.counterName(),
                    lastIdCount);
                reporter.incrCounter(COUNTER_GROUP,
                    ALIGNMENTS_WITH_MORE_ONE_HIT_COUNTER.counterName(),
                    lastIdCount);
              }

              lastIdCount = 1;
              lastId = id;
            }

          } else {

            reporter.incrCounter(COUNTER_GROUP,
                ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER.counterName(), 1);
          }

          lastRecord = samRecord;
        }

      } catch (SAMFormatException e) {

        reporter.incrCounter(COUNTER_GROUP,
            ALIGNMENTS_WITH_INVALID_SAM_FORMAT.counterName(), 1);
      }

    }

    if (lastIdCount == 1) {

      outputSam.addAlignment(lastRecord);
      reporter.incrCounter(COUNTER_GROUP,
          OUTPUT_FILTERED_ALIGNMENTS_COUNTER.counterName(), 1);

    } else if (lastIdCount > 1) {

      reporter.incrCounter(COUNTER_GROUP,
          ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER.counterName(), 1);
      reporter.incrCounter(COUNTER_GROUP,
          ALIGNMENTS_WITH_MORE_ONE_HIT_COUNTER.counterName(), 1);
    }

    // Close files
    inputSam.close();
    outputSam.close();

    return reporter.countersValuesToString(
        COUNTER_GROUP,
        "Filter SAM files ("
            + sample.getName()
            + ", "
            + context.getInputDataFile(DataFormats.MAPPER_RESULTS_SAM, sample)
                .getName() + ")");
  }*/

}
