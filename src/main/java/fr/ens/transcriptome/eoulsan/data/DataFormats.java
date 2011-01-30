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

import fr.ens.transcriptome.eoulsan.checkers.AnnotationChecker;
import fr.ens.transcriptome.eoulsan.checkers.Checker;
import fr.ens.transcriptome.eoulsan.checkers.GenomeChecker;
import fr.ens.transcriptome.eoulsan.checkers.ReadsChecker;
import fr.ens.transcriptome.eoulsan.steps.GenomeDescriptionGeneratorStep;
import fr.ens.transcriptome.eoulsan.steps.Step;
import fr.ens.transcriptome.eoulsan.steps.mapping.hadoop.ReadsIndexGeneratorStep;

/**
 * This class contains the defintion of some DataFormats.
 * @author Laurent Jourdren
 */
public class DataFormats {

  private static final DataFormatRegistry resgistry = DataFormatRegistry
      .getInstance();

  /** Reads fastq data format. */
  public static final DataFormat READS_FASTQ = resgistry
      .getDataFormatFromName(ReadsFastqDataFormat.FORMAT_NAME);

  public static final class ReadsFastqDataFormat extends AbstractDataFormat {

    public static final String FORMAT_NAME = "reads_fastq";

    public DataType getType() {

      return DataTypes.READS;
    }

    @Override
    public String getDefaultExtention() {

      return ".fq";
    }

    @Override
    public String getFormatName() {

      return FORMAT_NAME;
    }

    @Override
    public boolean isChecker() {

      return true;
    }

    @Override
    public Checker getChecker() {

      return new ReadsChecker();
    }

  };

  /** Reads tfq data format. */
  public static final DataFormat READS_TFQ = resgistry
      .getDataFormatFromName(ReadsTfqDataFormat.FORMAT_NAME);

  public static final class ReadsTfqDataFormat extends AbstractDataFormat {

    public static final String FORMAT_NAME = "reads_tfq";

    public DataType getType() {

      return DataTypes.READS;
    }

    @Override
    public String getDefaultExtention() {

      return ".tfq";
    }

    @Override
    public String getFormatName() {

      return FORMAT_NAME;
    }

  };

  /** Filtered reads fasta data format. */
  public static final DataFormat FILTERED_READS_FASTQ = resgistry
      .getDataFormatFromName(FilteredReadsFastqDataFormat.FORMAT_NAME);

  public static final class FilteredReadsFastqDataFormat extends
      AbstractDataFormat {

    public static final String FORMAT_NAME = "filtered_read_fastq";

    public DataType getType() {

      return DataTypes.FILTERED_READS;
    }

    @Override
    public String getDefaultExtention() {

      return ".fq";
    }

    @Override
    public String getFormatName() {

      return FORMAT_NAME;
    }

  };

  /** Filtered reads fasta data format. */
  public static final DataFormat FILTERED_READS_TFQ = resgistry
      .getDataFormatFromName(FilteredReadsTfqDataFormat.FORMAT_NAME);

  public static final class FilteredReadsTfqDataFormat extends
      AbstractDataFormat {

    public static final String FORMAT_NAME = "filtered_read_tfq";

    public DataType getType() {

      return DataTypes.FILTERED_READS;
    }

    @Override
    public String getDefaultExtention() {

      return ".tfq";
    }

    @Override
    public String getFormatName() {

      return FORMAT_NAME;
    }

  };

  /** SOAP index data format. */
  public static final DataFormat SOAP_INDEX_ZIP = resgistry
      .getDataFormatFromName(SOAPIndexZipDataFormat.FORMAT_NAME);

  public static final class SOAPIndexZipDataFormat extends AbstractDataFormat {

    public static final String FORMAT_NAME = "soap_index_zip";

    public DataType getType() {

      return DataTypes.SOAP_INDEX;
    }

    @Override
    public String getDefaultExtention() {

      return ".zip";
    }

    @Override
    public String getFormatName() {

      return FORMAT_NAME;
    }

    @Override
    public String getContentType() {

      return "application/zip";
    }

    @Override
    public boolean isGenerator() {

      return true;
    }

    @Override
    public Step getGenerator() {

      return new ReadsIndexGeneratorStep("soap");
    }

  };

  /** BWA index data format. */
  public static final DataFormat BWA_INDEX_ZIP = resgistry
      .getDataFormatFromName(BWAIndexZipDataFormat.FORMAT_NAME);

  public static final class BWAIndexZipDataFormat extends AbstractDataFormat {

    public static final String FORMAT_NAME = "bwa_index_zip";

    public DataType getType() {

      return DataTypes.BWA_INDEX;
    }

    @Override
    public String getDefaultExtention() {

      return ".zip";
    }

    @Override
    public String getFormatName() {

      return FORMAT_NAME;
    }

    @Override
    public String getContentType() {

      return "application/zip";
    }

    @Override
    public boolean isGenerator() {

      return true;
    }

    @Override
    public Step getGenerator() {

      return new ReadsIndexGeneratorStep("bwa");
    }

  };

  /** BWA index data format. */
  public static final DataFormat BOWTIE_INDEX_ZIP = resgistry
      .getDataFormatFromName(BowtieIndexZipDataFormat.FORMAT_NAME);

  public static final class BowtieIndexZipDataFormat extends AbstractDataFormat {

    public static final String FORMAT_NAME = "bowtie_index_zip";

    public DataType getType() {

      return DataTypes.BOWTIE_INDEX;
    }

    @Override
    public String getDefaultExtention() {

      return ".zip";
    }

    @Override
    public String getFormatName() {

      return FORMAT_NAME;
    }

    @Override
    public String getContentType() {

      return "application/zip";
    }

    @Override
    public boolean isGenerator() {

      return true;
    }

    @Override
    public Step getGenerator() {

      return new ReadsIndexGeneratorStep("bowtie");
    }

  };

  /** Filtered SAM data format. */
  public static final DataFormat FILTERED_MAPPER_RESULTS_SAM = resgistry
      .getDataFormatFromName(FilteredMapperResultsSamDataFormat.FORMAT_NAME);

  public static final class FilteredMapperResultsSamDataFormat extends
      AbstractDataFormat {

    public static final String FORMAT_NAME = "filtered_sam_results";

    public DataType getType() {

      return DataTypes.FILTERED_MAPPER_RESULTS;
    }

    @Override
    public String getDefaultExtention() {

      return ".sam";
    }

    @Override
    public String getFormatName() {

      return FORMAT_NAME;
    }

  };

  /** Filtered BAM data format. */
  public static final DataFormat FILTERED_MAPPER_RESULTS_BAM = resgistry
      .getDataFormatFromName(FilteredMapperResultsBamDataFormat.FORMAT_NAME);

  public static final class FilteredMapperResultsBamDataFormat extends
      AbstractDataFormat {

    public static final String FORMAT_NAME = "filtered_bam_results";

    public DataType getType() {

      return DataTypes.FILTERED_MAPPER_RESULTS;
    }

    @Override
    public String getDefaultExtention() {

      return ".bam";
    }

    @Override
    public String getFormatName() {

      return FORMAT_NAME;
    }

  };

  /** SAM results data format. */
  public static final DataFormat MAPPER_RESULTS_SAM = resgistry
      .getDataFormatFromName(MapperResultsSamDataFormat.FORMAT_NAME);

  public static final class MapperResultsSamDataFormat extends
      AbstractDataFormat {

    public static final String FORMAT_NAME = "sam_results";

    public DataType getType() {

      return DataTypes.MAPPER_RESULTS;
    }

    @Override
    public String getDefaultExtention() {

      return ".sam";
    }

    @Override
    public String getFormatName() {

      return FORMAT_NAME;
    }

  };

  /** BAM results data format. */
  public static final DataFormat MAPPER_RESULTS_BAM = resgistry
      .getDataFormatFromName(MapperResultsBamDataFormat.FORMAT_NAME);

  public static final class MapperResultsBamDataFormat extends
      AbstractDataFormat {

    public static final String FORMAT_NAME = "bam_results";

    public DataType getType() {

      return DataTypes.MAPPER_RESULTS;
    }

    @Override
    public String getDefaultExtention() {

      return ".bam";
    }

    @Override
    public String getFormatName() {

      return FORMAT_NAME;
    }

  };

  /** Expression results data format. */
  public static final DataFormat EXPRESSION_RESULTS_TXT = resgistry
      .getDataFormatFromName(ExpressionResultsTxtDataFormat.FORMAT_NAME);

  public static final class ExpressionResultsTxtDataFormat extends
      AbstractDataFormat {

    public static final String FORMAT_NAME = "expression";

    public DataType getType() {

      return DataTypes.EXPRESSION_RESULTS;
    }

    @Override
    public String getDefaultExtention() {

      return ".txt";
    }

    @Override
    public String getFormatName() {

      return FORMAT_NAME;
    }

  };

  /** Annotation data format. */
  public static final DataFormat ANNOTATION_GFF = resgistry
      .getDataFormatFromName(AnnotationGffDataFormat.FORMAT_NAME);

  public static final class AnnotationGffDataFormat extends AbstractDataFormat {

    public static final String FORMAT_NAME = "annotation";

    public DataType getType() {

      return DataTypes.ANNOTATION;
    }

    @Override
    public String getDefaultExtention() {

      return ".gff";
    }

    @Override
    public String getFormatName() {

      return FORMAT_NAME;
    }

    @Override
    public boolean isChecker() {

      return true;
    }

    @Override
    public Checker getChecker() {

      return new AnnotationChecker();
    }

  };

  /** Annotation data format. */
  public static final DataFormat ANNOTATION_INDEX_SERIAL = resgistry
      .getDataFormatFromName(AnnotationIndexSerialDataFormat.FORMAT_NAME);

  public static final class AnnotationIndexSerialDataFormat extends
      AbstractDataFormat {

    public static final String FORMAT_NAME = "annotation_index_serial";

    public DataType getType() {

      return DataTypes.ANNOTATION_INDEX;
    }

    @Override
    public String getDefaultExtention() {

      return ".ser";
    }

    @Override
    public String getContentType() {

      return "application/java-serialized-object";
    }

    @Override
    public String getFormatName() {

      return FORMAT_NAME;
    }

    @Override
    public boolean isChecker() {

      return false;
    }

  };

  /** Anadiff results data format. */
  public static final DataFormat ANADIF_RESULTS_TXT = resgistry
      .getDataFormatFromName(AnafifResultsTxtDataFormat.FORMAT_NAME);

  public static final class AnafifResultsTxtDataFormat extends
      AbstractDataFormat {

    public static final String FORMAT_NAME = "anadiff_results";

    public DataType getType() {

      return DataTypes.ANADIF_RESULTS;
    }

    @Override
    public String getDefaultExtention() {

      return ".txt";
    }

    @Override
    public String getFormatName() {

      return FORMAT_NAME;
    }

  };

  /** Genome data format. */
  public static final DataFormat GENOME_FASTA = resgistry
      .getDataFormatFromName(GenomeFastaDataFormat.FORMAT_NAME);

  public static final class GenomeFastaDataFormat extends AbstractDataFormat {

    public static final String FORMAT_NAME = "genome";

    public DataType getType() {

      return DataTypes.GENOME;
    }

    @Override
    public String getDefaultExtention() {

      return ".fasta";
    }

    @Override
    public String getFormatName() {

      return FORMAT_NAME;
    }

    @Override
    public boolean isChecker() {

      return true;
    }

    @Override
    public Checker getChecker() {

      return new GenomeChecker();
    }

  };

  /** Genome data format. */
  public static final DataFormat GENOME_DESC_TXT = resgistry
      .getDataFormatFromName(GenomeDescTxtDataFormat.FORMAT_NAME);

  public static final class GenomeDescTxtDataFormat extends AbstractDataFormat {

    public static final String FORMAT_NAME = "genome_desc_txt";

    public DataType getType() {

      return DataTypes.GENOME_DESC;
    }

    @Override
    public String getDefaultExtention() {

      return ".txt";
    }

    @Override
    public String getFormatName() {

      return FORMAT_NAME;
    }

    @Override
    public Step getGenerator() {

      return new GenomeDescriptionGeneratorStep();
    }

    @Override
    public boolean isGenerator() {

      return true;
    }

  };

  /** Unmap reads results data format. */
  public static final DataFormat UNMAP_READS_FASTA = resgistry
      .getDataFormatFromName(UnMapReadsFastaDataFormat.FORMAT_NAME);

  public static final class UnMapReadsFastaDataFormat extends
      AbstractDataFormat {

    public static final String FORMAT_NAME = "unmap_fasta";

    public DataType getType() {

      return DataTypes.UNMAP_READS;
    }

    @Override
    public String getDefaultExtention() {

      return ".fasta";
    }

    @Override
    public String getFormatName() {

      return FORMAT_NAME;
    }

  };

}
