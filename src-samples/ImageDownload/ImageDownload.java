/**
 * Content With Images Example 
 * 
 * This class is a basic example of how to integrate with the Skyword XML Feeds. Simply extend
 * the skywordFeed class and override the following methods: savetoCMS() removeFropmCMS() The SkywordFeed class
 * implements all of the work of downloading the XML feed and parsing it. 
 * 
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

import java.io.FileOutputStream;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.skyword.api.feed.FileAttachment;
import com.skyword.api.feed.MimeTypes;
import com.skyword.api.feed.SkywordFeed;

public class ImageDownload extends SkywordFeed {

    protected static Log log = LogFactory.getLog(ImageDownload.class);

    /**
     * Default constructor.
     */
    public ImageDownload() {
    }

    public static void main(String[] args) throws Exception {

        // Replace this with YOUR API Key!!
        String key = "20jc3jbfuehkguwsf5gp";

        ImageDownload sc = new ImageDownload();
        sc.setKey(key);

        // Main method to call
        sc.processSkywordFeed();
        log.info("All work committed successfuly");

    }

    /**
     * Overridden method that would actually store the content into your CMS. The return value should the the fully
     * qualified URL where the article was published to or NULL if not known or you want Skyword to auto-detect
     * publication.
     */
    public String saveToCMS(Map<String, Object> articleContents) throws Exception {

        log.info("Publishing content with Skyword Id: " + articleContents.get("id"));
        log.info("title: " + articleContents.get("title"));
        log.info("body: " + articleContents.get("body"));

        // The <file> node contains the identifier of the image file
        Integer fileId = new Integer((String) articleContents.get("file"));
        log.info("file: " + fileId );

        if ( fileId != null) {
            // Download the file data from Skyword
            FileAttachment fa = this.getFileAttachment(fileId);

            // Output the mime type
            log.info("mime-type: " + fa.getMimeType());

            String extension = MimeTypes.getExtension(fa.getMimeType());
            
            // Save the file to the file system
            String filename = fileId.toString() + "." + extension;
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(fa.getFileData());
            fos.close();

            // At this point, you would make sure to associate the content from 
            // Skyword to the image you just saved.
        }
        

        // Just return NULL if the published URL is not known (or you want Skyword to detect it automatically).
        return null;

    }

}
