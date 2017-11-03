package fr.ens.biologie.genomique.eoulsan.util.locker;

//=================================================================================================

//Copyright 2011 Twitter, Inc.
//-------------------------------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this work except in compliance with the License.
//You may obtain a copy of the License in the LICENSE file, or at:
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//=================================================================================================

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.concurrent.ThreadSafe;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntimeException;

/**
 * Distributed locking via ZooKeeper. Assuming there are N clients that all try
 * to acquire a lock, the algorithm works as follows. Each host creates an
 * ephemeral|sequential node, and requests a list of children for the lock node.
 * Due to the nature of sequential, all the ids are increasing in order,
 * therefore the client with the least ID according to natural ordering will
 * hold the lock. Every other client watches the id immediately preceding its
 * own id and checks for the lock in case of notification. The client holding
 * the lock does the work and finally deletes the node, thereby triggering the
 * next client in line to acquire the lock. Deadlocks are possible but avoided
 * in most cases because if a client drops dead while holding the lock, the ZK
 * session should timeout and since the node is ephemeral, it will be removed in
 * such a case. Deadlocks could occur if the the worker thread on a client hangs
 * but the zk-client thread is still alive. There could be an external monitor
 * client that ensures that alerts are triggered if the least-id ephemeral node
 * is present past a time-out.
 * <p/>
 * Note: Locking attempts will fail in case session expires!
 * @author Florian Leibert
 */
@ThreadSafe
public class DistributedLock {

  private static final Logger LOG =
      Logger.getLogger(DistributedLock.class.getName());

  /**
   * The magic version number that allows any mutation to always succeed
   * regardless of actual version number.
   */
  public static final int ANY_VERSION = -1;

  private final ZooKeeper zkClient;
  private final String lockPath;
  private final ImmutableList<ACL> acl;

  private final AtomicBoolean aborted = new AtomicBoolean(false);
  private CountDownLatch syncPoint;
  private boolean holdsLock = false;
  private String currentId;
  private String currentNode;
  private String watchedNode;
  private LockWatcher watcher;

  public DistributedLock(ZooKeeper zkClient, String lockPath) {
    this(zkClient, lockPath, ZooDefs.Ids.OPEN_ACL_UNSAFE);
  }

  /**
   * Creates a distributed lock using the given {@code zkClient} to coordinate
   * locking.
   * @param zkClient The ZooKeeper client to use.
   * @param lockPath The path used to manage the lock under.
   * @param acl The acl to apply to newly created lock nodes.
   */
  public DistributedLock(ZooKeeper zkClient, String lockPath,
      Iterable<ACL> acl) {
    this.zkClient = Objects.requireNonNull(zkClient);
    this.lockPath = checkNotBlank(lockPath);
    this.acl = ImmutableList.copyOf(acl);
    this.syncPoint = new CountDownLatch(1);
  }

  private synchronized void prepare()
      throws InterruptedException, KeeperException {

    ensurePath(zkClient, acl, lockPath);
    LOG.log(Level.FINE, "Working with locking path:" + lockPath);

    // Create an EPHEMERAL_SEQUENTIAL node.
    currentNode = zkClient.create(lockPath + "/member_", null, acl,
        CreateMode.EPHEMERAL_SEQUENTIAL);

    // We only care about our actual id since we want to compare ourselves to
    // siblings.
    if (currentNode.contains("/")) {
      currentId = currentNode.substring(currentNode.lastIndexOf("/") + 1);
    }
    LOG.log(Level.FINE, "Received ID from zk:" + currentId);
    this.watcher = new LockWatcher();
  }

  public synchronized void lock() throws IOException {
    if (holdsLock) {
      throw new IOException(
          "Error, already holding a lock. Call unlock first!");
    }
    try {
      prepare();
      watcher.checkForLock();
      syncPoint.await();
      if (!holdsLock) {
        throw new IOException("Error, couldn't acquire the lock!");
      }
    } catch (InterruptedException e) {
      cancelAttempt();
      throw new IOException(
          "InterruptedException while trying to acquire lock!", e);
    } catch (KeeperException e) {
      // No need to clean up since the node wasn't created yet.
      throw new IOException("KeeperException while trying to acquire lock!", e);
    }
  }

  public synchronized boolean tryLock(long timeout, TimeUnit unit)
      throws EoulsanException {
    if (holdsLock) {
      throw new EoulsanException(
          "Error, already holding a lock. Call unlock first!");
    }
    try {
      prepare();
      watcher.checkForLock();
      boolean success = syncPoint.await(timeout, unit);
      if (!success) {
        return false;
      }
      if (!holdsLock) {
        throw new EoulsanException("Error, couldn't acquire the lock!");
      }
    } catch (InterruptedException e) {
      cancelAttempt();
      return false;
    } catch (KeeperException e) {
      // No need to clean up since the node wasn't created yet.
      throw new EoulsanException(
          "KeeperException while trying to acquire lock!", e);
    }
    return true;
  }

  public synchronized void unlock() throws IOException {
    if (currentId == null) {
      throw new IOException(
          "Error, neither attempting to lock nor holding a lock!");
    }
    Objects.requireNonNull(currentId);
    // Try aborting!
    if (!holdsLock) {
      aborted.set(true);
      LOG.log(Level.INFO, "Not holding lock, aborting acquisition attempt!");
    } else {
      LOG.log(Level.INFO, "Cleaning up this locks ephemeral node.");
      cleanup();
    }
  }

  // TODO(Florian Leibert): Make sure this isn't a runtime exception. Put
  // exceptions into the token?

  private synchronized void cancelAttempt() {
    LOG.log(Level.INFO, "Cancelling lock attempt!");
    cleanup();
    // Bubble up failure...
    holdsLock = false;
    syncPoint.countDown();
  }

  private void cleanup() {
    LOG.info("Cleaning up!");
    Objects.requireNonNull(currentId);
    try {
      Stat stat = zkClient.exists(currentNode, false);
      if (stat != null) {
        zkClient.delete(currentNode, ANY_VERSION);
      } else {
        LOG.log(Level.WARNING, "Called cleanup but nothing to cleanup!");
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    holdsLock = false;
    aborted.set(false);
    currentId = null;
    currentNode = null;
    watcher = null;
    syncPoint = new CountDownLatch(1);
  }

  class LockWatcher implements Watcher {

    public synchronized void checkForLock() {
      checkNotBlank(currentId);

      try {
        List<String> candidates = zkClient.getChildren(lockPath, null);
        ImmutableList<String> sortedMembers =
            Ordering.natural().immutableSortedCopy(candidates);

        // Unexpected behavior if there are no children!
        if (sortedMembers.isEmpty()) {
          throw new EoulsanRuntimeException("Error, member list is empty!");
        }

        int memberIndex = sortedMembers.indexOf(currentId);

        // If we hold the lock
        if (memberIndex == 0) {
          holdsLock = true;
          syncPoint.countDown();
        } else {
          final String nextLowestNode = sortedMembers.get(memberIndex - 1);
          LOG.log(Level.INFO,
              String.format(
                  "Current LockWatcher with ephemeral node [%s], is "
                      + "waiting for [%s] to release lock.",
                  currentId, nextLowestNode));

          watchedNode = String.format("%s/%s", lockPath, nextLowestNode);
          Stat stat = zkClient.exists(watchedNode, this);
          if (stat == null) {
            checkForLock();
          }
        }
      } catch (InterruptedException e) {
        LOG.log(Level.WARNING,
            String.format(
                "Current LockWatcher with ephemeral node [%s] "
                    + "got interrupted. Trying to cancel lock acquisition.",
                currentId),
            e);
        cancelAttempt();
      } catch (KeeperException e) {
        LOG.log(Level.WARNING,
            String.format(
                "Current LockWatcher with ephemeral node [%s] "
                    + "got a KeeperException. Trying to cancel lock acquisition.",
                currentId),
            e);
        cancelAttempt();
      }
    }

    @Override
    public synchronized void process(WatchedEvent event) {
      // this handles the case where we have aborted a lock and deleted
      // ourselves but still have a
      // watch on the nextLowestNode. This is a workaround since ZK doesn't
      // support unsub.
      if (!event.getPath().equals(watchedNode)) {
        LOG.log(Level.INFO, "Ignoring call for node:" + watchedNode);
        return;
      }
      // TODO(Florian Leibert): Pull this into the outer class.
      if (event.getType() == Watcher.Event.EventType.None) {
        switch (event.getState()) {
        case SyncConnected:
          // TODO(Florian Leibert): maybe we should just try to "fail-fast" in
          // this case and abort.
          LOG.info("Reconnected...");
          break;
        case Expired:
          LOG.log(Level.WARNING,
              String.format("Current ZK session expired![%s]", currentId));
          cancelAttempt();
          break;
        }
      } else if (event.getType() == Event.EventType.NodeDeleted) {
        checkForLock();
      } else {
        LOG.log(Level.WARNING,
            String.format("Unexpected ZK event: %s", event.getType().name()));
      }
    }
  }

  /**
   * Ensures the given {@code path} exists in the ZK cluster accessed by
   * {@code zkClient}. If the path already exists, nothing is done; however if
   * any portion of the path is missing, it will be created with the given
   * {@code acl} as a persistent zookeeper node. The given {@code path} must be
   * a valid zookeeper absolute path.
   * @param zkClient the client to use to access the ZK cluster
   * @param acl the acl to use if creating path nodes
   * @param path the path to ensure exists
   * @throws InterruptedException if we were interrupted attempting to connect
   *           to the ZK cluster
   * @throws KeeperException if there was a problem in ZK
   */
  private static void ensurePath(ZooKeeper zkClient, List<ACL> acl, String path)
      throws InterruptedException, KeeperException {
    Objects.requireNonNull(zkClient);
    Objects.requireNonNull(path);
    Preconditions.checkArgument(path.startsWith("/"));

    ensurePathInternal(zkClient, acl, path);
  }

  private static void ensurePathInternal(ZooKeeper zkClient, List<ACL> acl,
      String path) throws InterruptedException, KeeperException {
    if (zkClient.exists(path, false) == null) {
      // The current path does not exist; so back up a level and ensure the
      // parent path exists
      // unless we're already a root-level path.
      int lastPathIndex = path.lastIndexOf('/');
      if (lastPathIndex > 0) {
        ensurePathInternal(zkClient, acl, path.substring(0, lastPathIndex));
      }

      // We've ensured our parent path (if any) exists so we can proceed to
      // create our path.
      try {
        zkClient.create(path, null, acl, CreateMode.PERSISTENT);
      } catch (KeeperException.NodeExistsException e) {
        // This ensures we don't die if a race condition was met between
        // checking existence and
        // trying to create the node.
        LOG.info("Node existed when trying to ensure path "
            + path + ", somebody beat us to it?");
      }
    }
  }

  private static String checkNotBlank(String argument) {
    Objects.requireNonNull(argument);
    Preconditions.checkArgument(!argument.trim().isEmpty(),
        "Argument cannot be blank");
    return argument;
  }

}
