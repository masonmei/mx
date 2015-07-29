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
        return this.profileId;
    }

    public Long getSamplePeriodInMillis() {
        return this.samplePeriodInMillis;
    }

    public Long getDurationInMillis() {
        return this.durationInMillis;
    }

    public boolean isRunnablesOnly() {
        return this.onlyRunnableThreads;
    }

    public boolean isOnlyRequestThreads() {
        return this.onlyRequestThreads;
    }

    public boolean isProfileAgentThreads() {
        return this.profileAgentCode;
    }

    public String getKeyTransaction() {
        return this.keyTransaction;
    }

    public Long getXraySessionId() {
        return this.xraySessionId;
    }

    public String getAppName() {
        return this.appName;
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = this.xraySessionId == null ? result : 31 * result + this.xraySessionId.hashCode();
        result = this.profileId == null ? result : 31 * result + this.profileId.hashCode();
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
        if (this.xraySessionId == null) {
            if (other.xraySessionId == null) {
                return this.profileId.longValue() == other.profileId.longValue();
            }
            return false;
        }

        if (other.xraySessionId != null) {
            return this.xraySessionId.longValue() == other.xraySessionId.longValue();
        }
        return false;
    }
}