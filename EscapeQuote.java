import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class EscapeQuote {
	
	public static String tempBeginTag = "<tempRoot>";
	public static String tempEndTag = "</tempRoot>";
	private static final String DEFAULT_ENCODING_REL = "UTF-8";
	
	public static void main(String[] args) {
		
		if (args.length != 2) {
			// may be throw some exceptions
			throw(new Error("Needs both input and output file names"));
		}
		
		String inputFileName = args[0];
		String outputFileName = args[1];
		boolean change = false;
		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();;
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setEntityResolver(new EntityResolver() {
		        @Override
		        public InputSource resolveEntity(String publicId, String systemId)
		                throws SAXException, IOException {
		            if (systemId.contains("asf_1_0.dtd")) {
		                return new InputSource(new StringReader(""));
		            } else {
		                return null;
		            }
		        }
		    });
			
			FileInputStream stream = new FileInputStream(inputFileName);
			Document doc = builder.parse(stream, DEFAULT_ENCODING_REL);
			NodeList valList = doc.getElementsByTagName("val");
			Node parent = null;
			for (int i=0; i < valList.getLength(); i++) {
				parent = valList.item(i).getParentNode();
				Node valNode = valList.item(i);
				if (valNode != null && valNode.getFirstChild() != null && valNode.getNodeType() == Node.ELEMENT_NODE ) {
					String val = nodeToString(valNode);
					
					if (hasSingleQuote(val)) {
						
						val = escapeSingleQuote(val);
						change = true;
						
						// create a new val node with new value
						String temp = "<val>" + val + "</val>";
						Document newDoc = builder.parse(new ByteArrayInputStream(temp.getBytes(DEFAULT_ENCODING_REL)));
						Node root = newDoc.getDocumentElement();
						Node newVal = doc.adoptNode(root.cloneNode(true));
						if (((Element)valNode).hasAttribute("plat")) {
							String plat = ((Element)valNode).getAttribute("plat");
							((Element)newVal).setAttribute("plat", plat);
							
						}
						
						if (parent != null && newVal != null) {						
							parent.replaceChild(newVal, valNode);
						}
	
					}

				}
				
			}
			// Product the new file
			if (change) {
			    // Write output to xml file
			    TransformerFactory tFactory = TransformerFactory.newInstance();
			    tFactory.setAttribute("indent-number", 4);
			    Transformer transformer = tFactory.newTransformer();		    
			    //transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			    DOMImplementation domImpl = doc.getImplementation();
			    DocumentType doctype = domImpl.createDocumentType("doctype",
			        "",
			        "http://ns.adobe.com/asf/asf_1_0.dtd");
			    //transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
			    transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());			    
			    doc.setXmlStandalone(true);
			    DOMSource source = new DOMSource(doc);
			    StreamResult result = new StreamResult(new FileOutputStream(outputFileName));
			    transformer.transform(source, result);
			}
			
		} catch (Exception e) {
			System.out.println("Error thrown from EscapeQuote." + e);
		}

	}
	
	/*
	 * Convert a DOM node to string without including the root node
	 * The default toString() is not sufficient, so here the custom function 
	 * @param node
	 * 				a DOM node from resource file
	 */
	public static String nodeToString(Node node) {
		String nodeName = node.getNodeName();
		String tempEndTag = "</" + nodeName + ">";
		String str = "";
		StringWriter sw = new StringWriter();
		try {
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			t.transform(new DOMSource(node), new StreamResult(sw));
		} catch (TransformerException te) {
			System.out.println("nodeToString Transformer Exception");
		}
		
		str = sw.toString();
		// remove end tag
		str = str.replace(tempEndTag, "");
		// remove begin tag
		String regex = "<" + nodeName + "\\s*" + ".*?" + "\\s*" + ">";
		str = str.replaceAll(regex, "");
		return str;
	}		
	
	/*
	 * Escape single quote/apostrophe and double quotes with a backslash such as \' and \"
	 * The negative look behind does not work. Below is workaround.
	 * @param str
	 *            a string from resource file
	 */
	public static String escapeQuote(String str) {
		String result = "";
		Pattern p = Pattern.compile("([\"'])");
		Matcher m = p.matcher(str);
		int initial = 0;
		String sub = "";
		String remain = "";
		String quote = "'";
		while (m.find()) {
			quote = m.group(1);
			int start = m.start();
			int end = m.end();
			if (start > initial) {
				if (str.charAt(start-1) != '\\') {
					sub = str.substring(initial, end);
					sub = sub.replace(quote, "\\" + quote);					
					result = result + sub;
					if (start < str.length()-1) {
						initial = end;
						remain = str.substring(initial, str.length());
					} else {
						remain = "";
					}
					
				}
			}
		}
		if (result != "") {
			result = result + remain;
			return result;
		}
		return str;
	}
	
	
	/*
	 * Escape single quote/apostrophe with a backslash as \' if it has not already been escaped.
	 * The negative look behind does not work:
	 * Pattern p = Pattern.compile(Pattern.quote("(?<!\\)'"));
	 * Below is workaround.
	 * @param str
	 *            a string from resource file
	 */
	public static String escapeSingleQuote(String str) {
		String result = "";
		Pattern p = Pattern.compile("'");
		Matcher m = p.matcher(str);
		int initial = 0;
		String sub = "";
		String remain = "";
		while (m.find()) {
			int start = m.start();
			int end = m.end();
			if (start > initial) {
				if (str.charAt(start-1) != '\\') {
					sub = str.substring(initial, end);
					sub = sub.replace("'", "\\'");					
					result = result + sub;
					if (start < str.length()-1) {
						initial = end;
						remain = str.substring(initial, str.length());
					} else {
						remain = "";
					}
					
				}
			}
		}
		if (result != "") {
			result = result + remain;
			return result;
		}
		return str;
	}
	
	/*
	 * Find out if there is a single quote that is not escaped yet.
	 * @param str
	 *            a string from resource file
	 */
	public static boolean hasSingleQuote(String str) {
		Pattern p = Pattern.compile("'");
		Matcher m = p.matcher(str);
		int initial = 0;
		while (m.find()) {
			int start = m.start();
			if (start > initial) {
				if (str.charAt(start-1) != '\\') {
					return true;
				}
			}
		}
		return false;
	}

}


