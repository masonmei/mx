package com.newrelic.agent.profile;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.commands.AbstractCommand;
import com.newrelic.agent.commands.CommandException;
import com.newrelic.agent.util.TimeConversion;

public class StartProfilerCommand extends AbstractCommand {
    public static final String COMMAND_NAME = "start_profiler";
    private static final String DISABLED_MESSAGE = "The profiler service is disabled";
    private static final String DURATION = "duration";
    private static final String SAMPLE_PERIOD = "sample_period";
    private static final String PROFILE_ID = "profile_id";
    private static final String ONLY_RUNNABLE_THREADS = "only_runnable_threads";
    private static final String ONLY_REQUEST_THREADS = "only_request_threads";
    private static final String PROFILE_AGENT_CODE = "profile_agent_code";
    private static final boolean DEFAULT_ONLY_RUNNABLE_THREADS = false;
    private static final boolean DEFAULT_ONLY_REQUEST_THREADS = false;
    private final ProfilerControl profilerControl;

    public StartProfilerCommand(ProfilerControl profilerControl) {
        super("start_profiler");
        this.profilerControl = profilerControl;
    }

    public Map<?, ?> process(IRPMService rpmService, Map arguments) throws CommandException {
        if (profilerControl.isEnabled()) {
            return processEnabled(rpmService, arguments);
        }
        return processDisabled(rpmService, arguments);
    }

    public Map<?, ?> processEnabled(IRPMService rpmService, Map<?, ?> arguments) throws CommandException {
        ProfilerParameters parameters = createProfilerParameters(arguments);
        profilerControl.startProfiler(parameters);
        return Collections.EMPTY_MAP;
    }

    public Map<?, ?> processDisabled(IRPMService rpmService, Map<?, ?> arguments) throws CommandException {
        Agent.LOG.info("The profiler service is disabled");
        Map result = new HashMap();
        result.put("error", "The profiler service is disabled");
        return result;
    }

    private ProfilerParameters createProfilerParameters(Map<?, ?> arguments) throws CommandException {
        long profileId = getProfileId(arguments);

        double samplePeriod = getSamplePeriod(arguments);
        double duration = getDuration(arguments);
        if (samplePeriod > duration) {
            String msg = MessageFormat.format("{0} > {1} in start_profiler command: {2} > {3}",
                                                     new Object[] {"sample_period", "duration",
                                                                          Double.valueOf(samplePeriod),
                                                                          Double.valueOf(duration)});

            throw new CommandException(msg);
        }
        long samplePeriodInMillis = TimeConversion.convertSecondsToMillis(samplePeriod);
        long durationInMillis = TimeConversion.convertSecondsToMillis(duration);

        boolean onlyRunnableThreads = getOnlyRunnableThreads(arguments);
        boolean onlyRequestThreads = getOnlyRequestThreads(arguments);
        boolean profileAgentCode = getProfileAgentCode(arguments);

        if (arguments.size() > 0) {
            String msg = MessageFormat.format("Unexpected arguments in start_profiler command: {0}",
                                                     new Object[] {arguments.keySet().toString()});

            Agent.LOG.warning(msg);
        }

        return new ProfilerParameters(Long.valueOf(profileId), samplePeriodInMillis, durationInMillis,
                                             onlyRunnableThreads, onlyRequestThreads, profileAgentCode, null, null,
                                             null);
    }

    private long getProfileId(Map<?, ?> arguments) throws CommandException {
        Object profileId = arguments.remove("profile_id");
        if ((profileId instanceof Number)) {
            return ((Number) profileId).longValue();
        }
        if (profileId == null) {
            String msg = MessageFormat.format("Missing {0} in start_profiler command", new Object[] {"profile_id"});
            throw new CommandException(msg);
        }
        String msg = MessageFormat.format("Invalid {0} in start_profiler command: {1}",
                                                 new Object[] {"profile_id", profileId});
        throw new CommandException(msg);
    }

    private double getSamplePeriod(Map<?, ?> arguments) throws CommandException {
        Object samplePeriod = arguments.remove("sample_period");
        if ((samplePeriod instanceof Number)) {
            return ((Number) samplePeriod).doubleValue();
        }
        if (samplePeriod == null) {
            String msg = MessageFormat.format("Missing {0} in start_profiler command", new Object[] {"sample_period"});
            throw new CommandException(msg);
        }
        String msg = MessageFormat.format("Invalid {0} in start_profiler command: {1}",
                                                 new Object[] {"sample_period", samplePeriod});
        throw new CommandException(msg);
    }

    private double getDuration(Map<?, ?> arguments) throws CommandException {
        Object duration = arguments.remove("duration");
        if ((duration instanceof Number)) {
            return ((Number) duration).doubleValue();
        }
        if (duration == null) {
            String msg = MessageFormat.format("Missing {0} in start_profiler command", new Object[] {"duration"});
            throw new CommandException(msg);
        }
        String msg =
                MessageFormat.format("Invalid {0} in start_profiler command: {1}", new Object[] {"duration", duration});
        throw new CommandException(msg);
    }

    private boolean getOnlyRunnableThreads(Map<?, ?> arguments) throws CommandException {
        Object onlyRunnableThreads = arguments.remove("only_runnable_threads");
        if ((onlyRunnableThreads instanceof Boolean)) {
            return ((Boolean) onlyRunnableThreads).booleanValue();
        }
        if (onlyRunnableThreads == null) {
            return false;
        }
        String msg = MessageFormat.format("Invalid {0} in start_profiler command: {1}",
                                                 new Object[] {"only_runnable_threads", onlyRunnableThreads});

        throw new CommandException(msg);
    }

    private boolean getOnlyRequestThreads(Map<?, ?> arguments) throws CommandException {
        Object onlyRequestThreads = arguments.remove("only_request_threads");
        if ((onlyRequestThreads instanceof Boolean)) {
            return ((Boolean) onlyRequestThreads).booleanValue();
        }
        if (onlyRequestThreads == null) {
            return false;
        }
        String msg = MessageFormat.format("Invalid {0} in start_profiler command: {1}",
                                                 new Object[] {"only_request_threads", onlyRequestThreads});

        throw new CommandException(msg);
    }

    private boolean getProfileAgentCode(Map<?, ?> arguments) throws CommandException {
        Object profileAgentCode = arguments.remove("profile_agent_code");
        if ((profileAgentCode instanceof Boolean)) {
            return ((Boolean) profileAgentCode).booleanValue();
        }
        if (profileAgentCode == null) {
            return Agent.isDebugEnabled();
        }
        String msg = MessageFormat.format("Invalid {0} in start_profiler command: {1}",
                                                 new Object[] {"profile_agent_code", profileAgentCode});

        throw new CommandException(msg);
    }
}