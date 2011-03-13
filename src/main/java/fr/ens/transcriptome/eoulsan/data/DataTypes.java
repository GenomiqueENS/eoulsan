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
 * This class contains all the built-in datatypes for Eoulsan:
 * @author Laurent Jourdren
 */
public class DataTypes {

  /** Reads datatype. */
  public static final DataType READS = new AbstractDataType() {

    @Override
    public String getName() {

      return "reads";
    }

    @Override
    public String getPrefix() {

      return "reads_";
    }

    @Override
    public boolean isDataTypeFromDesignFile() {

      return true;
    }

  };

  /** Filtered reads datatype. */
  public static final DataType FILTERED_READS = new AbstractDataType() {

    @Override
    public String getName() {

      return "filtered read";
    }

    @Override
    public String getPrefix() {

      return "filtered_reads_";
    }

  };

  /** SOAP index data type. */
  public static final DataType SOAP_INDEX = new AbstractDataType() {

    @Override
    public String getName() {

      return "soap_index";
    }

    @Override
    public String getPrefix() {

      return "soap_index_";
    }

    @Override
    public boolean isOneFilePerAnalysis() {

      return true;
    }
  };

  /** BWA index data type. */
  public static final DataType BWA_INDEX = new AbstractDataType() {

    @Override
    public String getName() {

      return "bwa_index";
    }

    @Override
    public String getPrefix() {

      return "bwa_index_";
    }

    @Override
    public boolean isOneFilePerAnalysis() {

      return true;
    }
  };

  /** Bowtie index data type. */
  public static final DataType BOWTIE_INDEX = new AbstractDataType() {

    @Override
    public String getName() {

      return "bowtie_index";
    }

    @Override
    public String getPrefix() {

      return "bowtie_index_";
    }

    @Override
    public boolean isOneFilePerAnalysis() {

      return true;
    }
  };

  /** Filtered Mapper results. */
  public static final DataType FILTERED_MAPPER_RESULTS =
      new AbstractDataType() {

        @Override
        public String getName() {

          return "filtered_mapper_results";
        }

        @Override
        public String getPrefix() {

          return "filtered_mapper_results_";
        }
      };

  /** Mapper results index datatype. */
  public static final DataType FILTERED_MAPPER_RESULTS_INDEX =
      new AbstractDataType() {

        @Override
        public String getName() {

          return "filtered_mapper_results_index";
        }

        @Override
        public String getPrefix() {

          return "filtered_mapper_results_index_";
        }
      };

  /** Mapper results datatype. */
  public static final DataType MAPPER_RESULTS = new AbstractDataType() {

    @Override
    public String getName() {

      return "mapper_results";
    }

    @Override
    public String getPrefix() {

      return "mapper_results_";
    }
  };

  /** Mapper results index datatype. */
  public static final DataType MAPPER_RESULTS_INDEX = new AbstractDataType() {

    @Override
    public String getName() {

      return "mapper_results_index";
    }

    @Override
    public String getPrefix() {

      return "mapper_results_index_";
    }
  };

  /** Expression results datatype. */
  public static final DataType EXPRESSION_RESULTS = new AbstractDataType() {

    @Override
    public String getName() {

      return "expression";
    }

    @Override
    public String getPrefix() {

      return "expression_";
    }
  };

  /** Annotation datatype. */
  public static final DataType ANNOTATION = new AbstractDataType() {

    @Override
    public String getName() {

      return "annotation";
    }

    @Override
    public String getPrefix() {

      return "annotation_";
    }

    @Override
    public boolean isDataTypeFromDesignFile() {

      return true;
    }

    @Override
    public boolean isOneFilePerAnalysis() {

      return true;
    }

  };

  /** Annotation datatype. */
  public static final DataType ANNOTATION_INDEX = new AbstractDataType() {

    @Override
    public String getName() {

      return "annotation_index";
    }

    @Override
    public String getPrefix() {

      return "annotation_index_";
    }

    @Override
    public boolean isDataTypeFromDesignFile() {

      return false;
    }

    @Override
    public boolean isOneFilePerAnalysis() {

      return true;
    }

  };

  /** Anadiff results datatype. */
  public static final DataType ANADIF_RESULTS = new AbstractDataType() {

    @Override
    public String getName() {

      return "anadiff_results";
    }

    @Override
    public String getPrefix() {

      return "anadiff_";
    }

    @Override
    public boolean isOneFilePerAnalysis() {

      return true;
    }
  };

  /** Genome datatype. */
  public static final DataType GENOME = new AbstractDataType() {

    @Override
    public String getName() {

      return "genome";
    }

    @Override
    public String getPrefix() {

      return "genome_";
    }

    @Override
    public boolean isDataTypeFromDesignFile() {

      return true;
    }

    @Override
    public boolean isOneFilePerAnalysis() {

      return true;
    }

  };

  /** Genome description datatype. */
  public static final DataType GENOME_DESC = new AbstractDataType() {

    @Override
    public String getName() {

      return "genome_desc";
    }

    @Override
    public String getPrefix() {

      return "genome_desc_";
    }

    @Override
    public boolean isOneFilePerAnalysis() {

      return true;
    }

  };

  /** Unmap reads results datatype. */
  public static final DataType UNMAP_READS = new AbstractDataType() {

    @Override
    public String getName() {

      return "unmap";
    }

    @Override
    public String getPrefix() {

      return "unmap_";
    }
  };

}
