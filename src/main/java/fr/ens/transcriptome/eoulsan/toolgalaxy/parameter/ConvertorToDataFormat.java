package fr.ens.transcriptome.eoulsan.toolgalaxy.parameter;

import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;

final class ConvertorToDataFormat {

  // Corresponding to value tag attribut with DataFormat if exist

  static DataFormat convert(final String format) {

    DataFormat dataFormat = null;

    switch (format) {
    case "fastq":
    case "fq":
      dataFormat = DataFormats.READS_FASTQ;
      break;

    case "sam":
      dataFormat = DataFormats.MAPPER_RESULTS_SAM;
      break;

    case "bam":
      dataFormat = DataFormats.MAPPER_RESULTS_BAM;
      break;

    }

    return dataFormat;
  }
}
