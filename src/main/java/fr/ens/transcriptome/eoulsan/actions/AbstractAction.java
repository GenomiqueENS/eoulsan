package fr.ens.transcriptome.eoulsan.actions;

import fr.ens.transcriptome.eoulsan.util.SystemUtils;

/**
 * This class define an abstract Action
 * @author Laurent Jourdren
 */
public abstract class AbstractAction implements Action {

  @Override
  public boolean isHadoopJarMode() {

    return false;
  }

  @Override
  public boolean isCurrentArchCompatible() {

    return SystemUtils.isApplicationAvailableForCurrentArch();
  }

  @Override
  public boolean isHidden() {

    return false;
  }

}
