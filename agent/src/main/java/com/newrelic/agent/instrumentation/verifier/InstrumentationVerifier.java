package com.newrelic.agent.instrumentation.verifier;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.logging.Level;

import com.newrelic.agent.instrumentation.verifier.mocks.MockInstrumentation;
import com.newrelic.agent.instrumentation.verifier.mocks.MockServiceManager;
import com.newrelic.agent.instrumentation.verifier.mocks.MockStatsService;
import com.newrelic.agent.instrumentation.weaver.InstrumentationMetadata;
import com.newrelic.agent.instrumentation.weaver.InstrumentationPackage;
import com.newrelic.agent.instrumentation.weaver.Verifier;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;

public class InstrumentationVerifier {
    private IAgentLogger logger;

    public InstrumentationVerifier(IAgentLogger logger) {
        this.logger = logger;
        setUpDummyServices();
    }

    private void setUpDummyServices() {
        MockServiceManager myServiceManager = new MockServiceManager();

        ServiceFactory.setServiceManager(myServiceManager);
        myServiceManager.setStatsService(new MockStatsService());
    }

    public boolean verify(String instrumentationJar, List<String> userJars) throws Exception {
        ClassLoader loader = createClassloaderForVerification(userJars);
        InstrumentationPackage instrumentationPackage = getInstrumentationPackage(instrumentationJar);

        Verifier verifier = instrumentationPackage.getVerifier();
        boolean result = verifier.verify(instrumentationPackage.getClassAppender(), loader,
                                                instrumentationPackage.getClassBytes(),
                                                instrumentationPackage.newClassLoadOrder);

        return result;
    }

    private InstrumentationPackage getInstrumentationPackage(String instrumentationJar) throws Exception {
        URL instrumentationJarUrl = new File(instrumentationJar).toURI().toURL();

        InputStream iStream = instrumentationJarUrl.openStream();
        JarInputStream jarStream = new JarInputStream(iStream);
        InstrumentationMetadata instrumentationMetadata =
                new InstrumentationMetadata(jarStream, instrumentationJarUrl.toString());

        return new InstrumentationPackage(new MockInstrumentation(), logger, instrumentationMetadata, jarStream);
    }

    private ClassLoader createClassloaderForVerification(List<String> jars) throws MalformedURLException {
        Set urls = new HashSet();
        logger.log(Level.FINE, "Creating user classloader with custom classpath:");
        for (String s : jars) {
            File jarFile = new File(s);
            if (!jarFile.exists()) {
                logger.log(Level.WARNING, "\tWARNING: Given jar does not exist: {0}", new Object[] {s});
            }
            urls.add(jarFile.toURI().toURL());
            logger.log(Level.FINE, "\t{0}", new Object[] {s});
        }
        return new VerificationClassLoader((URL[]) urls.toArray(new URL[urls.size()]));
    }

    public void printVerificationResults(PrintStream ps, List<String> results) {
        for (String logLine : results) {
            ps.println(logLine);
        }
    }
}