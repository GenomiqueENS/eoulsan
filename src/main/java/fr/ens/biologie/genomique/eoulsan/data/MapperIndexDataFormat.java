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

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.common.base.MoreObjects;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.checkers.Checker;
import fr.ens.biologie.genomique.eoulsan.core.Module;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.modules.generators.GenomeMapperIndexGeneratorModule;
import fr.ens.biologie.genomique.eoulsan.splitermergers.Merger;
import fr.ens.biologie.genomique.eoulsan.splitermergers.Splitter;
import fr.ens.biologie.genomique.kenetre.bio.readmapper.Mapper;

/**
 * This class define a DataFormat from an XML file.
 * @since 2.6
 * @author Laurent Jourdren
 */
public class MapperIndexDataFormat extends AbstractDataFormat
    implements Serializable {

  private static final long serialVersionUID = -943794645213547885L;

  private final String name;
  private final String prefix;

  //
  // Getters
  //

  @Override
  public String getName() {

    return this.name;
  }

  @Override
  public String getAlias() {

    return null;
  }

  @Override
  public String getPrefix() {

    return this.prefix;
  }

  @Override
  public boolean isOneFilePerAnalysis() {

    return true;
  }

  @Override
  public boolean isDataFormatFromDesignFile() {

    return false;
  }

  @Override
  public String getDesignMetadataKeyName() {

    return null;
  }

  @Override
  public String getSampleMetadataKeyName() {

    return null;
  }

  @Override
  public String getDefaultExtension() {

    return ".zip";
  }

  @Override
  public List<String> getExtensions() {

    return singletonList(".zip");
  }

  @Override
  public List<String> getGalaxyFormatNames() {
    return Collections.emptyList();
  }

  @Override
  public boolean isGenerator() {

    return true;
  }

  @Override
  public boolean isChecker() {

    return false;
  }

  @Override
  public boolean isSplitter() {

    return false;
  }

  @Override
  public boolean isMerger() {

    return false;
  }

  @Override
  public Module getGenerator() {

    final Module generator = new GenomeMapperIndexGeneratorModule();

    Parameter mapperNameParameter = new Parameter("mapperName", this.name);

    try {
      generator.configure(null, Collections.singleton(mapperNameParameter));

      return generator;
    } catch (EoulsanException e) {

      getLogger().severe("Cannot create generator: " + e.getMessage());
      return null;
    }
  }

  @Override
  public Checker getChecker() {

    return null;
  }

  @Override
  public Splitter getSplitter() {

    return null;
  }

  @Override
  public Merger getMerger() {

    return null;
  }

  @Override
  public String getContentType() {

    return "application/zip";
  }

  @Override
  public int getMaxFilesCount() {
    return 1;
  }

  //
  // Object methods
  //

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof DataFormat)) {
      return false;
    }

    if (!(o instanceof MapperIndexDataFormat)) {
      return super.equals(o);
    }

    final MapperIndexDataFormat that = (MapperIndexDataFormat) o;

    return Objects.equals(this.name, that.name);
  }

  @Override
  public int hashCode() {

    return Objects.hash(this.name);
  }

  @Override
  public String toString() {

    return MoreObjects.toStringHelper(this).add("name", this.name).toString();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param mapperName name of the mapper
   */
  public MapperIndexDataFormat(Mapper mapper) {

    this(mapper.getName());
  }

  /**
   * Public constructor.
   * @param mapperName name of the mapper
   */
  public MapperIndexDataFormat(String mapperName) {

    requireNonNull(mapperName);

    String mapperNameLowerCase = mapperName.toLowerCase();

    this.name = mapperNameLowerCase + "_index_zip";
    this.prefix = mapperNameLowerCase + "index";
  }

}
