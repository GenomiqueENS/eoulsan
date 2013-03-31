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

package fr.ens.transcriptome.eoulsan.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceConfigurationError;

/**
 * This class is a service loader that allow to filter class to retrieve and get
 * service by its name and not by its class name.
 * @author Laurent Jourdren
 * @since 1.3
 */
public abstract class ServiceNameLoader<S> {

  public static final String PREFIX = "META-INF/services/";

  private final Class<S> service;
  private final ClassLoader loader;
  private final Map<String, String> classNames =
      new LinkedHashMap<String, String>();

  /**
   * This method allow to filter class that can be returned by
   * EoulsanServiceLoader.
   * @param clazz class to test
   * @return true if the class is allowed
   */
  protected abstract boolean accept(Class<?> clazz);

  /**
   * Get the method of the class service to use to get the name of the service.
   * @return a string with the method name
   */
  protected abstract String getMethodName();

  /**
   * Reload the list of the available class services.
   */
  public void reload() {

    classNames.clear();

    if (getMethodName() == null) {
      throw new InvalidParameterException("getMethod() cannot return null");
    }

    try {
      for (URL url : Collections.list(this.loader.getResources(PREFIX
          + this.service.getName()))) {

        parseFile(url);
      }
    } catch (IOException e) {
      throw new ServiceConfigurationError(service.getName()
          + ": " + e.getMessage());
    }

  }

  /**
   * Parse a SPI file.
   * @param url URL of the file
   */
  private void parseFile(final URL url) {

    try {
      final BufferedReader br =
          new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));

      String line;
      int count = 0;

      while ((line = br.readLine()) != null) {

        count++;
        line = line.trim();

        // Remove comment lines and empty lines
        if (line.length() == 0 || line.startsWith("#")) {
          continue;
        }

        final String className = line;

        // Check if the class name is valid
        if (!checkClassName(className)) {
          throw new ServiceConfigurationError(service.getName()
              + ": " + url + ":" + count + ": Invalid Java class name");
        }

        final Class<?> clazz;

        // Check if the class exists
        try {
          clazz = Class.forName(className, true, this.loader);

        } catch (ClassNotFoundException e) {
          throw new ServiceConfigurationError(service.getName()
              + ": " + url + ": Class not found: " + className);
        }

        // Check type
        final S obj;
        try {
          obj = service.cast(clazz.newInstance());
        } catch (InstantiationException e) {
          throw new ServiceConfigurationError(service.getName()
              + ": " + url + ": Class cannot be instancied: " + className);
        } catch (IllegalAccessException e) {
          throw new ServiceConfigurationError(service.getName()
              + ": " + url + ": Class cannot be instancied: " + className);
        }

        // Filter classes
        if (!accept(clazz))
          continue;

        final Method m;
        try {
          m = obj.getClass().getMethod(getMethodName());
        } catch (SecurityException e) {
          throw new ServiceConfigurationError(service.getName()
              + ": " + url + ": Method " + getMethodName()
              + "() cannot be instancied in class: " + className);
        } catch (NoSuchMethodException e) {
          throw new ServiceConfigurationError(service.getName()
              + ": " + url + ": Method " + getMethodName()
              + "() cannot be instancied in class: " + className);
        }

        final String name;
        try {
          name = (String) m.invoke(obj);
        } catch (IllegalArgumentException e) {
          throw new ServiceConfigurationError(service.getName()
              + ": " + url + ": Method " + getMethodName()
              + "() cannot be invoked in class: " + className);
        } catch (IllegalAccessException e) {
          throw new ServiceConfigurationError(service.getName()
              + ": " + url + ": Method " + getMethodName()
              + "() cannot be invoked in class: " + className);
        } catch (InvocationTargetException e) {
          throw new ServiceConfigurationError(service.getName()
              + ": " + url + ": Method " + getMethodName()
              + "() cannot be invoked in class: " + className);
        }

        if (!this.classNames.containsKey(name)
            && !this.classNames.containsValue(className))
          this.classNames.put(name, className);
      }

      br.close();
    } catch (IOException e) {
      throw new ServiceConfigurationError(service.getName()
          + ": " + url + ": " + e.getMessage());
    }
  }

  /**
   * Check if the class name is a valid java class name
   * @param className class name to test
   * @return true if the class name is valid
   */
  private static final boolean checkClassName(final String className) {

    if (className == null)
      return false;

    final int len = className.length();
    if (len == 0)
      return false;

    int codePoint = className.codePointAt(0);
    if (!Character.isJavaIdentifierPart(codePoint))
      return false;

    for (int i = 1; i < len; i++) {

      codePoint = className.codePointAt(i);
      if (!Character.isJavaIdentifierPart(codePoint) && codePoint != '.')
        return false;
    }

    return true;
  }

  /**
   * Create a new service from its name.
   * @param serviceName name of the service
   * @return a new object
   */
  public S newService(final String serviceName) {

    if (serviceName == null)
      return null;
    final String serviceNameLower = serviceName.toLowerCase().trim();

    if (this.classNames.containsKey(serviceNameLower)) {

      try {
        final Class<?> clazz =
            Class.forName(this.classNames.get(serviceNameLower), true,
                this.loader);
        return service.cast(clazz.newInstance());
      } catch (InstantiationException e) {
        throw new ServiceConfigurationError(service.getName()
            + ": " + serviceNameLower + " cannot be instancied");
      } catch (IllegalAccessException e) {
        throw new ServiceConfigurationError(service.getName()
            + ": " + serviceNameLower + " cannot be instancied");
      } catch (ClassNotFoundException e) {
        throw new ServiceConfigurationError(service.getName()
            + ": Class for " + serviceNameLower + " cannot be found");
      }

    }

    return null;
  }

  /**
   * Return the list of the available services.
   * @return a Map with the availables services
   */
  public Map<String, String> getServiceClasses() {

    return Collections.unmodifiableMap(this.classNames);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param service service class
   */
  public ServiceNameLoader(final Class<S> service) {

    this(service, null);
  }

  /**
   * Public constructor.
   * @param service service class
   * @param loader class loader to use
   */
  public ServiceNameLoader(final Class<S> service, final ClassLoader loader) {

    if (service == null)
      throw new NullPointerException("The service is null");

    this.service = service;
    this.loader =
        loader == null
            ? Thread.currentThread().getContextClassLoader() : loader;
    reload();
  }

}
