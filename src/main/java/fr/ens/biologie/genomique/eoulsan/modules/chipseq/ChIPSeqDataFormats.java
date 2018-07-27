package fr.ens.biologie.genomique.eoulsan.modules.chipseq;

import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;

/**
 * This class contains the definition of some DataFormats for ChiP-Seq.
 * @since 2.3
 * @author Laurent Jourdren
 */
public class ChIPSeqDataFormats {

  private static final DataFormatRegistry registry =
      DataFormatRegistry.getInstance();

  /** Peak format data format. */
  public static final DataFormat PEAK = registry.getDataFormatFromName("peaks");

  /** MACS 2 R model format. */
  public static DataFormat MACS2_RMODEL =
      registry.getDataFormatFromName("macs2rmodel");

  /** Gapped peak format. */
  public static DataFormat GAPPED_PEAK =
      registry.getDataFormatFromName("gappedpeaks");

  /** Peaks XLS format. */
  public static DataFormat PEAK_XLS =
      registry.getDataFormatFromName("peaksxls");

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private ChIPSeqDataFormats() {
    throw new IllegalStateException("Conscrutor cannot be instancied");
  }

}
