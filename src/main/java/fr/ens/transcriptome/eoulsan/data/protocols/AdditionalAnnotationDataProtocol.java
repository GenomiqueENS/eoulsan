package fr.ens.transcriptome.eoulsan.data.protocols;

import java.util.List;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;
import fr.ens.transcriptome.eoulsan.data.DataFormats;

/**
 * This class define a additional annotation protocol.
 * @since 2.0
 * @author Laurent Jourdren
 */
@HadoopCompatible
public class AdditionalAnnotationDataProtocol extends StorageDataProtocol {

  @Override
  public String getName() {

    return "additionalannotation";
  }

  @Override
  protected List<String> getExtensions() {

    return DataFormats.ADDITIONAL_ANNOTATION_TSV.getExtensions();
  }

  @Override
  protected String getBasePath() {

    return EoulsanRuntime.getSettings().getAdditionalAnnotationStoragePath();
  }

}
