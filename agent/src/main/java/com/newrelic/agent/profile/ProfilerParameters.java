package com.newrelic.agent.profile;

public class ProfilerParameters {
    private final Long profileId;
    private final Long samplePeriodInMillis;
    private final Long durationInMillis;
    private final boolean onlyRunnableThreads;
    private final boolean onlyRequestThreads;
    private final boolean profileAgentCode;
    private final String keyTransaction;
    private final Long xraySessionId;
    private final String appName;

    public ProfilerParameters(Long profileId, long samplePeriodInMillis, long durationInMillis,
                              boolean onlyRunnableThreads, boolean onlyRequestThreads, boolean profileAgentCode,
                              String keyTransaction, Long xraySessionId, String appName) {
        this.profileId = profileId;
        this.samplePeriodInMillis = Long.valueOf(samplePeriodInMillis);
        this.durationInMillis = Long.valueOf(durationInMillis);
        this.onlyRunnableThreads = onlyRunnableThreads;
        this.onlyRequestThreads = onlyRequestThreads;
        this.profileAgentCode = profileAgentCode;
        this.keyTransaction = keyTransaction;
        this.xraySessionId = xraySessionId;
        this.appName = appName;
    }

    public Long getProfileId() {
        return profileId;
    }

    public Long getSamplePeriodInMillis() {
        return samplePeriodInMillis;
    }

    public Long getDurationInMillis() {
        return durationInMillis;
    }

    public boolean isRunnablesOnly() {
        return onlyRunnableThreads;
    }

    public boolean isOnlyRequestThreads() {
        return onlyRequestThreads;
    }

    public boolean isProfileAgentThreads() {
        return profileAgentCode;
    }

    public String getKeyTransaction() {
        return keyTransaction;
    }

    public Long getXraySessionId() {
        return xraySessionId;
    }

    public String getAppName() {
        return appName;
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = xraySessionId == null ? result : 31 * result + xraySessionId.hashCode();
        result = profileId == null ? result : 31 * result + profileId.hashCode();
        return result;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if ((obj == null) || (obj.getClass() != getClass())) {
            return false;
        }
        ProfilerParameters other = (ProfilerParameters) obj;
        if (xraySessionId == null) {
            if (other.xraySessionId == null) {
                return profileId.longValue() == other.profileId.longValue();
            }
            return false;
        }

        if (other.xraySessionId != null) {
            return xraySessionId.longValue() == other.xraySessionId.longValue();
        }
        return false;
    }
}