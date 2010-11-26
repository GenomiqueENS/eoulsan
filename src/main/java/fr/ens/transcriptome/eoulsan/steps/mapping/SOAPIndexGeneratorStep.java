package fr.ens.transcriptome.eoulsan.steps.mapping;

import java.io.File;
import java.io.IOException;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.SOAPWrapper;
import fr.ens.transcriptome.eoulsan.core.AbstractStep;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.datatypes.DataFile;
import fr.ens.transcriptome.eoulsan.datatypes.DataFormat;
import fr.ens.transcriptome.eoulsan.datatypes.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

public class SOAPIndexGeneratorStep extends AbstractStep {

  @Override
  public String getName() {

    return "_soapindexgenerator";
  }

  @Override
  public String getDescription() {

    return "Generate SOAP index";
  }

  @Override
  public DataFormat[] getInputFormats() {

    return new DataFormat[] {DataFormats.GENOME_FASTA};
  }

  @Override
  public DataFormat[] getOutputFormats() {

    return new DataFormat[] {DataFormats.SOAP_INDEX_ZIP};
  }

  @Override
  public ExecutionMode getExecutionMode() {

    return ExecutionMode.BOTH;
  }

  @Override
  public StepResult execute(final Design design, final Context context) {

    final long startTime = System.currentTimeMillis();

    try {

      if (design.getSampleCount() == 0)
        throw new EoulsanException("No sample found in design file.");

      final Sample s1 = design.getSamples().get(0);
      if (!s1.getMetadata().isGenomeField())
        throw new EoulsanException("No genome found in design file.");

      final String genomeSource = s1.getMetadata().getGenome();
      if (genomeSource == null)
        throw new EoulsanException("Genome source is null.");

      // Create index file
      final File soapIndexFile =
          SOAPWrapper.makeIndexInZipFile(context.getDataFile(
              DataFormats.GENOME_FASTA, s1).open());
      System.out.println("\t-> "+soapIndexFile);
      // Get the output DataFile
      final DataFile soapIndexDataFile =
          context.getDataFile(DataFormats.SOAP_INDEX_ZIP, s1);
      System.out.println("\t-> "+soapIndexDataFile);

      FileUtils.copy(FileUtils.createInputStream(soapIndexFile),
          soapIndexDataFile.create());

      if (!soapIndexFile.delete())
        context.getLogger().severe("Unbable to delete temporary SOAP index.");

    } catch (EoulsanException e) {

      return new StepResult(this, e);
    } catch (IOException e) {

      return new StepResult(this, e);
    }

    return new StepResult(this, startTime, "SOAP index creation");
  }

}
