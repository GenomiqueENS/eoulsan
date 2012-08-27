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
 * This class contains all the built-in datatypes for Eoulsan.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class DataTypes {

  private static final DataTypeRegistry registry = DataTypeRegistry
      .getInstance();

  /** Reads datatype. */
  public static final DataType READS = registry.getDataTypeFromName("reads");

  /** Filtered reads datatype. */
  public static final DataType FILTERED_READS = registry
      .getDataTypeFromName("filtered_reads");

  /** SOAP index data type. */
  public static final DataType SOAP_INDEX = registry
      .getDataTypeFromName("soap_index");

  /** BWA index data type. */
  public static final DataType BWA_INDEX = registry
      .getDataTypeFromName("bwa_index");

  /** Bowtie index data type. */
  public static final DataType BOWTIE_INDEX = registry
      .getDataTypeFromName("bowtie_index");

  /** Gmap index data type. */
  public static final DataType GMAP_INDEX = registry
      .getDataTypeFromName("gmap_index");

  /** Filtered Mapper results. */
  public static final DataType FILTERED_MAPPER_RESULTS = registry
      .getDataTypeFromName("filtered_mapper_results");

  /** Mapper results index datatype. */
  public static final DataType FILTERED_MAPPER_RESULTS_INDEX = registry
      .getDataTypeFromName("filtered_mapper_results_index");
  /** Mapper results datatype. */
  public static final DataType MAPPER_RESULTS = registry
      .getDataTypeFromName("mapper_results");

  /** Mapper results index datatype. */
  public static final DataType MAPPER_RESULTS_INDEX = registry
      .getDataTypeFromName("mapper_results_index");

  /** Expression results datatype. */
  public static final DataType EXPRESSION_RESULTS = registry
      .getDataTypeFromName("expression_results");

  /** Annotation datatype. */
  public static final DataType ANNOTATION = registry
      .getDataTypeFromName("annotation");

  /** Annotation datatype. */
  public static final DataType ANNOTATION_INDEX = registry
      .getDataTypeFromName("annotation_index");

  /** Diffana results datatype. */
  public static final DataType DIFFANA_RESULTS = registry
      .getDataTypeFromName("diffana_results");

  /** Genome datatype. */
  public static final DataType GENOME = registry.getDataTypeFromName("genome");

  /** Genome description datatype. */
  public static final DataType GENOME_DESC = registry
      .getDataTypeFromName("genome_desc");

  /** Unmap reads results datatype. */
  public static final DataType UNMAP_READS = registry
      .getDataTypeFromName("unmap");

}
