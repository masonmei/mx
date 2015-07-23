package com.newrelic.agent.utilization;

import java.util.HashMap;
import java.util.Map;

public class UtilizationData {
    public static final UtilizationData EMPTY = new UtilizationData(null, 0, null, AWS.AwsData.EMPTY_DATA, 0L);
    private static final String METADATA_VERSION_KEY = "metadata_version";
    private static final String LOGICAL_CORES_KEY = "logical_processors";
    private static final String RAM_KEY = "total_ram_mib";
    private static final String HOSTNAME_KEY = "hostname";
    private static final String VENDORS_KEY = "vendors";
    private static final String AWS_KEY = "aws";
    private static final String AWS_INSTANCE_ID_KEY = "id";
    private static final String AWS_INSTANCE_TYPE_KEY = "type";
    private static final String AWS_ZONE_KEY = "zone";
    private static final String DOCKER = "docker";
    private static final String DOCKER_ID_KEY = "id";
    private final String hostname;
    private final String dockerContainerId;
    private final String awsInstanceType;
    private final String awsInstanceId;
    private final String awsZone;
    private final Integer logicalProcessorCount;
    private final long total_ram_mib;

    public UtilizationData(String host, int logicalProcessorCt, String dockerId, AWS.AwsData awsData, long ram_in_mib) {
        hostname = host;
        logicalProcessorCount = Integer.valueOf(logicalProcessorCt);
        dockerContainerId = dockerId;
        awsInstanceId = awsData.getInstanceId();
        awsInstanceType = awsData.getInstanceType();
        awsZone = awsData.getAvailabityZone();
        total_ram_mib = ram_in_mib;
    }

    public Map<String, Object> map() {
        Map data = new HashMap();
        data.put("metadata_version", Integer.valueOf(1));
        data.put("logical_processors", logicalProcessorCount);
        data.put("total_ram_mib", Long.valueOf(total_ram_mib));
        data.put("hostname", hostname);

        Map vendors = new HashMap();
        if (awsInstanceId != null) {
            Map aws = new HashMap();
            aws.put("id", awsInstanceId);
            aws.put("type", awsInstanceType);
            aws.put("zone", awsZone);
            vendors.put(AWS_KEY, aws);
        }

        if (dockerContainerId != null) {
            Map docker = new HashMap();
            docker.put("id", dockerContainerId);
            vendors.put("docker", docker);
        }

        if (!vendors.isEmpty()) {
            data.put("vendors", vendors);
        }

        return data;
    }
}