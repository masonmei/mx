package com.newrelic.agent.attributes;

public class RootConfigAttributesNode extends AttributesNode
{
  public RootConfigAttributesNode(String dest)
  {
    super("", true, dest, true);
  }

  public Boolean applyRules(String key)
  {
    Boolean result = null;
    for (AttributesNode current : getChildren())
    {
      result = current.applyRules(key);
      if (result != null) {
        break;
      }
    }
    return result;
  }

  public boolean addNode(AttributesNode rule)
  {
    if (rule != null) {
      for (AttributesNode current : getChildren()) {
        if (current.addNode(rule)) {
          return true;
        }
      }

      addNodeToMe(rule);
      return true;
    }
    return false;
  }
}