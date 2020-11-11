package fr.ens.biologie.genomique.eoulsan.requirements;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Set;

import org.rosuda.REngine.REngineException;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.Progress;
import fr.ens.biologie.genomique.eoulsan.util.r.RSConnection;

/**
 * This class define a Rserve server requirement.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class RserveRequirement extends AbstractRequirement {

  public static final String REQUIREMENT_NAME = "rserve";

  private static final String SERVER_NAME_PARAMETER = "rserve.servername";

  private String serverName;

  @Override
  public String getName() {

    return REQUIREMENT_NAME;
  }

  @Override
  public void configure(Set<Parameter> parameters) throws EoulsanException {

    for (Parameter p : parameters) {

      switch (p.getName()) {

      case SERVER_NAME_PARAMETER:
        this.serverName = p.getValue();
        break;

      default:
        super.configure(Collections.singleton(p));
        break;
      }
    }
  }

  @Override
  public Set<Parameter> getParameters() {

    final Set<Parameter> result = super.getParameters();

    result.add(new Parameter(SERVER_NAME_PARAMETER, this.serverName));

    return Collections.unmodifiableSet(result);
  }

  @Override
  public boolean isAvailable() {

    final RSConnection connection = new RSConnection(this.serverName);

    try {
      connection.getRConnection();
      connection.disConnect();
    } catch (REngineException e) {
      return false;
    }

    return true;
  }

  @Override
  public void install(Progress progress) throws EoulsanException {

    // Do nothing as this requirement is not installable
  }

  //
  // Constructor
  //

  /**
   * Create a new mandatory Rserve requirement.
   * @param rserveServerName the Rserve server name
   */
  public static Requirement newRserveRequirement(
      final String rserveServerName) {

    return newRserveRequirement(rserveServerName, false);
  }

  /**
   * Create a new Rserve requirement.
   * @param rserveServerName the Rserve server name
   * @param optional true if the Rserve server is a mandatory requirement
   */
  public static Requirement newRserveRequirement(final String rserveServerName,
      final boolean optional) {

    requireNonNull(rserveServerName,
        "rserveServerName argument cannot be null");
    checkArgument(!rserveServerName.trim().isEmpty(),
        "rserveServerName argument cannot be empty");

    final RserveRequirement result = new RserveRequirement();

    result.serverName = rserveServerName.trim();
    result.setInstallable(false);
    result.setOptionnal(optional);

    return result;
  }

  //
  // Object method
  //

  @Override
  public String toString() {
    return "Rserve server: " + this.serverName;
  }

}
