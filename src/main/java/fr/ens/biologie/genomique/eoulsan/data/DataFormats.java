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
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.data;

/**
 * This class contains the definition of some DataFormats.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class DataFormats {

  private static final DataFormatRegistry registry =
      DataFormatRegistry.getInstance();

  /** Reads fastq data format. */
  public static final DataFormat READS_FASTQ =
      registry.getDataFormatFromName("reads_fastq");

  /** Reads tfq data format. */
  public static final DataFormat READS_TFQ =
      registry.getDataFormatFromName("reads_tfq");

  /** SOAP index data format. */
  public static final DataFormat SOAP_INDEX_ZIP =
      registry.getDataFormatFromName("soap_index_zip");

  /** BWA index data format. */
  public static final DataFormat BWA_INDEX_ZIP =
      registry.getDataFormatFromName("bwa_index_zip");

  /** Bowtie index data format. */
  public static final DataFormat BOWTIE_INDEX_ZIP =
      registry.getDataFormatFromName("bowtie_index_zip");

  /** Bowtie2 index data format. */
  public static final DataFormat BOWTIE2_INDEX_ZIP =
      registry.getDataFormatFromName("bowtie2_index_zip");

  /** Gmap index data format. */
  public static final DataFormat GSNAP_INDEX_ZIP =
      registry.getDataFormatFromName("gsnap_index_zip");

  /** STAR index data format. */
  public static final DataFormat STAR_INDEX_ZIP =
      registry.getDataFormatFromName("star_index_zip");

  /** SAM results data format. */
  public static final DataFormat MAPPER_RESULTS_SAM =
      registry.getDataFormatFromName("mapper_results_sam");

  /** BAM results data format. */
  public static final DataFormat MAPPER_RESULTS_BAM =
      registry.getDataFormatFromName("mapper_results_bam");

  /** BAM index results data format. */
  public static final DataFormat MAPPER_RESULTS_INDEX_BAI =
      registry.getDataFormatFromName("mapper_results_index_bai");

  /** Expression results data format. */
  public static final DataFormat EXPRESSION_RESULTS_TSV =
      registry.getDataFormatFromName("expression_results_tsv");

  /** Annotation data format. */
  public static final DataFormat ANNOTATION_GFF =
      registry.getDataFormatFromName("annotation_gff");

  /** Diffana results data format. */
  public static final DataFormat DIFFANA_RESULTS_TSV =
      registry.getDataFormatFromName("diffana_results_tsv");

  /** Genome data format. */
  public static final DataFormat GENOME_FASTA =
      registry.getDataFormatFromName("genome_fasta");

  /** Genome data format. */
  public static final DataFormat GENOME_DESC_TXT =
      registry.getDataFormatFromName("genome_desc_txt");

  /** Unmap reads results data format. */
  public static final DataFormat UNMAP_READS_FASTA =
      registry.getDataFormatFromName("unmap_fasta");

  /** TSV Annotated expression results data format. */
  public static final DataFormat ANNOTATED_EXPRESSION_RESULTS_TSV =
      registry.getDataFormatFromName("annotated_expression_results_tsv");

  /** ODS Annotated expression results data format. */
  public static final DataFormat ANNOTATED_EXPRESSION_RESULTS_ODS =
      registry.getDataFormatFromName("annotated_expression_results_ods");

  /** XLSX Annotated expression results data format. */
  public static final DataFormat ANNOTATED_EXPRESSION_RESULTS_XLSX =
      registry.getDataFormatFromName("annotated_expression_results_xlsx");

  /** Additional annotation data format. */
  public static final DataFormat ADDITIONAL_ANNOTATION_TSV =
      registry.getDataFormatFromName("additional_annotation_tsv");

  /** FastQC report format. */
  public static final DataFormat FASTQC_REPORT_HTML =
      registry.getDataFormatFromName("fastqc_report_html");

  /** Dummy format. */
  public static final DataFormat DUMMY_TXT =
      registry.getDataFormatFromName("dummy_txt");
}
