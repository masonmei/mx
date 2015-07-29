package com.newrelic.agent.profile;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.xray.XRaySession;
import com.newrelic.agent.xray.XRaySessionListener;

public class XrayProfileSession implements HarvestListener, XRaySessionListener {
    private static Long PROFILE_ID = Long.valueOf(-1L);
    private static boolean ONLY_RUNNABLE_THREADS = true;
    private static boolean ONLY_REQUEST_THREADS = false;
    private static boolean PROFILE_AGENT_CODE = false;
    private final ProfilingTaskController profilingTaskController;
    private final String defaultApplication;
    private final Map<Long, ProfilerParameters> profilerParameters = new HashMap();
    private ScheduledFuture<?> profileHandle;
    private boolean isSuspended;

    public XrayProfileSession() {
        this.defaultApplication = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
        this.profilingTaskController =
                ProfilingTaskControllerFactory.createProfilingTaskController(new XrayProfilingTask());
    }

    public void beforeHarvest(String appName, StatsEngine statsEngine) {
    }

    public void afterHarvest(String appName) {
        if (this.defaultApplication.equals(appName)) {
            if ((this.isSuspended) && (ServiceFactory.getProfilerService().getCurrentSession() == null)) {
                resume();
            }
            if (this.profileHandle != null) {
                long oSamplePeriod = this.profilingTaskController.getSamplePeriodInMillis();
                this.profilingTaskController.afterHarvest(appName);
                long nSamplePeriod = this.profilingTaskController.getSamplePeriodInMillis();
                if (nSamplePeriod != oSamplePeriod) {
                    stop();
                    start();
                }
            }
        }
    }

    public void xraySessionCreated(XRaySession session) {
        if (session.isRunProfiler()) {
            ProfilerParameters parameters = new ProfilerParameters(PROFILE_ID, session.getSamplePeriodMilliseconds(),
                                                                          session.getDurationMilliseconds(),
                                                                          ONLY_RUNNABLE_THREADS, ONLY_REQUEST_THREADS,
                                                                          PROFILE_AGENT_CODE,
                                                                          session.getKeyTransactionName(),
                                                                          session.getxRayId(),
                                                                          session.getApplicationName());

            if (!this.profilerParameters.containsKey(session.getxRayId())) {
                this.profilerParameters.put(parameters.getXraySessionId(), parameters);
                startProfiling(parameters);
            }
        }
    }

    public void xraySessionRemoved(XRaySession session) {
        if (session.isRunProfiler()) {
            ProfilerParameters parameters = (ProfilerParameters) this.profilerParameters.remove(session.getxRayId());
            if (parameters != null) {
                stopProfiling(parameters);
            }
        }
    }

    private void startProfiling(ProfilerParameters parameters) {
        this.profilingTaskController.addProfile(parameters);
        String msg = MessageFormat.format("Added xray session profiling for {0}",
                                                 new Object[] {parameters.getKeyTransaction()});
        Agent.LOG.info(msg);
        if (!this.isSuspended) {
            start();
        }
    }

    private void stopProfiling(ProfilerParameters parameters) {
        this.profilingTaskController.removeProfile(parameters);
        String msg = MessageFormat.format("Removed xray session profiling for {0}",
                                                 new Object[] {parameters.getKeyTransaction()});
        Agent.LOG.info(msg);
        if (this.profilerParameters.isEmpty()) {
            stop();
        }
    }

    private void start() {
        if (this.profileHandle == null) {
            long delay = this.profilingTaskController.getSamplePeriodInMillis();
            ScheduledExecutorService scheduler = ServiceFactory.getProfilerService().getScheduledExecutorService();
            this.profileHandle =
                    scheduler.scheduleWithFixedDelay(this.profilingTaskController, 0L, delay, TimeUnit.MILLISECONDS);
            Agent.LOG.fine(MessageFormat.format("Started xray profiling task delay = {0}",
                                                       new Object[] {Long.valueOf(delay)}));
        }
    }

    private void stop() {
        if (this.profileHandle != null) {
            this.profileHandle.cancel(false);
            this.profileHandle = null;
            ScheduledExecutorService scheduler = ServiceFactory.getProfilerService().getScheduledExecutorService();

            scheduler.schedule(this.profilingTaskController, 0L, TimeUnit.MILLISECONDS);
            Agent.LOG.fine("Stopped xray profiling task");
        }
    }

    public void suspend() {
        if (!this.isSuspended) {
            this.isSuspended = true;
            stop();
            Agent.LOG.fine("Suspended xray profiling session");
        }
    }

    private void resume() {
        if (this.isSuspended) {
            this.isSuspended = false;
            if (!this.profilerParameters.isEmpty()) {
                start();
            }
            Agent.LOG.fine("Resumed xray profiling session");
        }
    }

    ProfilingTaskController getProfilingTaskController() {
        return this.profilingTaskController;
    }
}