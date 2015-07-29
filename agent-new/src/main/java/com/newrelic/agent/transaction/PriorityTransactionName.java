package com.newrelic.agent.transaction;

import java.text.MessageFormat;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;

public class PriorityTransactionName {
  public static final PriorityTransactionName NONE = create((String) null, null, TransactionNamePriority.NONE);
  public static final String WEB_TRANSACTION_CATEGORY = "Web";
  public static final String UNDEFINED_TRANSACTION_CATEGORY = "Other";
  private final TransactionNamePriority priority;
  private final String prefix;
  private final String partialName;
  private final String category;

  private PriorityTransactionName(String prefix, String partialName, String category,
                                  TransactionNamePriority priority) {
    this.prefix = prefix;
    this.partialName = partialName;
    this.category = category;
    this.priority = priority;
  }

  public static PriorityTransactionName create(String transactionName, String category,
                                               TransactionNamePriority priority) {
    if (transactionName == null) {
      return new PriorityTransactionName(null, null, category, priority);
    }
    int index = transactionName.indexOf('/', 1);
    if (index > 0) {
      index = transactionName.indexOf('/', index + 1);
      if (index > 0) {
        String prefix = transactionName.substring(0, index);
        String partialName = transactionName.substring(index);
        return new PriorityTransactionName(prefix, partialName, category, priority);
      }
    }
    return new PriorityTransactionName(transactionName, null, category, priority);
  }

  public static PriorityTransactionName create(final Transaction tx, String partialName, String category,
                                               TransactionNamePriority priority) {
    if (priority == null) {
      return null;
    }
    if ((category == null) || (category.isEmpty())) {
      category = tx.isWebTransaction() ? "Web" : "Other";
    }
    return new PriorityTransactionName(null, partialName, category, priority) {
      public String getPrefix() {
        return tx.isWebTransaction() ? "WebTransaction" : "OtherTransaction";
      }
    };
  }

  public static PriorityTransactionName create(String prefix, String partialName, String category,
                                               TransactionNamePriority priority) {
    if (priority == null) {
      return null;
    }
    return new PriorityTransactionName(prefix, partialName, category, priority);
  }

  private String initializeName(String partialName) {
    if (getPrefix() == null) {
      return null;
    }
    if (partialName == null) {
      return getPrefix();
    }
    return getPrefix() + partialName;
  }

  public String getName() {
    return initializeName(partialName);
  }

  public String getPrefix() {
    return prefix;
  }

  public String getPartialName() {
    return partialName;
  }

  public String getCategory() {
    return category;
  }

  public boolean isFrozen() {
    return priority == TransactionNamePriority.FROZEN;
  }

  public PriorityTransactionName freeze() {
    if (isFrozen()) {
      return this;
    }
    return create(getPrefix(), getPartialName(), category, TransactionNamePriority.FROZEN);
  }

  public TransactionNamePriority getPriority() {
    return priority;
  }

  public String toString() {
    return MessageFormat.format("{0}[name={1}, priority={2}]",
                                       new Object[] {getClass().getName(), getName(), getPriority()});
  }

  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = 31 * result + (partialName == null ? 0 : partialName.hashCode());
    result = 31 * result + (prefix == null ? 0 : prefix.hashCode());
    result = 31 * result + (priority == null ? 0 : priority.hashCode());
    return result;
  }

  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof PriorityTransactionName)) {
      return false;
    }
    PriorityTransactionName other = (PriorityTransactionName) obj;
    String name = getName();
    String otherName = other.getName();
    if (name == null) {
      if (otherName != null) {
        return false;
      }
    } else if (!name.equals(otherName)) {
      return false;
    }
    return priority.equals(other.priority);
  }
}