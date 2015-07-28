package com.newrelic.deps.ch.qos.logback.classic.boolex;

import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.*;

import com.newrelic.deps.ch.qos.logback.classic.spi.ILoggingEvent;

import groovy.lang.*;
import groovy.util.*;

public class EvaluatorTemplate  implements
        IEvaluator, GroovyObject {
public  groovy.lang.MetaClass getMetaClass() { return (groovy.lang.MetaClass)null;}
public  void setMetaClass(groovy.lang.MetaClass mc) { }
public  Object invokeMethod(String method, Object arguments) { return null;}
public  Object getProperty(String property) { return null;}
public  void setProperty(String property, Object value) { }
public  boolean doEvaluate(ILoggingEvent event) { return false;}
}
