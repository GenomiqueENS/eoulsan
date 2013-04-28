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

package fr.ens.transcriptome.eoulsan.data.protocols;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.annotations.AnnotationUtils;
import fr.ens.transcriptome.eoulsan.util.ServiceNameLoader;

/**
 * This class define a service to retrieve a DataProtocol.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class DataProtocolService extends ServiceNameLoader<DataProtocol> {

  private static DataProtocolService service;

  private final boolean hadoopMode;
  private final FileDataProtocol defaultProtocol = new FileDataProtocol();
  private final String defaultProtocolName = defaultProtocol.getName();

  //
  // Static method
  //

  /**
   * Retrieve the singleton static instance of StepService.
   * @return A StepService instance
   */
  public static synchronized DataProtocolService getInstance() {

    if (service == null) {
      service = new DataProtocolService();
    }

    return service;
  }

  //
  // Instance methods
  //

  /**
   * Get the default protocol.
   * @return the default DataProtocol
   */
  public FileDataProtocol getDefaultProtocol() {

    return defaultProtocol;
  }

  @Override
  public DataProtocol newService(String serviceName) {

    if (serviceName == null)
      return null;

    final String lower = serviceName.trim().toLowerCase();

    if (lower.equals(this.defaultProtocolName))
      return this.defaultProtocol;

    return super.newService(serviceName);
  }

  @Override
  public Map<String, String> getServiceClasses() {

    final Map<String, String> result =
        new HashMap<String, String>(super.getServiceClasses());

    result.put(this.defaultProtocolName, this.defaultProtocol.getClass()
        .getName());

    return Collections.unmodifiableMap(result);
  }

  @Override
  public boolean isService(String serviceName) {

    if (serviceName == null)
      return false;

    final String lower = serviceName.trim().toLowerCase();

    if (lower.equals(this.defaultProtocolName))
      return true;

    return super.isService(serviceName);
  }

  //
  // Protected methods
  //

  @Override
  protected boolean accept(final Class<?> clazz) {

    return AnnotationUtils.accept(clazz, this.hadoopMode);
  }

  @Override
  protected String getMethodName() {

    return "getName";
  }

  //
  // Constructor
  //

  /**
   * Private protocol.
   */
  private DataProtocolService() {

    super(DataProtocol.class);
    this.hadoopMode = EoulsanRuntime.getRuntime().isHadoopMode();
  }

}
