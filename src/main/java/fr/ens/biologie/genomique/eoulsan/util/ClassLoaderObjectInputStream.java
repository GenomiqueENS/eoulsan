package fr.ens.biologie.genomique.eoulsan.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * This class allow to use ObjectInputStream with a ClassLoader that is not the default bootstrap
 * ClassLoader. This is very useful is Hadoop mode.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ClassLoaderObjectInputStream extends ObjectInputStream {

  private ClassLoader classLoader;

  @Override
  protected Class<?> resolveClass(ObjectStreamClass desc)
      throws IOException, ClassNotFoundException {

    try {
      String name = desc.getName();
      return Class.forName(name, false, classLoader);
    } catch (ClassNotFoundException e) {
      return super.resolveClass(desc);
    }
  }

  //
  // Constructors
  //

  /**
   * Constructor, use the thread ClassLoader to load classes of objects to instantiate.
   *
   * @param in input stream
   * @throws IOException if an I/O error occurs while reading stream header
   */
  public ClassLoaderObjectInputStream(final InputStream in) throws IOException {

    this(Thread.currentThread().getContextClassLoader(), in);
  }

  /**
   * Constructor.
   *
   * @param classLoader ClassLoader to use to load classes of objects to instantiate.
   * @param in input stream
   * @throws IOException if an I/O error occurs while reading stream header
   */
  public ClassLoaderObjectInputStream(final ClassLoader classLoader, final InputStream in)
      throws IOException {

    super(in);

    if (classLoader == null) {
      throw new NullPointerException("classLoader argument cannot be null");
    }

    this.classLoader = classLoader;
  }
}
