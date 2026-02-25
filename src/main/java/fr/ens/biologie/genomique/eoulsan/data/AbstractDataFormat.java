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

package fr.ens.biologie.genomique.eoulsan.data;

import com.google.common.base.MoreObjects;
import fr.ens.biologie.genomique.eoulsan.checkers.Checker;
import fr.ens.biologie.genomique.eoulsan.core.Module;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * This class define an abstract data format.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
abstract class AbstractDataFormat implements DataFormat {

  @Override
  public String getDescription() {

    return getName() + " description.";
  }

  @Override
  public List<String> getExtensions() {

    return Collections.singletonList(getDefaultExtension());
  }

  @Override
  public boolean isGenerator() {

    return false;
  }

  @Override
  public boolean isChecker() {

    return false;
  }

  @Override
  public Module getGenerator() {

    return null;
  }

  @Override
  public Checker getChecker() {

    return null;
  }

  @Override
  public String getContentType() {

    return "text/plain";
  }

  @Override
  public int getMaxFilesCount() {
    return 1;
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof DataFormat)) {
      return false;
    }

    final DataFormat that = (DataFormat) o;

    return Objects.equals(this.getName(), that.getName())
        && Objects.equals(this.getDescription(), that.getDescription())
        && Objects.equals(this.getContentType(), that.getContentType())
        && Objects.equals(this.getDefaultExtension(), that.getDefaultExtension())
        && Objects.equals(this.getExtensions(), that.getExtensions())
        && this.isGenerator() == that.isGenerator()
        && this.isChecker() == that.isChecker()
        && ((this.getGenerator() == null && that.getGenerator() == null)
            || (this.getGenerator() != null
                && that.getGenerator() != null
                && Objects.equals(
                    this.getGenerator().getClass().getName(),
                    that.getGenerator().getClass().getName())))
        && ((this.getChecker() == null && that.getChecker() == null)
            || (this.getChecker() != null
                && that.getChecker() != null
                && Objects.equals(
                    this.getChecker().getClass().getName(),
                    that.getChecker().getClass().getName())))
        && this.getMaxFilesCount() == that.getMaxFilesCount();
  }

  @Override
  public int hashCode() {

    final Integer extensionsHashCode = getExtensions() == null ? null : getExtensions().hashCode();
    final Integer generatorHashCode = isGenerator() ? getGenerator().getClass().hashCode() : null;
    final Integer checkerHashCode = isChecker() ? getChecker().getClass().hashCode() : null;

    return Objects.hash(
        getName(),
        getDescription(),
        getContentType(),
        getDefaultExtension(),
        extensionsHashCode,
        isGenerator(),
        isChecker(),
        generatorHashCode,
        checkerHashCode,
        getMaxFilesCount());
  }

  @Override
  public String toString() {

    final Module generator = getGenerator();
    final Checker checker = getChecker();

    return MoreObjects.toStringHelper(this)
        .add("name", getName())
        .add("description", getDescription())
        .add("contentType", getContentType())
        .add("defaultExtension", getDefaultExtension())
        .add("extensions", getExtensions())
        .add(
            "generatorClassName",
            generator != null ? generator.getClass().getCanonicalName() : null)
        .add("checkerClassName", checker != null ? checker.getClass().getCanonicalName() : null)
        .add("maxFilesCount", getMaxFilesCount())
        .toString();
  }
}
