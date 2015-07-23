package com.newrelic.agent.transport;

import com.newrelic.agent.config.AgentConfig;

public class DataSenderFactory {
    private static volatile IDataSenderFactory DATA_SENDER_FACTORY = new DefaultDataSenderFactory();

    public static IDataSenderFactory getDataSenderFactory() {
        return DATA_SENDER_FACTORY;
    }

    public static void setDataSenderFactory(IDataSenderFactory dataSenderFactory) {
        if (dataSenderFactory == null) {
            return;
        }
        DATA_SENDER_FACTORY = dataSenderFactory;
    }

    public static DataSender create(AgentConfig config) {
        return DATA_SENDER_FACTORY.create(config);
    }

    private static class DefaultDataSenderFactory implements IDataSenderFactory {
        public DataSender create(AgentConfig config) {
            return new DataSenderImpl(config);
        }
    }
}