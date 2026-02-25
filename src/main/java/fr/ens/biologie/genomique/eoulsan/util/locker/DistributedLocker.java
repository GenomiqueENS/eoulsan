package fr.ens.biologie.genomique.eoulsan.util.locker;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

/**
 * This class implements a locker using Zookeeper based on Twiter's DistributedLock class. See @see
 * <a href=
 * "https://github.com/twitter/commons/blob/master/src/java/com/twitter/common/zookeeper/DistributedLock.java">
 * Twitter Commons code</a>.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DistributedLocker implements Locker, Watcher {

  private final DistributedLock lock;
  private final CountDownLatch connectSignal = new CountDownLatch(1);

  @Override
  public void lock() throws IOException {

    this.lock.lock();
  }

  @Override
  public void unlock() throws IOException {

    this.lock.unlock();
  }

  @Override
  public void process(WatchedEvent event) {

    if (event.getState() == Event.KeeperState.SyncConnected) {
      this.connectSignal.countDown();
    }
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   *
   * @param connectString Zookeeper connection string
   * @param sessionTimeout session time out
   * @param lockBasePath lock base path
   * @param lockName lock name
   * @throws IOException if an error occurs while creating the ZooKeeper connection
   */
  public DistributedLocker(
      final String connectString,
      final int sessionTimeout,
      final String lockBasePath,
      final String lockName)
      throws IOException {

    ZooKeeper zk = new ZooKeeper(connectString, sessionTimeout, this);
    try {
      connectSignal.await();
    } catch (InterruptedException e) {
      throw new IOException(e);
    }

    this.lock = new DistributedLock(zk, lockBasePath + '/' + lockName);
  }
}
