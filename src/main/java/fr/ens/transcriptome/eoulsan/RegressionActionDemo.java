package fr.ens.transcriptome.eoulsan;

import java.io.IOException;

import fr.ens.transcriptome.eoulsan.actions.Action;
import fr.ens.transcriptome.eoulsan.actions.ActionService;
import fr.ens.transcriptome.eoulsan.actions.RegressionAction;

public class RegressionActionDemo {

  /**
   * Logger
   * @throws IOException
   */

  public static void main(String[] args) throws EoulsanException, IOException {

    RegressionActionDemo.mainbis();

  }

  public static void mainbis() {

    final String confpath =
        "/home/sperrin/Documents/test_eoulsan/tests_fonctionnels/test_fonctionnel.conf";

    final String fileTests =
        "/home/sperrin/Documents/test_eoulsan/tests_fonctionnels/list_tests.txt";

    // Set the default local for all the application
    Globals.setDefaultLocale();

    Action action0 = ActionService.getInstance().getAction("regression");
    RegressionAction action = (RegressionAction) action0;

    // Run action
    action.action(new String[] {"-c", confpath, "-generate", "new", "-exec",
        "/home/sperrin/home-net/eoulsan-1.2.2"});

    // action.action(new String[] {"-c", confpath, "-generate", "all", "-exec",
    // "/home/sperrin/home-net/eoulsan-1.2.2"});

    // action.action(new String[] {"-c", confpath, "-exec",
    // "/home/sperrin/home-net/eoulsan_newReadsMappers", "-f", fileTests,});

    // action.action(new String[] {"-c", confpath, "-exec",
    // "/home/sperrin/home-net/eoulsan_newReadsMappers"});
  }
}
