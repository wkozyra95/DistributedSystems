package agh.edu.pl.sr.zookeeper;


import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

public class Executor implements Watcher, Runnable, DataMonitor.DataMonitorListener {
    private DataMonitor dm;
    private ZooKeeper zk;
    private final String znode;
    private String[] exec;
    private Process child;

    public Executor(String hostPort, String znode) throws KeeperException, IOException, InterruptedException {
        this.znode = znode;
        this.exec = new String[0];
        zk = new ZooKeeper(hostPort, 10000, this);
        dm = new DataMonitor(zk, znode, null, this);
    }

    public static void main(String[] args) throws InterruptedException, IOException, KeeperException {
        String hostPort = "localhost:2183";
        String znode = "/znode_testowy";
        new Executor(hostPort, znode).run();
    }

    @Override
    public void process(WatchedEvent event) {
        dm.process(event);
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            switch (line) {
                case "t":
                    try {
                        show(znode, znode, 0);
                    } catch (KeeperException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    private void show(String child, String path, int indent) throws KeeperException, InterruptedException {
        for (int i = 0; i< indent; i++) {
            System.out.print("-");
        }

        List<String> children = zk.getChildren(path, dm);
        System.out.println(child);

        Executor context = this;
        children.forEach(new Consumer<String>() {
            @Override
            public void accept(String c) {
                try {
                    context.show(c, path + "/" + c, indent + 1);
                } catch (KeeperException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void closing(int rc) {
        synchronized (this) {
            notifyAll();
        }
    }

    static class StreamWriter extends Thread {
        OutputStream os;
        InputStream is;

        StreamWriter(InputStream is, OutputStream os) {
            this.is = is;
            this.os = os;
            start();
        }

        public void run() {
            byte b[] = new byte[80];
            int rc;
            try {
                while ((rc = is.read(b)) > 0) {
                    os.write(b, 0, rc);
                }
            } catch (IOException ignored) {
            }
        }
    }

    public void exists(byte[] data) {
        if (data == null) {
            if (child != null) {
                System.out.println("Killing process");
                child.destroyForcibly();
                child.destroy();
                try {
                    child.waitFor();
                } catch (InterruptedException ignored) {
                }
            }
            child = null;
        } else {
            if (child != null) {
                System.out.println("Stopping child");
                child.destroy();
                try {
                    child.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                zk.getChildren(znode, this);
                child = Runtime.getRuntime().exec(exec);
                System.out.println("Starting child ");
                new StreamWriter(child.getInputStream(), System.out);
                new StreamWriter(child.getErrorStream(), System.err);
            } catch (IOException | InterruptedException | KeeperException e) {
                e.printStackTrace();
            }
        }
    }
}