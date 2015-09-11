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

package fr.ens.transcriptome.eoulsan.bio.readsfilters;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;

/**
 * This class define a read filter that filter reads with a regex on of the name
 * of the read.
 * @since 1.2
 * @author Laurent Jourdren
 */
public class ReadNameRegexReadFilter extends AbstractReadFilter {

  private Pattern allowedPattern;
  private Pattern forbiddenPattern;

  @Override
  public String getName() {

    return "readnameregex";
  }

  @Override
  public String getDescription() {

    return "Filter reads names with regex";
  }

  @Override
  public void setParameter(final String key, final String value)
      throws EoulsanException {

    if (key == null || value == null) {
      return;
    }

    if ("forbidden.regex".equals(key.trim())) {

      try {
        this.forbiddenPattern = Pattern.compile(value);
      } catch (PatternSyntaxException e) {
        throw new EoulsanException("Invalid forbidden regex expression in "
            + getName() + " read filter: " + value);
      }

    } else if ("allowed.regex".equals(key.trim())) {

      try {
        this.allowedPattern = Pattern.compile(value);
      } catch (PatternSyntaxException e) {
        throw new EoulsanException("Invalid forbidden regex expression in "
            + getName() + " read filter: " + value);
      }

    } else {
      throw new EoulsanException(
          "Unknown parameter for " + getName() + " read filter: " + key);
    }
  }

  @Override
  public boolean accept(final ReadSequence read) {

    if (read == null) {
      return false;
    }

    final String name = read.getName();
    if (name == null) {
      return false;
    }

    if (this.allowedPattern != null
        && !this.allowedPattern.matcher(name).find()) {
      return false;
    }

    if (this.forbiddenPattern != null
        && this.forbiddenPattern.matcher(name).find()) {
      return false;
    }

    return true;
  }

}
