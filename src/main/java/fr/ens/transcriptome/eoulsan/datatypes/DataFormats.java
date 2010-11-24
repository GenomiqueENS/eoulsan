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

package fr.ens.transcriptome.eoulsan.datatypes;

import fr.ens.transcriptome.eoulsan.checkers.AnnotationChecker;
import fr.ens.transcriptome.eoulsan.checkers.Checker;
import fr.ens.transcriptome.eoulsan.checkers.GenomeChecker;
import fr.ens.transcriptome.eoulsan.checkers.ReadsChecker;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.steps.mapping.SOAPIndexGeneratorStep;

public class DataFormats {

  /** Reads fastq data format. */
  public static final DataFormat READS_FASTQ = new AbstractDataFormat() {

    public DataType getType() {

      return DataTypes.READS;
    }

    @Override
    public String getDefaultExtention() {

      return ".fq";
    }

    @Override
    public String getFormatName() {

      return "reads_fastq";
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
  public static final DataFormat READS_TFQ = new AbstractDataFormat() {

    public DataType getType() {

      return DataTypes.READS;
    }

    @Override
    public String getDefaultExtention() {

      return ".tfq";
    }

    @Override
    public String getFormatName() {

      return "reads_tfq";
    }

  };

  /** Filtered reads fasta data format. */
  public static final DataFormat FILTERED_READS_FASTQ =
      new AbstractDataFormat() {

        public DataType getType() {

          return DataTypes.FILTERED_READS;
        }

        @Override
        public String getDefaultExtention() {

          return ".fq";
        }

        @Override
        public String getFormatName() {

          return "filtered_read_fastq";
        }

      };

  /** SOAP index data format. */
  public static final DataFormat SOAP_INDEX_ZIP = new AbstractDataFormat() {

    public DataType getType() {

      return DataTypes.SOAP_INDEX;
    }

    @Override
    public String getDefaultExtention() {

      return ".zip";
    }

    @Override
    public String getFormatName() {

      return "soap_index_zip";
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

      return new SOAPIndexGeneratorStep();
    }

  };

  /** Filtered SOAP data format. */
  public static final DataFormat FILTERED_SOAP_RESULTS_TXT =
      new AbstractDataFormat() {

        public DataType getType() {

          return DataTypes.FILTERED_SOAP_RESULTS;
        }

        @Override
        public String getDefaultExtention() {

          return ".soap";
        }

        @Override
        public String getFormatName() {

          return "filtered_soap_results";
        }

      };

  /** SOAP results data format. */
  public static final DataFormat SOAP_RESULTS_TXT = new AbstractDataFormat() {

    public DataType getType() {

      return DataTypes.SOAP_RESULTS;
    }

    @Override
    public String getDefaultExtention() {

      return ".soap";
    }

    @Override
    public String getFormatName() {

      return "soap_results";
    }

  };

  /** Expression results data format. */
  public static final DataFormat EXPRESSION_RESULTS_TXT =
      new AbstractDataFormat() {

        public DataType getType() {

          return DataTypes.EXPRESSION_RESULTS;
        }

        @Override
        public String getDefaultExtention() {

          return ".txt";
        }

        @Override
        public String getFormatName() {

          return "expression";
        }

      };

  /** Annotation data format. */
  public static final DataFormat ANNOTATION_GFF = new AbstractDataFormat() {

    public DataType getType() {

      return DataTypes.ANNOTATION;
    }

    @Override
    public String getDefaultExtention() {

      return ".gff";
    }

    @Override
    public String getFormatName() {

      return "annotation";
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
  public static final DataFormat ANNOTATION_SERIAL = new AbstractDataFormat() {

    public DataType getType() {

      return DataTypes.ANNOTATION;
    }

    @Override
    public String getDefaultExtention() {

      return ".data";
    }

    @Override
    public String getFormatName() {

      return "annotation_serial";
    }

    @Override
    public boolean isChecker() {

      return false;
    }

  };

  /** Anadiff results data format. */
  public static final DataFormat ANADIF_RESULTS_TXT = new AbstractDataFormat() {

    public DataType getType() {

      return DataTypes.ANADIF_RESULTS;
    }

    @Override
    public String getDefaultExtention() {

      return ".txt";
    }

    @Override
    public String getFormatName() {

      return "anadiff_results";
    }

  };

  /** Genome data format. */
  public static final DataFormat GENOME_FASTA = new AbstractDataFormat() {

    public DataType getType() {

      return DataTypes.GENOME;
    }

    @Override
    public String getDefaultExtention() {

      return ".fasta";
    }

    @Override
    public String getFormatName() {

      return "genome";
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

  /** Unmap reads results data format. */
  public static final DataFormat UNMAP_READS_FASTA = new AbstractDataFormat() {

    public DataType getType() {

      return DataTypes.UNMAP_READS;
    }

    @Override
    public String getDefaultExtention() {

      return ".fasta";
    }

    @Override
    public String getFormatName() {

      return "unmap";
    }

  };

}
