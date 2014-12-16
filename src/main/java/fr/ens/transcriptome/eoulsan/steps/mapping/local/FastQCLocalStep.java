package fr.ens.transcriptome.eoulsan.steps.mapping.local;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import uk.ac.babraham.FastQC.Modules.AbstractQCModule;
import uk.ac.babraham.FastQC.Modules.AdapterContent;
import uk.ac.babraham.FastQC.Modules.BasicStats;
import uk.ac.babraham.FastQC.Modules.KmerContent;
import uk.ac.babraham.FastQC.Modules.NContent;
import uk.ac.babraham.FastQC.Modules.OverRepresentedSeqs;
import uk.ac.babraham.FastQC.Modules.PerBaseQualityScores;
import uk.ac.babraham.FastQC.Modules.PerBaseSequenceContent;
import uk.ac.babraham.FastQC.Modules.PerSequenceGCContent;
import uk.ac.babraham.FastQC.Modules.PerSequenceQualityScores;
import uk.ac.babraham.FastQC.Modules.PerTileQualityScores;
import uk.ac.babraham.FastQC.Modules.QCModule;
import uk.ac.babraham.FastQC.Modules.SequenceLengthDistribution;
import uk.ac.babraham.FastQC.Report.HTMLReportArchive;
import uk.ac.babraham.FastQC.Sequence.Sequence;
import uk.ac.babraham.FastQC.Sequence.SequenceFactory;
import uk.ac.babraham.FastQC.Sequence.SequenceFile;
import uk.ac.babraham.FastQC.Sequence.SequenceFormatException;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractFastQCStep;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.LocalReporter;
import fr.ens.transcriptome.eoulsan.util.Reporter;

@LocalOnly
public class FastQCLocalStep extends AbstractFastQCStep {

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    // Create the reporter
    final Reporter reporter = new LocalReporter();

    // Get input SAM data
    final Data inData = context.getInputData(getInputFormat());

    // Get output BAM data
    final Data outData = context.getOutputData(DataFormats.REPORT_HTML, inData);

    // Extract data file
    final DataFile inFile = inData.getDataFile();
    final DataFile reportFile = outData.getDataFile();

    SequenceFile seqFile = null;

    try {
      seqFile = SequenceFactory.getSequenceFile(inFile.toFile());

    } catch (SequenceFormatException | IOException e) {
      return status.createStepResult(e,
          "Error while init sequence file: " + e.getMessage());
    }

    // Define modules list
    final OverRepresentedSeqs os = new OverRepresentedSeqs();

    final List<AbstractQCModule> modules =
        Lists.newArrayList(new BasicStats(), new PerBaseQualityScores(),
            new PerTileQualityScores(), new PerSequenceQualityScores(),
            new PerBaseSequenceContent(), new PerSequenceGCContent(),
            new NContent(), new SequenceLengthDistribution(),
            os.duplicationLevelModule(), os, new AdapterContent(),
            new KmerContent());

    try {

      processSequences(modules, seqFile);

      createReport(modules, seqFile, reportFile.toFile());

      // Set the description of the context
      status.setDescription("Create FastQC report on "
          + inData.getDataFile().toFile().getAbsolutePath() + " in "
          + reportFile.getName() + ")");

      // Add counters for this sample to log file
      status.setCounters(reporter, COUNTER_GROUP);

      // Keep module data is now unnecessary
      modules.clear();

      return status.createStepResult();

    } catch (final SequenceFormatException e) {
      return status.createStepResult(e,
          "Error with sequence file format: " + e.getMessage());

    } catch (final IOException e) {
      return status.createStepResult(e,
          "Error while parsing file: " + e.getMessage());

    } catch (final XMLStreamException e) {
      return status.createStepResult(e, "Error while writing final report: "
          + e.getMessage());
    }

  }

  private void processSequences(final List<AbstractQCModule> modules,
      final SequenceFile seqFile) throws SequenceFormatException {

    while (seqFile.hasNext()) {

      final Sequence seq = seqFile.next();

      for (final QCModule module : modules) {

        module.processSequence(seq);
      }
    }

  }

  private void createReport(final List<AbstractQCModule> modules,
      final SequenceFile seqFile, final File reportFile) throws IOException,
      XMLStreamException {

    new HTMLReportArchive(seqFile, modules.toArray(new QCModule[] {}),
        reportFile);

    final String extension = ".html";
    final String outputDir = reportFile.getParent();

    // Report zip filename
    final String basefilename =
        reportFile.getName().substring(0,
            reportFile.getName().length() - extension.length());

    // Remove zip file
    if (!new File(outputDir, basefilename + ".zip").delete()) {
    }

    // Remove directory file
    final File zipDir = new File(outputDir, basefilename);
    if (!FileUtils.recursiveDelete(zipDir)) {
    }
  }
}
