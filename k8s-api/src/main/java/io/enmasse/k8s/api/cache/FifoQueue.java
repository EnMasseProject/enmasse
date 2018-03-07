/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

import static io.enmasse.k8s.api.cache.FifoQueue.EventType.*;

public class FifoQueue<T> implements WorkQueue<T> {
    private static final Logger log = LoggerFactory.getLogger(FifoQueue.class);
    private final KeyExtractor<T> keyExtractor;
    private final Map<String, T> store = new HashMap<>();
    private final Queue<Event<T>> queue = new LinkedBlockingDeque<>();
    private int initialPopulationCount = 0;
    private boolean populated = false;

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
    public synchronized void pop(Processor<T> processor, long timeout, TimeUnit timeUnit) throws Exception {
        if (queue.isEmpty()) {
            if (timeout > 0) {
                wait(timeUnit.toMillis(timeout));
            }
        }

        if (queue.isEmpty()) {
            log.debug("Woke up but queue is empty");
            return;
        }
        Event<T> event = queue.poll();
        if (event != null) {
            String key = null;
            if (event.obj != null) {
                key = keyExtractor.getKey(event.obj);
            }
            switch (event.eventType) {
                case Deleted:
                    store.remove(key);
                    break;
                case Updated:
                case Added:
                    store.put(key, event.obj);
                    break;
                case Sync:
                    if (initialPopulationCount > 0) {
                        initialPopulationCount--;
                    }
                    break;
            }
            log.debug("Processing event {} with key {}", event.eventType, key);
            processor.process(event.obj);
        }
    }

    private void queueEventLocked(EventType eventType, T obj) {
        populated = true;
        queue.add(new Event<>(eventType, obj));
        notifyAll();
    }

    @Override
    public synchronized boolean hasSynced() {
        return populated && initialPopulationCount == 0;
    }

    @Override
    public synchronized void add(T t) {
        queueEventLocked(Added, t);
    }

    @Override
    public synchronized void update(T t) {
        queueEventLocked(Updated, t);
    }

    @Override
    public synchronized void delete(T t) {
        queueEventLocked(Deleted, t);
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
    public synchronized void replace(List<T> list, String resourceVersion) {
        Map<String, T> newItems = new HashMap<>();
        for (T item : list) {
            String key = keyExtractor.getKey(item);
            newItems.put(key, item);
        }
        log.debug("Replacing queue with {} items. Populated {}", list.size(), populated);
        store.clear();
        store.putAll(newItems);
        if (!populated) {
            initialPopulationCount = 1;
        }
        queueEventLocked(Sync, null);
    }
}
