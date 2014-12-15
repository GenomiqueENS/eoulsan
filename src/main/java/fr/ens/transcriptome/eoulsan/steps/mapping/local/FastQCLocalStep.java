package fr.ens.transcriptome.eoulsan.steps.mapping.local;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
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

    // TODO
    System.out.println("launch in data found "
        + inData.getDataFilename() + "\n\tdata format define in parameter "
        + getInputFormat().getDescription());

    // Get output BAM data
    final Data outData = context.getOutputData(DataFormats.REPORT_HTML, inData);

    // Extract data file
    final DataFile inFile = inData.getDataFile();
    final DataFile reportFile = outData.getDataFile();

    SequenceFile seqFile = null;

    try {
      seqFile = SequenceFactory.getSequenceFile(inFile.toFile());

      // TODO
      System.out.println("fastQC read file "
          + inFile.toFile().getAbsolutePath());

    } catch (SequenceFormatException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
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
      // TODO Auto-generated catch block
      e.printStackTrace();

      return status.createStepResult(e);

    } catch (final IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();

      return status.createStepResult(e);

    } catch (final XMLStreamException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();

      return status.createStepResult(e);
    }

  }

  public StepResult execute(final Map<DataFormat, DataFile> inData,
      final Map<DataFormat, DataFile> outData) {

    final File inFile = inData.get(DataFormats.READS_FASTQ).toFile();
    final File reportFile = outData.get(DataFormats.REPORT_HTML).toFile();

    SequenceFile seqFile = null;

    try {
      seqFile = SequenceFactory.getSequenceFile(inFile);

    } catch (SequenceFormatException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
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

    } catch (final SequenceFormatException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    try {

      createReport(modules, seqFile, reportFile);

      // Keep module data is now unnecessary
      modules.clear();

    } catch (final IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (final XMLStreamException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return null;
  }

  //
  // Treatment
  //

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
      // TODO
      System.out.println("FAIL to delete "
          + new File(outputDir, basefilename + ".zip").getAbsolutePath());
    }

    final File zipDir = new File(outputDir, basefilename);
    if (!FileUtils.recursiveDelete(zipDir)) {
      // TODO
      System.out.println("FAIL to delete " + zipDir.getAbsolutePath());
    }
  }

  //
  // Main to test
  //

  public static void main(final String[] argv) throws EoulsanException {
    final File testDir =
        new File(
            "/home/sperrin/Documents/test_eoulsan/tests_fonctionnels/tests/00000X_fastqc_step");

    final FastQCLocalStep fastQC = new FastQCLocalStep();

    final Set<Parameter> params = new HashSet<>();

    final String name = "20120213";
    final String prefix = "fastqc_ouput_report_";

    final File reportHtml = new File(testDir, prefix + name + ".html");
    final File reportZip = new File(testDir, prefix + name + ".zip");
    final File reportDir = new File(testDir, prefix + name);

    // Remove
    reportDir.delete();
    reportZip.delete();
    reportHtml.delete();

    final Map<DataFormat, DataFile> inData = new HashMap<>();
    inData.put(DataFormats.READS_FASTQ, new DataFile(new File(testDir,
        "filtered_reads_1a.fq")));

    final Map<DataFormat, DataFile> outData = new HashMap<>();
    outData.put(DataFormats.REPORT_HTML, new DataFile(reportHtml));

    // Configure step
    fastQC.configure(params);

    // Execute step
    fastQC.execute(inData, outData);

    // Check report exit
    if (!reportHtml.exists()) {
      System.out.println("ERROR: report html missing.");
    }

    if (reportHtml.length() < 10) {
      System.out.println("ERROR: report html is empty.");
    }

    if (reportZip.exists()) {
      System.out.println("ERROR: report zip not delete.");
    }

    if (reportDir.exists()) {
      System.out.println("ERROR: report dir not delete.");
    }

    System.out.println("SUCCESS");
  }
}
