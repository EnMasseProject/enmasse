package enmasse.systemtest;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.LogWatch;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Collects logs from all EnMasse components and saves them to a file
 */
public class LogCollector implements Watcher<Pod>, AutoCloseable {
    private final File logDir;
    private final OpenShift openShift;
    private Watch watch;
    private final List<LogWatch> logWatches = new ArrayList<>();
    private final ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public LogCollector(OpenShift openShift, File logDir) {
        this.openShift = openShift;
        this.logDir = logDir;
        this.watch = openShift.watchPods(this);
    }

    @Override
    public void eventReceived(Action action, Pod pod) {
        switch (action) {
            case ADDED:
                executorService.execute(() -> collectLogs(pod));
                break;
        }
    }

    @Override
    public void onClose(KubernetesClientException cause) {
        if (cause != null) {
            Logging.log.info("LogCollector closed with message: " + cause.getMessage() + ", reconnecting");
            watch = openShift.watchPods(this);
        }
    }

    private void collectLogs(Pod pod) {
        while (!"Running".equals(pod.getStatus().getPhase())) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {

            }
            pod = openShift.getPod(pod.getMetadata().getName());
        }
        for (Container container : pod.getSpec().getContainers()) {
            try {
                FileOutputStream outputFile = new FileOutputStream(new File(logDir, pod.getMetadata().getName() + "." + container.getName()));

                synchronized (logWatches) {
                    logWatches.add(openShift.watchPodLog(pod.getMetadata().getName(), container.getName(), outputFile));
                }
            } catch (Exception e) {
                Logging.log.info("Unable to save log for " + pod.getMetadata().getName() + "." + container.getName());
            }
        }
    }

    @Override
    public void close() throws Exception {
        watch.close();
        executorService.shutdown();
        synchronized (logWatches) {
            for (LogWatch watch : logWatches) {
                watch.close();
            }
        }
    }
}
