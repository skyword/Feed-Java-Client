/*
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */

import com.skyword.api.feed.FileAttachment;
import com.skyword.api.feed.HelperMethods;
import com.skyword.api.feed.SkywordFeed;
import org.apache.jackrabbit.commons.JcrUtils;

import javax.jcr.*;
import java.io.*;
import java.util.*;

;

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

    public static final String NT_UNSTRUCTURED = "nt:unstructured";
    private Session session = null;

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

        // Main method to call.
        sc.processSkywordFeed();

        System.out.println("All work committed successfully");
        log.info("All work committed successfully");

    }

    public String saveToCMS(Map<String, Object> articleContents) throws Exception {

        System.out.println("Publishing content with Skyword Id: " + articleContents.get("id"));
        System.out.println("title: " + articleContents.get("title"));
        System.out.println("body: " + articleContents.get("body"));

        Long contentId = (Long) articleContents.get("id");

        //Connect to Repo amd get root Node
        Repository repo = JcrUtils.getRepository(authorUrl);
        Node root = getRootNode(repo, username, password);

        String publishUrl = "";
        try {
            //Create preliminary structure for article
            Node baseNode = getBaseNode(root);

            String titleSlug = HelperMethods.generateSlug((String) articleContents.get("title"));
            Node articleNode = getOrCreateNode(titleSlug, baseNode, "cq:Page");
            Node jcrContentNode = getOrCreateNode("jcr:content", articleNode, "cq:PageContent");

            jcrContentNode.setProperty("jcr:title", (String) articleContents.get("title"));
            jcrContentNode.setProperty("author", (String) articleContents.get("author"));
            jcrContentNode.setProperty("cq:template", templateName);
            jcrContentNode.setProperty("cq:lastModified", Calendar.getInstance());
            jcrContentNode.setProperty("cq:lastModifiedBy", session.getUserID());
            jcrContentNode.setProperty("cq:distribute", true);
            jcrContentNode.setProperty("sling:resourceType", slingPageResourceType);
            //Add contentID for tracking tag.
            jcrContentNode.setProperty("contentId", contentId);


            // For this example the content body is stored in a Paragraph System
            // http://wem.help.adobe.com/enterprise/en_US/10-0/wem/wcm/default_components.html#Paragraph%20System%20(parsys)
            //
            // This is the component that the demo platform provided by Adobe uses.
            Node parNode = getOrCreateNode("par", jcrContentNode, NT_UNSTRUCTURED);

            Node testEntryNode = getOrCreateNode("entry", parNode, NT_UNSTRUCTURED);

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

                Node textImageNode = getOrCreateNode("textimage", parNode, NT_UNSTRUCTURED);
                textImageNode.setProperty("sling:resourceType", slingFoundationType + "textimage");
                textImageNode.setProperty("text", (String) articleContents.get("featured_imagetitle"));

                Node imageNode = getOrCreateNode("image", textImageNode, NT_UNSTRUCTURED);
                imageNode.setProperty("sling:resourceType", slingFoundationType + "image");
                imageNode.setProperty("fileReference", imageUrl);
            }


            String contentPath = jcrContentNode.getPath();
            publishUrl = publishDomain + contentPath.substring(0, contentPath.lastIndexOf("/")) + ".html";
            session.save();
            session.logout();
        } catch (Exception e) {
            log.error("Error in post", e);
        }

        return publishUrl;

    }

    /**
     * 
     *
     * @param root
     * @return
     * @throws RepositoryException
     */
    private Node getBaseNode(Node root) throws RepositoryException {
        Node baseNode = getOrCreateNode(nodePathBase, root, "nt:folder");

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

            baseNode = getOrCreateNode(pathAppend, baseNode, "cq:Page");
        }
        return baseNode;
    }

    /**
     * Get root node
     *
     * @param repo
     * @param username
     * @param password
     * @return
     * @throws RepositoryException
     */
    private Node getRootNode(Repository repo, String username, String password) throws RepositoryException {
        //Create a Session instance
        session = repo.login(new SimpleCredentials(username, password.toCharArray()));

        Node root = session.getRootNode();
        log.info("rootNode: " + root.getIdentifier() + " : " + root.getName());

        return root;
    }


    /**
     * Create a new node
     *
     * @param nodeName
     * @param parentNode
     * @param jcrPrimaryType
     * @return
     * @throws RepositoryException Will be thrown if node already exists
     */
    private Node createNode(String nodeName, Node parentNode, String jcrPrimaryType) throws RepositoryException {
        return parentNode.addNode(nodeName, jcrPrimaryType);
    }

    /**
     * Get node by name or create one if it doesn't exist
     *
     * @param nodeName
     * @param parentNode
     * @param jcrPrimaryType
     * @return
     * @throws RepositoryException
     */
    private Node getOrCreateNode(String nodeName, Node parentNode, String jcrPrimaryType) throws RepositoryException {
        if (parentNode.hasNode(nodeName)) {
            return parentNode.getNode(nodeName);
        } else {
            return createNode(nodeName, parentNode, jcrPrimaryType);
        }
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

        Node folderNode = getOrCreateNode(imagesFolderNode, root, "sling:Folder");

        InputStream is = new ByteArrayInputStream(fa.getFileData());
        Binary binary = session.getValueFactory().createBinary(is);

        Node fileNode = getOrCreateNode(fileName, folderNode, "nt:file");

        //create the mandatory child node - jcr:content
        Node resNode = getOrCreateNode("jcr:content", fileNode, "nt:resource");
        resNode.setProperty ("jcr:mimeType", fa.getMimeType());
        resNode.setProperty("jcr:data", binary);
        resNode.setProperty("jcr:lastModified", (new Date()).getTime());

        String path = resNode.getPath();
        return path.substring(0, path.lastIndexOf("/"));

    }
}
