package com.newrelic.agent.extension.dom;

import com.newrelic.agent.Agent;
import com.newrelic.agent.extension.beans.Extension;
import com.newrelic.agent.extension.jaxb.Unmarshaller;
import com.newrelic.agent.extension.jaxb.UnmarshallerFactory;
import com.newrelic.agent.logging.IAgentLogger;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class ExtensionDomParser
{
  private static final ErrorHandler LOGGING_ERROR_HANDLER = new ErrorHandler()
  {
    public void warning(SAXParseException exception) throws SAXException {
      Agent.LOG.log(Level.FINEST, exception.toString(), exception);
    }

    public void fatalError(SAXParseException exception) throws SAXException
    {
      Agent.LOG.log(Level.FINER, exception.toString(), exception);
    }

    public void error(SAXParseException exception) throws SAXException
    {
      Agent.LOG.log(Level.FINEST, exception.toString(), exception);
    }
  };

  private static final ErrorHandler IGNORE_ERROR_HANDLER = new ErrorHandler() { public void warning(SAXParseException exception) throws SAXException {  } 
    public void fatalError(SAXParseException exception) throws SAXException {  } 
    public void error(SAXParseException exception) throws SAXException {  }  } ;
  private static final String NAMESPACE = "https://newrelic.com/docs/java/xsd/v1.0";

  public static Extension readStringGatherExceptions(String xml, List<Exception> exceptions)
  {
    if ((xml == null) || (xml.length() == 0)) {
      Agent.LOG.log(Level.FINE, "The input xml string is empty.");
      return null;
    }
    try {
      Document doc = getDocument(xml, false);
      return parseDocument(doc);
    }
    catch (Exception e) {
      exceptions.add(e);
    }return null;
  }

  public static Extension readStringCatchException(String xml)
  {
    if ((xml == null) || (xml.length() == 0)) {
      Agent.LOG.log(Level.FINE, "The input xml string is empty.");
      return null;
    }
    try {
      Document doc = getDocument(xml, false);
      return parseDocument(doc);
    }
    catch (Exception e)
    {
      Agent.LOG.log(Level.WARNING, MessageFormat.format("Failed to read extension {0}. Skipping the extension. Reason: {1}", new Object[] { xml, e.getMessage() }));

      if (Agent.LOG.isFinerEnabled())
        Agent.LOG.log(Level.FINER, "Reason For Failure: " + e.getMessage(), e);
    }
    return null;
  }

  public static Extension readFileCatchException(File file)
  {
    try
    {
      return readFile(file);
    } catch (Exception e) {
      Agent.LOG.log(Level.WARNING, MessageFormat.format("Failed to read extension {0}. Skipping the extension. Reason: {1}", new Object[] { file.getName(), e.getMessage() }));

      if (Agent.LOG.isFinerEnabled())
        Agent.LOG.log(Level.FINER, "Reason For Failure: " + e.getMessage(), e);
    }
    return null;
  }

  public static Extension readFile(File file)
    throws SAXException, IOException, ParserConfigurationException, JAXBException, NoSuchMethodException, SecurityException
  {
    return parseDocument(getDocument(file));
  }

  public static Extension readFile(InputStream inputStream)
    throws SAXException, IOException, ParserConfigurationException, JAXBException, NoSuchMethodException, SecurityException
  {
    return parseDocument(getDocument(new InputSource(inputStream), true));
  }

  public static Extension parseDocument(Document doc)
    throws SAXException, IOException, ParserConfigurationException, NoSuchMethodException, SecurityException
  {
    trimTextNodeWhitespace(doc.getDocumentElement());
    doc = fixNamespace(doc);

    Schema schema = getSchema();
    Validator validator = schema.newValidator();

    validator.validate(new DOMSource(doc));
    try
    {
      Unmarshaller unmarshaller = UnmarshallerFactory.create(Extension.class);
      return (Extension)unmarshaller.unmarshall(doc);
    } catch (Exception ex) {
      try {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty("indent", "yes");

        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);
        String xmlString = result.getWriter().toString();
        System.out.println(xmlString);
      } catch (Exception e) {
        e.printStackTrace();
      }
      throw new IOException(ex);
    }
  }

  private static Document getDocument(String pXml, boolean setSchema)
    throws SAXException, IOException, ParserConfigurationException, NoSuchMethodException, SecurityException
  {
    ByteArrayInputStream baos = null;
    try
    {
      baos = new ByteArrayInputStream(pXml.getBytes());
      return getDocument(new InputSource(baos), setSchema);
    }
    finally {
      if (baos != null)
        try {
          baos.close();
        }
        catch (IOException e)
        {
        }
    }
  }

  private static Document getDocument(File file)
    throws SAXException, IOException, ParserConfigurationException, NoSuchMethodException, SecurityException
  {
    FileInputStream fis = null;
    try
    {
      fis = new FileInputStream(file);
      return getDocument(new InputSource(fis), true);
    }
    finally {
      if (fis != null)
        try {
          fis.close();
        }
        catch (IOException e)
        {
        }
    }
  }

  private static Schema getSchema() throws IOException, SAXException, ParserConfigurationException, NoSuchMethodException, SecurityException
  {
    URL schemaFile = Agent.getClassLoader().getResource("META-INF/extensions/extension.xsd");
    if (schemaFile == null) {
      throw new IOException("Unable to load the extension schema");
    }

    Agent.LOG.finest("Loading extension schema from " + schemaFile);
    SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

    DocumentBuilderFactory factory = getDocumentBuilderFactory();

    DocumentBuilder builder = factory.newDocumentBuilder();
    builder.setErrorHandler(LOGGING_ERROR_HANDLER);
    Document schemaDoc = builder.parse(schemaFile.openStream());

    return schemaFactory.newSchema(new DOMSource(schemaDoc));
  }

  private static Document getDocument(InputSource inputSource, boolean setSchema)
    throws SAXException, IOException, ParserConfigurationException, NoSuchMethodException, SecurityException
  {
    DocumentBuilderFactory factory = getDocumentBuilderFactory();

    if (setSchema) {
      Schema schema = getSchema();

      factory.setSchema(schema);
    }
    DocumentBuilder builder = factory.newDocumentBuilder();
    builder.setErrorHandler(IGNORE_ERROR_HANDLER);

    return builder.parse(inputSource);
  }

  private static DocumentBuilderFactory getDocumentBuilderFactory()
    throws ParserConfigurationException, NoSuchMethodException, SecurityException
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    try
    {
      setupDocumentFactory(factory);
    }
    catch (AbstractMethodError e)
    {
      return getAndSetupDocumentBuilderComSunFactory();
    }

    return factory;
  }

  private static void setupDocumentFactory(DocumentBuilderFactory factory) throws ParserConfigurationException {
    factory.setNamespaceAware(true);

    factory.setExpandEntityReferences(false);

    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);

    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

    factory.setValidating(false);
    factory.setIgnoringElementContentWhitespace(true);
  }

  private static DocumentBuilderFactory getAndSetupDocumentBuilderComSunFactory() throws NoSuchMethodError {
    try {
      Class clazz = ClassLoader.getSystemClassLoader().loadClass("com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");

      DocumentBuilderFactory factory = (DocumentBuilderFactory)clazz.newInstance();
      setupDocumentFactory(factory);
      return factory;
    } catch (Throwable e) {
      Agent.LOG.info("Your application has loaded a Java 1.4 or below implementation of the class DocumentBuilderFactory. Please upgrade to a 1.5 version if you want to use Java agent XML instrumentation.");
    }throw new NoSuchMethodError("The method setFeature can not be called.");
  }

  public static void trimTextNodeWhitespace(Node e)
  {
    NodeList children = e.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);

      if ((child instanceof Text)) {
        Text text = (Text)child;
        text.setData(text.getData().trim());
      }
      trimTextNodeWhitespace(child);
    }
  }

  private static Document fixNamespace(Document doc)
  {
    try
    {
      Transformer transformer = getTransformerFactory().newTransformer();
      StreamResult result = new StreamResult(new StringWriter());
      DOMSource source = new DOMSource(doc);
      transformer.transform(source, result);
      String xmlString = result.getWriter().toString();
      xmlString = xmlString.replace("xmlns:urn=\"newrelic-extension\"", "xmlns:urn=\"https://newrelic.com/docs/java/xsd/v1.0\"");

      return getDocument(xmlString, true); } catch (Exception ex) {
    }
    return doc;
  }

  private static TransformerFactory getTransformerFactory()
    throws TransformerFactoryConfigurationError
  {
    try
    {
      return TransformerFactory.newInstance();
    } catch (TransformerFactoryConfigurationError ex) {
      try {
        Class clazz = ClassLoader.getSystemClassLoader().loadClass("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");

        Method method = clazz.getMethod("newTransformerFactoryNoServiceLoader", new Class[0]);
        return (TransformerFactory)method.invoke(null, new Object[0]); } catch (Exception e) {
      }
      throw ex;
    }
  }
}