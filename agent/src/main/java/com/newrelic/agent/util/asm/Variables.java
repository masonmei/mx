package com.newrelic.agent.util.asm;

import org.objectweb.asm.Type;

import com.newrelic.agent.bridge.Transaction;

public abstract interface Variables {
    public abstract Object loadThis(int paramInt);

    public abstract Transaction loadCurrentTransaction();

    public abstract <N extends Number> N loadLocal(int paramInt, Type paramType, N paramN);

    public abstract <N extends Number> N load(N paramN, Runnable paramRunnable);

    public abstract <O> O load(Class<O> paramClass, Runnable paramRunnable);

    public abstract <O> O loadLocal(int paramInt, Class<O> paramClass);
}