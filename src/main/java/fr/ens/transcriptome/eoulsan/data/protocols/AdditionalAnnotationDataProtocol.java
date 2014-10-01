package fr.ens.transcriptome.eoulsan.data.protocols;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;

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
  protected String getExtension() {

    return ".tsv";
  }

  @Override
  protected String getBasePath() {

    return EoulsanRuntime.getSettings().getAdditionalAnnotationStoragePath();
  }

}
