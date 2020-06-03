/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package org.apache.activemq.artemis.integration.amqp;

import java.util.concurrent.*;

public class ThreadPoolExecutor {
   public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
      return newFixedThreadPool(1, threadFactory);
   }

    public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
       return new java.util.concurrent.ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), threadFactory) {
          @Override
          protected void afterExecute(Runnable r, Throwable t) {
             super.afterExecute(r, t);
             if (t == null && r instanceof Future<?>) {
                try {
                   Future<?> future = (Future<?>) r;
                   if (future.isDone()) {
                      future.get();
                   }
                } catch (CancellationException ce) {
                   t = ce;
                } catch (ExecutionException ee) {
                   t = ee.getCause();
                } catch (InterruptedException ie) {
                   Thread.currentThread().interrupt();
                }
             }
             if (t != null) {
                if (t instanceof Error) {
                   ActiveMQAMQPLogger.LOGGER.fatalf(t, "Connector service failed");
                   Runtime.getRuntime().halt(1);
                } else {
                   ActiveMQAMQPLogger.LOGGER.warnf(t, "Connector service failed");
                }
             }
          }
       };
    }
}
