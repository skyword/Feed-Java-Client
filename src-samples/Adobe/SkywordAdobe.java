/*
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */

import com.skyword.api.feed.FileAttachment;
import com.skyword.api.feed.HelperMethods;
import com.skyword.api.feed.SkywordFeed;
import org.apache.jackrabbit.commons.JcrUtils;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.SimpleCredentials;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Adobe CQ/AEM Example
 * 
 * An example class that fetches content via the Skyword XML feed and then inserts
 * the content into the Adobe CQ/AEM CMS.  
 * 
 * This class extends the base class SkywordFeed, which impements all of the major steps
 * of downloading and parsing the XML feed.  
 *
 */
public class SkywordAdobe extends SkywordFeed {

    private javax.jcr.Session session = null;

    /**
     * Default constructor.
     */
    public SkywordAdobe() {
    }

    // Replace this with YOUR API Key!!
    // API_TEST_KEY is a default key you may use for initial testing
    // This test key will output a static sample XML feed from the URL: https://api.skyword.com/feed?key=API_TEST_KEY
    // String key = "API_TEST_KEY";
    private static String key;
    private static String authorUrl;

    private static String username;
    private static String password;

    // Set nodePathBase to be the base directory path in adobe cq/aem to save the Skyword content.
    private static String nodePathBase;

    // If the node path includes any prefix variable elements such as /year/month put them in this Array.
    // This permits a more dynamic node path, for example /article/2015/01/this-is-the-title
    private static final ArrayList<String> nodePathAppend = new ArrayList<String>(Arrays.asList("{year}", "{month}"));

    // This is the name of the template that will be used to display the content on the publish instance
    private static String templateName;
    private static String slingPageResourceType;
    private static String slingFoundationType;
    private static String slingEntryType;
    private static String publishDomain;

    private static String imagesFolderNode;

    /**
     * Loads the static variables above from the configuration file config.properties.
     */
    public static boolean loadPropertyValues() throws IOException {
        try {
            Properties prop = new Properties();
            String propFileName = "config.properties";

            InputStream inputStream = new FileInputStream(propFileName);

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }

            key = prop.getProperty("key");
            authorUrl = prop.getProperty("authorUrl");
            username = prop.getProperty("username");
            password = prop.getProperty("password");
            nodePathBase = prop.getProperty("nodePathBase");
            templateName = prop.getProperty("templateName");
            slingPageResourceType = prop.getProperty("slingPageResourceType");
            slingFoundationType = prop.getProperty("slingFoundationType");
            slingEntryType = prop.getProperty("slingEntryType");
            publishDomain = prop.getProperty("publishDomain");
            imagesFolderNode = prop.getProperty("imagesFolderNode");

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * The main starting point for the XMLF feed client.
     * SkywordAdobe extends the class SkywordFeed which implements all of the 
     * details of fetching the XML feed, parsing, and inserting int o the CMS.  
     */
    public static void main(String[] args) throws Exception {

        if (!loadPropertyValues()) {
            System.out.println("Error reading in the properties file.");
        }

        System.out.println("Initializing SkywordAdobe object.");
        SkywordAdobe sc = new SkywordAdobe();
        sc.setKey(key);

        // Main method to call. This version is to call a development or qa version of the feed server.
        sc.processSkywordFeed();

        System.out.println("All work committed successfully");
        log.info("All work committed successfully");

    }

    /**
     * Overridden method that would actually store the content into your CMS. The return value should the the fully
     * qualified URL where the article was published to or NULL if not known or you want Skyword to auto-detect
     * publication.
     * 
     * @param articleContents The parsed contents of the XML feed
     * @return The public URL that the content is published to 
     */
    
    public String saveToCMS(Map<String, Object> articleContents) throws Exception {

        System.out.println("Publishing content with Skyword Id: " + articleContents.get("id"));
        System.out.println("title: " + articleContents.get("title"));
        System.out.println("body: " + articleContents.get("body"));
        
        Repository repo = JcrUtils.getRepository(authorUrl);
        String publishUrl = "";

        try {

            //Create a Session instance
            session = repo.login(new SimpleCredentials(username, password.toCharArray()));

            //Create a Node
            Node root = session.getRootNode();
            System.out.println("rootNode: " + root.getIdentifier() + " : " + root.getName());
            
            Node leafNode;
            try {
                leafNode = root.getNode(nodePathBase);
            } catch (Exception e) {
                leafNode = root.addNode(nodePathBase, "cq:Page");
            }

            Calendar pCal = new GregorianCalendar();
            int yr = pCal.get(Calendar.YEAR);
            int mnth = pCal.get(Calendar.MONTH) + 1;

            //
            // any variable elements added to the node path array above need to be handled here
            //
            for (String pathAppend : nodePathAppend) {

                if (pathAppend.equals("{year}")) {
                    pathAppend = String.valueOf(yr);
                } else if (pathAppend.equals("{month}")) {
                    pathAppend = String.valueOf(mnth);
                }

                try {
                    leafNode = leafNode.getNode(pathAppend);
                } catch (Exception e) {
                    leafNode = leafNode.addNode(pathAppend, "cq:Page");
                }
            }

            String newNodeName = HelperMethods.generateSlug((String) articleContents.get("title"));
            Node titleNode;

            try {
                titleNode = leafNode.getNode(newNodeName);
            } catch (Exception e) {
                titleNode = leafNode.addNode(newNodeName, "cq:Page");
            }

            Node testContentNode;

            try {
                testContentNode = titleNode.getNode("jcr:content");
            } catch (Exception e) {
                testContentNode = titleNode.addNode("jcr:content", "cq:PageContent");
            }

            testContentNode.setProperty("jcr:title", (String) articleContents.get("title"));
            testContentNode.setProperty("author", (String) articleContents.get("author"));
            testContentNode.setProperty("cq:template", templateName);
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            Date date = df.parse((String) articleContents.get("publishedDate"));
            Calendar thisTime = new GregorianCalendar();
            thisTime.setTime(date);
            testContentNode.setProperty("published", thisTime);
            testContentNode.setProperty("sling:resourceType", slingPageResourceType);

            // For this example the content body is stored in a Paragraph System
            // http://wem.help.adobe.com/enterprise/en_US/10-0/wem/wcm/default_components.html#Paragraph%20System%20(parsys)
            //
            // This is the component that the demo platform provided by Adobe uses.
            Node testParNode;
            try {
                testParNode = testContentNode.getNode("par");
            } catch (Exception e) {
                testParNode = testContentNode.addNode("par", "nt:unstructured");
            }
            testParNode.setProperty("sling:resourceType", slingFoundationType + "parsys");

            Node testEntryNode;
            try {
                testEntryNode = testParNode.getNode("entry");
            } catch (Exception e) {
                testEntryNode = testParNode.addNode("entry", "nt:unstructured");
            }
            testEntryNode.setProperty("sling:resourceType", slingEntryType);
            testEntryNode.setProperty("text", (String) articleContents.get("body"));

            // If the content contains an image it will be downloaded and processed by the saveAttachment method below.
            // The <file> node contains the identifier of the image file
            Object fileNum = articleContents.get("file");
            Integer fileId = null;
            if (fileNum != null) {
                fileId = new Integer((String) fileNum);
                System.out.println("file: " + fileId);
            }

            if ( fileId != null) {

                // Download the file data from Skyword via this helper method
                FileAttachment fa = this.getFileAttachment(fileId);

                // Output the mime type of the file
                System.out.println("mime-type: " + fa.getMimeType());

                String imageUrl = saveAttachment(fa, root, (String) articleContents.get("filename"));

                Node textImageNode;
                try {
                    textImageNode = testParNode.getNode("textimage");
                } catch (Exception e) {
                    textImageNode = testParNode.addNode("textimage", "nt:unstructured");
                }
                textImageNode.setProperty("sling:resourceType", slingFoundationType + "textimage");
                textImageNode.setProperty("text", (String) articleContents.get("featured_imagetitle"));

                Node imageNode;
                try {
                    imageNode = textImageNode.getNode("image");
                } catch (Exception e) {
                    imageNode = textImageNode.addNode("image", "nt:unstructured");
                }
                imageNode.setProperty("sling:resourceType", slingFoundationType + "image");
                imageNode.setProperty("fileReference", imageUrl);
            }

            System.out.println("contentnode path: " + testContentNode.getPath());
            String contentPath = testContentNode.getPath();
            publishUrl = publishDomain + contentPath.substring(0, contentPath.lastIndexOf("/")) + ".html";

        } catch (Exception e) {
            System.out.println("Error in post");
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.save();
                session.logout();
            }
        }

        return publishUrl;

    }

    
    /**
     * Saves a file to the AEM/CQ repository.
     * 
     *  @param fa The file to save
     *  @param root The root node to save the file contents to
     *  @param fileName The name of the file to save
     *  
     *  @return the parent root path of the content removed
     */
    public String saveAttachment(FileAttachment fa, Node root, String fileName) throws Exception {

        Node folderNode;
        try {
            folderNode = root.getNode(imagesFolderNode);
        } catch (Exception e) {
            folderNode = root.addNode(imagesFolderNode, "sling:Folder");
        }

        InputStream is = new ByteArrayInputStream(fa.getFileData());
        Binary binary = session.getValueFactory().createBinary(is);

        Node fileNode;
        try {
            fileNode = folderNode.getNode(fileName);
        } catch (Exception e) {
            fileNode = folderNode.addNode(fileName, "nt:file");
        }

        //create the mandatory child node - jcr:content
        Node resNode;
        try {
            resNode = fileNode.getNode("jcr:content");
        } catch (Exception e) {
            resNode = fileNode.addNode("jcr:content", "nt:resource");
        }

        resNode.setProperty ("jcr:mimeType", fa.getMimeType());
        resNode.setProperty("jcr:data", binary);
        resNode.setProperty("jcr:lastModified", (new Date()).getTime());

        String path = resNode.getPath();
        return path.substring(0, path.lastIndexOf("/"));

    }

    /**
     * Removes a content item from the CMS.
     * 
     * @param articleContents the parsed article contents from the XML feed
     * 
     */
    public void removeFromCMS(Map<String, Object> articleContents) throws Exception {
        System.out.println("Removing content with Title: " + articleContents.get("title"));

        Repository repo = JcrUtils.getRepository(authorUrl);

        try {

            //Create a Session instance
            session = repo.login(new SimpleCredentials(username, password.toCharArray()));

            //Create a Node
            Node root = session.getRootNode();
            System.out.println("rootNode: " + root.getIdentifier() + " : " + root.getName());

            Node leafNode;
            try {
                leafNode = root.getNode(nodePathBase);
            } catch (Exception e) {
                leafNode = root.addNode(nodePathBase, "cq:Page");
            }

            String newNodeName = HelperMethods.generateSlug((String) articleContents.get("title"));
            Node titleNode = null;
            try {
                titleNode = leafNode.getNode(newNodeName);
            } catch (Exception e) {
                System.out.println("No title node exists for : " + newNodeName);
            }

            if (titleNode != null) {
                titleNode.remove();
            }
        } catch (Exception e) {
            System.out.println("Exception in removeFromCMS.");
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.save();
                session.logout();
            }
        }
    }
}
