/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.HdrHistogram.DoubleHistogram;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.TestTag;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.scale.metrics.MessagesCountRecord;

@Tag(TestTag.FRAMEWORK)
public class ScaleTestManagerTest {

    @Test
    void testGatherPerformanceData1MsgSec() {

        long start = 0;

        AtomicInteger total = new AtomicInteger(0);
        List<MessagesCountRecord> records = new ArrayList<MessagesCountRecord>();
        IntStream.range(1, 11)
            .forEach(i -> {
                var curr = total.incrementAndGet();
                records.add(new MessagesCountRecord(i * 1000, curr));
            });


        ScaleTestManager manager = new ScaleTestManager(new Endpoint("host", 9999), new UserCredentials("foo"));
        DoubleHistogram histogram = new DoubleHistogram(20000, 4);

        var data = manager.gatherPerformanceData("aaa", records, start, histogram);

        for (String r : data.getMsgPerSecond()) {
            assertThat(r, equalTo("1.0 msg/sec"));
        }

        var global = manager.gatherGlobalPerformanceData(histogram, 1);
        assertThat(global.getPerClientThroughput99p(), equalTo("1.0 msg/sec"));
        assertThat(global.getPerClientThroughputMedian(), equalTo("1.0 msg/sec"));

    }

    @Test
    void testGatherPerformanceData1AndHalfMsgSec() {

        long start = 0;

        AtomicInteger total = new AtomicInteger(0);
        List<MessagesCountRecord> records = new ArrayList<MessagesCountRecord>();
        AtomicInteger index = new AtomicInteger(0);
        IntStream.generate(() -> {
                return index.addAndGet(2);
            })
            .limit(12)
            .forEach(i -> {
                var curr = total.addAndGet(3);
                records.add(new MessagesCountRecord(i * 1000, curr));
            });

        ScaleTestManager manager = new ScaleTestManager(new Endpoint("host", 9999), new UserCredentials("foo"));
        DoubleHistogram histogram = new DoubleHistogram(20000, 4);

        var data = manager.gatherPerformanceData("aaa", records, start, histogram);

        for (String r : data.getMsgPerSecond()) {
            assertThat(r, equalTo("1.5 msg/sec"));
        }

        var global = manager.gatherGlobalPerformanceData(histogram, 1);
        assertThat(global.getPerClientThroughput99p(), equalTo("1.5 msg/sec"));
        assertThat(global.getPerClientThroughputMedian(), equalTo("1.5 msg/sec"));

    }

    @Test
    void testGatherPerformanceData1And25MsgSec() {

        long start = 0;

        List<MessagesCountRecord> records = new ArrayList<MessagesCountRecord>();
        records.add(new MessagesCountRecord(4000, 5));

        ScaleTestManager manager = new ScaleTestManager(new Endpoint("host", 9999), new UserCredentials("foo"));
        DoubleHistogram histogram = new DoubleHistogram(20000, 4);

        var data = manager.gatherPerformanceData("aaa", records, start, histogram);

        for (String r : data.getMsgPerSecond()) {
            assertThat(r, equalTo("1.25 msg/sec"));
        }

        var global = manager.gatherGlobalPerformanceData(histogram, 1);
        assertThat(global.getPerClientThroughput99p(), equalTo("1.25 msg/sec"));
        assertThat(global.getPerClientThroughputMedian(), equalTo("1.25 msg/sec"));

    }

    @Test
    void testGatherPerformanceData3And33MsgSec() {

        long start = 0;

        List<MessagesCountRecord> records = new ArrayList<MessagesCountRecord>();
        records.add(new MessagesCountRecord(3000, 10));

        ScaleTestManager manager = new ScaleTestManager(new Endpoint("host", 9999), new UserCredentials("foo"));
        DoubleHistogram histogram = new DoubleHistogram(20000, 4);


        var data = manager.gatherPerformanceData("aaa", records, start, histogram);
        for (String r : data.getMsgPerSecond()) {
            assertThat(r, equalTo("3.33 msg/sec"));
        }

        var global = manager.gatherGlobalPerformanceData(histogram, 1);
        assertThat(global.getPerClientThroughput99p(), equalTo("3.33 msg/sec"));
        assertThat(global.getPerClientThroughputMedian(), equalTo("3.33 msg/sec"));

    }

    @Test
    void testGatherPerformanceData0And66MsgSec() {

        long start = 0;

        AtomicInteger total = new AtomicInteger(0);
        List<MessagesCountRecord> records = new ArrayList<MessagesCountRecord>();
        AtomicInteger index = new AtomicInteger(0);
        IntStream.generate(() -> {
                return index.addAndGet(3);
            })
            .limit(12)
            .forEach(i -> {
                var curr = total.addAndGet(2);
                records.add(new MessagesCountRecord(i * 1000, curr));
            });

        ScaleTestManager manager = new ScaleTestManager(new Endpoint("host", 9999), new UserCredentials("foo"));
        DoubleHistogram histogram = new DoubleHistogram(20000, 4);

        var data = manager.gatherPerformanceData("aaa", records, start, histogram);

        for (String r : data.getMsgPerSecond()) {
            assertThat(r, equalTo("0.67 msg/sec"));
        }

        var global = manager.gatherGlobalPerformanceData(histogram, 1);
        assertThat(global.getPerClientThroughput99p(), equalTo("0.67 msg/sec"));
        assertThat(global.getPerClientThroughputMedian(), equalTo("0.67 msg/sec"));

    }

}
