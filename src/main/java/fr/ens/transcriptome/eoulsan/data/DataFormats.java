/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.data;

/**
 * This class contains the definition of some DataFormats.
 * @author Laurent Jourdren
 */
public class DataFormats {

  private static final DataFormatRegistry resgistry =
      DataFormatRegistry.getInstance();

  /** Reads fastq data format. */
  public static final DataFormat READS_FASTQ =
      resgistry.getDataFormatFromName(ReadsFastqDataFormat.FORMAT_NAME);

  /** Reads tfq data format. */
  public static final DataFormat READS_TFQ =
      resgistry.getDataFormatFromName(ReadsTfqDataFormat.FORMAT_NAME);

  /** Filtered reads fasta data format. */
  public static final DataFormat FILTERED_READS_FASTQ =
      resgistry.getDataFormatFromName(FilteredReadsFastqDataFormat.FORMAT_NAME);

  /** Filtered reads fasta data format. */
  public static final DataFormat FILTERED_READS_TFQ =
      resgistry.getDataFormatFromName(FilteredReadsTfqDataFormat.FORMAT_NAME);

  /** SOAP index data format. */
  public static final DataFormat SOAP_INDEX_ZIP =
      resgistry.getDataFormatFromName(SOAPIndexZipDataFormat.FORMAT_NAME);

  /** BWA index data format. */
  public static final DataFormat BWA_INDEX_ZIP =
      resgistry.getDataFormatFromName(BWAIndexZipDataFormat.FORMAT_NAME);

  /** BWA index data format. */
  public static final DataFormat BOWTIE_INDEX_ZIP =
      resgistry.getDataFormatFromName(BowtieIndexZipDataFormat.FORMAT_NAME);

  /** Filtered SAM data format. */
  public static final DataFormat FILTERED_MAPPER_RESULTS_SAM =
      resgistry
          .getDataFormatFromName(FilteredMapperResultsSamDataFormat.FORMAT_NAME);

  /** Filtered BAM data format. */
  public static final DataFormat FILTERED_MAPPER_RESULTS_BAM =
      resgistry
          .getDataFormatFromName(FilteredMapperResultsBamDataFormat.FORMAT_NAME);

  /** SAM results data format. */
  public static final DataFormat MAPPER_RESULTS_SAM =
      resgistry.getDataFormatFromName(MapperResultsSamDataFormat.FORMAT_NAME);

  /** BAM results data format. */
  public static final DataFormat MAPPER_RESULTS_BAM =
      resgistry.getDataFormatFromName(MapperResultsBamDataFormat.FORMAT_NAME);

  /** Expression results data format. */
  public static final DataFormat EXPRESSION_RESULTS_TXT =
      resgistry
          .getDataFormatFromName(ExpressionResultsTxtDataFormat.FORMAT_NAME);

  /** Annotation data format. */
  public static final DataFormat ANNOTATION_GFF =
      resgistry.getDataFormatFromName(AnnotationGffDataFormat.FORMAT_NAME);

  /** Annotation data format. */
  public static final DataFormat ANNOTATION_INDEX_SERIAL =
      resgistry
          .getDataFormatFromName(AnnotationIndexSerialDataFormat.FORMAT_NAME);

  /** Anadiff results data format. */
  public static final DataFormat ANADIF_RESULTS_TXT =
      resgistry.getDataFormatFromName(AnadifResultsTxtDataFormat.FORMAT_NAME);

  /** Genome data format. */
  public static final DataFormat GENOME_FASTA =
      resgistry.getDataFormatFromName(GenomeFastaDataFormat.FORMAT_NAME);

  /** Genome data format. */
  public static final DataFormat GENOME_DESC_TXT =
      resgistry.getDataFormatFromName(GenomeDescTxtDataFormat.FORMAT_NAME);

  /** Unmap reads results data format. */
  public static final DataFormat UNMAP_READS_FASTA =
      resgistry.getDataFormatFromName(UnMapReadsFastaDataFormat.FORMAT_NAME);;

}
