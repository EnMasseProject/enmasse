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

import static io.enmasse.k8s.api.cache.EventCache.EventType.*;

public class EventCache<T> implements WorkQueue<T> {
    private static final Logger log = LoggerFactory.getLogger(EventCache.class);
    private final FieldExtractor<T> fieldExtractor;
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

    public EventCache(FieldExtractor<T> fieldExtractor) {
        this.fieldExtractor = fieldExtractor;
    }

    @Override
    public void pop(Processor<T> processor, long timeout, TimeUnit timeUnit) throws Exception {
        Event<T> firstEvent = queue.poll(timeout, timeUnit);
        if (firstEvent == null) {
            log.debug("Woke up but queue is empty");
            return;
        }

        List<Event<T>> events = new ArrayList<>();
        events.add(firstEvent);
        queue.drainTo(events);

        synchronized (this) {
            for (Event<T> event : events) {
                String key = null;
                if (event.obj != null) {
                    key = fieldExtractor.getKey(event.obj);
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
                        if (initialPopulationCount.get() > 0) {
                            initialPopulationCount.decrementAndGet();
                        }
                        break;
                }
            }
        }
        String key = firstEvent.obj != null ? fieldExtractor.getKey(firstEvent.obj) : null;
        log.debug("Processing event {} with key {}", firstEvent.eventType, key);
        processor.process(firstEvent.obj);
    }

    private boolean hasVersionChanged(T ths, T that) {
        String thisVersion = fieldExtractor.getResourceVersion(ths);
        String thatVersion = fieldExtractor.getResourceVersion(that);
        if (thisVersion == null || thatVersion == null) {
            return true;
        }
        return !thisVersion.equals(thatVersion);
    }

    private void queueEvent(EventType eventType, T obj) throws InterruptedException {
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

        log.debug("Replacing queue with {} items. Populated {}.", list.size(), populated);
        Map<String, T> newItems = new HashMap<>();
        for (T item : list) {
            String key = fieldExtractor.getKey(item);
            newItems.put(key, item);
        }

        log.debug("Current store size: {}", store.size());
        store.clear();
        store.putAll(newItems);
        log.debug("New store size: {}", store.size());

        if (!populated) {
            initialPopulationCount.set(1);
        }
        queueEvent(Sync, null);
    }

    @Override
    public synchronized void replace(T item) {
        String key = fieldExtractor.getKey(item);
        T current = store.get(key);
        if (current != null) {
            String currentVersion = fieldExtractor.getResourceVersion(current);
            String replaceVersion = fieldExtractor.getResourceVersion(item);
            if (!hasVersionChanged(current, item)) {
                log.debug("Replacing {} (old {}, new {})", key, currentVersion, replaceVersion);
                store.put(key, item);
            } else {
                log.debug("Not replacing {} (old {}, new {})", key, currentVersion, replaceVersion);
            }
        }
    }
}
