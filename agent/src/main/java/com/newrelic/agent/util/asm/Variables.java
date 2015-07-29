package com.newrelic.agent.util.asm;

import com.newrelic.deps.org.objectweb.asm.Type;

import com.newrelic.agent.bridge.Transaction;

public interface Variables {
    Object loadThis(int paramInt);

    Transaction loadCurrentTransaction();

    <N extends Number> N loadLocal(int paramInt, Type paramType, N paramN);

    <N extends Number> N load(N paramN, Runnable paramRunnable);

    <O> O load(Class<O> paramClass, Runnable paramRunnable);

    <O> O loadLocal(int paramInt, Class<O> paramClass);
}