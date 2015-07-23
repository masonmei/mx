package com.newrelic.agent.profile;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import com.newrelic.agent.IgnoreSilentlyException;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;

public class ProfileSession {
    private final ProfileSampler profileSampler = new ProfileSampler();
    private final IProfile profile;
    private final List<IProfile> profiles = new ArrayList();
    private final ProfilerService profilerService;
    private final AtomicBoolean done = new AtomicBoolean(false);
    private final AtomicReference<ScheduledFuture<?>> profileHandle = new AtomicReference();
    private final AtomicReference<ScheduledFuture<?>> timeoutHandle = new AtomicReference();

    public ProfileSession(ProfilerService profilerService, ProfilerParameters profilerParameters) {
        this.profilerService = profilerService;
        profile = createProfile(profilerParameters);
        profile.start();
        profiles.add(profile);
    }

    private IProfile createProfile(ProfilerParameters profilerParameters) {
        return new Profile(profilerParameters);
    }

    void start() {
        long samplePeriodInMillis = profile.getProfilerParameters().getSamplePeriodInMillis().longValue();
        long durationInMillis = profile.getProfilerParameters().getDurationInMillis().longValue();
        if (samplePeriodInMillis == durationInMillis) {
            getLogger().info("Starting single sample profiling session");
            startSingleSample();
        } else {
            getLogger().info(MessageFormat.format("Starting profiling session. Duration: {0} ms, sample period: {1} ms",
                                                         new Object[] {Long.valueOf(durationInMillis),
                                                                              Long.valueOf(samplePeriodInMillis)}));

            startMultiSample(samplePeriodInMillis, durationInMillis);
        }
    }

    private void startMultiSample(long samplePeriodInMillis, long durationInMillis) {
        ScheduledExecutorService scheduler = profilerService.getScheduledExecutorService();
        ScheduledFuture handle = scheduler.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                try {
                    profileSampler.sampleStackTraces(profiles);
                } catch (Throwable t) {
                    String msg = MessageFormat.format("An error occurred collecting a thread sample: {0}",
                                                             new Object[] {t.toString()});

                    if (ProfileSession.this.getLogger().isLoggable(Level.FINER)) {
                        ProfileSession.this.getLogger().log(Level.SEVERE, msg, t);
                    } else {
                        ProfileSession.this.getLogger().severe(msg);
                    }
                }
            }
        }, 0L, samplePeriodInMillis, TimeUnit.MILLISECONDS);

        profileHandle.set(handle);
        handle = scheduler.schedule(new Runnable() {
            public void run() {
                ((ScheduledFuture) profileHandle.get()).cancel(false);
                if (!done.getAndSet(true)) {
                    ProfileSession.this.report();
                }
                ProfileSession.this.sessionCompleted();
            }
        }, durationInMillis, TimeUnit.MILLISECONDS);

        timeoutHandle.set(handle);
    }

    private void startSingleSample() {
        ScheduledExecutorService scheduler = profilerService.getScheduledExecutorService();
        ScheduledFuture handle = scheduler.schedule(new Runnable() {
            public void run() {
                try {
                    profileSampler.sampleStackTraces(profiles);
                } catch (Throwable t) {
                    String msg = MessageFormat.format("An error occurred collecting a thread sample: {0}",
                                                             new Object[] {t.toString()});
                    if (ProfileSession.this.getLogger().isLoggable(Level.FINER)) {
                        ProfileSession.this.getLogger().log(Level.SEVERE, msg, t);
                    } else {
                        ProfileSession.this.getLogger().severe(msg);
                    }
                }
                if (!done.getAndSet(true)) {
                    ProfileSession.this.report();
                }
                ProfileSession.this.sessionCompleted();
            }
        }, 0L, TimeUnit.MILLISECONDS);

        profileHandle.set(handle);
    }

    private void report() {
        try {
            profile.end();
            profile.markInstrumentedMethods();
            getLogger().info(MessageFormat.format("Profiler finished with {0} samples",
                                                         new Object[] {Integer.valueOf(profile.getSampleCount())}));
        } catch (Throwable e) {
            getLogger().log(Level.SEVERE, "Error finishing profile - no profiles will be sent", e);
            return;
        }
        try {
            List ids = ServiceFactory.getRPMService().sendProfileData(profiles);
            getLogger().info(MessageFormat.format("Server profile ids: {0}", new Object[] {ids}));
        } catch (IgnoreSilentlyException e) {
        } catch (Throwable e) {
            String msg = MessageFormat.format("Unable to send profile data: {0}", new Object[] {e.toString()});
            if (getLogger().isLoggable(Level.FINER)) {
                getLogger().log(Level.SEVERE, msg, e);
            } else {
                getLogger().severe(msg);
            }
        }
    }

    private void sessionCompleted() {
        profilerService.sessionCompleted(this);
    }

    void stop(final boolean shouldReport) {
        if (done.getAndSet(true)) {
            return;
        }
        getLogger().log(Level.INFO, "Stopping profiling session");
        ScheduledFuture handle = (ScheduledFuture) profileHandle.get();
        if (handle != null) {
            handle.cancel(false);
        }
        handle = (ScheduledFuture) timeoutHandle.get();
        if (handle != null) {
            handle.cancel(false);
        }
        profilerService.getScheduledExecutorService().schedule(new Runnable() {
            public void run() {
                if (shouldReport) {
                    ProfileSession.this.report();
                }
                ProfileSession.this.sessionCompleted();
            }
        }, 0L, TimeUnit.MILLISECONDS);
    }

    public boolean isDone() {
        return done.get();
    }

    public Long getProfileId() {
        return profile.getProfileId();
    }

    public IProfile getProfile() {
        return profile;
    }

    private IAgentLogger getLogger() {
        return profilerService.getLogger();
    }
}