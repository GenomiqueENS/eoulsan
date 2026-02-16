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

package fr.ens.biologie.genomique.eoulsan.modules.mgmt.upload;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.modules.mgmt.upload.HDFSDataDownloadModule.DATAFORMATS_TO_DOWNLOAD_SETTING;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.Settings;
import fr.ens.biologie.genomique.eoulsan.annotations.HadoopCompatible;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.kenetre.util.Version;

/**
 * This Step allow to define the list of the formats of the files to download at
 * the end of a Hadoop execution.
 * @since 1.0
 * @author Laurent Jourdren
 */
@HadoopCompatible
public class DefineDataFormatToDownload extends AbstractModule {

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
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    String formatNames = null;

    for (Parameter p : stepParameters) {

      if ("formats".equals(p.getName())) {
        formatNames = p.getStringValue();
      } else {
        throw new EoulsanException(
            "Unknown parameter for " + getName() + " step: " + p.getName());
      }
    }

    if (formatNames == null) {
      throw new EoulsanException("No format to download set.");
    }

    final Set<DataFormat> formats = new HashSet<>();
    final DataFormatRegistry registry = DataFormatRegistry.getInstance();

    for (String format : Splitter.on(',').split(formatNames)) {

      if ("".equals(format.trim())) {
        continue;
      }

      final DataFormat df = registry.getDataFormatFromName(format.trim());
      if (df == null) {
        throw new EoulsanException("Format not found : " + format.trim());
      }

      formats.add(df);
    }

    this.inFormats = formats.toArray(new DataFormat[0]);
  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    final StringBuilder sb = new StringBuilder();
    boolean first = true;

    for (DataFormat df : this.inFormats) {

      if (first) {
        first = false;
      } else {
        sb.append(',');
      }

      sb.append(df.getName());
    }

    final String formats = sb.toString();

    getLogger().info("Format to download: " + formats);

    // Save the list of the DataFormat to download in the settings
    final Settings settings = context.getRuntime().getSettings();

    settings.setSetting(DATAFORMATS_TO_DOWNLOAD_SETTING, formats, false);

    status.setProgressMessage("Formats to download: " + formats);
    return status.createTaskResult();
  }

}
