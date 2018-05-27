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

package fr.ens.biologie.genomique.eoulsan.core;

import java.util.List;

import fr.ens.biologie.genomique.eoulsan.util.Utils;

/**
 * Describe a version of a software.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class Version implements Comparable<Version> {

  private static final char SEPARATOR = '.';

  private int major;
  private int minor;
  private int revision;
  private String type = "";

  //
  // Getters
  //

  /**
   * Get the number of major version of the Version.
   * @return The major version
   */
  public int getMajor() {
    return this.major;
  }

  /**
   * Get the number of minor version of the Version.
   * @return The minor version
   */
  public int getMinor() {
    return this.minor;
  }

  /**
   * Get the number of revision of the Version
   * @return the number of revision of the version
   */
  public int getRevision() {
    return this.revision;
  }

  /**
   * Get the type of the Version
   * @return the type of the version
   */
  public String getType() {
    return this.type;
  }

  //
  // Setters
  //

  /**
   * Set the number of major version of the Version.
   * @param major The major version of the version
   */
  public void setMajor(final int major) {
    if (major >= 0) {
      this.major = major;
    } else {
      this.major = 0;
    }
  }

  /**
   * Set the number of minor version of the Version.
   * @param minor The minor version of the version
   */
  public void setMinor(final int minor) {
    if (minor >= 0) {
      this.minor = minor;
    } else {
      this.minor = 0;
    }
  }

  /**
   * Set the number of revision of the Version
   * @param revision The number of revision of the version
   */
  public void setRevision(final int revision) {
    if (revision >= 0) {
      this.revision = revision;
    } else {
      this.revision = 0;
    }
  }

  /**
   * Set the type of the Version
   * @param type The type of revision. The value cannot be null
   */
  public void setType(final String type) {

    if (type == null) {
      this.type = "";
    } else {
      this.type = type.trim();
    }
  }

  //
  // Other methods
  //

  /**
   * Get the version in a string format.
   * @return The version in a string format
   */
  @Override
  public String toString() {

    final StringBuilder sb = new StringBuilder();

    sb.append(getMajor());
    sb.append(SEPARATOR);
    sb.append(getMinor());

    if (getRevision() > 0) {
      sb.append(SEPARATOR);
      sb.append(getRevision());
    }
    sb.append(getType());

    return sb.toString();
  }

  /**
   * Set the version.
   * @param major The major version of the version
   * @param minor The minor version of the version
   * @param revision The number of revision of the version
   */
  public void setVersion(final int major, final int minor, final int revision) {
    setVersion(major, minor, revision, null);
  }

  /**
   * Set the version.
   * @param major The major version of the version
   * @param minor The minor version of the version
   * @param revision The number of revision of the version
   * @param type The type of the version
   */
  public void setVersion(final int major, final int minor, final int revision,
      final String type) {
    setMajor(major);
    setMinor(minor);
    setRevision(revision);
    setType(type);
  }

  /**
   * Set the version.
   * @param version The version to set
   */
  public void setVersion(final String version) {

    if (version == null) {
      return;
    }

    String v = version.trim();

    int fieldCount = 0;
    final StringBuilder sb = new StringBuilder();
    boolean inType = false;

    try {
      for (int i = 0; i < v.length(); i++) {

        final char c = v.charAt(i);

        if (inType || Character.isDigit(c)) {
          sb.append(c);
        } else {

          if (sb.length() > 0) {
            switch (fieldCount) {
            case 0:
              setMajor(Integer.parseInt(sb.toString()));
              break;

            case 1:
              setMinor(Integer.parseInt(sb.toString()));
              break;

            case 2:
              setRevision(Integer.parseInt(sb.toString()));
              inType = true;
              break;

            default:
              break;
            }
            sb.setLength(0);
          }

          if (c == SEPARATOR) {
            fieldCount++;
          } else {
            inType = true;
            sb.append(c);
          }
        }
      }

      if (sb.length() > 0) {
        if (inType) {
          setType(sb.toString());
        } else {

          switch (fieldCount) {
          case 0:
            setMajor(Integer.parseInt(sb.toString()));
            break;

          case 1:
            setMinor(Integer.parseInt(sb.toString()));
            break;

          case 2:
            setRevision(Integer.parseInt(sb.toString()));
            break;

          default:
            break;
          }

        }
      }

    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Invalid version format in string: " + version);
    }
  }

  /**
   * Compare 2 Version object.
   * @param version Version to compare.
   * @return a negative integer, zero, or a positive integer as this object is
   *         less than, equal to, or greater than the specified object.
   */
  @Override
  public int compareTo(final Version version) {

    if (version == null) {
      return 1;
    }

    final int compMajor = Integer.compare(getMajor(), version.getMajor());

    if (compMajor != 0) {
      return compMajor;
    }

    final int compMinor = Integer.compare(getMinor(), version.getMinor());
    if (compMinor != 0) {
      return compMinor;
    }

    final int compRevision =
        Integer.compare(getRevision(), version.getRevision());
    if (compRevision != 0) {
      return compRevision;
    }

    return getType().compareTo(version.getType());
  }

  /**
   * Test if a version is less than the current version.
   * @param version version to test
   * @return true if a version is less than the current version
   */
  public boolean lessThan(final Version version) {

    return compareTo(version) < 0;
  }

  /**
   * Test if a version is less than or equals to the current version.
   * @param version version to test
   * @return true if a version is less than or equals to the current version
   */
  public boolean lessThanOrEqualTo(final Version version) {

    return compareTo(version) <= 0;
  }

  /**
   * Test if a version is greater than the current version.
   * @param version version to test
   * @return true if a version is greater than the current version
   */
  public boolean greaterThan(final Version version) {

    return compareTo(version) > 0;
  }

  /**
   * Test if a version if greater than or equals to the current version.
   * @param version version to test
   * @return true if a version is greater than or equals to the current version
   */
  public boolean greaterThanOrEqualTo(final Version version) {

    return compareTo(version) >= 0;
  }

  /**
   * Get the minimal version from an array of versions.
   * @param versions The array of versions
   * @return The minimal version
   */
  public static Version getMinimalVersion(final List<Version> versions) {

    if (versions == null || versions.size() == 0) {
      return null;
    }

    Version min = versions.get(0);

    for (Version v : versions) {
      if (min.compareTo(v) > 0) {
        min = v;
      }
    }

    return min;
  }

  /**
   * Get the maximal version from an array of versions.
   * @param versions The array of versions
   * @return The maximal version
   */
  public static Version getMaximalVersion(final List<Version> versions) {

    if (versions == null || versions.size() == 0) {
      return null;
    }

    Version max = versions.get(0);

    for (Version v : versions) {
      if (max.compareTo(v) < 0) {
        max = v;
      }
    }

    return max;
  }

  //
  // Object methods overrides
  //

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof Version)) {
      return false;
    }

    final Version v = (Version) o;

    return v.major == this.major
        && v.minor == this.minor && v.revision == this.revision
        && this.type.equals(v.type);
  }

  @Override
  public int hashCode() {

    return Utils.hashCode(this.major, this.minor, this.revision, this.type);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public Version() {

    this(null);
  }

  /**
   * Public constructor.
   * @param major The major version of the version
   * @param minor The minor version of the version
   * @param revision The number of revision of the version
   * @param type The type of the version
   */
  public Version(final int major, final int minor, final int revision,
      final String type) {

    setVersion(major, minor, revision, type);
  }

  /**
   * Public constructor.
   * @param major The major version of the version
   * @param minor The minor version of the version
   * @param revision The number of revision of the version
   */
  public Version(final int major, final int minor, final int revision) {

    setVersion(major, minor, revision);
  }

  /**
   * Public constructor.
   * @param version The version to set
   */
  public Version(final String version) {

    setVersion(version);
  }

}
