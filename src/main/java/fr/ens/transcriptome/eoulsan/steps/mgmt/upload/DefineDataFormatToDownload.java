package fr.ens.transcriptome.eoulsan.steps.mgmt.upload;

import static fr.ens.transcriptome.eoulsan.steps.mgmt.upload.HDFSDataDownloadStep.DATAFORMATS_TO_DOWNLOAD_SETTING;

import java.util.Set;
import java.util.logging.Logger;

import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.steps.StepResult;

/**
 * This Step allow to define the list of the formats of the files to download at
 * the end of a Hadoop execution.
 * @author jourdren
 */
@HadoopCompatible
public class DefineDataFormatToDownload extends AbstractStep {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  protected static final String STEP_NAME = "defineformatstodownload";

  private DataFormat[] inFormats;

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public String getDescription() {

    return "define the list of the formats of the files to download "
        + "at the end of a Hadoop execution";
  }

  @Override
  public String getLogName() {

    return null;
  }

  @Override
  public void configure(final Set<Parameter> stepParameters,
      final Set<Parameter> globalParameters) throws EoulsanException {

    String formatNames = null;

    for (Parameter p : stepParameters) {

      if ("formats".equals(p.getName()))
        formatNames = p.getStringValue();
      else
        throw new EoulsanException("Unknown parameter for "
            + getName() + " step: " + p.getName());
    }

    if (formatNames == null) {
      throw new EoulsanException("No format to download set.");
    }

    final String[] fields = formatNames.split(",");
    final Set<DataFormat> formats = Sets.newHashSet();
    final DataFormatRegistry registery = DataFormatRegistry.getInstance();

    LOGGER.info("available formats: " + registery.getAllFormats());

    for (String format : fields) {
      final DataFormat df = registery.getDataFormatFromName(format.trim());
      if (df == null) {
        throw new EoulsanException("Format not found : " + format.trim());
      }

      formats.add(df);
    }

    this.inFormats = formats.toArray(new DataFormat[0]);
  }

  @Override
  public StepResult execute(Design design, Context context) {

    final long startTime = System.currentTimeMillis();

    final StringBuilder sb = new StringBuilder();
    boolean first = true;

    for (DataFormat df : this.inFormats) {

      if (first) {
        first = false;
      } else {
        sb.append(',');
      }

      sb.append(df.getFormatName());
    }

    final String formats = sb.toString();

    LOGGER.info("Format to download: " + formats);

    // Save the list of the DataFormat to download in the settings
    final Settings settings = context.getRuntime().getSettings();

    settings.setSetting(DATAFORMATS_TO_DOWNLOAD_SETTING, formats);

    return new StepResult(context, startTime, "Formats to download: " + formats);
  }

}
