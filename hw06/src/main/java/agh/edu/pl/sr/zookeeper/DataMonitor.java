package agh.edu.pl.sr.zookeeper;


import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.Arrays;
import java.util.List;
import java.util.function.ToLongFunction;

public class DataMonitor implements Watcher, StatCallback {
    private ZooKeeper zk;
    private String znode;
    private Watcher chainedWatcher;
    private DataMonitorListener listener;

    byte prevData[];

    public DataMonitor(ZooKeeper zk, String znode, Watcher chainedWatcher,
                       DataMonitorListener listener) throws KeeperException, InterruptedException {
        this.zk = zk;
        this.znode = znode;
        this.chainedWatcher = chainedWatcher;
        this.listener = listener;
        zk.exists(znode, true, this, null);
        try {
            zk.getChildren(znode, this);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public interface DataMonitorListener {
        void exists(byte data[]);
        void closing(int rc);
    }

    @Override
    public void process(WatchedEvent event) {
        String path = event.getPath();
        if (event.getType() == Event.EventType.None) {
            switch (event.getState()) {
                case SyncConnected:
                    break;
                case Expired:
                    listener.closing(KeeperException.Code.SessionExpired);
                    break;
            }
        } else if(event.getType() == Event.EventType.NodeChildrenChanged) {
            try {
                System.out.println(count());
            } catch (KeeperException | InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            if (path != null && path.equals(znode)) {
                zk.exists(znode, true, this, null);
            }
        }
        if (chainedWatcher != null) {
            chainedWatcher.process(event);
        }
    }

    private long count() throws KeeperException, InterruptedException {
        List<String> children = zk.getChildren(znode, this);
        DataMonitor context = this;
        return children.size() + children.stream().mapToLong(new ToLongFunction<String>() {
            @Override
            public long applyAsLong(String child) {
                return context.count(znode + "/" + child);
            }
        }).sum();
    }

    private long count(String path) {
        try {
            List<String> children = zk.getChildren(path, this);
            DataMonitor context = this;
            return children.size() + children.stream().mapToLong(new ToLongFunction<String>() {
                @Override
                public long applyAsLong(String c) {
                    return context.count(path + "/" + c);
                }
            }).sum();
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        boolean exists;
        switch (rc) {
            case Code.Ok:
                exists = true;
                break;
            case Code.NoNode:
                exists = false;
                break;
            case Code.SessionExpired:
            case Code.NoAuth:
                listener.closing(rc);
                return;
            default:
                zk.exists(znode, true, this, null);
                return;
        }

        byte b[] = null;
        if (exists) {
            try {
                b = zk.getData(znode, false, null);
            } catch (KeeperException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        if ((b == null && b != prevData) || (b != null && !Arrays.equals(prevData, b))) {
            listener.exists(b);
            prevData = b;
        }
    }
}