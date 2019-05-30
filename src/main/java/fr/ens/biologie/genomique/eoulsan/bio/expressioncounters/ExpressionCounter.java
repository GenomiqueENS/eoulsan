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
 * of the Institut de Biologie de l'École normale supérieure and
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

package fr.ens.biologie.genomique.eoulsan.bio.expressioncounters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.GFFEntry;
import fr.ens.biologie.genomique.eoulsan.bio.GenomeDescription;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.util.ReporterIncrementer;
import htsjdk.samtools.SAMRecord;

/**
 * This class define an interface for a wrapper on an ExpressionCounter.
 * @since 1.2
 * @author Claire Wallon
 */
public interface ExpressionCounter {

  //
  // Getters
  //

  /**
   * Get the counter name.
   * @return a string with the counter name
   */
  String getName();

  /**
   * Get the description of the filter.
   * @return the description of the filter
   */
  String getDescription();

  /**
   * Set a parameter of the counter.
   * @param key name of the parameter to set
   * @param value value of the parameter to set
   * @throws EoulsanException if the parameter is invalid
   */
  void setParameter(String key, String value) throws EoulsanException;

  /**
   * Check the counter configuration.
   * @throws EoulsanException if counter configuration is invalid
   */
  void checkConfiguration() throws EoulsanException;

  /**
   * Initialize the counter
   * @param genomeDescFile genome description file
   * @param annotationFile annotation file
   * @param gtfFormat true if the input format is in GTF format
   * @throws EoulsanException if an error occurs while initialize the counter
   * @throws IOException if an error occurs while reading input files
   */
  void init(DataFile genomeDescFile, DataFile annotationFile, boolean gtfFormat)
      throws EoulsanException, IOException;

  /**
   * Initialize the counter
   * @param descIs genome description file
   * @param annotationIs annotation file
   * @param gtfFormat true if the input format is in GTF format
   * @throws EoulsanException if an error occurs while initialize the counter
   * @throws IOException if an error occurs while reading input files
   */
  void init(InputStream descIs, InputStream annotationIs, boolean gtfFormat)
      throws EoulsanException, IOException;

  /**
   * Initialize the counter
   * @param desc genome description
   * @param annotationFile annotation file
   * @param gtfFormat true if the input format is in GTF format
   * @throws EoulsanException if an error occurs while initialize the counter
   * @throws IOException if an error occurs while reading input files
   */
  void init(GenomeDescription desc, DataFile annotationFile, boolean gtfFormat)
      throws EoulsanException, IOException;

  /**
   * Initialize the counter
   * @param desc genome description
   * @param annotationIs annotation file
   * @param gtfFormat true if the input format is in GTF format
   * @throws EoulsanException if an error occurs while initialize the counter
   * @throws IOException if an error occurs while reading the annotation file
   */
  void init(GenomeDescription desc, InputStream annotationIs, boolean gtfFormat)
      throws EoulsanException, IOException;

  /**
   * Initialize the counter
   * @param desc genome description
   * @param annotations annotation entries
   * @throws EoulsanException if an error occurs while initialize the counter
   */
  void init(GenomeDescription desc, Iterable<GFFEntry> annotations)
      throws EoulsanException;

  /**
   * Count the the features.
   * @param samFile SAM file
   * @param reporter the reporter
   * @param counterGroup the counter group of the reporter
   * @return a map with the counts
   * @throws EoulsanException if an error occurs while counting
   * @throws IOException if an error occurs while reading the input file
   */
  public Map<String, Integer> count(DataFile samFile,
      ReporterIncrementer reporter, String counterGroup)
      throws EoulsanException, IOException;

  /**
   * Count the the features.
   * @param inputSam SAM file as an InputStream
   * @param reporter the reporter
   * @param counterGroup the counter group of the reporter
   * @return a map with the counts
   * @throws EoulsanException if an error occurs while counting
   */
  public Map<String, Integer> count(InputStream inputSam,
      ReporterIncrementer reporter, String counterGroup)
      throws EoulsanException;

  /**
   * Count the the features.
   * @param inputSam SAM file as an InputStream
   * @param outputSam SAM file as an OutputStream
   * @param reporter the reporter
   * @param counterGroup the counter group of the reporter
   * @return a map with the counts
   * @throws EoulsanException if an error occurs while counting
   */
  public Map<String, Integer> count(InputStream inputSam,
      OutputStream outputSam, File temporaryDirectory,
      ReporterIncrementer reporter, String counterGroup)
      throws EoulsanException;

  /**
   * Count the the features.
   * @param samRecords SAM entries
   * @param reporter the reporter
   * @param counterGroup the counter group of the reporter
   * @return a map with the counts
   * @throws EoulsanException if an error occurs while counting
   */
  public Map<String, Integer> count(Iterable<SAMRecord> samRecords,
      ReporterIncrementer reporter, String counterGroup)
      throws EoulsanException;

  /**
   * Add missing zero count features.
   * @param counts the counts
   */
  public void addZeroCountFeatures(Map<String, Integer> counts);

}
