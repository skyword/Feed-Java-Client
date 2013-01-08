/*
 * Basic Publication Example This class is a basic example of how to integrate with the Skyword XML Feeds. Simply extend
 * the skywordFeed class and override the following methods: savetoCMS() removeFropmCMS() The SkywordFeed class
 * implements all of the work of ddownloading the XML feed and parseing it. This file is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.skyword.api.feed.SkywordFeed;

public class SkywordBasicFeedJob extends SkywordFeed {

    protected static Log log = LogFactory.getLog(SkywordBasicFeedJob.class);

    /**
     * Default constructor.
     */
    public SkywordBasicFeedJob() {
    }

    public static void main(String[] args) throws Exception {

        // Replace this with YOUR API Key!!
        String key = "20jc3jbfuehkguwsf5gp";

        SkywordBasicFeedJob sc = new SkywordBasicFeedJob();
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
        log.info("titleTag: " + articleContents.get("titleTag"));
        log.info("metaDescription: " + articleContents.get("metaDescription"));
        log.info("metaKeyword: " + articleContents.get("metaKeywordTag"));
        log.info("author: " + articleContents.get("author"));

        // Parse the <tags> node for comma delimited keywords
        if (articleContents.get("tags") != null) {
            StringTokenizer st = new StringTokenizer((String) articleContents.get("tags"), ",");
            while (st.hasMoreElements()) {
                String token = (String) st.nextElement();
                if (token != null && token.length() > 255) {
                    token = token.substring(0, 255);
                }
                token = token.trim();
                log.info("tag: " + token);
            }
        }

        // Just return NULL if the published URL is not known.
        return null;

    }

}
