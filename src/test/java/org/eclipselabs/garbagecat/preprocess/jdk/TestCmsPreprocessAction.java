/**********************************************************************************************************************
 * garbagecat                                                                                                         *
 *                                                                                                                    *
 * Copyright (c) 2008-2020 Mike Millson                                                                              *
 *                                                                                                                    * 
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse *
 * Public License v1.0 which accompanies this distribution, and is available at                                       *
 * http://www.eclipse.org/legal/epl-v10.html.                                                                         *
 *                                                                                                                    *
 * Contributors:                                                                                                      *
 *    Mike Millson - initial API and implementation                                                                  *
 *********************************************************************************************************************/
package org.eclipselabs.garbagecat.preprocess.jdk;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipselabs.garbagecat.domain.JvmRun;
import org.eclipselabs.garbagecat.service.GcManager;
import org.eclipselabs.garbagecat.util.Constants;
import org.eclipselabs.garbagecat.util.jdk.Analysis;
import org.eclipselabs.garbagecat.util.jdk.JdkUtil;
import org.eclipselabs.garbagecat.util.jdk.JdkUtil.LogEventType;
import org.eclipselabs.garbagecat.util.jdk.JdkUtil.PreprocessActionType;
import org.eclipselabs.garbagecat.util.jdk.Jvm;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author <a href="mailto:mmillson@redhat.com">Mike Millson</a>
 * 
 */
public class TestCmsPreprocessAction extends TestCase {

    public void testLogLineParNewMixedConcurrent() {
        String logLine = "46674.719: [GC (Allocation Failure)46674.719: [ParNew46674.749: "
                + "[CMS-concurrent-abortable-preclean: 1.427/2.228 secs] [Times: user=1.56 sys=0.01, real=2.23 secs]";
        String nextLogLine = " (concurrent mode failure): 2542828K->2658278K(2658304K), 12.3447910 secs] "
                + "3925228K->2702358K(4040704K), [Metaspace: 72175K->72175K(1118208K)] icms_dc=100 , 12.3480570 secs] "
                + "[Times: user=15.38 sys=0.02, real=12.35 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, null, nextLogLine));
    }

    public void testLogLineParNewMixedConcurrentReset() {
        String priorLogLine = null;
        String logLine = "2017-03-21T16:02:23.633+0530: 53277.279: [GC 53277.279: [ParNew: "
                + "2853312K->2853312K(2853312K), 0.0000310 secs]53277.279: [CMS2017-03-21T16:02:23.655+0530: "
                + "53277.301: [CMS-concurrent-reset: 0.019/0.023 secs] [Times: user=0.02 sys=0.00, real=0.02 secs]";
        String nextLogLine = ": 8943881K->8813432K(9412608K), 7.7851270 secs] 11797193K->9475525K(12265920K), [CMS "
                + "Perm : 460344K->460331K(770956K)], 7.7854740 secs] [Times: user=7.79 sys=0.01, real=7.78 secs]";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, null, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(priorLogLine, logLine, nextLogLine, entangledLogLines,
                context);
        Assert.assertEquals("Log line not parsed correctly.",
                "2017-03-21T16:02:23.633+0530: 53277.279: [GC 53277.279: [ParNew: 2853312K->2853312K(2853312K), "
                        + "0.0000310 secs]53277.279: [CMS",
                event.getLogEntry());
    }

    public void testLogLineParNewMixedConcurrentWithWhitespaceEnd() {
        String logLine = "46674.719: [GC (Allocation Failure)46674.719: [ParNew46674.749: "
                + "[CMS-concurrent-abortable-preclean: 1.427/2.228 secs] "
                + "[Times: user=1.56 sys=0.01, real=2.23 secs]   ";
        String nextLogLine = " (concurrent mode failure): 2542828K->2658278K(2658304K), 12.3447910 secs] "
                + "3925228K->2702358K(4040704K), [Metaspace: 72175K->72175K(1118208K)] icms_dc=100 , 12.3480570 secs] "
                + "[Times: user=15.38 sys=0.02, real=12.35 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, null, nextLogLine));
    }

    public void testLogLineEnd() {
        String logLine = ": 153599K->17023K(153600K), 0.0383370 secs] 229326K->114168K(494976K), 0.0384820 secs] "
                + "[Times: user=0.15 sys=0.01, real=0.04 secs]";
        String priorLogLine = "46674.719: [GC (Allocation Failure)46674.719: [ParNew46674.749: "
                + "[CMS-concurrent-abortable-preclean: 1.427/2.228 secs] "
                + "[Times: user=1.56 sys=0.01, real=2.23 secs]   ";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, null));
    }

    public void testLogLineEndWithWhitespaceEnd() {
        String logLine = ": 153599K->17023K(153600K), 0.0383370 secs] 229326K->114168K(494976K), 0.0384820 secs] "
                + "[Times: user=0.15 sys=0.01, real=0.04 secs]    ";
        String priorLogLine = "46674.719: [GC (Allocation Failure)46674.719: [ParNew46674.749: "
                + "[CMS-concurrent-abortable-preclean: 1.427/2.228 secs] "
                + "[Times: user=1.56 sys=0.01, real=2.23 secs]   ";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, null));
    }

    public void testLogLineParNewNoTriggerMixedConcurrent() {
        String logLine = "10.963: [GC10.963: [ParNew10.977: [CMS-concurrent-abortable-preclean: 0.088/0.197 secs] "
                + "[Times: user=0.33 sys=0.05, real=0.20 secs]";
        String nextLogLine = " (concurrent mode failure): 2542828K->2658278K(2658304K), 12.3447910 secs] "
                + "3925228K->2702358K(4040704K), [Metaspace: 72175K->72175K(1118208K)] icms_dc=100 , 12.3480570 secs] "
                + "[Times: user=15.38 sys=0.02, real=12.35 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, null, nextLogLine));
    }

    public void testLogLineParNewPromotionFailed() {
        String logLine = "233333.318: [GC 233333.319: [ParNew (promotion failed): 673108K->673108K(707840K), "
                + "1.5366054 secs]233334.855: [CMS233334.856: [CMS-concurrent-abortable-preclean: 12.033/27.431 secs]";
        String nextLogLine = " (concurrent mode failure): 2542828K->2658278K(2658304K), 12.3447910 secs] "
                + "3925228K->2702358K(4040704K), [Metaspace: 72175K->72175K(1118208K)] icms_dc=100 , 12.3480570 secs] "
                + "[Times: user=15.38 sys=0.02, real=12.35 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, null, nextLogLine));
    }

    public void testLogLineMiddle() {
        String logLine = "1907.974: [CMS-concurrent-mark: 23.751/40.476 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, null, null));
    }

    public void testLogLineParNewTriggerMixedConcurrentJdk8() {
        String logLine = "45.574: [GC (Allocation Failure) 45.574: [ParNew45.670: [CMS-concurrent-abortable-preclean: "
                + "3.276/4.979 secs] [Times: user=7.75 sys=0.28, real=4.98 secs]";
        String nextLogLine = " (concurrent mode failure): 2542828K->2658278K(2658304K), 12.3447910 secs] "
                + "3925228K->2702358K(4040704K), [Metaspace: 72175K->72175K(1118208K)] icms_dc=100 , 12.3480570 secs] "
                + "[Times: user=15.38 sys=0.02, real=12.35 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, null, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        Set<String> context = new HashSet<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(null, logLine, nextLogLine, entangledLogLines, context);
        Assert.assertEquals("Log line not parsed correctly.", "45.574: [GC (Allocation Failure) 45.574: [ParNew",
                event.getLogEntry());
    }

    public void testLogLineParNewConcurrentModeFailureMixedConcurrentJdk8() {
        String logLine = "719.519: [GC (Allocation Failure) 719.521: [ParNew: 1382400K->1382400K(1382400K), "
                + "0.0000470 secs]719.521: [CMS722.601: [CMS-concurrent-mark: 3.567/3.633 secs] "
                + "[Times: user=10.91 sys=0.69, real=3.63 secs]";
        String nextLogLine = " (concurrent mode failure): 2542828K->2658278K(2658304K), 12.3447910 secs] "
                + "3925228K->2702358K(4040704K), [Metaspace: 72175K->72175K(1118208K)] icms_dc=100 , 12.3480570 secs] "
                + "[Times: user=15.38 sys=0.02, real=12.35 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, null, nextLogLine));
    }

    public void testLogLineCmsSerialOldMixedConcurrentMark() {
        String logLine = "2016-02-26T16:37:58.740+1100: 44.684: [Full GC2016-02-26T16:37:58.740+1100: 44.684: [CMS"
                + "2016-02-26T16:37:58.933+1100: 44.877: [CMS-concurrent-mark: 1.508/2.428 secs] "
                + "[Times: user=3.44 sys=0.49, real=2.42 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, null, null));
    }

    public void testLogLineCmsSerialOldMixedConcurrentAbortablePreclean() {
        String logLine = "85217.903: [Full GC 85217.903: [CMS85217.919: [CMS-concurrent-abortable-preclean: "
                + "0.723/3.756 secs] [Times: user=2.54 sys=0.08, real=3.76 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, null, null));
    }

    public void testLogLineCmsSerialOldMixedAbortPrecleanConcurrentAbortablePreclean() {
        String logLine = "2017-06-22T21:22:03.269-0400: 23.858: [Full GC 23.859: [CMS CMS: abort preclean due to "
                + "time 2017-06-22T21:22:03.269-0400: 23.859: [CMS-concurrent-abortable-preclean: 0.338/5.115 secs] "
                + "[Times: user=14.57 sys=0.83, real=5.11 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, null, null));
        String nextLogLine = " (concurrent mode failure): 8156K->36298K(7864320K), 1.0166580 secs] "
                + "89705K->36298K(8336192K), [CMS Perm : 34431K->34268K(34548K)], 1.0172840 secs] "
                + "[Times: user=0.86 sys=0.14, real=1.02 secs]";
        Set<String> context = new HashSet<String>();
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(null, logLine, nextLogLine, entangledLogLines, context);
        Assert.assertEquals("Log line not parsed correctly.",
                "2017-06-22T21:22:03.269-0400: 23.858: [Full GC 23.859: [CMS", event.getLogEntry());
    }

    public void testLogLineCmsSerialOldMixedConcurrentSpaceAfterGC() {
        String logLine = "85238.030: [Full GC 85238.030: [CMS85238.672: [CMS-concurrent-mark: 0.666/0.686 secs] "
                + "[Times: user=1.40 sys=0.01, real=0.69 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, null, null));
    }

    public void testLogLineCmsSerialOldWithTriggerMixedConcurrent() {
        String logLine = "706.707: [Full GC (Allocation Failure) 706.708: [CMS709.137: [CMS-concurrent-mark: "
                + "3.381/5.028 secs] [Times: user=23.92 sys=3.02, real=5.03 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, null, null));
    }

    public void testLogLineEndWithPerm() {
        String logLine = " (concurrent mode failure): 1218548K->413373K(1465840K), 1.3656970 secs] "
                + "1229657K->413373K(1581168K), [CMS Perm : 83805K->80520K(83968K)], 1.3659420 secs] "
                + "[Times: user=1.33 sys=0.01, real=1.37 secs]";
        String priorLogLine = "44.684: [Full GC44.684: [CMS44.877: [CMS-concurrent-mark: 1.508/2.428 secs] "
                + "[Times: user=3.44 sys=0.49, real=2.42 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, null));
    }

    public void testLogLineEndWithMetaspace() {
        String logLine = " (concurrent mode failure): 2655937K->2373842K(2658304K), 11.6746550 secs] "
                + "3973407K->2373842K(4040704K), [Metaspace: 72496K->72496K(1118208K)] icms_dc=77 , 11.6770830 secs] "
                + "[Times: user=14.05 sys=0.02, real=11.68 secs]";
        String priorLogLine = "46674.719: [GC (Allocation Failure)46674.719: [ParNew46674.749: "
                + "[CMS-concurrent-abortable-preclean: 1.427/2.228 secs] "
                + "[Times: user=1.56 sys=0.01, real=2.23 secs]   ";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, null));
    }

    public void testLogLineEndPromotionFailedConcurrentModeFailure() {
        String logLine = " (promotion failed): 471871K->471872K(471872K), 0.7685416 secs]66645.266: [CMS (concurrent "
                + "mode failure): 1572864K->1572863K(1572864K), 6.3611861 secs] 2001479K->1657572K(2044736K), "
                + "[Metaspace: 567956K->567956K(1609728K)], 7.1304658 secs] "
                + "[Times: user=8.60 sys=0.01, real=7.13 secs]";
        String priorLogLine = "46674.719: [GC (Allocation Failure)46674.719: [ParNew46674.749: "
                + "[CMS-concurrent-abortable-preclean: 1.427/2.228 secs] "
                + "[Times: user=1.56 sys=0.01, real=2.23 secs]   ";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, null));
    }

    public void testLogLineParNewNoTriggerMixedConcurrentWithCommas() {
        String logLine = "32552,602: [GC32552,602: [ParNew32552,610: "
                + "[CMS-concurrent-abortable-preclean: 3,090/4,993 secs] "
                + "[Times: user=3,17 sys=0,02, real=5,00 secs]";
        String nextLogLine = " (concurrent mode failure): 2542828K->2658278K(2658304K), 12.3447910 secs] "
                + "3925228K->2702358K(4040704K), [Metaspace: 72175K->72175K(1118208K)] icms_dc=100 , 12.3480570 secs] "
                + "[Times: user=15.38 sys=0.02, real=12.35 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, null, nextLogLine));
    }

    public void testLogLineEndWithCommas() {
        String logLine = ": 289024K->17642K(306688K), 0,0788160 secs] 4086255K->3814874K(12548864K), 0,0792920 secs] "
                + "[Times: user=0,28 sys=0,00, real=0,08 secs]";
        String priorLogLine = "46674.719: [GC (Allocation Failure)46674.719: [ParNew46674.749: "
                + "[CMS-concurrent-abortable-preclean: 1.427/2.228 secs] "
                + "[Times: user=1.56 sys=0.01, real=2.23 secs]   ";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, null));
    }

    public void testLogLineSerialBailing() {
        String logLine = "4300.825: [Full GC 4300.825: [CMSbailing out to foreground collection";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, null, null));
    }

    public void testLogLineParNewBailing() {
        String logLine = "2137.769: [GC 2137.769: [ParNew (promotion failed): 242304K->242304K(242304K), "
                + "8.4066690 secs]2146.176: [CMSbailing out to foreground collection";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, null, null));
    }

    public void testLogLineParNewHotspotBailing() {
        String logLine = "1901.217: [GC 1901.217: [ParNew: 261760K->261760K(261952K), 0.0000570 secs]1901.217: "
                + "[CMSJava HotSpot(TM) Server VM warning: bailing out to foreground collection";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, null, ""));
    }

    public void testLogLineParTriggerPromotionFailed() {
        String logLine = "182314.858: [GC 182314.859: [ParNew (promotion failed)";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, null, ""));
    }

    public void testLogLineParNewConcurrentModeFailurePermDataMixedConcurrentSweep() {
        String logLine = "11202.526: [GC (Allocation Failure) 1202.528: [ParNew: 1355422K->1355422K(1382400K), "
                + "0.0000500 secs]1202.528: [CMS1203.491: [CMS-concurrent-sweep: 1.009/1.060 secs] "
                + "[Times: user=1.55 sys=0.12, real=1.06 secs]";
        String nextLogLine = " (concurrent mode failure): 2656311K->2658289K(2658304K), 9.3575580 secs] "
                + "4011734K->2725109K(4040704K), [Metaspace: 72111K->72111K(1118208K)], 9.3610080 secs] "
                + "[Times: user=9.35 sys=0.01, real=9.36 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, null, nextLogLine));
    }

    public void testLogLineCmsSerialOldWithConcurrentModeFailureMixedConcurrentPreclean() {
        String logLine = "1278.200: [Full GC (Allocation Failure) 1278.202: [CMS1280.173: "
                + "[CMS-concurrent-preclean: 2.819/2.865 secs] [Times: user=6.97 sys=0.41, real=2.87 secs]";
        String nextLogLine = " (concurrent mode failure): 2658303K->2658303K(2658304K), 9.1546180 secs] "
                + "4040703K->2750110K(4040704K), [Metaspace: 72113K->72113K(1118208K)], 9.1581450 secs] "
                + "[Times: user=9.15 sys=0.00, real=9.16 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, null, nextLogLine));
    }

    public void testLogLineCmsSerialOldWithConcurrentModeFailureMixedConcurrentSweep() {
        String logLine = "2440.336: [Full GC (Allocation Failure) 2440.338: [CMS"
                + "2440.542: [CMS-concurrent-sweep: 1.137/1.183 secs] [Times: user=5.33 sys=0.51, real=1.18 secs]";
        String nextLogLine = " (concurrent mode failure): 2658304K->2658303K(2658304K), 9.4908960 secs] "
                + "4040703K->2996946K(4040704K), [Metaspace: 72191K->72191K(1118208K)], 9.4942330 secs] "
                + "[Times: user=9.49 sys=0.00, real=9.49 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, null, nextLogLine));
    }

    public void testLogLineParNewMixedCmsConcurrentAbortablePreclean() {
        String priorLogLine = "";
        String logLine = "2210.281: [GC 2210.282: [ParNew2210.314: [CMS-concurrent-abortable-preclean: "
                + "0.043/0.144 secs] [Times: user=0.58 sys=0.03, real=0.14 secs]";
        String nextLogLine = ": 212981K->3156K(242304K), 0.0364435 secs] 4712182K->4502357K(4971420K), "
                + "0.0368807 secs] [Times: user=0.18 sys=0.02, real=0.04 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogLineParNewMixedCmsConcurrentAbortablePreclean2() {
        String priorLogLine = "2210.281: [GC 2210.282: [ParNew2210.314: [CMS-concurrent-abortable-preclean: "
                + "0.043/0.144 secs] [Times: user=0.58 sys=0.03, real=0.14 secs]";
        String logLine = ": 212981K->3156K(242304K), 0.0364435 secs] 4712182K->4502357K(4971420K), "
                + "0.0368807 secs] [Times: user=0.18 sys=0.02, real=0.04 secs]";
        String nextLogLine = "";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogLineParNewMixedCmsConcurrentSweep() {
        String priorLogLine = "";
        String logLine = "1821.661: [GC 1821.661: [ParNew1821.661: [CMS-concurrent-sweep: "
                + "42.841/48.076 secs] [Times: user=19.45 sys=0.45, real=48.06 secs]";
        String nextLogLine = ": 36500K->3770K(38336K), 0.1767060 secs] 408349K->375618K(2092928K), "
                + "0.1769190 secs] [Times: user=0.05 sys=0.00, real=0.18 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogLineConcurrentModeInterrupted() {
        String priorLogLine = "";
        String logLine = " (concurrent mode interrupted): 861863K->904027K(1797568K), 42.9053262 secs] "
                + "1045947K->904027K(2047232K), [CMS Perm : 252246K->252202K(262144K)], 42.9070278 secs] "
                + "[Times: user=43.11 sys=0.18, real=42.91 secs]";
        String nextLogLine = "";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogLinePrintHeapAtGcBeginSerial() {
        String priorLogLine = "";
        String logLine = "28282.075: [Full GC {Heap before gc invocations=528:";
        String nextLogLine = "";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogLinePrintHeapAtGcBeginParNew() {
        String priorLogLine = "";
        String logLine = "27067.966: [GC {Heap before gc invocations=498:";
        String nextLogLine = "";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogLinePrintHeapAtGcBeginCmsRemark() {
        String priorLogLine = "";
        String logLine = "2017-04-03T08:55:45.544-0500: 20653.796: [GC (CMS Final Remark) {Heap before GC "
                + "invocations=686 (full 15):";
        String nextLogLine = "";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogLinePrintHeapAtGcBeginCmsRemarkJdk8() {
        String priorLogLine = "";
        String logLine = "2017-06-18T05:23:16.634-0500: 15.364: [GC (CMS Final Remark) [YG occupancy: 576424 K "
                + "(1677760 K)]{Heap before GC invocations=8 (full 2):";
        String nextLogLine = "";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogMiddleSerialConcurrentPrecleanMixed() {
        String priorLogLine = "";
        String logLine = "28282.075: [CMS28284.687: [CMS-concurrent-preclean: 3.706/3.706 secs]";
        String nextLogLine = "";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogMiddleParNewConcurrentAbortablePrecleanMixed() {
        String priorLogLine = "";
        String logLine = "27067.966: [ParNew: 261760K->261760K(261952K), 0.0000160 secs]27067.966: [CMS"
                + "27067.966: [CMS-concurrent-abortable-preclean: 2.272/29.793 secs]";
        String nextLogLine = "";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogMiddleParNewConcurrentMarkMixed() {
        String priorLogLine = "";
        String logLine = "28308.701: [ParNew (promotion failed): 261951K->261951K(261952K), 0.7470390 secs]28309.448: "
                + "[CMS28312.544: [CMS-concurrent-mark: 5.114/5.863 secs]";
        String nextLogLine = "";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogMiddleParNewTruncatedConcurrentMarkMixed() {
        String priorLogLine = "";
        String logLine = ": 153344K->153344K(153344K), 0.2049130 secs]2017-02-15T16:22:05.602+0900: 1223922.433: "
                + "[CMS2017-02-15T16:22:06.001+0900: 1223922.832: [CMS-concurrent-mark: 3.589/4.431 secs] "
                + "[Times: user=6.13 sys=0.89, real=4.43 secs]";
        String nextLogLine = "";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogLinePrintHeapAtGcMiddleSerial() {
        String priorLogLine = "";
        String logLine = "49830.934: [CMS: 1640998K->1616248K(3407872K), 11.0964500 secs] "
                + "1951125K->1616248K(4193600K), [CMS Perm : 507386K->499194K(786432K)]"
                + "Heap after gc invocations=147:";
        String nextLogLine = "";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogLinePrintClassHistogramMiddleSerial() {
        String priorLogLine = "";
        String logLine = "11700.930: [CMS: 2844387K->635365K(7331840K), 46.4488813 secs]11747.379: [Class Histogram";
        String nextLogLine = "";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogLinePrintHeapAtGcMiddleSerialConcurrentModeFailure() {
        String priorLogLine = "";
        String logLine = " (concurrent mode failure): 1179601K->1179648K(1179648K), 10.7510650 secs] "
                + "1441361K->1180553K(1441600K), [CMS Perm : 71172K->71171K(262144K)]Heap after gc invocations=529:";
        String nextLogLine = "";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogLinePrintHeapAtGcParNewConcurrentModeFailure() {
        String priorLogLine = "";
        String logLine = " (concurrent mode failure): 1147900K->1155037K(1179648K), 7.3953900 secs] "
                + "1409660K->1155037K(1441600K)Heap after gc invocations=499:";
        String nextLogLine = "";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogLineDuration() {
        String priorLogLine = "";
        String logLine = ", 10.7515460 secs]";
        String nextLogLine = "";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogLineClassHistogramTrigger() {
        String priorLogLine = "";
        String logLine = "1662.232: [Full GC 11662.233: [Class Histogram:";
        String nextLogLine = "";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogLineDateStampClassHistogramTrigger() {
        String priorLogLine = "";
        String logLine = "2017-04-24T21:07:32.713+0100: 669928.617: [Full GC 669928.619: [Class Histogram:";
        String nextLogLine = "";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogLineRetainMiddleClassHistogram() {
        String priorLogLine = "";
        String logLine = ": 516864K->516864K(516864K), 2.0947428 secs]182316.954: [Class Histogram: ";
        String nextLogLine = "";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogLineRetainEndClassHistogram() {
        String priorLogLine = "";
        String logLine = " 3863904K->756393K(7848704K), [CMS Perm : 682507K->442221K(1048576K)], 107.6553710 secs]"
                + " [Times: user=112.83 sys=0.28, real=107.66 secs]";
        String nextLogLine = "";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogLineRetainBeginningConcurrentModeFailure() {
        String priorLogLine = "";
        String logLine = " (concurrent mode failure): 5355855K->991044K(7331840K), 58.3748587 secs]639860.666: "
                + "[Class Histogram";
        String nextLogLine = "";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogLineRetainMiddleSerialConcurrentMixed() {
        String priorLogLine = "";
        String logLine = ": 917504K->917504K(917504K), 5.5887120 secs]877375.047: [CMS877378.691: "
                + "[CMS-concurrent-mark: 5.714/11.380 secs] [Times: user=14.72 sys=4.81, real=11.38 secs]";
        String nextLogLine = "";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogLineRetainBeginningParNewNoSpaceAfterGc() {
        String priorLogLine = "";
        String logLine = "12.891: [GC12.891: [ParNew";
        String nextLogLine = "";
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
    }

    public void testLogLineParNewPrefixed() {
        String priorLogLine = "";
        String logLine = "831626.089: [ParNew831628.158: [ParNew833918.729: [GC (Allocation Failure) 833918.729: "
                + "[ParNew: 595103K->12118K(619008K), 0.0559019 secs] 1247015K->664144K(4157952K), 0.0561698 secs] "
                + "[Times: user=0.09 sys=0.00, real=0.06 secs]";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        CmsPreprocessAction event = new CmsPreprocessAction(null, logLine, nextLogLine, null, context);
        Assert.assertEquals("Log line not parsed correctly.",
                "833918.729: [GC (Allocation Failure) 833918.729: [ParNew: 595103K->12118K(619008K), 0.0559019 secs] "
                        + "1247015K->664144K(4157952K), 0.0561698 secs] [Times: user=0.09 sys=0.00, real=0.06 secs]",
                event.getLogEntry());
    }

    public void testLogLineBeginningSerialConcurrentWithJvmtiEnvForceGarbageCollectionTrigger() {
        String priorLogLine = "";
        String logLine = "262372.344: [Full GC (JvmtiEnv ForceGarbageCollection) 262372.344: [CMS262372.426: "
                + "[CMS-concurrent-mark: 0.082/0.083 secs] [Times: user=0.08 sys=0.00, real=0.09 secs]";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(null, logLine, nextLogLine, entangledLogLines, context);
        Assert.assertEquals("Log line not parsed correctly.",
                "262372.344: [Full GC (JvmtiEnv ForceGarbageCollection) 262372.344: [CMS", event.getLogEntry());
    }

    public void testLogLineBeginningSerialConcurrentWithMetadataGcThreshold() {
        String priorLogLine = "";
        String logLine = "262375.122: [Full GC (Metadata GC Threshold) 262375.122: [CMS262375.200: "
                + "[CMS-concurrent-mark: 0.082/0.082 secs] [Times: user=0.08 sys=0.00, real=0.08 secs]";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(null, logLine, nextLogLine, entangledLogLines, context);
        Assert.assertEquals("Log line not parsed correctly.",
                "262375.122: [Full GC (Metadata GC Threshold) 262375.122: [CMS", event.getLogEntry());
    }

    public void testLogLineBeginningSerialNoSpaceAfterTrigger() {
        String priorLogLine = "";
        String logLine = "5026.107: [Full GC (Allocation Failure)5026.108: [CMS"
                + "5027.062: [CMS-concurrent-sweep: 9.543/33.853 secs] "
                + "[Times: user=107.27 sys=5.82, real=33.85 secs]";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(null, logLine, nextLogLine, entangledLogLines, context);
        Assert.assertEquals("Log line not parsed correctly.", "5026.107: [Full GC (Allocation Failure)5026.108: [CMS",
                event.getLogEntry());
    }

    public void testLogLineBeginningSerialConcurrentWithGcLockerInitiatedGc() {
        String priorLogLine = "";
        String logLine = "58626.878: [Full GC (GCLocker Initiated GC)58626.878: [CMS"
                + "58630.075: [CMS-concurrent-sweep: 3.220/3.228 secs] [Times: user=3.38 sys=0.01, real=3.22 secs]";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(null, logLine, nextLogLine, entangledLogLines, context);
        Assert.assertEquals("Log line not parsed correctly.",
                "58626.878: [Full GC (GCLocker Initiated GC)58626.878: [CMS", event.getLogEntry());
    }

    public void testLogLineBeginningParNewWithFlsStatistics() {
        String priorLogLine = "";
        String logLine = "1.118: [GC Before GC:";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(null, logLine, nextLogLine, entangledLogLines, context);
        Assert.assertEquals("Log line not parsed correctly.", "1.118: [GC ", event.getLogEntry());
    }

    public void testLogLineMiddleParNewWithFlsStatistics() {
        String priorLogLine = "";
        String logLine = "1.118: [ParNew: 377487K->8426K(5505024K), 0.0535260 secs] 377487K->8426K(43253760K)After GC:";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(null, logLine, nextLogLine, entangledLogLines, context);
        Assert.assertEquals("Log line not parsed correctly.",
                "1.118: [ParNew: 377487K->8426K(5505024K), 0.0535260 secs] 377487K->8426K(43253760K)",
                event.getLogEntry());
    }

    public void testLogLineDurationWithTimeStamp() {
        String priorLogLine = "";
        String logLine = ", 0.0536040 secs] [Times: user=0.89 sys=0.01, real=0.06 secs]";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(null, logLine, nextLogLine, entangledLogLines, context);
        Assert.assertEquals("Log line not parsed correctly.", logLine, event.getLogEntry());
    }

    public void testLogLinParNewPromotionFailedTruncatedWithCmsConcurrentMark() {
        String priorLogLine = "";
        String logLine = "36455.096: [GC 36455.096: [ParNew (promotion failed): 153344K->153344K(153344K), "
                + "0.6818450 secs]36455.778: [CMS36459.090: [CMS-concurrent-mark: 3.439/4.155 secs] "
                + "[Times: user=8.27 sys=0.17, real=4.16 secs]";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(null, logLine, nextLogLine, entangledLogLines, context);
        Assert.assertEquals("Log line not parsed correctly.",
                "36455.096: [GC 36455.096: [ParNew (promotion failed): 153344K->153344K(153344K), 0.6818450 secs]"
                        + "36455.778: [CMS",
                event.getLogEntry());
    }

    public void testLogLinParNewPromotionFailedTruncatedWithCmsConcurrentPreclean() {
        String priorLogLine = "";
        String logLine = "65778.258: [GC65778.258: [ParNew (promotion failed): 8300210K->8088352K(8388608K), "
                + "1.4967400 secs]65779.755: [CMS65781.579: [CMS-concurrent-preclean: 2.150/47.638 secs] "
                + "[Times: user=81.22 sys=2.02, real=47.63 secs]";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(null, logLine, nextLogLine, entangledLogLines, context);
        Assert.assertEquals("Log line not parsed correctly.",
                "65778.258: [GC65778.258: [ParNew (promotion failed): 8300210K->8088352K(8388608K), "
                        + "1.4967400 secs]65779.755: [CMS",
                event.getLogEntry());
    }

    public void testLogLineBeginningParNewWithNoParNewWithCmsConcurrentPreclean() {
        String priorLogLine = "";
        String logLine = "3576157.596: [GC 3576157.596: [CMS-concurrent-abortable-preclean: 0.997/1.723 secs] "
                + "[Times: user=3.20 sys=0.03, real=1.73 secs]";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(null, logLine, nextLogLine, entangledLogLines, context);
        Assert.assertEquals("Log line not parsed correctly.", "3576157.596: [GC ", event.getLogEntry());
    }

    public void testLogLinParNewPromotionFailedWithCmsAbortPrecleanDueToTime() {
        String priorLogLine = "";
        String logLine = "73241.738: [GC (Allocation Failure)73241.738: [ParNew (promotion failed): "
                + "8205461K->8187503K(8388608K), 2.1449990 secs]73243.883: [CMS CMS: abort preclean due to time "
                + "3244.984: [CMS-concurrent-abortable-preclean: 3.335/9.080 secs] "
                + "[Times: user=43.26 sys=1.66, real=9.08 secs]";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(null, logLine, nextLogLine, entangledLogLines, context);
        Assert.assertEquals("Log line not parsed correctly.",
                "73241.738: [GC (Allocation Failure)73241.738: [ParNew (promotion failed): "
                        + "8205461K->8187503K(8388608K), 2.1449990 secs]73243.883: [CMS",
                event.getLogEntry());
    }

    public void testLogLineEndParNew() {
        String priorLogLine = "";
        String logLine = "3576157.596: [ParNew: 147599K->17024K(153344K), 0.0795160 secs] "
                + "2371401K->2244459K(6274432K), 0.0810030 secs] [Times: user=0.44 sys=0.00, real=0.08 secs]";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(priorLogLine, logLine, nextLogLine, entangledLogLines,
                context);
        Assert.assertEquals("Log line not parsed correctly.", logLine, event.getLogEntry());
    }

    public void testLogLineBeginningParNewDatestamp() {
        String priorLogLine = "";
        String logLine = "2016-09-07T16:59:44.005-0400: 26536.942: [GC"
                + "2016-09-07T16:59:44.005-0400: 26536.943: [ParNew";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(priorLogLine, logLine, nextLogLine, entangledLogLines,
                context);
        Assert.assertEquals("Log line not parsed correctly.", logLine, event.getLogEntry());
    }

    public void testLogLineCmsSerialOldCombinedConcurrentDatestamp() {
        String priorLogLine = "";
        String logLine = "2016-10-10T19:17:37.771-0700: 2030.108: [GC (Allocation Failure) "
                + "2016-10-10T19:17:37.771-0700: 2030.108: [ParNew2016-10-10T19:17:37.773-0700: "
                + "2030.110: [CMS-concurrent-abortable-preclean: 0.050/0.150 secs] "
                + "[Times: user=0.11 sys=0.03, real=0.15 secs]";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(priorLogLine, logLine, nextLogLine, entangledLogLines,
                context);
        Assert.assertEquals("Log line not parsed correctly.",
                "2016-10-10T19:17:37.771-0700: 2030.108: [GC (Allocation Failure) "
                        + "2016-10-10T19:17:37.771-0700: 2030.108: [ParNew",
                event.getLogEntry());
    }

    public void testLogLineBeginParNewCombinedFlsStatistics() {
        String priorLogLine = "";
        String logLine = "2017-02-27T14:29:54.533+0000: 2.730: [GC (Allocation Failure) Before GC:";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(priorLogLine, logLine, nextLogLine, entangledLogLines,
                context);
        Assert.assertEquals("Log line not parsed correctly.",
                "2017-02-27T14:29:54.533+0000: 2.730: [GC (Allocation Failure) ", event.getLogEntry());
    }

    public void testLogLineMiddleParNewCombinedFlsStatistics() {
        String priorLogLine = "";
        String logLine = "2017-02-27T14:29:54.534+0000: 2.730: [ParNew: 2048000K->191475K(2304000K), 0.0366288 secs] "
                + "2048000K->191475K(7424000K)After GC:";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(priorLogLine, logLine, nextLogLine, entangledLogLines,
                context);
        Assert.assertEquals("Log line not parsed correctly.",
                "2017-02-27T14:29:54.534+0000: 2.730: [ParNew: 2048000K->191475K(2304000K), 0.0366288 secs] "
                        + "2048000K->191475K(7424000K)",
                event.getLogEntry());
    }

    public void testLogLineMiddleParNewCombinedFlsStatisticsPrintPromotionFailure() {
        String priorLogLine = "";
        String logLine = "2017-02-28T00:43:55.587+0000: 36843.783: [ParNew (0: promotion failure size = 200)  "
                + "(1: promotion failure size = 8)  (2: promotion failure size = 200)  "
                + "(3: promotion failure size = 200)  (4: promotion failure size = 200)  "
                + "(5: promotion failure size = 200)  (6: promotion failure size = 200)  "
                + "(7: promotion failure size = 200)  (8: promotion failure size = 10)  "
                + "(9: promotion failure size = 10)  (10: promotion failure size = 10)  "
                + "(11: promotion failure size = 200)  (12: promotion failure size = 200)  "
                + "(13: promotion failure size = 10)  (14: promotion failure size = 200)  "
                + "(15: promotion failure size = 200)  (16: promotion failure size = 200)  "
                + "(17: promotion failure size = 200)  (18: promotion failure size = 200)  "
                + "(19: promotion failure size = 200)  (20: promotion failure size = 10)  "
                + "(21: promotion failure size = 200)  (22: promotion failure size = 10)  "
                + "(23: promotion failure size = 45565)  (24: promotion failure size = 10)  "
                + "(25: promotion failure size = 4)  (26: promotion failure size = 200)  "
                + "(27: promotion failure size = 200)  (28: promotion failure size = 10)  "
                + "(29: promotion failure size = 200)  (30: promotion failure size = 200)  "
                + "(31: promotion failure size = 200)  (32: promotion failure size = 200)  "
                + "(promotion failed): 2304000K->2304000K(2304000K), 0.4501923 secs]"
                + "2017-02-28T00:43:56.037+0000: 36844.234: [CMSCMS: Large block 0x0000000730892bb8";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(priorLogLine, logLine, nextLogLine, entangledLogLines,
                context);
        String logLinePreprocessed = "2017-02-28T00:43:55.587+0000: 36843.783: [ParNew "
                + "(0: promotion failure size = 200)  (1: promotion failure size = 8)  "
                + "(2: promotion failure size = 200)  (3: promotion failure size = 200)  "
                + "(4: promotion failure size = 200)  (5: promotion failure size = 200)  "
                + "(6: promotion failure size = 200)  (7: promotion failure size = 200)  "
                + "(8: promotion failure size = 10)  (9: promotion failure size = 10)  "
                + "(10: promotion failure size = 10)  (11: promotion failure size = 200)  "
                + "(12: promotion failure size = 200)  (13: promotion failure size = 10)  "
                + "(14: promotion failure size = 200)  (15: promotion failure size = 200)  "
                + "(16: promotion failure size = 200)  (17: promotion failure size = 200)  "
                + "(18: promotion failure size = 200)  (19: promotion failure size = 200)  "
                + "(20: promotion failure size = 10)  (21: promotion failure size = 200)  "
                + "(22: promotion failure size = 10)  (23: promotion failure size = 45565)  "
                + "(24: promotion failure size = 10)  (25: promotion failure size = 4)  "
                + "(26: promotion failure size = 200)  (27: promotion failure size = 200)  "
                + "(28: promotion failure size = 10)  (29: promotion failure size = 200)  "
                + "(30: promotion failure size = 200)  (31: promotion failure size = 200)  "
                + "(32: promotion failure size = 200)  (promotion failed): 2304000K->2304000K(2304000K), "
                + "0.4501923 secs]2017-02-28T00:43:56.037+0000: 36844.234: [CMS";
        Assert.assertEquals("Log line not parsed correctly.", logLinePreprocessed, event.getLogEntry());
    }

    public void testLogLineMiddleSerialFlsStatistics() {
        String priorLogLine = "";
        String logLine = ": 2818067K->2769354K(5120000K), 3.8341757 secs] 5094036K->2769354K(7424000K), "
                + "[Metaspace: 18583K->18583K(1067008K)]After GC:";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(priorLogLine, logLine, nextLogLine, entangledLogLines,
                context);
        Assert.assertEquals("Log line not parsed correctly.",
                ": 2818067K->2769354K(5120000K), 3.8341757 secs] 5094036K->2769354K(7424000K), "
                        + "[Metaspace: 18583K->18583K(1067008K)]",
                event.getLogEntry());
    }

    public void testLogLineMiddleParNewTruncatedBeginMixedConcurrentFlsStatistics2() {
        String priorLogLine = "";
        String logLine = "2017-03-19T11:48:55.207+0000: 356616.193: [ParNew2017-03-19T11:48:55.211+0000: 356616.198: "
                + "[CMS-concurrent-abortable-preclean: 1.046/3.949 secs] [Times: user=1.16 sys=0.05, real=3.95 secs]";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(priorLogLine, logLine, nextLogLine, entangledLogLines,
                context);
        Assert.assertEquals("Log line not parsed correctly.", "2017-03-19T11:48:55.207+0000: 356616.193: [ParNew",
                event.getLogEntry());
    }

    public void testLogLineMiddleParNewTruncatedEndMixedConcurrentFlsStatistics2() {
        String priorLogLine = "";
        String logLine = ": 66097K->7194K(66368K), 0.0440189 secs] 5274098K->5219953K(10478400K)After GC:";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(priorLogLine, logLine, nextLogLine, entangledLogLines,
                context);
        Assert.assertEquals("Log line not parsed correctly.",
                ": 66097K->7194K(66368K), 0.0440189 secs] 5274098K->5219953K(10478400K)", event.getLogEntry());
    }

    public void testLogLineEndCmsScavengeBeforeRemark() {
        String priorLogLine = "";
        String logLine = " 1677988K(7992832K), 0.3055773 secs]";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(priorLogLine, logLine, nextLogLine, entangledLogLines,
                context);
        Assert.assertEquals("Log line not parsed correctly.", " 1677988K(7992832K), 0.3055773 secs]",
                event.getLogEntry());
    }

    public void testLogLineCmsRemarkWithoutGcDetails() {
        String priorLogLine = "";
        String logLine = "2017-04-03T03:12:02.134-0500: 30.385: [GC (CMS Final Remark)  890910K->620060K(7992832K), "
                + "0.1223879 secs]";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(priorLogLine, logLine, nextLogLine, entangledLogLines,
                context);
        Assert.assertEquals("Log line not parsed correctly.", logLine, event.getLogEntry());
    }

    public void testLogLineMiddleCmsSerialOldMixedAbortPrecleanDueToTime() {
        String priorLogLine = "";
        String logLine = "471419.156: [CMS CMS: abort preclean due to time 2017-04-22T13:59:06.831+0100: 471423.282: "
                + "[CMS-concurrent-abortable-preclean: 3.663/31.735 secs] "
                + "[Times: user=39.81 sys=0.23, real=31.74 secs]";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(priorLogLine, logLine, nextLogLine, entangledLogLines,
                context);
        Assert.assertEquals("Log line not parsed correctly.", "471419.156: [CMS", event.getLogEntry());
    }

    public void testLogLineMiddleCmsSerialOldMixedConcurrentSweep() {
        String priorLogLine = "";
        String logLine = "669950.539: [CMS2017-04-24T21:08:04.965+0100: 669960.868: [CMS-concurrent-sweep: "
                + "13.324/39.970 secs] [Times: user=124.31 sys=2.44, real=39.97 secs]";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(priorLogLine, logLine, nextLogLine, entangledLogLines,
                context);
        Assert.assertEquals("Log line not parsed correctly.", "669950.539: [CMS", event.getLogEntry());
    }

    public void testLogLineMiddleCmsSerialOldWithDatestampMixedConcurrentSweep() {
        String priorLogLine = "";
        String logLine = "2017-05-03T14:47:16.910-0400: 1801.570: [CMS2017-05-03T14:47:22.416-0400: 1807.075: "
                + "[CMS-concurrent-mark: 29.707/71.001 secs] [Times: user=121.03 sys=35.41, real=70.99 secs]";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(priorLogLine, logLine, nextLogLine, entangledLogLines,
                context);
        Assert.assertEquals("Log line not parsed correctly.", "2017-05-03T14:47:16.910-0400: 1801.570: [CMS",
                event.getLogEntry());
    }

    public void testLogLineEndCmsSerialOld() {
        String priorLogLine = "";
        String logLine = " 7778348K->1168095K(7848704K), [CMS Perm : 481281K->451017K(771512K)], 123.0277354 secs] "
                + "[Times: user=123.19 sys=0.18, real=123.03 secs]";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(priorLogLine, logLine, nextLogLine, entangledLogLines,
                context);
        Assert.assertEquals("Log line not parsed correctly.", logLine, event.getLogEntry());
    }

    public void testLogLineBeginningParNewConcurrentModeFailureClassHistogram() {
        String priorLogLine = "";
        String logLine = "2017-04-22T12:43:48.008+0100: 466904.470: [GC 466904.473: [ParNew: "
                + "516864K->516864K(516864K), 0.0001999 secs]466904.473: [Class Histogram:";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(priorLogLine, logLine, nextLogLine, entangledLogLines,
                context);
        Assert.assertEquals("Log line not parsed correctly.", logLine, event.getLogEntry());
    }

    public void testLogLineBeginningParNewConcurrentModeFailureClassHistogramWithDatestamps() {
        String priorLogLine = "";
        String logLine = "2017-05-03T14:47:00.002-0400: 1784.661: [GC 2017-05-03T14:47:00.006-0400: 1784.664: "
                + "[ParNew: 4147200K->4147200K(4147200K), 0.0677200 secs]"
                + "2017-05-03T14:47:00.075-0400: 1784.735: [Class Histogram:";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(priorLogLine, logLine, nextLogLine, entangledLogLines,
                context);
        Assert.assertEquals("Log line not parsed correctly.", logLine, event.getLogEntry());
    }

    public void testLogLineMiddleConcurrentModeFailureMixedClassHistogram() {
        String priorLogLine = "";
        String logLine = " (concurrent mode failure): 7835032K->8154090K(9216000K), 56.0787320 secs]"
                + "2017-05-03T14:48:13.002-0400: 1857.661: [Class Histogram";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(null, logLine, nextLogLine, entangledLogLines, context);
        Assert.assertEquals("Log line not parsed correctly.", logLine, event.getLogEntry());
    }

    public void testLogLineBeginningSerialMixedClassHistogramWithDatestamp() {
        String priorLogLine = "";
        String logLine = "2017-05-03T14:51:32.659-0400: 2057.323: [Full GC "
                + "2017-05-03T14:51:32.680-0400: 2057.341: [Class Histogram:";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(null, logLine, nextLogLine, entangledLogLines, context);
        Assert.assertEquals("Log line not parsed correctly.", logLine, event.getLogEntry());
    }

    public void testLogLineBeginningCmsConcurrentMixedApplicationConcurrentTime() {
        String priorLogLine = "";
        String logLine = "2017-06-18T05:23:03.452-0500: 2.182: 2017-06-18T05:23:03.452-0500: "
                + "[CMS-concurrent-preclean: 0.016/0.048 secs]2.182: Application time: 0.0055079 seconds";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(null, logLine, nextLogLine, entangledLogLines, context);
        Assert.assertEquals("Log line not parsed correctly.",
                "2017-06-18T05:23:03.452-0500: 2.182: [CMS-concurrent-preclean: 0.016/0.048 secs]",
                event.getLogEntry());
    }

    public void testLogLineEndTimesData() {
        String priorLogLine = "2017-06-18T05:23:03.452-0500: 2.182: 2017-06-18T05:23:03.452-0500: "
                + "[CMS-concurrent-preclean: 0.016/0.048 secs]2.182: Application time: 0.0055079 seconds";
        String logLine = " [Times: user=0.15 sys=0.02, real=0.05 secs]";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(priorLogLine, logLine, nextLogLine, entangledLogLines,
                context);
        Assert.assertEquals("Log line not parsed correctly.", logLine, event.getLogEntry());
    }

    public void testLogLineMiddleCmsRemarkJdk8() {
        String priorLogLine = "";
        String logLine = "2017-06-18T05:23:16.634-0500: 15.364: [GC (CMS Final Remark) 2017-06-18T05:23:16.634-0500: "
                + "15.364: [ParNew";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(null, logLine, nextLogLine, entangledLogLines, context);
        Assert.assertEquals("Log line not parsed correctly.", logLine, event.getLogEntry());
    }

    public void testLogLineBeginningParNewTenuringDistribution() {
        String priorLogLine = "";
        String logLine = "2016-09-23T09:05:18.745-0700: 2.372: [GC (Allocation Failure) "
                + "2016-09-23T09:05:18.745-0700: 2.372: [ParNew";
        String nextLogLine = "Desired survivor size 78643200 bytes, new threshold 15 (max 15)";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(null, logLine, nextLogLine, entangledLogLines, context);
        Assert.assertEquals("Log line not parsed correctly.", logLine, event.getLogEntry());
    }

    public void testLogLineBeginningParNewMixedHeapAtGc() {
        String priorLogLine = "";
        String logLine = "4237.297: [GC[YG occupancy: 905227 K (4194240 K)]{Heap before GC invocations=85 (full 1):";
        String nextLogLine = "";
        Set<String> context = new HashSet<String>();
        Assert.assertTrue("Log line not recognized as " + PreprocessActionType.CMS.toString() + ".",
                CmsPreprocessAction.match(logLine, priorLogLine, nextLogLine));
        List<String> entangledLogLines = new ArrayList<String>();
        CmsPreprocessAction event = new CmsPreprocessAction(null, logLine, nextLogLine, entangledLogLines, context);
        Assert.assertEquals("Log line not parsed correctly.", "4237.297: [GC[YG occupancy: 905227 K (4194240 K)]",
                event.getLogEntry());
    }

    /**
     * Test preprocessing <code>PrintHeapAtGcEvent</code> with underlying <code>CmsSerialOldEvent</code>.
     */
    public void testSplitPrintHeapAtGcCmsSerialOldLogging() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset6.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 1, jvmRun.getEventTypes().size());
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_SERIAL_OLD.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_SERIAL_OLD));
        Assert.assertTrue(Analysis.WARN_PRINT_HEAP_AT_GC + " analysis not identified.",
                jvmRun.getAnalysis().contains(Analysis.WARN_PRINT_HEAP_AT_GC));
    }

    /**
     * Test with underlying <code>CmsSerialOld</code> triggered by concurrent mode failure.
     */
    public void testSplitPrintHeapAtGcCmsSerialOldConcurrentModeFailureLogging() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset8.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_SERIAL_OLD.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_SERIAL_OLD));
        Assert.assertTrue(Analysis.ERROR_CMS_CONCURRENT_MODE_FAILURE + " analysis not identified.",
                jvmRun.getAnalysis().contains(Analysis.ERROR_CMS_CONCURRENT_MODE_FAILURE));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_CONCURRENT));
        Assert.assertTrue(Analysis.WARN_PRINT_HEAP_AT_GC + " analysis not identified.",
                jvmRun.getAnalysis().contains(Analysis.WARN_PRINT_HEAP_AT_GC));
    }

    /**
     * Test <code>CmsPreprocessAction</code>: split <code>CmsSerialOldEvent</code> and <code>CmsConcurrentEvent</code>.
     */
    public void testSplitCmsConcurrentModeFailureEventMarkLogging() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset10.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertFalse(LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_SERIAL_OLD.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_SERIAL_OLD));
        Assert.assertTrue(JdkUtil.TriggerType.CMS_CONCURRENT_MODE_FAILURE.toString() + " trigger not identified.",
                jvmRun.getAnalysis().contains(Analysis.ERROR_CMS_CONCURRENT_MODE_FAILURE));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_CONCURRENT));
    }

    /**
     * Test <code>CmsPreprocessAction</code>: split <code>CmsSerialOldEvent</code> and <code>CmsConcurrentEvent</code>.
     */
    public void testSplitCmsConcurrentModeFailureEventAbortablePrecleanLogging() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset11.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertFalse(LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_SERIAL_OLD.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_SERIAL_OLD));
        Assert.assertTrue(JdkUtil.TriggerType.CMS_CONCURRENT_MODE_FAILURE.toString() + " trigger not identified.",
                jvmRun.getAnalysis().contains(Analysis.ERROR_CMS_CONCURRENT_MODE_FAILURE));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_CONCURRENT));
    }

    /**
     * Test preprocessing <code>CmsSerialOldConcurrentModeFailureEvent</code> split over 3 lines.
     */
    public void testSplit3LinesCmsConcurrentModeFailureEventLogging() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset14.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertFalse(LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_SERIAL_OLD.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_SERIAL_OLD));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_CONCURRENT));
    }

    /**
     * Test preprocessing a split <code>ParNewPromotionFailedCmsConcurrentModeFailurePermDataEvent</code> with
     * -XX:+PrintTenuringDistribution logging between the initial and final lines.
     */
    public void testSplitMixedTenuringParNewPromotionFailedEventLogging() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset18.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertFalse(LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_SERIAL_OLD.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_SERIAL_OLD));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_CONCURRENT));
    }

    /**
     * Test preprocessing <code>PrintHeapAtGcEvent</code> with underlying <code>ParNewConcurrentModeFailureEvent</code>.
     */
    public void testSplitPrintHeapAtGcParNewPromotionFailedCmsConcurrentModeFailureEventLogging() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset21.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertFalse(LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_SERIAL_OLD.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_SERIAL_OLD));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_CONCURRENT));
        Assert.assertTrue(Analysis.WARN_PRINT_HEAP_AT_GC + " analysis not identified.",
                jvmRun.getAnalysis().contains(Analysis.WARN_PRINT_HEAP_AT_GC));
    }

    public void testParNewPromotionFailedTruncatedEventLogging() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset23.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertFalse(LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertEquals("Event type count not correct.", 3, jvmRun.getEventTypes().size());
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.CMS_SERIAL_OLD.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.CMS_SERIAL_OLD));
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.CMS_CONCURRENT));
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.PAR_NEW.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.PAR_NEW));
        Assert.assertTrue(Analysis.ERROR_CMS_PROMOTION_FAILED + " analysis not identified.",
                jvmRun.getAnalysis().contains(Analysis.ERROR_CMS_PROMOTION_FAILED));
    }

    /**
     * Test PAR_NEW mixed with CMS_CONCURRENT over 2 lines.
     * 
     */
    public void testParNewMixedCmsConcurrent() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset58.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertFalse(LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertTrue(LogEventType.PAR_NEW.toString() + " collector not identified.",
                jvmRun.getEventTypes().contains(LogEventType.PAR_NEW));
        Assert.assertTrue(LogEventType.CMS_CONCURRENT.toString() + " collector not identified.",
                jvmRun.getEventTypes().contains(LogEventType.CMS_CONCURRENT));
    }

    /**
     * Test CMS_SERIAL_OLD with concurrent mode failure trigger mixed with CMS_CONCURRENT over 2 lines.
     * 
     */
    public void testCmsSerialConcurrentModeFailureMixedCmsConcurrent() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset61.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertFalse(LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertTrue(LogEventType.CMS_SERIAL_OLD.toString() + " collector not identified.",
                jvmRun.getEventTypes().contains(LogEventType.CMS_SERIAL_OLD));
        Assert.assertTrue(LogEventType.CMS_CONCURRENT.toString() + " collector not identified.",
                jvmRun.getEventTypes().contains(LogEventType.CMS_CONCURRENT));
        Assert.assertTrue(Analysis.ERROR_CMS_CONCURRENT_MODE_FAILURE + " analysis not identified.",
                jvmRun.getAnalysis().contains(Analysis.ERROR_CMS_CONCURRENT_MODE_FAILURE));
    }

    /**
     * Test split <code>ParNewEvent</code> with a trigger and -XX:+PrintTenuringDistribution logging between the initial
     * and final lines.
     */
    public void testSplitMixedTenuringParNewPromotionEventWithTriggerLogging() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset67.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 1, jvmRun.getEventTypes().size());
        Assert.assertTrue("Log line not recognized as " + LogEventType.PAR_NEW.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.PAR_NEW));
    }

    /**
     * Test CMS_SERIAL_OLD with concurrent mode failure trigger mixed with CMS_CONCURRENT over 2 lines on JDK8.
     * 
     */
    public void testCmsSerialConcurrentModeFailureMixedCmsConcurrentJdk8() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset69.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertFalse(LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertTrue(LogEventType.CMS_SERIAL_OLD.toString() + " collector not identified.",
                jvmRun.getEventTypes().contains(LogEventType.CMS_SERIAL_OLD));
        Assert.assertTrue(LogEventType.CMS_CONCURRENT.toString() + " collector not identified.",
                jvmRun.getEventTypes().contains(LogEventType.CMS_CONCURRENT));
        Assert.assertTrue(Analysis.WARN_CMS_INCREMENTAL_MODE + " analysis not identified.",
                jvmRun.getAnalysis().contains(Analysis.WARN_CMS_INCREMENTAL_MODE));
        Assert.assertTrue(Analysis.ERROR_CMS_CONCURRENT_MODE_FAILURE + " analysis not identified.",
                jvmRun.getAnalysis().contains(Analysis.ERROR_CMS_CONCURRENT_MODE_FAILURE));
    }

    /**
     * Test CMS_SERIAL_OLD with concurrent mode interrupted trigger mixed with CMS_CONCURRENT over 2 lines.
     * 
     */
    public void testCmsSerialOldConcurrentModeInterruptedMixedCmsConcurrent() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset71.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertFalse(LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_SERIAL_OLD.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_SERIAL_OLD));
        Assert.assertTrue(JdkUtil.TriggerType.CMS_CONCURRENT_MODE_INTERRUPTED.toString() + " trigger not identified.",
                jvmRun.getAnalysis().contains(Analysis.ERROR_CMS_CONCURRENT_MODE_INTERRUPTED));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_CONCURRENT));
    }

    /**
     * Test preprocessing CMS_SERIAL_OLD triggered by <code>PrintClassHistogramEvent</code> across many lines.
     * 
     */
    public void testCmsSerialOldPrintClassHistogramTriggerAcross5Lines() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset81.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 1, jvmRun.getEventTypes().size());
        Assert.assertFalse(LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_SERIAL_OLD.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_SERIAL_OLD));
    }

    /**
     * Test preprocessing PAR_NEW mixed with <code>PrintHeapAtGcEvent</code>.
     * 
     */
    public void testParNewPrintHeapAtGc() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset83.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 1, jvmRun.getEventTypes().size());
        Assert.assertFalse(LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertTrue("Log line not recognized as " + LogEventType.PAR_NEW.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.PAR_NEW));
        Assert.assertTrue(Analysis.WARN_PRINT_HEAP_AT_GC + " analysis not identified.",
                jvmRun.getAnalysis().contains(Analysis.WARN_PRINT_HEAP_AT_GC));
    }

    /**
     * Test preprocessing PAR_NEW with extraneous prefix.
     * 
     */
    public void testParNewPrefixed() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset89.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 1, jvmRun.getEventTypes().size());
        Assert.assertFalse(LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertTrue("Log line not recognized as " + LogEventType.PAR_NEW.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.PAR_NEW));
    }

    /**
     * Test preprocessing CMS_SERIAL_OLD with JvmtiEnv ForceGarbageCollection and concurrent mode interrupted.
     * 
     */
    public void testCmsSerialOldTriggerJvmtiEnvForceGarbageCollectionWithConcurrentModeInterrupted() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset90.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertFalse(LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_SERIAL_OLD.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_SERIAL_OLD));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_CONCURRENT));
        Assert.assertTrue(Analysis.ERROR_CMS_CONCURRENT_MODE_INTERRUPTED + " analysis not identified.",
                jvmRun.getAnalysis().contains(Analysis.ERROR_CMS_CONCURRENT_MODE_INTERRUPTED));
    }

    /**
     * Test preprocessing CMS_SERIAL_OLD with Metadata GC Threshold and concurrent mode interrupted.
     * 
     */
    public void testCmsSerialOldTriggerMetadataGcThresholdWithConcurrentModeInterrupted() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset91.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertFalse(LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_SERIAL_OLD.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_SERIAL_OLD));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_CONCURRENT));
        Assert.assertTrue(Analysis.ERROR_CMS_CONCURRENT_MODE_FAILURE + " analysis not identified.",
                jvmRun.getAnalysis().contains(Analysis.ERROR_CMS_CONCURRENT_MODE_FAILURE));
    }

    /**
     * Test preprocessing PAR_NEW with FLS_STATISTICS.
     * 
     */
    public void testParNewWithFlsStatistics() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset94.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 1, jvmRun.getEventTypes().size());
        Assert.assertFalse(LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertTrue("Log line not recognized as " + LogEventType.PAR_NEW.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.PAR_NEW));
        Assert.assertTrue(Analysis.INFO_PRINT_FLS_STATISTICS + " analysis not identified.",
                jvmRun.getAnalysis().contains(Analysis.INFO_PRINT_FLS_STATISTICS));
    }

    public void testBeginningParNewWithNoParNewWithCmsConcurrentPreclean() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset105.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 3, jvmRun.getEventTypes().size());
        Assert.assertFalse(LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertTrue("Log line not recognized as " + LogEventType.PAR_NEW.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.PAR_NEW));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_REMARK.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_REMARK));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_CONCURRENT));
        Assert.assertFalse(Analysis.WARN_CMS_CLASS_UNLOADING_NOT_ENABLED + " analysis identified.",
                jvmRun.getAnalysis().contains(Analysis.WARN_CMS_CLASS_UNLOADING_NOT_ENABLED));
        Assert.assertFalse(Analysis.WARN_CMS_CLASS_UNLOADING_DISABLED + " analysis identified.",
                jvmRun.getAnalysis().contains(Analysis.WARN_CMS_CLASS_UNLOADING_DISABLED));
    }

    public void testUnknownWithCmsConcurrent() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset111.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        String lastLogLineUnprocessed = "130454.251: [Full GC (Allocation Failure) 130454.251: [CMS130456.427: "
                + "[CMS-concurrent-mark: 2.176/2.182 secs] [Times: user=2.18 sys=0.00, real=2.18 secs]";
        Assert.assertEquals("Last unprocessed log line not correct.", lastLogLineUnprocessed,
                gcManager.getLastLogLineUnprocessed());
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertTrue("Log line not recognized as " + LogEventType.UNKNOWN.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_CONCURRENT));
        // Not the last preprocessed line, but part of last unpreprocessed line
        Assert.assertTrue(Analysis.INFO_UNIDENTIFIED_LOG_LINE_LAST + " analysis not identified.",
                jvmRun.getAnalysis().contains(Analysis.INFO_UNIDENTIFIED_LOG_LINE_LAST));
    }

    public void testParNewCmsConcurrentOver3Lines() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset112.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.CMS_SERIAL_OLD.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.CMS_SERIAL_OLD));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_CONCURRENT));
        Assert.assertTrue(Analysis.ERROR_CMS_CONCURRENT_MODE_FAILURE + " analysis not identified.",
                jvmRun.getAnalysis().contains(Analysis.ERROR_CMS_CONCURRENT_MODE_FAILURE));
    }

    public void testPrintPromotionFailure() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset115.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 4, jvmRun.getEventTypes().size());
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.CMS_SERIAL_OLD.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.CMS_SERIAL_OLD));
        Assert.assertTrue(Analysis.ERROR_CMS_PROMOTION_FAILED + " analysis not identified.",
                jvmRun.getAnalysis().contains(Analysis.ERROR_CMS_PROMOTION_FAILED));
    }

    public void testPrintFLSStatistics2ParNewOver4Lines() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset117.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.PAR_NEW.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.PAR_NEW));
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.CMS_CONCURRENT));
        Assert.assertTrue(Analysis.INFO_PRINT_FLS_STATISTICS + " analysis not identified.",
                jvmRun.getAnalysis().contains(Analysis.INFO_PRINT_FLS_STATISTICS));
    }

    public void testCmsScavengeBeforeRemarkNoPrintGcDetails() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset120.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 4, jvmRun.getEventTypes().size());
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.PAR_NEW.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.PAR_NEW));
    }

    public void testParNewConcurrentModeFailureMixedAbortPrecleanDueToTime() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset121.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.CMS_SERIAL_OLD.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.CMS_SERIAL_OLD));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_CONCURRENT));
        Assert.assertTrue(Analysis.ERROR_CMS_CONCURRENT_MODE_FAILURE + " analysis not identified.",
                jvmRun.getAnalysis().contains(Analysis.ERROR_CMS_CONCURRENT_MODE_FAILURE));
    }

    public void testParNewConcurrentModeFailureMixedConcurrentPreclean() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset122.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.CMS_SERIAL_OLD.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.CMS_SERIAL_OLD));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_CONCURRENT));
        Assert.assertTrue(Analysis.ERROR_CMS_CONCURRENT_MODE_FAILURE + " analysis not identified.",
                jvmRun.getAnalysis().contains(Analysis.ERROR_CMS_CONCURRENT_MODE_FAILURE));
    }

    public void testParNewConcurrentModeFailureMixedConcurrentMark() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset123.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.CMS_SERIAL_OLD.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.CMS_SERIAL_OLD));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_CONCURRENT));
        Assert.assertTrue(Analysis.ERROR_CMS_CONCURRENT_MODE_FAILURE + " analysis not identified.",
                jvmRun.getAnalysis().contains(Analysis.ERROR_CMS_CONCURRENT_MODE_FAILURE));
    }

    public void testCmsSerialOldConcurrentModeFailureMixedConcurrentMark() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset124.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.CMS_SERIAL_OLD.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.CMS_SERIAL_OLD));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_CONCURRENT));
        Assert.assertTrue(Analysis.ERROR_CMS_CONCURRENT_MODE_FAILURE + " analysis not identified.",
                jvmRun.getAnalysis().contains(Analysis.ERROR_CMS_CONCURRENT_MODE_FAILURE));
    }

    public void testCmsConcurrentMixedApplicationConcurrentTime() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset135.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 3, jvmRun.getEventTypes().size());
        Assert.assertFalse(LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_CONCURRENT));
        Assert.assertTrue(
                "Log line not recognized as " + JdkUtil.LogEventType.APPLICATION_STOPPED_TIME.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.APPLICATION_STOPPED_TIME));
        Assert.assertTrue(
                "Log line not recognized as " + JdkUtil.LogEventType.APPLICATION_CONCURRENT_TIME.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.APPLICATION_CONCURRENT_TIME));
    }

    public void testCmsScavengeBeforeRemarkJdk8MixedHeapAtGc() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset136.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertFalse(LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertTrue("Log line not recognized as " + LogEventType.PAR_NEW.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.PAR_NEW));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_REMARK.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_REMARK));
    }

    public void testCmsScavengeBeforeRemarkJMixedHeapAtGc() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset140.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertFalse(LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertTrue("Log line not recognized as " + LogEventType.PAR_NEW.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.PAR_NEW));
        Assert.assertTrue("Log line not recognized as " + LogEventType.CMS_REMARK.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.CMS_REMARK));
    }

    public void testParNewMixedHeapAtGc() {
        File testFile = new File(Constants.TEST_DATA_DIR + "dataset141.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 1, jvmRun.getEventTypes().size());
        Assert.assertFalse(LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertTrue("Log line not recognized as " + LogEventType.PAR_NEW.toString() + ".",
                jvmRun.getEventTypes().contains(LogEventType.PAR_NEW));
    }
}
