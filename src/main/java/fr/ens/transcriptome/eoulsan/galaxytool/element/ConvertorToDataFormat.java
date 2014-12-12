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
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */
package fr.ens.transcriptome.eoulsan.galaxytool.element;

import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;

// TODO: Auto-generated Javadoc
/**
 * The Class ConvertorToDataFormat.
 * @author Sandrine Perrin
 * @since 2.1
 */
final class ConvertorToDataFormat {

  // Corresponding to value tag attribut with DataFormat if exist

  /**
   * Convert.
   * @param format the format
   * @return the data format
   */
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
