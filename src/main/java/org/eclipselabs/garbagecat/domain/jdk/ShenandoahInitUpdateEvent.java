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
package org.eclipselabs.garbagecat.domain.jdk;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipselabs.garbagecat.domain.BlockingEvent;
import org.eclipselabs.garbagecat.domain.ParallelEvent;
import org.eclipselabs.garbagecat.util.jdk.JdkMath;
import org.eclipselabs.garbagecat.util.jdk.JdkRegEx;
import org.eclipselabs.garbagecat.util.jdk.JdkUtil;
import org.eclipselabs.garbagecat.util.jdk.unified.UnifiedRegEx;
import org.eclipselabs.garbagecat.util.jdk.unified.UnifiedUtil;

/**
 * <p>
 * SHENANDOAH_INIT_UPDATE
 * </p>
 * 
 * <p>
 * Initializes the update references phase. It does almost nothing except making sure all GC and applications threads
 * have finished evacuation, and then preparing GC for next phase. This is the third pause in the cycle, the shortest of
 * them all[1].
 * 
 * [1]<a href="https://wiki.openjdk.java.net/display/shenandoah/Main">Shenandoah GC</a>
 * </p>
 * 
 * <h2>Example Logging</h2>
 * 
 * <p>
 * 1) JDK8:
 * </p>
 * 
 * <pre>
 * 2020-03-10T08:03:46.284-0400: 17.346: [Pause Init Update Refs, 0.017 ms]
 * </pre>
 * 
 * <p>
 * 2) Unified:
 * </p>
 * 
 * <pre>
 * [5.312s][info][gc] GC(110) Pause Init Update Refs 0.005ms
 * </pre>
 * 
 * @author <a href="mailto:mmillson@redhat.com">Mike Millson</a>
 * 
 */
public class ShenandoahInitUpdateEvent extends ShenandoahCollector implements BlockingEvent, ParallelEvent {

    public static final Pattern pattern = Pattern.compile(ShenandoahInitUpdateEvent.REGEX);

    /**
     * Regular expressions defining the logging.
     */
    public static final String REGEX = "^(" + JdkRegEx.DECORATOR + "|" + UnifiedRegEx.DECORATOR
            + ") [\\[]{0,1}Pause Init Update Refs[,]{0,1} " + JdkRegEx.DURATION_MS + "[\\]]{0,1}[ ]*$";

    /**
     * Determine if the logLine matches the logging pattern(s) for this event.
     * 
     * @param logLine
     *            The log line to test.
     * @return true if the log line matches the event pattern, false otherwise.
     */
    public static final boolean match(String logLine) {
        return pattern.matcher(logLine).matches();
    }

    /**
     * The elapsed clock time for the GC event in microseconds (rounded).
     */
    private long duration;

    /**
     * The log entry for the event. Can be used for debugging purposes.
     */
    private String logEntry;

    /**
     * The time when the GC event started in milliseconds after JVM startup.
     */
    private long timestamp;

    /**
     * Create event from log entry.
     * 
     * @param logEntry
     *            The log entry for the event.
     */
    public ShenandoahInitUpdateEvent(String logEntry) {
        this.logEntry = logEntry;
        if (logEntry.matches(REGEX)) {
            Pattern pattern = Pattern.compile(REGEX);
            Matcher matcher = pattern.matcher(logEntry);
            if (matcher.find()) {
                duration = JdkMath
                        .convertMillisToMicros(matcher.group(JdkUtil.DECORATOR_SIZE + UnifiedUtil.DECORATOR_SIZE + 2))
                        .intValue();
                if (matcher.group(1).matches(UnifiedRegEx.DECORATOR)) {
                    long endTimestamp;
                    if (matcher.group(15).matches(UnifiedRegEx.UPTIMEMILLIS)) {
                        endTimestamp = Long.parseLong(matcher.group(UnifiedUtil.DECORATOR_SIZE + 8));
                    } else if (matcher.group(15).matches(UnifiedRegEx.UPTIME)) {
                        endTimestamp = JdkMath.convertSecsToMillis(matcher.group(JdkUtil.DECORATOR_SIZE + 12))
                                .longValue();
                    } else {
                        if (matcher.group(JdkUtil.DECORATOR_SIZE + 14) != null) {
                            if (matcher.group(JdkUtil.DECORATOR_SIZE + 15).matches(UnifiedRegEx.UPTIMEMILLIS)) {
                                endTimestamp = Long.parseLong(matcher.group(JdkUtil.DECORATOR_SIZE + 17));
                            } else {
                                endTimestamp = JdkMath.convertSecsToMillis(matcher.group(JdkUtil.DECORATOR_SIZE + 16))
                                        .longValue();
                            }
                        } else {
                            // Datestamp only.
                            endTimestamp = JdkUtil.convertDatestampToMillis(matcher.group(15));
                        }
                    }
                    timestamp = endTimestamp - JdkMath.convertMicrosToMillis(duration).longValue();
                } else {
                    // JDK8
                    if (matcher.group(14) != null && matcher.group(14).matches(JdkRegEx.TIMESTAMP)) {
                        timestamp = JdkMath.convertSecsToMillis(matcher.group(14)).longValue();
                    } else if (matcher.group(2).matches(JdkRegEx.TIMESTAMP)) {
                        timestamp = JdkMath.convertSecsToMillis(matcher.group(2)).longValue();
                    } else {
                        // Datestamp only.
                        timestamp = JdkUtil.convertDatestampToMillis(matcher.group(2));
                    }
                }
            }
        }

    }

    /**
     * Alternate constructor. Create event from values.
     * 
     * @param logEntry
     *            The log entry for the event.
     * @param timestamp
     *            The time when the GC event started in milliseconds after JVM startup.
     * @param duration
     *            The elapsed clock time for the GC event in microseconds.
     */
    public ShenandoahInitUpdateEvent(String logEntry, long timestamp, int duration) {
        this.logEntry = logEntry;
        this.timestamp = timestamp;
        this.duration = duration;
    }

    public long getDuration() {
        return duration;
    }

    public String getLogEntry() {
        return logEntry;
    }

    public String getName() {
        return JdkUtil.LogEventType.SHENANDOAH_INIT_UPDATE.toString();
    }

    public long getTimestamp() {
        return timestamp;
    }
}
