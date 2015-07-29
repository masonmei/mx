package com.newrelic.bootstrap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import com.newrelic.agent.config.AgentJarHelper;
import com.newrelic.agent.config.JarResource;
import com.newrelic.agent.util.Streams;
import com.newrelic.agent.util.asm.ClassStructure;
import com.newrelic.deps.org.objectweb.asm.ClassReader;

public class BootstrapLoader {
    public static final String AGENT_BRIDGE_JAR_NAME = "agent-bridge-1.0";
    public static final String API_JAR_NAME = "agent-api-1.0";
    public static final String WEAVER_API_JAR_NAME = "weaver-api-1.0";
    private static final String NEWRELIC_BOOTSTRAP = "newrelic-bootstrap";
    private static final String NEWRELIC_API_INTERNAL_CLASS_NAME = "com/newrelic/api/agent/NewRelic";

    private static void addBridgeJarToClassPath(Instrumentation instrProxy) throws ClassNotFoundException, IOException {
        JarFile jarFileInAgent = new JarFile(EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(AGENT_BRIDGE_JAR_NAME));

        forceCorrectNewRelicApi(instrProxy, jarFileInAgent);
        addJarToClassPath(instrProxy, jarFileInAgent);
    }

    private static void forceCorrectNewRelicApi(Instrumentation instrProxy, JarFile bridgeJarFile) throws IOException {
        JarEntry jarEntry = bridgeJarFile.getJarEntry(NEWRELIC_API_INTERNAL_CLASS_NAME + ".class");
        byte[] bytes = read(bridgeJarFile.getInputStream(jarEntry), true);
        instrProxy.addTransformer(new ApiClassTransformer(bytes), true);
    }

    private static void addJarToClassPath(Instrumentation instrProxy, JarFile jarfile) {
        instrProxy.appendToBootstrapClassLoaderSearch(jarfile);
    }

    public static Collection<URL> getJarURLs() throws ClassNotFoundException, IOException {
        List urls = new ArrayList();
        for (String name : new String[] {AGENT_BRIDGE_JAR_NAME, API_JAR_NAME, WEAVER_API_JAR_NAME}) {
            File jarFileInAgent = EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(name);
            urls.add(jarFileInAgent.toURI().toURL());
        }
        return urls;
    }

    static void load(Instrumentation inst) {
        try {
            addMixinInterfacesToBootstrap(inst);
            addBridgeJarToClassPath(inst);
            addJarToClassPath(inst, new JarFile(EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(API_JAR_NAME)));
            addJarToClassPath(inst, new JarFile(EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(WEAVER_API_JAR_NAME)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void addMixinInterfacesToBootstrap(Instrumentation inst) {
        if (isDisableMixinsOnBootstrap()) {
            System.out.println("New Relic Agent: mixin interfaces not moved to bootstrap");
            return;
        }
        JarResource agentJarResource = null;
        try {
            agentJarResource = AgentJarHelper.getAgentJarResource();
            URL agentJarUrl = AgentJarHelper.getAgentJarUrl();
            addMixinInterfacesToBootstrap(agentJarResource, agentJarUrl, inst);
        } finally {
            try {
                agentJarResource.close();
            } catch (Throwable th) {
                logIfNRDebug("closing Agent jar resource", th);
            }
        }
    }

    public static void addMixinInterfacesToBootstrap(JarResource agentJarResource, URL agentJarUrl,
                                                     Instrumentation inst) {
        boolean succeeded = false;
        Pattern packageSearchPattern = Pattern.compile("com/newrelic/agent/instrumentation/pointcuts/(.*).class");

        String interfaceMixinAnnotation = "Lcom/newrelic/agent/instrumentation/pointcuts/InterfaceMixin;";
        String loadOnBootstrapAnnotation = "Lcom/newrelic/agent/instrumentation/pointcuts/LoadOnBootstrap;";
        String interfaceMapperAnnotation = "Lcom/newrelic/agent/instrumentation/pointcuts/InterfaceMapper;";
        String methodMapperAnnotation = "Lcom/newrelic/agent/instrumentation/pointcuts/MethodMapper;";
        String fieldAccessorAnnotation = "Lcom/newrelic/agent/instrumentation/pointcuts/FieldAccessor;";

        List<String> bootstrapAnnotations =
                Arrays.asList(interfaceMixinAnnotation, interfaceMapperAnnotation, methodMapperAnnotation,
                                     fieldAccessorAnnotation, loadOnBootstrapAnnotation);

        File generatedFile = null;
        JarOutputStream outputJarStream = null;
        try {
            generatedFile = File.createTempFile(NEWRELIC_BOOTSTRAP, ".jar");
            Manifest manifest = createManifest();
            outputJarStream = createJarOutputStream(generatedFile, manifest);
            long modTime = System.currentTimeMillis();

            Collection<String> fileNames = AgentJarHelper.findJarFileNames(agentJarUrl, packageSearchPattern);
            for (String fileName : fileNames) {
                int size = (int) agentJarResource.getSize(fileName);
                ByteArrayOutputStream out = new ByteArrayOutputStream(size);
                Streams.copy(agentJarResource.getInputStream(fileName), out, size, true);
                byte[] classBytes = out.toByteArray();

                ClassReader cr = new ClassReader(classBytes);
                ClassStructure structure = ClassStructure.getClassStructure(cr, 4);
                Collection annotations = structure.getClassAnnotations().keySet();
                if (containsAnyOf(bootstrapAnnotations, annotations)) {
                    JarEntry entry = new JarEntry(fileName);
                    entry.setTime(modTime);
                    outputJarStream.putNextEntry(entry);
                    outputJarStream.write(classBytes);
                }
            }

            outputJarStream.closeEntry();
            succeeded = true;
        } catch (IOException iox) {
            logIfNRDebug("generating mixin jar file", iox);
        } finally {
            try {
                outputJarStream.close();
            } catch (Throwable th) {
                logIfNRDebug("closing outputJarStream", th);
            }
        }

        if (succeeded) {
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(generatedFile);
                inst.appendToBootstrapClassLoaderSearch(jarFile);
                generatedFile.deleteOnExit();
            } catch (IOException iox) {
                logIfNRDebug("adding dynamic mixin jar to bootstrap", iox);
            } finally {
                try {
                    jarFile.close();
                } catch (Throwable th) {
                    logIfNRDebug("closing generated jar file", th);
                }
            }
        }
    }

    private static final boolean containsAnyOf(Collection<?> searchFor, Collection<?> searchIn) {
        for (Iterator i$ = searchFor.iterator(); i$.hasNext(); ) {
            Object key = i$.next();
            if (searchIn.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private static final boolean isNewRelicDebug() {
        String newrelicDebug = "newrelic.debug";
        return (System.getProperty("newrelic.debug") != null) && (Boolean.getBoolean("newrelic.debug"));
    }

    private static final boolean isDisableMixinsOnBootstrap() {
        String newrelicDisableMixinsOnBootstrap = "newrelic.disable.mixins.on.bootstrap";
        return (System.getProperty(newrelicDisableMixinsOnBootstrap) != null)
                       && (Boolean.getBoolean(newrelicDisableMixinsOnBootstrap));
    }

    private static final void logIfNRDebug(String msg, Throwable th) {
        if (isNewRelicDebug()) {
            System.out.println("While bootstrapping the Agent: " + msg + ": " + th.getStackTrace());
        }
    }

    private static final JarOutputStream createJarOutputStream(File jarFile, Manifest manifest) throws IOException {
        FileOutputStream outStream = new FileOutputStream(jarFile);
        return new JarOutputStream(outStream, manifest);
    }

    private static final Manifest createManifest() {
        Manifest manifest = new Manifest();
        Attributes a = manifest.getMainAttributes();
        a.put(Name.MANIFEST_VERSION, "1.0");
        a.put(Name.IMPLEMENTATION_TITLE, "Interface Mixins");
        a.put(Name.IMPLEMENTATION_VERSION, "1.0");
        a.put(Name.IMPLEMENTATION_VENDOR, "New Relic");
        return manifest;
    }

    static int copy(InputStream input, OutputStream output, int bufferSize, boolean closeStreams) throws IOException {
        try {
            byte[] buffer = new byte[bufferSize];
            int count = 0;
            int n = 0;
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
                count += n;
            }
            return count;
        } finally {
            if (closeStreams) {
                input.close();
                output.close();
            }
        }
    }

    static byte[] read(InputStream input, boolean closeInputStream) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        copy(input, outStream, input.available(), closeInputStream);
        return outStream.toByteArray();
    }

    static final class ApiClassTransformer implements ClassFileTransformer {
        private final byte[] bytes;

        ApiClassTransformer(byte[] bytes) {
            this.bytes = bytes;
        }

        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer)
                throws IllegalClassFormatException {
            if (NEWRELIC_API_INTERNAL_CLASS_NAME.equals(className)) {
                return this.bytes;
            }
            return null;
        }
    }
}