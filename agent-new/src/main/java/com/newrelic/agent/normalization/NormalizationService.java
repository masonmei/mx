package com.newrelic.agent.normalization;

import com.newrelic.agent.service.Service;

public abstract interface NormalizationService extends Service
{
  public abstract Normalizer getMetricNormalizer(String paramString);

  public abstract Normalizer getTransactionNormalizer(String paramString);

  public abstract Normalizer getUrlNormalizer(String paramString);

  public abstract String getUrlBeforeParameters(String paramString);
}