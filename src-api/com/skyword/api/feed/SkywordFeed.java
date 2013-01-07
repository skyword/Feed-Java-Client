package com.skyword.api.feed;

import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SkywordFeed {

    protected static Log log = LogFactory.getLog(SkywordFeed.class);

    private String baseUrl = "http://api.skyword.com";
    private String key;
    private String feedUrl;
    private String publishUrl;
    private String fileUrl;

    /**
     * Sets the key to be used in the URL calls to get the articles.
     * It is required to call this method after instantiating the class.
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

    /**
     * Sets the URLs to use after the key has been set.
     */
    private void setUrls() {
        feedUrl = baseUrl.concat("/feed?key=" + key);
        publishUrl = baseUrl.concat("/publish?key=" + key);
        fileUrl = baseUrl.concat("/file?key=" + key);
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
    public void processSkywordFeed() throws Exception {

        // getArticles retrieves the list of articles that are currently approved.
        ArticleContents listData = this.getArticles();
        if (listData == null) {
            return;
        }

        String publicUrl;
        Long contentId;

        // loop through the returned articles and call saveToCMS for each one.
        // saveToCMS needs to be overridden in the client's code to add/update/delete
        // each article from their own CMS. It should return the full public URL for the article.
        for (Map<String, Object> oneArticle : listData) {
            String action = (String) oneArticle.get("action");
            log.info("id: " + oneArticle.get("id"));
            contentId = Long.valueOf((String) oneArticle.get("id"));
            try {
                if ("create".equals(action) || "update".equals(action)) {
                    publicUrl = saveToCMS(oneArticle);
                    if ( publicUrl != null ) {
                        publishNotify(contentId, publicUrl);
                    }
                } else if ("delete".equals(action)) {
                    removeFromCMS(oneArticle);
                    publishNotify(contentId, null);
                }
            } catch (Exception e) {
                log.error("Exception in processing of article id: " + contentId, e);
            }
        }

    }

    /**
     * Returns List of the current articles that are approved for publication.
     * 
     * Gets the XML in a string and then each article in the XML 
     * will be placed in to a Map<String, String> object. The collection of
     * these Maps will be returned in an ArrayList.
     * 
     * @return an ArrayList of LinkedHashMaps containing the name,value pairs that make
     * up each article.
     * 
     * @throws Exception
     */
    public ArticleContents getArticles() throws Exception {

        ArticleContents listData = null;
        String xmlString = getArticlesAsXMLString();

        if (xmlString != null) {
            listData = convertXMLToArticleContents(xmlString, "entry");
        }

        return listData;

    }

    /**
     * Returns the articles approved for publication as an XML string.
     * 
     * This will set up the HTTP connection to Skyword's feed server and retrieve
     * an XML document of the currently approved articles.
     * 
     * @return String containing the XML received
     * @throws Exception
     */
    public String getArticlesAsXMLString() throws Exception {
        String postData = null;
        HttpMethodBase baseMethod = new GetMethod(feedUrl);
        baseMethod.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);

        log.debug("Retrieve Request for URL: " + feedUrl);

        HttpClient client = HelperMethods.setupClient();
        try {
            Integer responseCode = client.executeMethod(baseMethod);
            log.debug("Response code: " + responseCode.toString());
            postData = HelperMethods.getPostData(baseMethod);
            log.debug(baseMethod.getStatusLine().toString() + "\n\n" + postData);
            if (baseMethod.getStatusCode() != 200) { // javax.servlet.http.HttpServletResponse.SC_OK
                log.error(baseMethod.getStatusLine().toString() + "\n\n" + postData);
            }
        } catch (Exception e) {
            log.error("Processing of get method failed.", e);
        } finally {
            baseMethod.releaseConnection();
        }

        return postData;
    }

    /**
     * Retrieve the contents of a given node name in the XML text.
     * 
     * @param xmlString
     * @param containerName
     * @return
     * @throws Exception
     */
    public ArticleContents convertXMLToArticleContents(String xmlString, String containerName) throws Exception {
        Document document = HelperMethods.convertXMLStringToDocument(xmlString);
        final NodeList nodes = HelperMethods.performXPathEvaluation(document, "//" + containerName);

        final ArticleContents out = new ArticleContents();
        int len = (nodes != null) ? nodes.getLength() : 0;
        for (int i = 0; i < len; i++) {
            NodeList children = nodes.item(i).getChildNodes();
            Map<String, Object> childMap = getContainerChildren(children);
            out.add(childMap);
        }
        return out;
    }

    /**
     * Convert the children nodes into a Map. This method is recursive in the case where a container is found.
     * 
     * @param children
     * @return a Map of the elements from the inputed NodeList
     * @throws Exception
     */
    public Map<String, Object> getContainerChildren(NodeList children) throws Exception {
        Map<String, Object> containerChildMap = new LinkedHashMap<String, Object>();
        for (int k = 0; k < children.getLength(); k++) {
            Node child = children.item(k);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                NodeList grandChildren = child.getChildNodes();
                if (grandChildren.getLength() > 1) {
                    log.info("Container name: " + child.getNodeName());
                    containerChildMap.put(child.getNodeName(), getContainerChildren(grandChildren));
                } else if (!child.getNodeName().startsWith("#")) {
                    containerChildMap.put(child.getNodeName(), child.getTextContent());
                    log.info("NodeName: " + child.getNodeName());
                    log.info("Value: " + child.getTextContent());
                }
            }
        }
        return containerChildMap;
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
    public String saveToCMS(Map<String, Object> articleContents) throws Exception {
        // This method is just a stub that does nothing. This needs to be overridden to insert
        // the article data into your own CMS.
        throw new Exception("This method must be overridden to save the article contents into the CMS.");
    }

    /**
     * Stub method that must be overridden by an extending class to remove the article
     * from your local CMS.
     * 
     * This method is called automatically by the getSkywordContents() method above.
     * 
     * @param articleContents
     * @return
     * @throws Exception
     */
    public void removeFromCMS(Map<String, Object> articleContents) throws Exception {
        // This method is just a stub that does nothing. This needs to be overridden to insert
        // the article data into your own CMS.
        throw new Exception("This method must be overridden to remove the article contents from the CMS.");
    }

    /**
     * Method to record the public URL of an article in the Skyword system.
     * 
     * @param contentId -- In the pull method this is the value for "id".
     * @param publishedUrl -- unencoded URL for the article.
     * @return HTML confirmation message.
     * @throws Exception
     */
    public String publishNotify(Long contentId, String publishedUrl) throws Exception {

        // This method will notify the Skyword system that the article has been received and provide
        // the article's public URL so that it can be recorded in Skyword's CMS.
        String publicUrl = publishUrl.concat("&contentId=" + contentId);

        if (publishedUrl != null) {
            publicUrl = publicUrl.concat("&url=" + URLEncoder.encode(publishedUrl, "UTF-8"));
        }

        String postData = null;

        HttpMethodBase baseMethod = new GetMethod(publicUrl);
        baseMethod.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);

        log.debug("Publish Request for URL: " + publicUrl);

        HttpClient client = HelperMethods.setupClient();
        try {
            Integer responseCode = client.executeMethod(baseMethod);
            log.debug("Response code from publish: " + responseCode.toString());
            postData = HelperMethods.getPostData(baseMethod);
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
        HttpClient client = HelperMethods.setupClient();
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

    /**
     * Print out the article contents
     * 
     * @param listData
     * @throws Exception
     */
    public static void printData(ArticleContents listData) throws Exception {
        for (Map<String, Object> oneArticle : listData) {
            log.info("\n---------------------------------\n");
            printOneArticle(oneArticle);
        }
    }

    /**
     * Print the contents of a single article.
     * 
     * @param oneArticle
     * @throws Exception
     */
    public static void printOneArticle(Map<String, Object> oneArticle) throws Exception {
        for (String oneKey : oneArticle.keySet()) {
            Object value = oneArticle.get(oneKey);
            if (value instanceof String) {
                log.info("Name: " + oneKey);
                log.info("Value: " + oneArticle.get(oneKey));
            } else {
                log.info("Container Name: " + oneKey);
                printOneArticle((Map<String, Object>) value);
            }
        }
    }

    /**
     * @return the feedUrl
     */
    public String getFeedUrl() {
        return feedUrl;
    }

    /**
     * @return the publishUrl
     */
    public String getPublishUrl() {
        return publishUrl;
    }

    /**
     * @return the fileUrl
     */
    public String getFileUrl() {
        return fileUrl;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

}
