package com.newrelic.agent;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.newrelic.deps.org.apache.commons.cli.CommandLine;
import com.newrelic.deps.org.apache.commons.cli.CommandLineParser;
import com.newrelic.deps.org.apache.commons.cli.HelpFormatter;
import com.newrelic.deps.org.apache.commons.cli.Option;
import com.newrelic.deps.org.apache.commons.cli.Options;
import com.newrelic.deps.org.apache.commons.cli.ParseException;
import com.newrelic.deps.org.apache.commons.cli.PosixParser;

import com.newrelic.agent.command.XmlInstrumentOptions;
import com.newrelic.agent.command.XmlInstrumentValidator;
import com.newrelic.agent.install.AppServerIdentifier;
import com.newrelic.agent.install.AppServerIdentifier.AppServerType;
import com.newrelic.agent.install.ConfigInstaller;
import com.newrelic.agent.install.SelfInstaller;
import com.newrelic.agent.install.SelfInstallerFactory;
import com.newrelic.agent.instrumentation.verifier.InstrumentationVerifier;
import com.newrelic.agent.instrumentation.verifier.VerificationLogger;
import com.newrelic.agent.service.ServiceFactory;

class AgentCommandLineParser {
    private static final String INSTALL_COMMAND = "install";
    private static final String DEPLOYMENT_COMMAND = "deployment";
    private static final String VERIFY_INSTRUMENTATION_COMMAND = "verifyInstrumentation";
    private static final String INSTRUMENT_COMMAND = "instrument";
    private static final Map<String, Options> commandOptionsMap = new HashMap();
    private static final Map<String, String> commandDescriptions;

    static {
        commandOptionsMap.put(DEPLOYMENT_COMMAND, getDeploymentOptions());
        commandOptionsMap.put(INSTALL_COMMAND, getInstallOptions());
        commandOptionsMap.put(INSTRUMENT_COMMAND, getInstrumentOptions());
        commandOptionsMap.put(VERIFY_INSTRUMENTATION_COMMAND, getVerifyInstrumentationOptions());

        commandDescriptions = new HashMap();
        commandDescriptions.put(DEPLOYMENT_COMMAND, "[OPTIONS] [description]  Records a deployment");
        commandDescriptions.put(INSTALL_COMMAND,
                                       "[OPTIONS]                Generates a newrelic.yml configuration with the "
                                               + "given license key and attempts to integrate with app server");

        commandDescriptions.put(INSTRUMENT_COMMAND,
                                       "[OPTIONS]                Validates a custom instrumentation xml configuration"
                                               + " file.");
    }

    static Options getCommandLineOptions() {
        Collection values = new ArrayList(Collections.singletonList(getBasicOptions()));
        values.addAll(commandOptionsMap.values());
        return combineOptions(values);
    }

    private static Options combineOptions(Collection<Options> optionsList) {
        Options newOptions = new Options();
        for (Options options : optionsList) {
            for (Object option : options.getOptions()) {
                newOptions.addOption((Option) option);
            }
        }
        return newOptions;
    }

    private static Options getBasicOptions() {
        Options options = new Options();
        options.addOption("v", false, "Prints the agent version");
        options.addOption("version", false, "Prints the agent version");
        options.addOption("h", false, "Prints help");

        return options;
    }

    private static Options getInstallOptions() {
        Options options = new Options();
        options.addOption("l", true, "Use the given license key");
        options.addOption("s", true, "Path to application server");

        return options;
    }

    private static Options getDeploymentOptions() {
        Options options = new Options();

        options.addOption("appname", true, "Set the application name.  Default is app_name setting in newrelic.yml");

        options.addOption("environment", true, "Set the environment (staging, production, test, development)");

        options.addOption("user", true, "Specify the user deploying");
        options.addOption("revision", true, "Specify the revision being deployed");
        options.addOption("changes", false, "Reads the change log for a deployment from standard input");

        return options;
    }

    private static Options getInstrumentOptions() {
        Options options = new Options();
        XmlInstrumentOptions[] instrumentOps = XmlInstrumentOptions.values();
        for (XmlInstrumentOptions op : instrumentOps) {
            options.addOption(op.getFlagName(), op.isArgRequired(), op.getDescription());
        }
        return options;
    }

    private static Options getVerifyInstrumentationOptions() {
        Options options = new Options();

        return options;
    }

    public void parseCommand(String[] args) {
        CommandLineParser parser = new PosixParser();
        try {
            CommandLine cmd = parser.parse(getCommandLineOptions(), args);

            List argList = cmd.getArgList();
            String command = argList.size() > 0 ? (String) argList.get(0) : null;

            if (cmd.hasOption('h')) {
                printHelp(command);
                return;
            }
            if (command != null) {
                Options commandOptions = commandOptionsMap.get(command);
                if (commandOptions == null) {
                    printHelp();
                    System.err.println("\nInvalid command - " + command);
                    System.exit(1);
                }
                cmd = parser.parse(commandOptions, args);
            }

            if (DEPLOYMENT_COMMAND.equals(command)) {
                deploymentCommand(cmd);
            } else if (INSTALL_COMMAND.equals(command)) {
                installCommand(cmd);
            } else if (INSTRUMENT_COMMAND.equals(command)) {
                instrumentCommand(cmd);
            } else if (VERIFY_INSTRUMENTATION_COMMAND.equals(command)) {
                verifyInstrumentation(cmd);
            } else if ((cmd.hasOption('v')) || (cmd.hasOption("version"))) {
                System.out.println(Agent.getVersion());
            } else {
                printHelp();
                System.exit(1);
            }
        } catch (ParseException e) {
            System.err.println("Error parsing arguments");
            printHelp();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error executing command");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void instrumentCommand(CommandLine cmd) throws Exception {
        XmlInstrumentValidator.validateInstrumentation(cmd);
    }

    private void deploymentCommand(CommandLine cmd) throws Exception {
        Deployments.recordDeployment(cmd);
    }

    private void installCommand(CommandLine cmd) throws Exception {
        System.out.println("***** ( ( o))  New Relic Java Agent Installer");
        System.out.println("***** Installing version " + Agent.getVersion() + " ...");

        File newRelicDir =
                new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();

        File appServerDir = null;
        if (cmd.getOptionValue('s') != null) {
            appServerDir = new File(cmd.getOptionValue('s'));
        }
        if ((appServerDir == null) || (!appServerDir.exists()) || (!appServerDir.isDirectory())) {
            appServerDir = newRelicDir.getParentFile();
        }
        appServerDir = appServerDir.getCanonicalFile();

        boolean startup_patched = false;
        boolean config_installed = false;
        AppServerType type = AppServerIdentifier.getAppServerType(appServerDir);

        if ((type == null) || (type == AppServerType.UNKNOWN)) {
            printUnknownAppServer(appServerDir);
        } else {
            SelfInstaller installer = SelfInstallerFactory.getSelfInstaller(type);
            if (installer != null) {
                startup_patched = installer.backupAndEditStartScript(appServerDir.toString());
            }
        }

        if ((newRelicDir.exists()) && (newRelicDir.isDirectory())) {
            if (ConfigInstaller.isConfigInstalled(newRelicDir)) {
                System.out.println("No need to create New Relic configuration file because:");
                System.out.println(MessageFormat.format(" .:. A config file already exists: {0}",
                                                               new Object[] {ConfigInstaller.configPath(newRelicDir)}));

                config_installed = true;
            } else {
                try {
                    ConfigInstaller.install(cmd.getOptionValue('l'), newRelicDir);
                    config_installed = true;
                    System.out.println("Generated New Relic configuration file " + ConfigInstaller
                                                                                           .configPath(newRelicDir));
                } catch (IOException e) {
                    System.err.println(MessageFormat
                                               .format("An error occurred generating the configuration file {0} : {1}",
                                                              new Object[] {ConfigInstaller.configPath(newRelicDir),
                                                                                   e.toString()}));

                    Agent.LOG.log(Level.FINE, "Config file generation error", e);
                }
            }
        } else {
            System.err.println("Could not create New Relic configuration file because:");
            System.err.println(MessageFormat.format(" .:. {0} does not exist or is not a directory",
                                                           new Object[] {newRelicDir.getAbsolutePath()}));
        }

        if ((startup_patched) && (config_installed)) {
            printInstallSuccess();
            System.exit(0);
        } else {
            printInstallIncomplete();
            System.exit(1);
        }
    }

    private void printInstallSuccess() {
        System.out.println("***** Install successful");
        System.out.println("***** Next steps:");
        System.out.println("You're almost done! To see performance data for your app:" + SelfInstaller.lineSep
                                   + " .:. Restart your app server" + SelfInstaller.lineSep + " .:. Exercise your app"
                                   + SelfInstaller.lineSep + " .:. Log into http://rpm.newrelic.com"
                                   + SelfInstaller.lineSep
                                   + "Within two minutes, your app should show up, ready to monitor and troubleshoot."
                                   + SelfInstaller.lineSep
                                   + "If app data doesn't appear, check newrelic/logs/newrelic_agent.log for errors.");
    }

    private void printInstallIncomplete() {
        System.out.println("***** Install incomplete");
        System.out.println("***** Next steps:");
        System.out.println("For help completing the install, see https://newrelic.com/docs/java/new-relic-for-java");
    }

    private void printUnknownAppServer(File appServerLoc) {
        StringBuilder knownAppServers = new StringBuilder();
        for (int i = 0; i < AppServerType.values().length - 1; i++) {
            AppServerType type = AppServerType.values()[i];
            knownAppServers.append(type.getName());
            if (i < AppServerType.values().length - 3) {
                knownAppServers.append(", ");
            } else if (i == AppServerType.values().length - 3) {
                knownAppServers.append(" or ");
            }
        }

        System.out.println("Could not edit start script because:");
        System.out.println(" .:. Could not locate a " + knownAppServers.toString() + " instance in " + appServerLoc
                                                                                                               .toString());

        System.out.println("Try re-running the install command with the -s <AppServerRootDirectory> option or from "
                                   + "<AppServerRootDirectory>" + SelfInstaller.fileSep + "newrelic.");

        System.out.println("If that doesn't work, locate and edit the start script manually.");
    }

    private void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        System.out.println(MessageFormat.format("New Relic Agent Version {0}", new Object[] {Agent.getVersion()}));
        formatter.printHelp("java -jar newrelic.jar", "", getBasicOptions(), getCommandLineFooter());
    }

    private void printHelp(String command) {
        if (command == null) {
            printHelp();
            return;
        }
        HelpFormatter formatter = new HelpFormatter();
        System.out.println(MessageFormat.format("New Relic Agent Version {0}", new Object[] {Agent.getVersion()}));
        String footer = "\n  " + command + ' ' + commandDescriptions.get(command);
        formatter.printHelp("java -jar newrelic.jar " + command, "", commandOptionsMap.get(command), footer);
    }

    private void verifyInstrumentation(CommandLine cmd) {
        List args = cmd.getArgList().subList(1, cmd.getArgList().size());

        String instrumentationJar = (String) args.get(0);
        boolean expectedVerificationResult = Boolean.valueOf((String) args.get(1)).booleanValue();
        List userJars = new ArrayList();
        if (args.size() > 2) {
            userJars = args.subList(2, args.size());
        }

        try {
            ServiceFactory.setServiceManager(null);

            VerificationLogger logger = new VerificationLogger();
            InstrumentationVerifier instrumentationVerifier = new InstrumentationVerifier(logger);
            boolean passed = instrumentationVerifier.verify(instrumentationJar, userJars);
            List output = logger.getOutput();
            logger.flush();
            if (passed == expectedVerificationResult) {
                instrumentationVerifier.printVerificationResults(System.out, output);
            } else {
                instrumentationVerifier.printVerificationResults(System.err, output);
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Unexpected error while verifying");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private String getCommandLineFooter() {
        int maxCommandLength = getMaxCommandLength();
        String minSpaces = "    ";

        StringBuilder builder = new StringBuilder("\nCommands:");
        for (Entry entry : commandDescriptions.entrySet()) {
            String extraSpaces =
                    new String(new char[maxCommandLength - ((String) entry.getKey()).length()]).replace('\000', ' ');
            builder.append("\n  ").append((String) entry.getKey()).append(extraSpaces).append(minSpaces)
                    .append((String) entry.getValue());
        }
        return builder.toString();
    }

    private int getMaxCommandLength() {
        int max = 0;
        for (String command : commandDescriptions.keySet()) {
            max = Math.max(max, command.length());
        }
        return max;
    }
}