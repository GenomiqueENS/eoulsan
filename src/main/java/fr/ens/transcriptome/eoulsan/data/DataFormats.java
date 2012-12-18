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
      .getDataFormatFromName("reads_fastq");

  /** Reads tfq data format. */
  public static final DataFormat READS_TFQ = registry
      .getDataFormatFromName("reads_tfq");

  /** Filtered reads fasta data format. */
  public static final DataFormat FILTERED_READS_FASTQ = registry
      .getDataFormatFromName("filtered_reads_fastq");

  /** Filtered reads fasta data format. */
  public static final DataFormat FILTERED_READS_TFQ = registry
      .getDataFormatFromName("filtered_reads_tfq");

  /** SOAP index data format. */
  public static final DataFormat SOAP_INDEX_ZIP = registry
      .getDataFormatFromName("soap_index_zip");

  /** BWA index data format. */
  public static final DataFormat BWA_INDEX_ZIP = registry
      .getDataFormatFromName("bwa_index_zip");

  /** Bowtie index data format. */
  public static final DataFormat BOWTIE_INDEX_ZIP = registry
      .getDataFormatFromName("bowtie_index_zip");

  /** Bowtie2 index data format. */
  public static final DataFormat BOWTIE2_INDEX_ZIP = registry
      .getDataFormatFromName("bowtie2_index_zip");

  /** Gmap index data format. */
  public static final DataFormat GSNAP_INDEX_ZIP = registry
      .getDataFormatFromName("gsnap_index_zip");

  /** Filtered SAM data format. */
  public static final DataFormat FILTERED_MAPPER_RESULTS_SAM = registry
      .getDataFormatFromName("filtered_mapper_results_sam");

  /** Filtered BAM data format. */
  public static final DataFormat FILTERED_MAPPER_RESULTS_BAM = registry
      .getDataFormatFromName("filtered_mapper_results_bam");

  /** Filtered BAM index data format. */
  public static final DataFormat FILTERED_MAPPER_RESULTS_INDEX_BAI = registry
      .getDataFormatFromName("filtered_mapper_results_index_bai");

  /**
   * Filtered SAM data format for paired-end data : the two alignments of a read
   * are on the same line, separated by a '£'.
   */
  public static final DataFormat FILTERED_MAPPER_RESULTS_TSAM = registry
      .getDataFormatFromName("filtered_mapper_results_tsam");

  /** SAM results data format. */
  public static final DataFormat MAPPER_RESULTS_SAM = registry
      .getDataFormatFromName("mapper_results_sam");

  /** BAM results data format. */
  public static final DataFormat MAPPER_RESULTS_BAM = registry
      .getDataFormatFromName("mapper_results_bam");

  /** BAM index results data format. */
  public static final DataFormat MAPPER_RESULTS_INDEX_BAI = registry
      .getDataFormatFromName("mapper_results_index_bai");

  /** Expression results data format. */
  public static final DataFormat EXPRESSION_RESULTS_TXT = registry
      .getDataFormatFromName("expression_results_tsv");

  /** Annotation data format. */
  public static final DataFormat ANNOTATION_GFF = registry
      .getDataFormatFromName("annotation_gff");

  /** Annotation data format. */
  public static final DataFormat ANNOTATION_INDEX_SERIAL = registry
      .getDataFormatFromName("annotation_index_serial");

  /** Diffana results data format. */
  public static final DataFormat DIFFANA_RESULTS_TSV = registry
      .getDataFormatFromName("diffana_results_tsv");

  /** Genome data format. */
  public static final DataFormat GENOME_FASTA = registry
      .getDataFormatFromName("genome_fasta");

  /** Genome data format. */
  public static final DataFormat GENOME_DESC_TXT = registry
      .getDataFormatFromName("genome_desc_txt");

  /** Unmap reads results data format. */
  public static final DataFormat UNMAP_READS_FASTA = registry
      .getDataFormatFromName("unmap_fasta");

}
