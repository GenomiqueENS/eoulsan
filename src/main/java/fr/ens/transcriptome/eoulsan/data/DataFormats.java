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

package fr.ens.transcriptome.eoulsan.data;

/**
 * This class contains the definition of some DataFormats.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class DataFormats {

  private static final DataFormatRegistry registry = DataFormatRegistry
      .getInstance();

  /** Reads fastq data format. */
  public static final DataFormat READS_FASTQ = registry
      .getDataFormatFromName(ReadsFastqDataFormat.FORMAT_NAME);

  /** Reads tfq data format. */
  public static final DataFormat READS_TFQ = registry
      .getDataFormatFromName(ReadsTfqDataFormat.FORMAT_NAME);

  /** Filtered reads fasta data format. */
  public static final DataFormat FILTERED_READS_FASTQ = registry
      .getDataFormatFromName(FilteredReadsFastqDataFormat.FORMAT_NAME);

  /** Filtered reads fasta data format. */
  public static final DataFormat FILTERED_READS_TFQ = registry
      .getDataFormatFromName(FilteredReadsTfqDataFormat.FORMAT_NAME);

  /** SOAP index data format. */
  public static final DataFormat SOAP_INDEX_ZIP = registry
      .getDataFormatFromName(SOAPIndexZipDataFormat.FORMAT_NAME);

  /** BWA index data format. */
  public static final DataFormat BWA_INDEX_ZIP = registry
      .getDataFormatFromName(BWAIndexZipDataFormat.FORMAT_NAME);

  /** BWA index data format. */
  public static final DataFormat BOWTIE_INDEX_ZIP = registry
      .getDataFormatFromName(BowtieIndexZipDataFormat.FORMAT_NAME);

  /** Gmap index data format. */
  public static final DataFormat GMAP_INDEX_ZIP = registry
      .getDataFormatFromName(GmapIndexZipDataFormat.FORMAT_NAME);

  /** Filtered SAM data format. */
  public static final DataFormat FILTERED_MAPPER_RESULTS_SAM = registry
      .getDataFormatFromName(FilteredMapperResultsSamDataFormat.FORMAT_NAME);

  /** Filtered BAM data format. */
  public static final DataFormat FILTERED_MAPPER_RESULTS_BAM = registry
      .getDataFormatFromName(FilteredMapperResultsBamDataFormat.FORMAT_NAME);

  /** Filtered BAM index data format. */
  public static final DataFormat FILTERED_MAPPER_RESULTS_BAM_INDEX =
      registry
          .getDataFormatFromName(FilteredMapperResultsBamIndexDataFormat.FORMAT_NAME);

  /**
   * Filtered SAM data format for paired-end data : the two alignments of a read
   * are on the same line, separated by a '£'.
   */
  public static final DataFormat TAB_FILTERED_MAPPER_RESULTS_SAM = registry
      .getDataFormatFromName(TabFilteredMapperResultsSamDataFormat.FORMAT_NAME);

  /** SAM results data format. */
  public static final DataFormat MAPPER_RESULTS_SAM = registry
      .getDataFormatFromName(MapperResultsSamDataFormat.FORMAT_NAME);

  /** BAM results data format. */
  public static final DataFormat MAPPER_RESULTS_BAM = registry
      .getDataFormatFromName(MapperResultsBamDataFormat.FORMAT_NAME);

  /** BAM index results data format. */
  public static final DataFormat MAPPER_RESULTS_BAM_INDEX = registry
      .getDataFormatFromName(MapperResultsBamIndexDataFormat.FORMAT_NAME);

  /** Expression results data format. */
  public static final DataFormat EXPRESSION_RESULTS_TXT = registry
      .getDataFormatFromName(ExpressionResultsTxtDataFormat.FORMAT_NAME);

  /** Annotation data format. */
  public static final DataFormat ANNOTATION_GFF = registry
      .getDataFormatFromName(AnnotationGffDataFormat.FORMAT_NAME);

  /** Annotation data format. */
  public static final DataFormat ANNOTATION_INDEX_SERIAL = registry
      .getDataFormatFromName(AnnotationIndexSerialDataFormat.FORMAT_NAME);

  /** Diffana results data format. */
  public static final DataFormat DIFFANA_RESULTS_TXT = registry
      .getDataFormatFromName(DiffAnaResultsTxtDataFormat.FORMAT_NAME);

  /** Genome data format. */
  public static final DataFormat GENOME_FASTA = registry
      .getDataFormatFromName(GenomeFastaDataFormat.FORMAT_NAME);

  /** Genome data format. */
  public static final DataFormat GENOME_DESC_TXT = registry
      .getDataFormatFromName(GenomeDescTxtDataFormat.FORMAT_NAME);

  /** Unmap reads results data format. */
  public static final DataFormat UNMAP_READS_FASTA = registry
      .getDataFormatFromName(UnMapReadsFastaDataFormat.FORMAT_NAME);;

}
