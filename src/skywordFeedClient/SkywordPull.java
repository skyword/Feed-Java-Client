package skywordFeedClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class SkywordPull {

    protected static Log log = LogFactory.getLog(SkywordPull.class);

    private String baseUrl = "http://api.skyword.com";
    private String key;
    private String feedUrl;
    private String publishUrl;
    private String fileUrl;

    public SkywordPull() {
    }

    public SkywordPull(String tKey) {
        key = tKey;
        setUrls();
    }

    /**
     * Sets the key to be used in the URL calls to get the articles.
     * You need to call this method after instantiating the class.
     * 
     * @param newKey
     */
    public void setKey(String newKey) {
        key = newKey;
        setUrls();
    }

    /**
     * This should only be called in a non-production environment.
     * 
     * @param overrideUrl
     */
    public void setBaseUrl(String overrideUrl) {
        baseUrl = overrideUrl;
        setUrls();
    }

    private void setUrls() {
        feedUrl = baseUrl.concat("/feed?key=" + key);
        publishUrl = baseUrl.concat("/publish?key=" + key);
        fileUrl = baseUrl.concat("/file?key=" + key);
    }

    public static void main(String[] args) {
        try {
            String key = "";
            if (args.length > 0) {
                key = args[0];
            }

            String overrideUrl = null;
            if (args.length > 1) {
                overrideUrl = args[1];
            }

            log.debug("Starting pullapi client");
            SkywordPull sp = new SkywordPull(key);
            if (overrideUrl != null && overrideUrl.length() > 0) {
                sp.setBaseUrl(overrideUrl);
            }

            List<Map<String, String>> listData = sp.pull();
            printData(listData);

        } catch (Exception e) {
            log.error("Error in pull", e);
        }
    }

    /**
     * Method to pull content from Skyword and save to local CMS.
     * 
     * This method will pull all of the content from Skyword that is currently
     * approved. It will call the method saveToOurCMS which should be overridden to
     * write the content into your local CMS. It will then notify Skyword of the 
     * public URL for each article.
     * 
     * There are no parameters to call this method, but setKey(String) should be called
     * before calling this method.
     * 
     * @throws Exception
     */
    public void getSkywordContent() throws Exception {

        // pull retrieves the list of articles that are currently approved.
        List<Map<String, String>> listData = this.pull();
        if (listData == null) {
            return;
        }

        String publicUrl;
        Long contentId;

        // loop through the returned articles and call saveToOurCMS for each one.
        // saveToOurCMS needs to be overridden in the client's code to add/update/delete
        // each article from their own CMS. It should return the full public URL for the article.
        for (Map<String, String> oneArticle : listData) {
            publicUrl = saveToOurCMS(oneArticle);
            contentId = Long.valueOf(oneArticle.get("id"));
            String createUpdate = oneArticle.get("action");
            if ("create".equals(createUpdate) || "update".equals(createUpdate)) {
                publishNotify(contentId, publicUrl);
            }
        }

    }

    /**
     * Returns List of the current articles that are approved for publication.
     * 
     * This will set up the HTTP connection to Skyword's feed server and retrieve
     * an XML document of the currently approved articles. Each article in the XML 
     * document will be placed in to a Map<String, String> object. The collection of
     * these Maps will be returned in an ArrayList.
     * 
     * @return an ArrayList of LinkedHashMaps containing the name,value pairs that make
     * up each article.
     * 
     * @throws Exception
     */
    public List<Map<String, String>> pull() throws Exception {

        HttpMethodBase baseMethod = new GetMethod(feedUrl);
        baseMethod.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);

        log.debug("Retrieve Request for URL: " + feedUrl);
        List<Map<String, String>> listData = null;

        HttpClient client = setupClient(feedUrl);
        try {
            Integer responseCode = client.executeMethod(baseMethod);
            log.debug("Response code: " + responseCode.toString());
            String postData = getPostData(baseMethod);
            log.debug(baseMethod.getStatusLine().toString() + "\n\n" + postData);

            if (baseMethod.getStatusCode() == 200) { // javax.servlet.http.HttpServletResponse.SC_OK
                listData = fromNodeList(postData);
            } else {
                log.error(baseMethod.getStatusLine().toString() + "\n\n" + postData);
            }
        } catch (Exception e) {
            log.error("Processing of retrieve failed.", e);
        } finally {
            // Release the connection.
            baseMethod.releaseConnection();
        }

        return listData;

    }

    /**
     * Stub method that must be overridden by an extending class to save the article
     * to your local CMS.
     * 
     * This method is called automatically by the getSkywordContents() method above.
     * 
     * @param LinkedHashMap containing a single article in name, value format.
     * @return The public URL that the article is/will be viewable by end users.
     * @throws Exception
     */
    public String saveToOurCMS(Map<String, String> articleContents) throws Exception {
        // This method is just a stub that does nothing. This needs to be overridden to insert
        // the article data into your own CMS.

        return "";
    }

    /**
     * Method to record the public URL of an article in the skyword system.
     * 
     * @param contentId -- In the pull method this is the value for "id".
     * @param publishedUrl -- unencoded url for the article.
     * @return Html confirmation message.
     * @throws Exception
     */
    public String publishNotify(Long contentId, String publishedUrl) throws Exception {

        // This method will notify the Skyword system that the article has been received and provide
        // the article's public Url so that it can be recorded in Skyword's CMS.
        String publicUrl = publishUrl.concat("&contentId=" + contentId + "&url=" + URLEncoder.encode(publishedUrl, "UTF-8"));

        String postData = null;

        HttpMethodBase baseMethod = new GetMethod(publicUrl);
        baseMethod.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);

        log.debug("Publish Request for URL: " + publicUrl);

        HttpClient client = setupClient(publicUrl);
        try {
            Integer responseCode = client.executeMethod(baseMethod);
            log.debug("Response code from publish: " + responseCode.toString());
            postData = getPostData(baseMethod);
            log.debug(baseMethod.getStatusLine().toString() + "\n\n" + postData);
        } catch (Exception e) {
            log.error("Processing of publish failed.", e);
        } finally {
            baseMethod.releaseConnection();
        }
        return postData;
    }

    /**
     * 
     * @param fileId -- returned as attachmentid in the attachment container.
     * @return FileAttachment is a class that contains 2 fields. 
     *      - mimeType - contains the HTTP Header Content-Type: value
     *      - fileData - byte[] that is the raw data of the attached file.
     * @throws Exception
     */
    public FileAttachment getFileAttachment(Integer fileId) throws Exception {

        // FileAttachment is defined at the bottom of this file. It is a simple java class that contains
        // 2 fields. The byte array (byte[]) of the file and its MimeType. 

        String getFileUrl = fileUrl.concat("&file=" + fileId);

        HttpMethodBase baseMethod = new GetMethod(getFileUrl);
        baseMethod.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);

        log.debug("Retrieve Request for URL: " + getFileUrl);

        FileAttachment fa = new FileAttachment();
        HttpClient client = setupClient(feedUrl);
        try {
            Integer responseCode = client.executeMethod(baseMethod);
            log.debug("Response code: " + responseCode.toString());
            byte[] fileByteArray = baseMethod.getResponseBody();
            String mimeType = baseMethod.getResponseHeader("Content-Type").toExternalForm();
            fa.setMimeType(mimeType);
            fa.setFileData(fileByteArray);
        } catch (Exception e) {
            log.error("Processing of retrieve failed.", e);
        } finally {
            // Release the connection.
            baseMethod.releaseConnection();
        }

        return fa;

    }

    private String getPostData(HttpMethodBase method) throws Exception {
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

    private NodeList eval(final Document doc, final String pathStr)
            throws XPathExpressionException {
        final XPath xpath = XPathFactory.newInstance().newXPath();
        final XPathExpression expr = xpath.compile(pathStr);
        return (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
    }

    private List<Map<String, String>> fromNodeList(String postData) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(postData)));

        final NodeList nodes = eval(document, "//entry");

        final List<Map<String, String>> out = new ArrayList<Map<String, String>>();
        int len = (nodes != null) ? nodes.getLength() : 0;
        for (int i = 0; i < len; i++) {
            NodeList children = nodes.item(i).getChildNodes();
            Map<String, String> childMap = new LinkedHashMap<String, String>();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (child.getNodeType() == Node.ELEMENT_NODE)
                    childMap.put(child.getNodeName(), child.getTextContent());
            }
            out.add(childMap);
        }
        return out;
    }

    private HttpClient setupClient(String pullUrl) throws Exception {
        HttpClient client = new HttpClient();

        // Set HttpClient preferences
        // See http://jakarta.apache.org/commons/httpclient/preference-api.html
        client.getParams().setParameter("http.protocol.version", HttpVersion.HTTP_1_1);
        client.getParams().setParameter("http.socket.timeout", new Integer(60000));
        client.getParams().setParameter("http.connection.timeout", new Integer(60000));
        client.getParams().setParameter("http.protocol.content-charset", "UTF-8");

        return client;
    }

    private static void printData(List<Map<String, String>> listData) throws Exception {
        for (Map<String, String> oneArticle : listData) {
            log.info("\n---------------------------------\n");
            for (String oneKey : oneArticle.keySet()) {
                log.info("Name: " + oneKey);
                log.info("Value: " + oneArticle.get(oneKey));
            }
        }
    }

    public class FileAttachment {
        String mimeType;
        byte[] fileData;

        /**
         * @return the mimeType
         */
        public String getMimeType() {
            return mimeType;
        }

        /**
         * @param mimeType the mimeType to set
         */
        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        /**
         * @return the fileData
         */
        public byte[] getFileData() {
            return fileData;
        }

        /**
         * @param fileData the fileData to set
         */
        public void setFileData(byte[] fileData) {
            this.fileData = fileData;
        }

    }
}
