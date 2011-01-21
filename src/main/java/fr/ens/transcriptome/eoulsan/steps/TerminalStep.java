package fr.ens.transcriptome.eoulsan.steps;

import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.design.Design;

/**
 * This class define a terminal step that do nothing. After this execution the
 * workflow will stop.
 * @author Laurent Jourdren
 */
@HadoopCompatible
public class TerminalStep extends AbstractStep {

  @Override
  public String getName() {

    return "Terminal";
  }

  @Override
  public StepResult execute(Design design, Context context) {

    return new StepResult(context, System.currentTimeMillis(), "Terminal step.");
  }

  @Override
  public boolean isTerminalStep() {

    return true;
  }

}
