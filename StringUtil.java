package com.adobe.alf.plugin.androidxml;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;

public final class StringUtil {
	
	public static String tempBeginTag = "<tempRoot>";
	public static String tempEndTag = "</tempRoot>";
	
	/*
	 * Convert a DOM node to string without including the root node
	 * root node matched pattern such as <string name="login_dialog_connection_error">
	 * Used in importResource/Read - import string into ALF db
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
		str = str.replace(tempEndTag, "");
		String regex = "<" + nodeName + "\\s*" + "name=[\"'][^\\s<>]+[\"']" + "\\s*" + ">";
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
}
