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

      return "filteredlfastq_";
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

  /** Filtered SOAP results. */
  public static final DataType FILTERED_SOAP_RESULTS = new AbstractDataType() {

    @Override
    public String getName() {

      return "filtered_soap_results";
    }

    @Override
    public String getPrefix() {

      return "filtered_soap_results_";
    }
  };

  /** SOAP results datatype. */
  public static final DataType SOAP_RESULTS = new AbstractDataType() {

    @Override
    public String getName() {

      return "soap_results";
    }

    @Override
    public String getPrefix() {

      return "soap_results_";
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
