/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static io.enmasse.k8s.api.cache.FifoQueue.EventType.*;

public class FifoQueue<T> implements WorkQueue<T> {
    private static final Logger log = LoggerFactory.getLogger(FifoQueue.class);
    private final KeyExtractor<T> keyExtractor;
    private final Map<String, T> store = new HashMap<>();
    private final BlockingQueue<Event<T>> queue = new LinkedBlockingDeque<>();
    private AtomicInteger initialPopulationCount = new AtomicInteger(0);
    private volatile boolean populated = false;

    enum EventType {
        Added,
        Updated,
        Deleted,
        Sync
    }

    private static class Event<T> {
        final EventType eventType;
        final T obj;

        private Event(EventType eventType, T obj) {
            this.eventType = eventType;
            this.obj = obj;
        }
    }

    public FifoQueue(KeyExtractor<T> keyExtractor) {
        this.keyExtractor = keyExtractor;
    }

    @Override
    public void pop(Processor<T> processor, long timeout, TimeUnit timeUnit) throws Exception {
        Event<T> event = queue.poll(timeout, timeUnit);
        if (event == null) {
            log.debug("Woke up but queue is empty");
            return;
        }

        List<Event<T>> events = new ArrayList<>();
        queue.drainTo(events);

        String key = null;
        if (event.obj != null) {
            key = keyExtractor.getKey(event.obj);
        }

        if (initialPopulationCount.get() > 0) {
            initialPopulationCount.decrementAndGet();
        }
        log.info("Processing event {} with key {}", event.eventType, key);
        processor.process(event.obj);
    }

    private void queueEvent(EventType eventType, T obj) throws InterruptedException {
        synchronized (this) {
            String key = null;
            if (obj != null) {
                key = keyExtractor.getKey(obj);
            }

            switch (eventType) {
                case Deleted:
                    store.remove(key);
                    break;
                case Updated:
                case Added:
                    store.put(key, obj);
                    break;
            }
        }
        populated = true;
        queue.put(new Event<>(eventType, obj));
    }

    @Override
    public boolean hasSynced() {
        return populated && initialPopulationCount.get() == 0;
    }

    @Override
    public void add(T t) throws InterruptedException {
        queueEvent(Added, t);
    }

    @Override
    public void update(T t) throws InterruptedException {
        queueEvent(Updated, t);
    }

    @Override
    public void delete(T t) throws InterruptedException {
        queueEvent(Deleted, t);
    }

    @Override
    public synchronized List<T> list() {
        return new ArrayList<>(store.values());
    }

    @Override
    public synchronized List<String> listKeys() {
        return new ArrayList<>(store.keySet());
    }

    @Override
    public synchronized void replace(List<T> list, String resourceVersion) throws InterruptedException {
        Map<String, T> newItems = new HashMap<>();
        for (T item : list) {
            String key = keyExtractor.getKey(item);
            newItems.put(key, item);
        }
        log.debug("Replacing queue with {} items. Populated {}", list.size(), populated);
        store.clear();
        store.putAll(newItems);
        if (!populated) {
            initialPopulationCount.set(1);
        }
        queueEvent(Sync, null);
    }
}
