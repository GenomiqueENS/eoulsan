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

package fr.ens.biologie.genomique.eoulsan.data.protocols;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.annotations.ExecutionMode;
import fr.ens.biologie.genomique.kenetre.util.ServiceNameLoader;

/**
 * This class define a service to retrieve a DataProtocol.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class DataProtocolService extends ServiceNameLoader<DataProtocol> {

  private static DataProtocolService service;

  private final FileDataProtocol defaultProtocol = new FileDataProtocol();
  private final String defaultProtocolName = this.defaultProtocol.getName();

  //
  // Static method
  //

  /**
   * Retrieve the singleton static instance of DataProtocolService.
   * @return A DataProtocol instance
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

  @Override
  protected boolean isCache() {

    return true;
  }

  /**
   * Get the default protocol.
   * @return the default DataProtocol
   */
  public FileDataProtocol getDefaultProtocol() {

    return this.defaultProtocol;
  }

  @Override
  public DataProtocol newService(final String serviceName) {

    if (serviceName == null) {
      return null;
    }

    final String lower = serviceName.trim().toLowerCase();

    if (lower.equals(this.defaultProtocolName)) {
      return this.defaultProtocol;
    }

    return super.newService(serviceName);
  }

  @Override
  public ListMultimap<String, String> getServiceClasses() {

    final ListMultimap<String, String> result =
        ArrayListMultimap.create(super.getServiceClasses());

    result.put(this.defaultProtocolName,
        this.defaultProtocol.getClass().getName());

    return Multimaps.unmodifiableListMultimap(result);
  }

  @Override
  public boolean isService(final String serviceName) {

    if (serviceName == null) {
      return false;
    }

    final String lower = serviceName.trim().toLowerCase();

    if (lower.equals(this.defaultProtocolName)) {
      return true;
    }

    return super.isService(serviceName);
  }

  //
  // Protected methods
  //

  @Override
  protected boolean accept(final Class<?> clazz) {

    return ExecutionMode.accept(clazz,
        EoulsanRuntime.getRuntime().getMode().isHadoopProtocolMode());
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
  }

}
