/**********************************************************************************************************************
 * garbagecat                                                                                                         *
 *                                                                                                                    *
 * Copyright (c) 2008-2023 Mike Millson                                                                               *
 *                                                                                                                    * 
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse *
 * Public License v1.0 which accompanies this distribution, and is available at                                       *
 * http://www.eclipse.org/legal/epl-v10.html.                                                                         *
 *                                                                                                                    *
 * Contributors:                                                                                                      *
 *    Mike Millson - initial API and implementation                                                                   *
 *********************************************************************************************************************/
package org.eclipselabs.garbagecat.domain.jdk.unified;

import static org.eclipselabs.garbagecat.util.Memory.kilobytes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipselabs.garbagecat.TestUtil;
import org.eclipselabs.garbagecat.domain.JvmRun;
import org.eclipselabs.garbagecat.service.GcManager;
import org.eclipselabs.garbagecat.util.Constants;
import org.eclipselabs.garbagecat.util.jdk.GcTrigger;
import org.eclipselabs.garbagecat.util.jdk.JdkUtil;
import org.eclipselabs.garbagecat.util.jdk.JdkUtil.LogEventType;
import org.eclipselabs.garbagecat.util.jdk.unified.UnifiedUtil;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:mmillson@redhat.com">Mike Millson</a>
 * 
 */
class TestUnifiedG1YoungPrepareMixedEvent {

    @Test
    void testHydration() {
        LogEventType eventType = JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED;
        String logLine = "[16.627s][info][gc,start      ] GC(1354) Pause Young (Prepare Mixed) (G1 Evacuation Pause) "
                + "Humongous regions: 13->13 Metaspace: 3801K->3801K(1056768K) 24M->13M(31M) 0.361ms "
                + "User=0.00s Sys=0.00s Real=0.00s";
        long timestamp = 15108;
        int duration = 0;
        assertTrue(
                JdkUtil.hydrateBlockingEvent(eventType, logLine, timestamp,
                        duration) instanceof UnifiedG1YoungPrepareMixedEvent,
                JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED.toString() + " not parsed.");
    }

    @Test
    void testIdentityEventType() {
        String logLine = "[16.627s][info][gc,start      ] GC(1354) Pause Young (Prepare Mixed) (G1 Evacuation Pause) "
                + "Other: 0.1ms Humongous regions: 13->13 Metaspace: 3801K->3801K(1056768K) 24M->13M(31M) 0.361ms "
                + "User=0.00s Sys=0.00s Real=0.00s";
        assertEquals(JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED, JdkUtil.identifyEventType(logLine, null),
                JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED + "not identified.");
    }

    @Test
    void testIsBlocking() {
        String logLine = "[16.627s][info][gc,start      ] GC(1354) Pause Young (Prepare Mixed) (G1 Evacuation Pause) "
                + "Other: 0.1ms Humongous regions: 13->13 Metaspace: 3801K->3801K(1056768K) 24M->13M(31M) 0.361ms "
                + "User=0.00s Sys=0.00s Real=0.00s";
        assertTrue(JdkUtil.isBlocking(JdkUtil.identifyEventType(logLine, null)),
                JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED.toString() + " not indentified as blocking.");
    }

    @Test
    void testLogLinePreprocessed() {
        String logLine = "[16.627s][info][gc,start      ] GC(1354) Pause Young (Prepare Mixed) (G1 Evacuation Pause) "
                + "Other: 0.1ms Humongous regions: 13->13 Metaspace: 3801K->3801K(1056768K) 24M->13M(31M) 0.361ms "
                + "User=0.00s Sys=0.00s Real=0.00s";
        assertTrue(UnifiedG1YoungPrepareMixedEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED.toString() + ".");
        UnifiedG1YoungPrepareMixedEvent event = new UnifiedG1YoungPrepareMixedEvent(logLine);
        assertEquals(JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED.toString(), event.getName(),
                "Event name incorrect.");
        assertEquals((long) (16627 - 0), event.getTimestamp(), "Time stamp not parsed correctly.");
        assertTrue(event.getTrigger() == GcTrigger.G1_EVACUATION_PAUSE, "Trigger not parsed correctly.");
        assertEquals(kilobytes(3801), event.getPermOccupancyInit(), "Perm gen begin size not parsed correctly.");
        assertEquals(kilobytes(3801), event.getPermOccupancyEnd(), "Perm gen end size not parsed correctly.");
        assertEquals(kilobytes(1056768), event.getPermSpace(), "Perm gen allocation size not parsed correctly.");
        assertEquals(kilobytes(24 * 1024), event.getCombinedOccupancyInit(),
                "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(13 * 1024), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(31 * 1024), event.getCombinedSpace(), "Combined allocation size not parsed correctly.");
        assertEquals(100, event.getOtherTime(), "Other time not parsed correctly.");
        assertEquals(461, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(0, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(0, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(100, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testLogLineWhitespaceAtEnd() {
        String logLine = "[16.627s][info][gc,start      ] GC(1354) Pause Young (Prepare Mixed) (G1 Evacuation Pause) "
                + "Other: 0.1ms Humongous regions: 13->13 Metaspace: 3801K->3801K(1056768K) 24M->13M(31M) 0.361ms "
                + "User=0.00s Sys=0.00s Real=0.00s    ";
        assertTrue(UnifiedG1YoungPrepareMixedEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED.toString() + ".");
    }

    @Test
    void testParseLogLine() {
        String logLine = "[16.627s][info][gc,start      ] GC(1354) Pause Young (Prepare Mixed) (G1 Evacuation Pause) "
                + "Other: 0.1ms Humongous regions: 13->13 Metaspace: 3801K->3801K(1056768K) 24M->13M(31M) 0.361ms "
                + "User=0.00s Sys=0.00s Real=0.00s";
        assertTrue(JdkUtil.parseLogLine(logLine, null) instanceof UnifiedG1YoungPrepareMixedEvent,
                JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED.toString() + " not parsed.");
    }

    @Test
    void testPreprocessedJdk17() {
        String logLine = "[2022-08-05T05:08:51.394+0000][1908][gc,start    ] GC(1360) Pause Young (Prepare Mixed) "
                + "(G1 Evacuation Pause) Other: 0.1ms Humongous regions: 13->13 "
                + "Metaspace: 147162K(149824K)->147162K(149824K) 24336M->9999M(32768M) 26,821ms "
                + "User=0,18s Sys=0,00s Real=0,03s";
        assertTrue(UnifiedG1YoungPrepareMixedEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED.toString() + ".");
        UnifiedG1YoungPrepareMixedEvent event = new UnifiedG1YoungPrepareMixedEvent(logLine);
        assertEquals(JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED.toString(), event.getName(),
                "Event name incorrect.");
    }

    @Test
    void testPreprocessedTriggerG1HumongousAllocation() {
        String logLine = "[2021-10-29T20:56:08.426+0000][info][gc,start      ] GC(734) Pause Young (Prepare Mixed) "
                + "(G1 Humongous Allocation) Other: 0.1ms Humongous regions: 13->13 "
                + "Metaspace: 66401K->66401K(151552K) 15678M->1575M(16384M) 24.193ms User=0.12s Sys=0.00s Real=0.03s";
        assertTrue(UnifiedG1YoungPrepareMixedEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED.toString() + ".");
        UnifiedG1YoungPrepareMixedEvent event = new UnifiedG1YoungPrepareMixedEvent(logLine);
        assertEquals(JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED.toString(), event.getName(),
                "Event name incorrect.");
    }

    @Test
    void testPreprocessedTriggerG1PreventiveCollection() {
        String logLine = "[2022-08-22T16:07:11.203+0000][248.117s] GC(26) Pause Young (Prepare Mixed) "
                + "(G1 Preventive Collection) Other: 0.1ms Humongous regions: 13->13 "
                + "Metaspace: 52236K(52736K)->52236K(52736K) 269M->81M(300M) 14.821ms User=0.02s Sys=0.00s Real=0.01s";
        assertTrue(UnifiedG1YoungPrepareMixedEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED.toString() + ".");
        UnifiedG1YoungPrepareMixedEvent event = new UnifiedG1YoungPrepareMixedEvent(logLine);
        assertEquals(JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED.toString(), event.getName(),
                "Event name incorrect.");
    }

    @Test
    void testPreprocessedTriggerGcLockerInitiatedGc() {
        String logLine = "[2021-10-14T00:22:54.796+0400][info][gc,start      ] GC(891) Pause Young (Prepare Mixed) "
                + "(GCLocker Initiated GC) Other: 0.1ms Humongous regions: 13->13 "
                + "Metaspace: 360792K->360792K(1380352K) 10311M->3024M(12288M) 33.928ms User=0.25s "
                + "Sys=0.04s Real=0.03s";
        assertTrue(UnifiedG1YoungPrepareMixedEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED.toString() + ".");
        UnifiedG1YoungPrepareMixedEvent event = new UnifiedG1YoungPrepareMixedEvent(logLine);
        assertEquals(JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED.toString(), event.getName(),
                "Event name incorrect.");
    }

    @Test
    void testPreprocessing() throws IOException {
        File testFile = TestUtil.getFile("dataset168.txt");
        GcManager gcManager = new GcManager();
        URI logFileUri = testFile.toURI();
        List<String> logLines = Files.readAllLines(Paths.get(logFileUri));
        logLines = gcManager.preprocess(logLines, null);
        gcManager.store(logLines, false);
        JvmRun jvmRun = gcManager.getJvmRun(null, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        assertEquals(1, jvmRun.getEventTypes().size(), "Event type count not correct.");
        assertFalse(jvmRun.getEventTypes().contains(LogEventType.UNKNOWN),
                JdkUtil.LogEventType.UNKNOWN.toString() + " collector identified.");
        assertTrue(jvmRun.getEventTypes().contains(JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED),
                "Log line not recognized as " + JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED.toString() + ".");
    }

    @Test
    void testReportable() {
        assertTrue(JdkUtil.isReportable(JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED),
                JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED.toString() + " not indentified as reportable.");
    }

    /**
     * Test with time, uptime decorator.
     */
    @Test
    void testTimeUptime() {
        String logLine = "[2021-03-09T14:45:02.441-0300][12.082s] GC(6) Pause Young (Prepare Mixed) "
                + "(G1 Evacuation Pause) Other: 0.1ms Humongous regions: 13->13 Metaspace: 3801K->3801K(1056768K) "
                + "24M->13M(31M) 0.361ms User=0.00s Sys=0.00s Real=0.00s";
        assertTrue(UnifiedG1YoungPrepareMixedEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED.toString() + ".");
        UnifiedG1YoungPrepareMixedEvent event = new UnifiedG1YoungPrepareMixedEvent(logLine);
        assertEquals(JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED.toString(), event.getName(),
                "Event name incorrect.");
    }

    @Test
    void testUnified() {
        List<LogEventType> eventTypes = new ArrayList<LogEventType>();
        eventTypes.add(LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED);
        assertTrue(UnifiedUtil.isUnifiedLogging(eventTypes),
                JdkUtil.LogEventType.UNIFIED_G1_YOUNG_PREPARE_MIXED.toString() + " not indentified as unified.");
    }
}
