//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent;

import java.util.concurrent.atomic.AtomicReference;

public enum TransactionErrorPriority {
    API {
        protected boolean updatePriority(AtomicReference<TransactionErrorPriority> current) {
            return this == current.get() ? false
                           : current.compareAndSet(TRACER, this) || current.compareAndSet(ASYNC_POINTCUT, this);
        }
    },
    TRACER {
        protected boolean updatePriority(AtomicReference<TransactionErrorPriority> current) {
            return this == current.get() ? true : current.compareAndSet(ASYNC_POINTCUT, this);
        }
    },
    ASYNC_POINTCUT {
        protected boolean updatePriority(AtomicReference<TransactionErrorPriority> current) {
            return false;
        }
    };

    private TransactionErrorPriority() {
    }

    protected abstract boolean updatePriority(AtomicReference<TransactionErrorPriority> var1);

    public boolean updateCurrentPriority(AtomicReference<TransactionErrorPriority> current) {
        return current.compareAndSet(null, this) ? true : this.updatePriority(current);
    }
}
