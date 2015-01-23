package com.skyword.api.feed;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.text.Normalizer.Form;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpVersion;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Helper methods that can be used generically. 
 * 
 *
 */
public class HelperMethods {

    /**
     * Method to generate a slug based on the inputed string.
     * 
     * @param input
     * @return String slug created from input parameter
     * @throws UnsupportedEncodingException
     */
    public static String generateSlug(String input) throws UnsupportedEncodingException {
        if (null == input || 0 == input.length()) {
            return "";
        }
        String removelist = "a|an|as|at|before|but|by|for|from|is|in|into|like|of|off|on|onto|per|since|than|the|this|that|to|up|via|with";

        String s = normalize(input);
        s = s.toLowerCase();
        s = s.replaceAll("\\b(" + removelist + ")\\b", "");
        s = s.replaceAll("[^-\\w\\s]", ""); // remove unneeded chars
        s = s.trim();
        s = s.replaceAll("[\\s]+", "-"); // convert spaces to hyphens
        s = s.replaceAll("-+", "-"); // collapse multiple hyphens to one hyphen
        s = URLEncoder.encode(s, "UTF-8");
        if (s.length() > 50) {
            return s.substring(0, 50);
        } else {
            return s;
        }
    }

    /**
     * Normalize the string and convert any remaining non-ascii characters to empty strings.
     * 
     * @param input
     * @return String
     */
    public static String normalize(String input) {
        if (input == null || input.length() == 0)
            return "";
        return Normalizer.normalize(input, Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    }

    /**
     * Extract the post data from the given HTTP Method.
     * 
     * @param method
     * @return String 
     * @throws Exception
     */
    public static String getPostData(HttpMethodBase method) throws Exception {
        InputStream is = method.getResponseBodyAsStream();
        StringBuffer input = new StringBuffer();
        String line = null;
        BufferedReader rdr;
        rdr = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        while ((line = rdr.readLine()) != null) {
            input.append(line + "\n");
        }
        rdr.close();
        is.close();
        return input.toString();
    }

    /**
     * Receives an XML document as a string and converts it to a W3C Document.
     * 
     * @param xmlString
     * @return Document
     * @throws Exception
     */
    public static Document convertXMLStringToDocument(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xmlString)));

        return document;
    }

    /**
     * Setup the HttpClient and return it.
     * 
     * @return HttpClient
     * @throws Exception
     */
    public static HttpClient setupClient() throws Exception {
        HttpClient client = new HttpClient();

        // Set HttpClient preferences
        // See http://jakarta.apache.org/commons/httpclient/preference-api.html
        client.getParams().setParameter("http.protocol.version", HttpVersion.HTTP_1_1);
        client.getParams().setParameter("http.socket.timeout", new Integer(60000));
        client.getParams().setParameter("http.connection.timeout", new Integer(60000));
        client.getParams().setParameter("http.protocol.content-charset", "UTF-8");

        return client;
    }
}
