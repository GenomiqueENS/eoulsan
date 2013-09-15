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

import static com.google.common.base.Objects.equal;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Objects;

import fr.ens.transcriptome.eoulsan.checkers.Checker;
import fr.ens.transcriptome.eoulsan.core.Step;

/**
 * This class define an abstract data format.
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

    return Collections.singletonList(getDefaultExtention());
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
  public Step getGenerator() {

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

    if (o == this)
      return true;

    if (!(o instanceof DataFormat)) {
      return false;
    }

    final DataFormat that = (DataFormat) o;

    return equal(this.getName(), that.getName())
        && equal(this.getDescription(), that.getDescription())
        && equal(this.getContentType(), that.getContentType())
        && equal(this.getDefaultExtention(), that.getDefaultExtention())
        && equal(this.getExtensions(), that.getExtensions())
        && equal(this.isGenerator(), that.isGenerator())
        && equal(this.isChecker(), that.isChecker())
        && ((this.getGenerator() == null && that.getGenerator() == null) || (this
            .getGenerator() != null && that.getGenerator() != null && equal(
              this.getGenerator().getClass().getName(), that.getGenerator()
                  .getClass().getName())))
        && ((this.getChecker() == null && that.getChecker() == null) || (this
            .getChecker() != null && that.getChecker() != null && equal(this
            .getChecker().getClass().getName(), that.getChecker().getClass()
            .getName())))
        && equal(this.getMaxFilesCount(), that.getMaxFilesCount());
  }

  @Override
  public int hashCode() {

    final Integer extensionsHashCode =
        getExtensions() == null ? null : getExtensions().hashCode();
    final Integer generatorHashCode =
        isGenerator() ? getGenerator().getClass().hashCode() : null;
    final Integer checkerHashCode =
        isChecker() ? getChecker().getClass().hashCode() : null;

    return Objects.hashCode(getName(), getDescription(), getContentType(),
        getDefaultExtention(), extensionsHashCode, isGenerator(), isChecker(),
        generatorHashCode, checkerHashCode, getMaxFilesCount());
  }

  @Override
  public String toString() {

    final Step generator = getGenerator();
    final Checker checker = getChecker();

    return Objects
        .toStringHelper(this)
        .add("name", getName())
        .add("description", getDescription())
        .add("contentType", getContentType())
        .add("defaultExtension", getDefaultExtention())
        .add("extensions", getExtensions())
        .add("generatorClassName",
            generator != null ? generator.getClass().getCanonicalName() : null)
        .add("checkerClassName",
            checker != null ? checker.getClass().getCanonicalName() : null)
        .add("maxFilesCount", getMaxFilesCount()).toString();
  }

}
