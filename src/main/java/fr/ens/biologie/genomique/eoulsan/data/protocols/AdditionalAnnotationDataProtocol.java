package fr.ens.biologie.genomique.eoulsan.data.protocols;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.annotations.HadoopCompatible;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import java.util.List;

/**
 * This class define a additional annotation protocol.
 *
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
