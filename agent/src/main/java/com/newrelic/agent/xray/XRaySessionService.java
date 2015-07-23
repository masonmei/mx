package com.newrelic.agent.xray;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.commands.Command;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;

public class XRaySessionService extends AbstractService implements HarvestListener, IXRaySessionService {
    public static final int MAX_SESSION_COUNT = 50;
    public static final long MAX_SESSION_DURATION_SECONDS = 86400L;
    public static final long MAX_TRACE_COUNT = 100L;
    private final Map<Long, XRaySession> sessions = new HashMap();
    private final boolean enabled;
    private final List<XRaySessionListener> listeners = new CopyOnWriteArrayList();

    public XRaySessionService() {
        super(XRaySessionService.class.getSimpleName());
        enabled = ServiceFactory.getConfigService().getDefaultAgentConfig().isXraySessionEnabled();
    }

    public boolean isEnabled() {
        return enabled;
    }

    protected void doStart() throws Exception {
        addCommands();
        ServiceFactory.getHarvestService().addHarvestListener(this);
    }

    protected void doStop() throws Exception {
        ServiceFactory.getHarvestService().removeHarvestListener(this);
    }

    private void addCommands() {
        ServiceFactory.getCommandParser().addCommands(new Command[] {new StartXRayCommand(this)});
    }

    private void addSession(XRaySession newSession) {
        if (listeners.size() < 50) {
            Agent.LOG.info("Adding X-Ray session: " + newSession.toString());
            sessions.put(newSession.getxRayId(), newSession);
            for (XRaySessionListener listener : listeners) {
                listener.xraySessionCreated(newSession);
            }
        } else {
            Agent.LOG.error("Unable to add X-Ray Session because this would exceed the maximum number of concurrent "
                                    + "X-Ray Sessions allowed.  Max allowed is 50");
        }
    }

    private void removeSession(Long sessionId) {
        XRaySession session = (XRaySession) sessions.remove(sessionId);
        if (null == session) {
            Agent.LOG.info("Tried to remove X-Ray session " + sessionId + " but no such session exists.");
        } else {
            Agent.LOG.info("Removing X-Ray session: " + session.toString());
            for (XRaySessionListener listener : listeners) {
                listener.xraySessionRemoved(session);
            }
        }
    }

    public void beforeHarvest(String appName, StatsEngine statsEngine) {
    }

    public void afterHarvest(String appName) {
        Set<Long> expired = new HashSet<Long>();
        for (XRaySession session : sessions.values()) {
            if (session.sessionHasExpired()) {
                expired.add(session.getxRayId());
                Agent.LOG.debug("Identified X-Ray session for expiration: " + session.toString());
            }
        }
        for (Long key : expired) {
            XRaySession session = (XRaySession) sessions.get(key);
            if (null != session) {
                Agent.LOG.info("Expiring X-Ray session: " + session.getxRaySessionName());
                removeSession(key);
            }
        }
    }

    void setupSession(Map<?, ?> sessionMap, String applicationName) {
        Long xRayId = null;
        Boolean runProfiler = null;
        String keyTransactionName = null;
        Double samplePeriod = null;
        String xRaySessionName = null;
        Long duration = null;
        Long requestedTraceCount = null;

        Object x_ray_id = sessionMap.remove("x_ray_id");
        if ((x_ray_id instanceof Long)) {
            xRayId = (Long) x_ray_id;
        }
        Object run_profiler = sessionMap.remove("run_profiler");
        if ((run_profiler instanceof Boolean)) {
            runProfiler = (Boolean) run_profiler;
        }
        Object key_transaction_name = sessionMap.remove("key_transaction_name");
        if ((key_transaction_name instanceof String)) {
            keyTransactionName = (String) key_transaction_name;
        }
        Object sample_period = sessionMap.remove("sample_period");
        if ((sample_period instanceof Double)) {
            samplePeriod = (Double) sample_period;
        }
        Object xray_session_name = sessionMap.remove("xray_session_name");
        if ((xray_session_name instanceof String)) {
            xRaySessionName = (String) xray_session_name;
        }
        Object duration_obj = sessionMap.remove("duration");
        if ((duration_obj instanceof Long)) {
            duration = (Long) duration_obj;
            if (duration.longValue() < 0L) {
                duration = Long.valueOf(0L);
                Agent.LOG.error("Tried to create an X-Ray Session with negative duration, setting duration to 0");
            } else if (duration.longValue() > 86400L) {
                Agent.LOG.error("Tried to create an X-Ray session with a duration (" + duration + ") longer than "
                                        + 86400L + " seconds.  Setting the duration to " + 86400L + " seconds");

                duration = Long.valueOf(86400L);
            }
        }
        Object requested_trace_count = sessionMap.remove("requested_trace_count");
        if ((requested_trace_count instanceof Long)) {
            requestedTraceCount = (Long) requested_trace_count;
            if (requestedTraceCount.longValue() > 100L) {
                Agent.LOG.error("Tried to create an X-Ray session with a requested trace count (" + requestedTraceCount
                                        + ") larger than " + 100L + ".  Setting the max trace count to " + 100L);

                requestedTraceCount = Long.valueOf(100L);
            } else if (requestedTraceCount.longValue() < 0L) {
                Agent.LOG.error("Tried to create an X-Ray Session with negative trace count, setting trace count to 0");
                requestedTraceCount = Long.valueOf(0L);
            }
        }
        XRaySession newSession =
                new XRaySession(xRayId, runProfiler.booleanValue(), keyTransactionName, samplePeriod.doubleValue(),
                                       xRaySessionName, duration, requestedTraceCount, applicationName);

        addSession(newSession);
    }

    public Map<?, ?> processSessionsList(List<Long> incomingList, IRPMService rpmService) {
        Set<Long> sessionIdsToAdd = new HashSet<Long>();
        Set<Long> sessionIdsToRemove = new HashSet<Long>();

        String applicationName = rpmService.getApplicationName();

        for (Long id : incomingList) {
            if (!sessions.keySet().contains(id)) {
                sessionIdsToAdd.add(id);
            }

        }

        for (Iterator i$ = sessions.keySet().iterator(); i$.hasNext(); ) {
            long id = ((Long) i$.next()).longValue();
            if (!incomingList.contains(Long.valueOf(id))) {
                Agent.LOG.debug("Identified " + id + " for removal from the active list of X-Ray sessions");
                sessionIdsToRemove.add(Long.valueOf(id));
            }

        }

        if (sessionIdsToRemove.size() > 0) {
            Agent.LOG.debug("Removing " + sessionIdsToRemove + " from the active list of X-Ray sessions");
            for (Long id : sessionIdsToRemove) {
                removeSession(id);
            }
        }
        Iterator i$;
        if (sessionIdsToAdd.size() > 0) {
            Agent.LOG.debug("Fetching details for " + sessionIdsToAdd + " to add to the active list of X Ray Sessions");
            Collection newSessionDetails;
            try {
                newSessionDetails = rpmService.getXRaySessionInfo(sessionIdsToAdd);
            } catch (Exception e) {
                Agent.LOG.error("Unable to fetch X-Ray session details from RPM" + e.getMessage());
                return Collections.EMPTY_MAP;
            }

            for (i$ = newSessionDetails.iterator(); i$.hasNext(); ) {
                Object newSession = i$.next();
                if ((newSession instanceof Map)) {
                    Map newSessionMap = (Map) newSession;
                    setupSession(newSessionMap, applicationName);
                } else {
                    Agent.LOG.error("Unable to read X-Ray session details: " + newSession);
                }
            }
        }

        Agent.LOG.debug("Resulting collection of X-Ray sessions: " + sessions);
        return Collections.EMPTY_MAP;
    }

    public void addListener(XRaySessionListener listener) {
        listeners.add(listener);
    }

    public void removeListener(XRaySessionListener listener) {
        listeners.remove(listener);
    }
}