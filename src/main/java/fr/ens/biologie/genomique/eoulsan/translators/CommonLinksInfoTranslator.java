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

package fr.ens.biologie.genomique.eoulsan.translators;

import java.util.List;

/**
 * This class define a translator that add commons links information.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class CommonLinksInfoTranslator extends AbstractTranslator {

  private final Translator translator;

  /**
   * Get an ordered list of the translator fields
   * @return an ordered list of the translator fields.
   */
  @Override
  public List<String> getFields() {

    return this.translator.getFields();
  }

  /**
   * Get a translation for a feature
   * @param id Identifier of the feature
   * @param field the field to get
   * @return An array with the annotation of the Feature
   */
  @Override
  public String translateField(final String id, final String field) {

    return this.translator.translateField(id, field);
  }

  /**
   * Test if the link information is available for the field
   * @param field Field to test
   * @return true if link information is available
   */
  @Override
  public boolean isLinkInfo(final String field) {

    if (field == null) {
      return false;
    }

    return field.equals("EnsemblGeneID")
        || field.equals("EntrezGeneID") || field.equals("MGI ID")
        || field.equals("SGDID") || field.equals("Phatr2 Protein HyperLink");
  }

  /**
   * Get link information.
   * @param translatedId Translated id
   * @param field field of the id
   * @return a link for the translated id
   */
  @Override
  public String getLinkInfo(final String translatedId, final String field) {

    if (translatedId == null || field == null) {
      throw new NullPointerException(
          "field and translateId arguments can't be null.");
    }

    if (field.equals("GI")) {
      return "http://www.ncbi.nlm.nih.gov/nuccore/" + translatedId;
    }

    if (field.equals("EnsemblGeneID")) {
      // return
      // "http://www.ensembl.org/Homo_sapiens/Search/Summary?species=all;q="
      return "http://www.ensembl.org/Multi/Search/Results?species=all;q="
          + translatedId;
    }

    if (field.equals("EntrezGeneID")) {
      return "http://www.ncbi.nlm.nih.gov/sites/entrez?Db=gene&Cmd=ShowDetailView&TermToSearch="
          + translatedId;
    }

    if (field.equals("MGI ID") && translatedId.startsWith("MGI:")) {

      final String id = translatedId.substring(4, translatedId.length());

      return "http://www.informatics.jax.org/searches/accession_report.cgi?id=MGI%3A"
          + id;
    }

    // if (field.equals("EntrezID"))
    // return "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?"
    // + "db=gene&cmd=Retrieve&dopt=Graphics&list_uids=" + translatedId;

    if (field.equals("SGDID")) {
      return "http://db.yeastgenome.org/cgi-bin/locus.pl?dbid=" + translatedId;
    }

    if (field.equals("Phatr2 Protein HyperLink")) {
      return "http://genome.jgi-psf.org/cgi-bin/dispGeneModel?db=Phatr2&tid="
          + translatedId;
    }

    return null;
  }

  /**
   * Get the available identifiers by the translator if possible.
   * @return a array of string with the identifiers
   */
  @Override
  public List<String> getIds() {

    return this.translator.getIds();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param translator Translator to use
   */
  public CommonLinksInfoTranslator(final Translator translator) {

    if (translator == null) {
      throw new NullPointerException("Translator can't be null");
    }

    this.translator = translator;
  }
}
