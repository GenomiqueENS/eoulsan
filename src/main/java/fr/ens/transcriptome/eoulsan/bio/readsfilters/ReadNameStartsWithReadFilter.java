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

import java.util.HashSet;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;

/**
 * This class define a read filter that filter reads on the start of the name of
 * the read.
 * @since 1.2
 * @author Laurent Jourdren
 */
public class ReadNameStartsWithReadFilter extends AbstractReadFilter {

  private Set<String> forbiddenPrefixes;
  private Set<String> allowedPrefixes;

  @Override
  public String getName() {

    return "readnamestartwith";
  }

  @Override
  public String getDescription() {

    return "Filter reads names that starts with some prefixes";
  }

  @Override
  public void setParameter(final String key, final String value)
      throws EoulsanException {

    if (key == null || value == null) {
      return;
    }

    if ("forbidden.prefixes".equals(key.trim())) {

      if (this.forbiddenPrefixes == null) {
        this.forbiddenPrefixes = new HashSet<>();
      }

      final String[] prefixes = value.split(",");

      if (prefixes != null) {
        for (String s : prefixes) {
          this.forbiddenPrefixes.add(s.trim());
        }
      }

    } else if ("allowed.prefixes".equals(key.trim())) {

      if (this.allowedPrefixes == null) {
        this.allowedPrefixes = new HashSet<>();
      }

      final String[] prefixes = value.split(",");

      if (prefixes != null) {
        for (String s : prefixes) {
          this.allowedPrefixes.add(s.trim());
        }
      }

    } else {
      throw new EoulsanException("Unknown parameter for "
          + getName() + " read filter: " + key);
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

    if (this.forbiddenPrefixes != null) {
      for (final String prefix : this.forbiddenPrefixes) {
        if (name.startsWith(prefix)) {
          return false;
        }
      }
    }

    if (this.allowedPrefixes != null) {

      for (final String prefix : this.allowedPrefixes) {
        if (name.startsWith(prefix)) {
          return true;
        }
      }

      return false;
    }

    return true;
  }

}
