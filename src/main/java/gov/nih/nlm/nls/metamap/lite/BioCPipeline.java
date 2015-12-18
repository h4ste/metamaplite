
//
package gov.nih.nlm.nls.metamap.lite;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;

import java.lang.reflect.InvocationTargetException;

import org.apache.lucene.queryparser.classic.ParseException;

import gov.nih.nlm.nls.utils.StringUtils;
import gov.nih.nlm.nls.metamap.lite.pipeline.plugins.Plugin;
import gov.nih.nlm.nls.metamap.lite.pipeline.plugins.PluginRegistry;
import gov.nih.nlm.nls.metamap.lite.pipeline.plugins.PipelineRegistry;

import gov.nih.nlm.nls.metamap.lite.types.Entity;
import gov.nih.nlm.nls.metamap.lite.SentenceExtractor;
import gov.nih.nlm.nls.metamap.document.ChemDNER;
import gov.nih.nlm.nls.metamap.document.ChemDNERSLDI;
import gov.nih.nlm.nls.metamap.document.FreeText;
import gov.nih.nlm.nls.metamap.document.NCBICorpusDocument;
import gov.nih.nlm.nls.metamap.document.SingleLineInput;

import gov.nih.nlm.nls.metamap.lite.resultformats.Brat;

import bioc.BioCDocument;
import bioc.BioCPassage;
import bioc.BioCAnnotation;
import bioc.BioCSentence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 */

public class BioCPipeline {
  /** log4j logger instance */
  private static final Logger logger = LogManager.getLogger(Pipeline.class);
  /** location of metamaplite.properties configuration file */
  static String configPropertyFilename =
    System.getProperty("metamaplite.property.file", "config/bioc.metamaplite.properties");

  /**
   * Invoke sentence processing pipeline on asentence
   * @param sentence
   * @return updated sentence
   */
  public static BioCSentence processSentence(BioCSentence sentence)
    throws IllegalAccessException, InvocationTargetException
  {
    logger.debug("enter processSentence");
    List<Plugin> pipeSequence = PipelineRegistry.get("simple.sentence");
    Object current = sentence;
    Object resultObject = null;
    for (Plugin plugin: pipeSequence) {
      resultObject = plugin.getMethod().invoke(plugin.getClassInstance(), current);
      current = resultObject;
    }
    BioCSentence result = null;
    logger.debug("exit processSentence");
    if (resultObject instanceof BioCSentence) {
      result = (BioCSentence)resultObject;
    }
    logger.debug("exit processSentence");
    return result;
 }

  /**
   * Invoke sentence processing pipeline on each sentence in supplied sentence list.
   * @param passage containing list of sentences
   * @return list of results from sentence processing pipeline, one per sentence in input list.
   */
  public static BioCPassage processSentences(BioCPassage passage) 
    throws IllegalAccessException, InvocationTargetException
  {
    logger.debug("enter processSentences");
    List<BioCSentence> resultList = new ArrayList<BioCSentence>();
    for (BioCSentence sentence: passage.getSentences()) {
      logger.info("Processing: " + sentence.getText());
      resultList.add(BioCPipeline.processSentence(sentence));
    }
    logger.debug("exit processSentences");
    // passage.setSentences(resultList);
    return passage;
  }

  public void processPassage(BioCPassage passage)
    throws IllegalAccessException, InvocationTargetException
  {
    logger.debug("enter processPassage");
    List<Plugin> pipeSequence = PipelineRegistry.get("simple.passage");
    Object current = passage;
    for (Plugin plugin: pipeSequence) {
      Object result = plugin.getMethod().invoke(plugin.getClassInstance(), current);
      current = result;
    }
    // BioCPassage newPassage = BioCPipeline.processSentences(SentenceExtractor.createSentences(passage));
    logger.debug("exit processPassage");
  }

  public BioCDocument processDocument(BioCDocument document) 
    throws IllegalAccessException, InvocationTargetException
  {
    for (BioCPassage passage: document.getPassages()) {
      logger.info(passage.getText());
      this.processPassage(passage);
    }
    return document;
  }
  
  public List<BioCDocument> processDocumentList(List<BioCDocument> documentList)
    throws IllegalAccessException, InvocationTargetException, IOException, ParseException
  {
    List<BioCDocument> newDocumentList = new ArrayList<BioCDocument>();
    for (BioCDocument document: documentList) {
      newDocumentList.add(this.processDocument(document));
    }
    return newDocumentList;
  }

  /**
   * Initialize pipeline application.
   * @return pipeline application instance
   */
  static BioCPipeline initPipeline()
    throws IOException, FileNotFoundException,
	   ClassNotFoundException, InstantiationException,
	   NoSuchMethodException, IllegalAccessException {
    logger.debug("enter initPipeline");

    Properties properties = new Properties();
    properties.load(new FileReader(configPropertyFilename));
    if (logger.isDebugEnabled()) {
      for (Map.Entry<Object,Object> entry: properties.entrySet()) {
	logger.debug(entry.getKey() + " -> " + entry.getValue());
      }
    }
    BioCPipeline pipeline = new BioCPipeline();
    PluginRegistry.registerPlugins(properties);
    logger.info("plugins:");
    for (String name: PluginRegistry.listPlugins()) {
      logger.info(" " + name);
    }
    logger.info("pipesequence keys:");
    PipelineRegistry.registerPipeSequences("metamaplite.pipeline", properties);
    for (String content: PipelineRegistry.listPipeContents()) {
      logger.info(" " + content);
    }
    logger.debug("exit initPipeline");
    return pipeline;
  }

  static void displayHelp() {
    System.err.println("usage: [options] filename");
    System.err.println("options:");
    System.err.println("  --freetext (default)");
    System.err.println("  --ncbicorpus");
    System.err.println("  --chemdner");
    System.err.println("  --chemdnersldi");
  }

  /**
   * Pipeline application commandline.
   * <p>
   * You'll need the model file for the sentence extractor
   * "en-sent.bin" which can be downloaded from the opennlp project at
   * http://opennlp.sourceforge.net/models-1.5
   * <p>
   * Set the system property "en-sent.bin.path":
   * <pre>
   *  -Den-sent.bin.path=location of en-sent.bin
   * </pre>
   * Run the program using a command of the form:
   * <pre>
   * java -cp classpath -Den-sent.bin.path={location of en-sent.bin} gov.nih.nlm.nls.metamap.lite.Pipeline [options] input-file
   * </pre>
   * Or modify metamaplite.sh to set the location of the model file and then
   * run the script to the test application.
   * <pre>
   * ./metamaplite.sh [options] input-file
   * </pre>
   * Current options are:
   * <dl>
   * <dt>--freetext
   * <dd>Text with no markup.
   * <dt>--chemdner
   * <dd>CHEMDNER document: tab separated fields: id \t title \t abstract
   * <dt>--chemdnerSLDI
   * <dd>CHEMDNER document: id with pipe followed by tab separated fields: id | title \t abstract
   * </dl>
   * The application currently only outputs to standard output. (See
   * method:
   * gov.nih.nlm.nls.metamap.lite.EntityLookup.displayEntitySet)
   * 
   * @param args - Arguments passed from the command line
   * @throws FileNotFoundException file not found exception
   * @throws IOException IO exception
   * @throws InstantiationException exception instantiating instance of class
   * @throws NoSuchMethodException  no method in class
   * @throws IllegalAccessException illegal access of class
   * @throws ParseException except while parsing
   * @throws InvocationTargetException exception while invoking target class
   * @throws ClassNotFoundException class not found exception
   */
  public static void main(String[] args)
    throws IOException, FileNotFoundException,
	   ClassNotFoundException, InstantiationException,
	   NoSuchMethodException, IllegalAccessException,
	   ParseException , InvocationTargetException {

    if (args.length > 0) {
      BioCPipeline pipeline = initPipeline();
      List<String> filenameList = new ArrayList<String>();
      String processingOption = "--freetext";
      String displayOption = "--mmi";
      String outputFile = null;
      String entityLookupResultLengthString = "";
      int i = 0;
      while (i < args.length) {
	if (args[i].equals("--chemdnersldi")) {
	  processingOption = args[i];
	} else if (args[i].equals("--chemdner")) {
	  processingOption = args[i];
	} else if (args[i].equals("--ncbicorpus")) {
	  processingOption = args[i];
	} else if (args[i].equals("--freetext")) {
	  processingOption = args[i];
	} else if (args[i].equals("--sli")) {
	  processingOption = args[i];
	} else if (args[i].equals("--bc-evaluate") ||
		   args[i].equals("--bioc") ||
		   args[i].equals("--bc") ||
		   args[i].equals("--cdi")) {
	  displayOption = args[i];
	} else if (args[i].equals("--mmi") || 
		   args[i].equals("--mmilike")) {
	  displayOption = args[i];
	} else if (args[i].equals("--brat") || 
		   args[i].equals("--BRAT")) {
	  displayOption = args[i];
	} else if (args[i].equals("--luceneresultlen")) {
	  i++;
	  entityLookupResultLengthString = args[i];
	} else if (args[i].equals("--help")) {
	  displayHelp();
	  System.exit(1);
	} else {
	  filenameList.add(args[i]);
	}
	i++;
      }

      if (entityLookupResultLengthString.length() > 0) {
	System.setProperty("metamaplite.entitylookup.resultlength", 
			   entityLookupResultLengthString);
      }

      logger.info("Loading and processing documents");
      List<BioCDocument> newDocumentList = new ArrayList<BioCDocument>();;
      if (processingOption.equals("--chemdnersldi")) {
	List<BioCDocument> documentList = ChemDNERSLDI.bioCLoadSLDIFile(filenameList.get(0));
	/*CHEMDNER SLDI style documents*/
	newDocumentList = pipeline.processDocumentList(documentList);
      } else if (processingOption.equals("--chemdner")) {
	List<BioCDocument> documentList = ChemDNER.bioCLoadFile(filenameList.get(0));
	/*CHEMDNER SLDI style documents*/
	newDocumentList = pipeline.processDocumentList(documentList);
      } else if (processingOption.equals("--ncbicorpus")) {
	List<BioCDocument> documentList = NCBICorpusDocument.bioCLoadFile(filenameList.get(0));
	/*CHEMDNER SLDI style documents*/
	newDocumentList = pipeline.processDocumentList(documentList);
      } else if (processingOption.equals("--freetext")) {

	// String inputtext = FreeText.loadFile(filenameList.get(0));
	// BioCDocument document = new BioCDocument();
	// logger.info(inputtext);
	// BioCPassage passage = new BioCPassage();
	// passage.setText(inputtext);
	// passage.putInfon("docid", "00000000.tx");
	// passage.putInfon("freetext", "freetext");
	// document.addPassage(passage);
	// document.setID("00000000.tx");
	// List<BioCDocument> documentList = new ArrayList<BioCDocument>();
	// documentList.add(document);
	
	List<BioCDocument> documentList = FreeText.loadFreeTextFile(filenameList.get(0));
	newDocumentList = pipeline.processDocumentList(documentList);
      } else if (processingOption.equals("--sli")) {
	List<BioCDocument> documentList = SingleLineInput.bioCLoadFile(filenameList.get(0));
	/*Single line documents*/
	newDocumentList = pipeline.processDocumentList(documentList);
      } else if (processingOption.equals("--help")) {
	displayHelp();
	System.exit(1);
      } 
      logger.debug("document list length: " + newDocumentList.size());
      for (BioCDocument doc: newDocumentList) {
	logger.debug(doc);
      }

      logger.info("outputing results ");

      if (displayOption.equals("--bc-evaluate") ||
	  displayOption.equals("--bc") ||
	  displayOption.equals("--bioc") ||
	  displayOption.equals("--cdi")) {
	logger.info("writing BC evaluate format file...");
	for (BioCDocument document: newDocumentList) {
	  EntityLookup.writeBcEvaluateAnnotations(System.out, document);
	}
      } else if (displayOption.equals("--brat")) {
	logger.debug("writing mmi format output");
	for (BioCDocument document: newDocumentList) {
	  Brat.writeBratAnnotations("BioCPipeline",
					    new PrintWriter
					    (new BufferedWriter
					     (new OutputStreamWriter(System.out))), 
					    document);
	}
      } else if (displayOption.equals("--mmi")) {
	logger.debug("writing mmi format output");
	for (BioCDocument document: newDocumentList) {
	  EntityLookup.writeEntities(System.out, document);
	}
      } else {
	logger.debug("writing mmi format output");
	for (BioCDocument document: newDocumentList) {
	  EntityLookup.writeEntities(System.out, document);
	}
      }


    } else {
      displayHelp();
      System.exit(1);
    }   
  }
}
