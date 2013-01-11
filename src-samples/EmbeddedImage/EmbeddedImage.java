/*
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */
import java.io.FileOutputStream;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.skyword.api.feed.FileAttachment;
import com.skyword.api.feed.MimeTypes;
import com.skyword.api.feed.SkywordFeed;

/**
 * Embedded Images Example
 * 
 * An example class that fetches content via the XML feed and then processes 
 * any embedded images that are stored with the HTML body of the content.
 *
 */
public class EmbeddedImage extends SkywordFeed {

    protected static Log log = LogFactory.getLog(EmbeddedImage.class);

    /**
     * Default constructor.
     */
    public EmbeddedImage() {
    }

    public static void main(String[] args) throws Exception {

        // Replace this with YOUR API Key!!
        String key = "API_TEST_KEY";

        EmbeddedImage sc = new EmbeddedImage();
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

        log.info("Publishing content with Skyword Id: " + articleContents.get("id"));
        log.info("title: " + articleContents.get("title"));
        log.info("body: " + articleContents.get("body"));
        
        String body = (String)articleContents.get("body");
        
        /* Find all occurences strings similar to SKYWORD-FILE:123456
         * These strings are placeholders embedded within the HTML body content
         * within <img> tags as the src attribute. For example: 
         * 
         * <img src="SKYWORD-FILE:12345">
         * 
         * The number to the right is the ID of the file within the Skyword database.
         * You must download this file and replace the src attribute with the final URL
         * where you are hosting the image at.
         */
        
        Pattern regex = Pattern.compile("SKYWORD-FILE:([0-9]+)");
        Matcher m = regex.matcher(body);
        while (m.find()) {
            log.info("Processing skyword image tag found in body: " + m.group());
            
            // parse the number out of the skyword file marker (e.g. SKYWORD-FILE:24205 becomes 24205)
            Integer fileId = Integer.parseInt(m.group().substring(13));
            
            // download the file data from Skyword, save it, and generate a url for the new image.
            if ( fileId != null) {
            	// Download the file data from Skyword
            	FileAttachment fa = this.getFileAttachment(fileId);

            	// Output the mime type
            	log.info("mime-type: " + fa.getMimeType());

                // Determine the extension to use to store the file
            	String extension = MimeTypes.getExtension(fa.getMimeType());

            	// Save the file to the file system
                String path = "";  // The full path to the file
                String filename = path + fileId.toString() + "." + extension;
                log.info("Saving the file as: " + filename);
            	FileOutputStream fos = new FileOutputStream(filename);
            	fos.write(fa.getFileData());
            	fos.close();

            	// construct a url that takes you to the previously stored image
            	String newImageUrl = "http://url/path/to/image/" + filename;
            
            	// substitute the new url back into the img src attribute in place of the skyword file marker.
            	body = body.replaceAll(m.group(), newImageUrl);
            }
        }
        
        log.info("processed body is now: " + body);
        
        // Just return NULL if the published URL is not known (or you want Skyword to detect it automatically).
        return null;

    }

}
