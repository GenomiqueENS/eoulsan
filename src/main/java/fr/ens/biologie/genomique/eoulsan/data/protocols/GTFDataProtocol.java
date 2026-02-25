package fr.ens.biologie.genomique.eoulsan.data.protocols;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.annotations.HadoopCompatible;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import java.util.List;

/**
 * This class define a annotation protocol.
 *
 * @since 2.0
 * @author Laurent Jourdren
 */
@HadoopCompatible
public class GTFDataProtocol extends StorageDataProtocol {

  public static final String PROTOCOL_NAME = "gtf";

  @Override
  public String getName() {

    return PROTOCOL_NAME;
  }

  @Override
  protected List<String> getExtensions() {

    return DataFormats.ANNOTATION_GTF.getExtensions();
  }

  @Override
  protected String getBasePath() {

    return EoulsanRuntime.getSettings().getGTFStoragePath();
  }
}
