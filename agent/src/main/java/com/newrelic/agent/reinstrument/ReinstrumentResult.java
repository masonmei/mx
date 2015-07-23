package com.newrelic.agent.reinstrument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReinstrumentResult {
    protected static final String ERROR_KEY = "errors";
    protected static final String PCS_SPECIFIED_KEY = "pointcuts_specified";
    protected static final String RETRANSFORM_INIT_KEY = "retransform_init";
    private final List<String> errorMessages = new ArrayList();

    private int pointCutsSpecified = 0;

    private int pointCutsAdded = 0;

    private Set<String> retranformedInitializedClasses = new HashSet();

    public Map<String, Object> getStatusMap() {
        Map statusMap = new HashMap();
        if (errorMessages.size() > 0) {
            StringBuilder sb = new StringBuilder();
            Iterator it = errorMessages.iterator();
            while (it.hasNext()) {
                sb.append((String) it.next());
                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
            statusMap.put("errors", sb.toString());
        }
        statusMap.put("pointcuts_specified", Integer.valueOf(pointCutsSpecified));

        if (retranformedInitializedClasses.size() > 0) {
            StringBuilder sb = new StringBuilder();
            Iterator it = retranformedInitializedClasses.iterator();
            while (it.hasNext()) {
                sb.append((String) it.next());
                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
            statusMap.put("retransform_init", sb.toString());
        }
        return statusMap;
    }

    public void addErrorMessage(String pErrorMessages) {
        errorMessages.add(pErrorMessages);
    }

    public void setPointCutsSpecified(int pPointCutsSpecified) {
        pointCutsSpecified = pPointCutsSpecified;
    }

    public void setRetranformedInitializedClasses(Set<String> pRetranformedInitializedClasses) {
        retranformedInitializedClasses = pRetranformedInitializedClasses;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("pointcuts_specified");
        sb.append(":");
        sb.append(pointCutsSpecified);
        sb.append(", ");
        if ((errorMessages != null) && (errorMessages.size() > 0)) {
            sb.append(",");
            sb.append("errors");
            sb.append(":[");
            for (String msg : errorMessages) {
                sb.append(" ");
                sb.append(msg);
            }
            sb.append("]");
        }
        if ((retranformedInitializedClasses != null) && (retranformedInitializedClasses.size() > 0)) {
            sb.append(", ");
            sb.append("retransform_init");
            sb.append(":[");
            for (String msg : retranformedInitializedClasses) {
                sb.append(" ");
                sb.append(msg);
            }
            sb.append("]");
        }
        return sb.toString();
    }
}