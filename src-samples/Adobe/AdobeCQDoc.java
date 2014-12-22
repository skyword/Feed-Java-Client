/*
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.SimpleCredentials;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.JcrUtils;

import com.skyword.api.feed.FileAttachment;
import com.skyword.api.feed.SkywordFeed;
import com.skyword.api.feed.HelperMethods;

/**
 * Embedded Images Example
 * 
 * An example class that fetches content via the XML feed and then processes 
 * any embedded images that are stored with the HTML body of the content.
 *
 */
public class AdobeCQDoc extends SkywordFeed {

    protected static Log log = LogFactory.getLog(AdobeCQDoc.class);
    private javax.jcr.Session session = null;

    /**
     * Default constructor.
     */
    public AdobeCQDoc() {
    }

    public static void main(String[] args) throws Exception {

        // Replace this with YOUR API Key!!
        // API_TEST_KEY is a default key you may use for initial testing
        // String key = "API_TEST_KEY";
        String key = "1dwxnzsolf5384v1au9k";

        AdobeCQDoc sc = new AdobeCQDoc();
        sc.setKey(key);

        // Main method to call
        sc.processSkywordFeed();
        log.info("All work committed successfully");

    }

    /**
     * Overridden method that would actually store the content into your CMS. The return value should the the fully
     * qualified URL where the article was published to or NULL if not known or you want Skyword to auto-detect
     * publication.
     */
    public String saveToCMS(Map<String, Object> articleContents) throws Exception {

        System.out.println("Publishing content with Skyword Id: " + articleContents.get("id"));
        System.out.println("title: " + articleContents.get("title"));
        System.out.println("body: " + articleContents.get("body"));

        String nodePathBase = "content/geometrixx-outdoors/en/company/unlimited-blog";
        ArrayList<String> nodePathAppend = new ArrayList<String>(Arrays.asList("{year}", "{month}"));

        String templateName = "/libs/social/blog/templates/page";
        String slingPageResourceType = "geometrixx-outdoors/components/social/journal/page-company";
        String slingFoundationType = "foundation/components/";
        String slingEntryType = "social/blog/components/entrytext";
        String publishDomain = "http://54.85.10.189:4503";
        
        Repository repo = JcrUtils.getRepository("http://54.85.10.189:4502/crx/server");

        String username = "admin";
        String password = "admin";

        try {

            //Create a Session instance
            session = repo.login(new SimpleCredentials(username, password.toCharArray()));

            //Create a Node
            Node root = session.getRootNode();
            System.out.println("rootNode: " + root.getIdentifier() + " : " + root.getName());

            Calendar pCal = new GregorianCalendar();
            int yr = pCal.get(Calendar.YEAR);
            int mnth = pCal.get(Calendar.MONTH) + 1;
            
            Node leafNode;
            try {
                leafNode = root.getNode(nodePathBase);
            } catch (Exception e) {
                leafNode = root.addNode(nodePathBase, "cq:Page");
            }

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

                String publishUrl = saveAttachment(fa, root, (String) articleContents.get("filename"));

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
                    imageNode.setProperty("fileReference", publishUrl);
            }

            System.out.println("contentnode path: " + testContentNode.getPath());
            String contentPath = testContentNode.getPath();
            String publishUrl = publishDomain + contentPath.substring(0, contentPath.lastIndexOf("/")) + ".html";
        } catch (Exception e) {
            System.out.println("Error in post");
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.save();
                session.logout();
            }
        }

        // Just return NULL if the published URL is not known (or you want Skyword to detect it automatically).
        return null;

    }

    public String saveAttachment(FileAttachment fa, Node root, String fileName) throws Exception {

        String imagesFolderNode = "content/geometrixx-outdoors/en/company/photos";

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

    public void removeFromCMS(Map<String, Object> articleContents) throws Exception {

    }
}
