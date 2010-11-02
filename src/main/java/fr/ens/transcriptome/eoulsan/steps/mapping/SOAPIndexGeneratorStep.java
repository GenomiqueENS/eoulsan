package fr.ens.transcriptome.eoulsan.steps.mapping;

import java.io.File;
import java.io.IOException;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.SOAPWrapper;
import fr.ens.transcriptome.eoulsan.core.AbstractStep;
import fr.ens.transcriptome.eoulsan.core.ExecutorInfo;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.datatypes.DataType;
import fr.ens.transcriptome.eoulsan.datatypes.DataTypes;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.io.protocol.DataFile;
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
  public DataType[] getInputTypes() {

    return new DataType[] {DataTypes.GENOME};
  }

  @Override
  public DataType[] getOutputType() {

    return new DataType[] {DataTypes.SOAP_INDEX};
  }

  @Override
  public ExecutionMode getExecutionMode() {

    return ExecutionMode.BOTH;
  }

  @Override
  public StepResult execute(final Design design, final ExecutorInfo info) {

    final long startTime = System.currentTimeMillis();

    try {

      if (design.getSampleCount() == 0)
        throw new EoulsanException("No sample found in design file.");

      final Sample s1 = design.getSamples().get(0);
      if (s1.getMetadata().isGenomeField())
        throw new EoulsanException("No genome found in design file.");

      final String genomeSource = s1.getMetadata().getGenome();
      if (genomeSource == null)
        throw new EoulsanException("Genome source is null.");

      // Create index file
      final File soapIndexFile =
          SOAPWrapper.makeIndexInZipFile(info.getDataFile(DataTypes.GENOME, s1)
              .open());

      // Get the output DataFile
      final DataFile soapIndexDataFile =
          info.getDataFile(DataTypes.SOAP_INDEX, s1);

      if (info.getRuntime().isAmazonMode()) {

        // In Hadoop mode copy the SOAP index

        FileUtils.copy(FileUtils.createInputStream(soapIndexFile),
            soapIndexDataFile.create());
        if (!soapIndexFile.delete())
          info.getLogger().severe("Unbable to delete temporary SOAP index.");
      } else {

        // In local mode move SOAP index
        soapIndexFile.renameTo(new File(soapIndexDataFile.getSource()));
      }

    } catch (EoulsanException e) {

      return new StepResult(this, e);
    } catch (IOException e) {

      return new StepResult(this, e);
    }

    return new StepResult(this, startTime, "SOAP index creation");
  }

}
