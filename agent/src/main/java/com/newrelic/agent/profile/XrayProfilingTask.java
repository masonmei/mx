package com.newrelic.agent.profile;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IgnoreSilentlyException;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;

public class XrayProfilingTask implements ProfilingTask {
    private final List<ProfilerParameters> profilesToAdd = new CopyOnWriteArrayList();
    private final List<ProfilerParameters> profilesToRemove = new CopyOnWriteArrayList();
    private final List<IProfile> profiles = new ArrayList();
    private final AtomicBoolean sendProfiles = new AtomicBoolean(false);
    private final ProfileSampler profileSampler = new ProfileSampler();

    public void addProfile(ProfilerParameters parameters) {
        profilesToAdd.add(parameters);
    }

    public void removeProfile(ProfilerParameters parameters) {
        profilesToRemove.add(parameters);
    }

    public void beforeHarvest(String appName, StatsEngine statsEngine) {
    }

    public void afterHarvest(String appName) {
        sendProfiles.set(true);
    }

    public void run() {
        try {
            sampleStackTraces();
        } catch (Throwable t) {
            String msg = MessageFormat.format("Error sampling stack traces: {0}", new Object[] {t});
            if (Agent.LOG.isLoggable(Level.FINEST)) {
                Agent.LOG.log(Level.FINEST, msg, t);
            } else {
                Agent.LOG.finer(msg);
            }
        }
    }

    private void sampleStackTraces() {
        removeProfiles();
        if (sendProfiles.getAndSet(false)) {
            sendProfiles();
        }
        addProfiles();
        profileSampler.sampleStackTraces(profiles);
    }

    private void removeProfiles() {
        for (ProfilerParameters parameters : profilesToRemove) {
            IProfile profile = getProfile(parameters);
            if (profile != null) {
                profiles.remove(profile);
                Agent.LOG.info(MessageFormat.format("Stopped xray session profiling: {0}",
                                                           new Object[] {parameters.getKeyTransaction()}));
            }

            profilesToRemove.remove(parameters);
        }
    }

    private void addProfiles() {
        for (ProfilerParameters parameters : profilesToAdd) {
            IProfile profile = getProfile(parameters);
            if (profile == null) {
                profile = createProfile(parameters);
                profile.start();
                profiles.add(profile);
                Agent.LOG.info(MessageFormat.format("Started xray session profiling: {0}",
                                                           new Object[] {parameters.getKeyTransaction()}));
            }

            profilesToAdd.remove(parameters);
        }
    }

    List<IProfile> getProfiles() {
        return new CopyOnWriteArrayList(profiles);
    }

    private IProfile getProfile(ProfilerParameters parameters) {
        for (IProfile profile : profiles) {
            if (profile.getProfilerParameters().equals(parameters)) {
                return profile;
            }
        }
        return null;
    }

    private IProfile createProfile(ProfilerParameters parameters) {
        return new KeyTransactionProfile(parameters);
    }

    private void sendProfiles() {
        ListIterator it = profiles.listIterator();
        while (it.hasNext()) {
            IProfile profile = (IProfile) it.next();
            int requestCallSiteCount = profile.getProfileTree(ThreadType.BasicThreadType.REQUEST).getCallSiteCount();
            int backgroundCallSiteCount =
                    profile.getProfileTree(ThreadType.BasicThreadType.BACKGROUND).getCallSiteCount();

            if ((requestCallSiteCount > 0) || (backgroundCallSiteCount > 0)) {
                it.remove();
                profile.end();
                IProfile nProfile = createProfile(profile.getProfilerParameters());
                nProfile.start();
                it.add(nProfile);
                sendProfile(profile);
            }
        }
    }

    private void sendProfile(IProfile profile) {
        try {
            if (Agent.LOG.isLoggable(Level.FINE)) {
                String msg = MessageFormat.format("Sending Xray profile: {0}",
                                                         new Object[] {profile.getProfilerParameters()
                                                                               .getXraySessionId()});

                Agent.LOG.fine(msg);
            }
            String appName = profile.getProfilerParameters().getAppName();
            List ids = ServiceFactory.getRPMService(appName).sendProfileData(Arrays.asList(new IProfile[] {profile}));
            if (Agent.LOG.isLoggable(Level.FINE)) {
                Agent.LOG.fine(MessageFormat.format("Xray profile id: {0}", new Object[] {ids}));
            }
        } catch (IgnoreSilentlyException e) {
        } catch (Exception e) {
            String msg = MessageFormat.format("Unable to send profile data: {0}", new Object[] {e});
            if (Agent.LOG.isLoggable(Level.FINEST)) {
                Agent.LOG.log(Level.FINEST, msg, e);
            } else {
                Agent.LOG.fine(msg);
            }
        }
    }
}