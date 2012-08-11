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

public class DataTypesDemo {

  public static final void main(String[] args) {

    final ClassLoader loader = Thread.currentThread().getContextClassLoader();
    System.out
        .println(loader
            .getResource("META-INF/services/fr.ens.transcriptome.eoulsan.data.XMLDataType"));

    DataTypeRegistry registry = DataTypeRegistry.getInstance();

    for (DataType dt : registry.getAllTypes())
      System.out.println(dt.getName());

    System.out.println();

    // annotation_index.xml
    test("annotation_index", DataTypes.ANNOTATION_INDEX);
    // annotation.xml
    test("annotation", DataTypes.ANNOTATION);
    // bowtie_index.xml
    test("bowtie_index", DataTypes.BOWTIE_INDEX);
    // bwa_index.xml
    test("bwa_index", DataTypes.BWA_INDEX);
    // diffana.xml
    test("diffana_results", DataTypes.DIFFANA_RESULTS);
    // expression.xml
    test("expression", DataTypes.EXPRESSION_RESULTS);
    // filtered_mapper_results_index.xml
    test("filtered_mapper_results_index",
        DataTypes.FILTERED_MAPPER_RESULTS_INDEX);
    // filtered_mapper_results.xml
    test("filtered_mapper_results", DataTypes.FILTERED_MAPPER_RESULTS);
    // filtered_reads.xml
    test("filtered_reads", DataTypes.FILTERED_READS);
    // genome_desc.xml
    test("genome_desc", DataTypes.GENOME_DESC);
    // genome.xml
    test("genome", DataTypes.GENOME);
    // gmap_index.xml
    test("gmap_index", DataTypes.GMAP_INDEX);
    // mapper_results_index.xml
    test("mapper_results_index", DataTypes.MAPPER_RESULTS_INDEX);
    // mapper_results.xml
    test("mapper_results", DataTypes.MAPPER_RESULTS);
    // reads.xml
    test("reads", DataTypes.READS);
    // soap_index.xml
    test("mapper_results", DataTypes.MAPPER_RESULTS);
    // unmap.xml
    test("unmap", DataTypes.UNMAP_READS);
  }

  private static void test(final String name, final DataType dt) {

    final DataType dt2 =
        DataTypeRegistry.getInstance().getDataTypeFromName(name);
    final boolean result = dt.equals(dt2);

    if (!result) {
      System.out.println("class: "
          + dt.getName() + "\t" + dt.getPrefix() + "\t" + dt.getDescription()
          + "\t" + dt.getDesignFieldName() + "\t"
          + dt.isDataTypeFromDesignFile() + "\t" + dt.isOneFilePerAnalysis());
      System.out.println("xml  : "
          + dt2.getName() + "\t" + dt2.getPrefix() + "\t"
          + dt2.getDescription() + "\t" + dt2.getDesignFieldName()
          + dt.isDataTypeFromDesignFile() + "\t" + dt.isOneFilePerAnalysis());
    }

  }

}
