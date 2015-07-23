//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.reinstrument;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.ConnectionListener;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.commands.Command;
import com.newrelic.agent.commands.InstrumentUpdateCommand;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.ReinstrumentConfig;
import com.newrelic.agent.extension.beans.Extension;
import com.newrelic.agent.extension.dom.ExtensionDomParser;
import com.newrelic.agent.extension.util.ExtensionConversionUtility;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.custom.ClassRetransformer;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;

public class RemoteInstrumentationServiceImpl extends AbstractService implements RemoteInstrumentationService,
                                                                                         ConnectionListener,
                                                                                         AgentConfigListener {
    private static final String INSTRUMENTATION_CONFIG = "instrumentation";
    private static final String CONFIG_KEY = "config";
    private final ReinstrumentConfig reinstrumentConfig;
    private final boolean isEnabled;
    private volatile boolean isLiveAttributesEnabled;
    private volatile String mostRecentXml = null;

    public RemoteInstrumentationServiceImpl() {
        super(RemoteInstrumentationService.class.getSimpleName());
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        this.reinstrumentConfig = config.getReinstrumentConfig();
        this.isEnabled = this.reinstrumentConfig.isEnabled();
        this.isLiveAttributesEnabled = this.reinstrumentConfig.isAttributesEnabled();
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    protected void doStart() throws Exception {
        if (this.isEnabled) {
            ServiceFactory.getCommandParser().addCommands(new Command[] {new InstrumentUpdateCommand(this)});
            ServiceFactory.getRPMServiceManager().addConnectionListener(this);
            ServiceFactory.getConfigService().addIAgentConfigListener(this);
        }

    }

    protected void doStop() throws Exception {
        if (this.isEnabled) {
            ServiceFactory.getRPMServiceManager().removeConnectionListener(this);
            ServiceFactory.getConfigService().removeIAgentConfigListener(this);
        }

    }

    public void connected(IRPMService pRpmService, Map<String, Object> pConnectionInfo) {
        if (pConnectionInfo != null) {
            Object value = pConnectionInfo.get("instrumentation");
            if (value != null && value instanceof List) {
                List daMaps = (List) value;
                Iterator i$ = daMaps.iterator();

                while (true) {
                    while (i$.hasNext()) {
                        Map current = (Map) i$.next();
                        Object config = current.get("config");
                        if (config != null && config instanceof String) {
                            this.processXml((String) config);
                        } else {
                            Agent.LOG
                                    .info("The instrumentation configuration passed down does not contain a config "
                                                  + "key.");
                        }
                    }

                    return;
                }
            }
        }

    }

    public void disconnected(IRPMService pRpmService) {
    }

    public ReinstrumentResult processXml(String pXml) {
        ReinstrumentResult result = new ReinstrumentResult();

        try {
            if (this.isEnabled) {
                if (ServiceFactory.getAgent().getInstrumentation().isRetransformClassesSupported()) {
                    if (!ServiceFactory.getConfigService().getDefaultAgentConfig().isHighSecurity()) {
                        this.mostRecentXml = pXml;
                        if (this.isAllXmlRemoved(pXml)) {
                            Agent.LOG.info("The XML file is empty. All custom instrumentation will be removed.");
                            this.updateJvmWithExtension((Extension) null, result);
                        } else {
                            Agent.LOG.log(Level.FINE,
                                                 "Instrumentation modifications received from the server with "
                                                         + "attributes {0}.",
                                                 new Object[] {this.isLiveAttributesEnabled ? "enabled" : "disabled"});
                            Extension e = this.getExtensionAndAddErrors(result, pXml);
                            if (e != null) {
                                this.updateJvmWithExtension(e, result);
                            }
                        }
                    } else {
                        this.handleErrorNoInstrumentation(result,
                                                                 "Remote instrumentation is not supported in high "
                                                                         + "security mode.",
                                                                 pXml);
                    }
                } else {
                    this.handleErrorNoInstrumentation(result,
                                                             "Retransform classes is not supported on the current "
                                                                     + "instrumentation.",
                                                             pXml);
                }
            } else {
                this.handleErrorNoInstrumentation(result, "The Reinstrument Service is currently disabled.", pXml);
            }
        } catch (Exception var4) {
            this.handleErrorPartialInstrumentation(result, "An unexpected exception occured: " + var4.getMessage(),
                                                          pXml);
        }

        return result;
    }

    private boolean isAllXmlRemoved(String pXml) {
        return pXml == null || pXml.trim().length() == 0;
    }

    private Extension getExtensionAndAddErrors(ReinstrumentResult result, String pXml) {
        ArrayList exceptions = new ArrayList();
        Extension currentExt = ExtensionDomParser.readStringGatherExceptions(pXml, exceptions);
        ReinstrumentUtils.handleErrorPartialInstrumentation(result, exceptions, pXml);
        return currentExt;
    }

    private void updateJvmWithExtension(Extension ext, ReinstrumentResult result) {
        List pointCuts = null;
        if (ext != null && ext.isEnabled()) {
            pointCuts = ExtensionConversionUtility.convertToEnabledPointCuts(Arrays.asList(new Extension[] {ext}), true,
                                                                                    InstrumentationType.RemoteCustomXml,
                                                                                    this.isLiveAttributesEnabled);
        } else {
            pointCuts = Collections.emptyList();
        }

        result.setPointCutsSpecified(pointCuts.size());
        ClassRetransformer remoteRetransformer = ServiceFactory.getClassTransformerService().getRemoteRetransformer();
        remoteRetransformer.setClassMethodMatchers(pointCuts);
        Class[] allLoadedClasses = ServiceFactory.getAgent().getInstrumentation().getAllLoadedClasses();
        Set classesToRetransform =
                InstrumentationContext.getMatchingClasses(remoteRetransformer.getMatchers(), allLoadedClasses);
        ReinstrumentUtils.checkClassExistsAndRetransformClasses(result, pointCuts, ext, classesToRetransform);
    }

    private void handleErrorPartialInstrumentation(ReinstrumentResult result, String msg, String pXml) {
        result.addErrorMessage(msg);
        if (Agent.LOG.isFineEnabled()) {
            Agent.LOG.fine(MessageFormat.format(msg + " This xml being processed was: {0}", new Object[] {pXml}));
        }

    }

    private void handleErrorNoInstrumentation(ReinstrumentResult result, String msg, String pXml) {
        result.addErrorMessage(msg);
        if (Agent.LOG.isFineEnabled()) {
            Agent.LOG.fine(MessageFormat.format(msg + " This xml will not be instrumented: {0}", new Object[] {pXml}));
        }

    }

    public void configChanged(String appName, AgentConfig agentConfig) {
        boolean attsEnabled = agentConfig.getReinstrumentConfig().isAttributesEnabled();
        if (this.isLiveAttributesEnabled != attsEnabled) {
            this.isLiveAttributesEnabled = attsEnabled;
            Agent.LOG.log(Level.FINE, "RemoteInstrumentationService: Remote attributes are {0}",
                                 new Object[] {this.isLiveAttributesEnabled ? "enabled" : "disabled"});
            if (this.mostRecentXml != null) {
                this.processXml(this.mostRecentXml);
            }
        }

    }
}
