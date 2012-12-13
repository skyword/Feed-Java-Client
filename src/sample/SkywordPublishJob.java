package sample;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.skyword.api.feed.HelperMethods;
import com.skyword.api.feed.SkywordFeed;

public class SkywordPublishJob extends SkywordFeed {

    protected static Log log = LogFactory.getLog(SkywordPublishJob.class);

    protected Integer processActivityId;

    protected static Map<String, String> parameters;

    /**
     * Default constructor.
     */
    public SkywordPublishJob() {
    }

    public static void main(String[] args) throws Exception {

        // Parse the command line
        parameters = parseCommandLine(args);

        String key = "";
        String overrideUrl = null;

        try {

            if (getParameter("key") != null) {
                key = getParameter("key");
            }

            if (getParameter("url") != null) {
                overrideUrl = getParameter("url");
            }

            SkywordPublishJob sc = new SkywordPublishJob();
            sc.setKey(key);
            if (overrideUrl != null) {
                sc.setBaseUrl(overrideUrl);
            }

            log.info("FeedUrl: " + sc.getFeedUrl());

            sc.processSkywordFeed();
            log.info("All work committed successfuly");

        } catch (Exception e) {
            log.error("Fatal Exception Raised!", e);
        }

        return;

    }

    public String saveToCMS(Map<String, Object> articleContents) throws Exception {

        String articleUrl = null;

        log.info("Publishing content with id: " + articleContents.get("id"));

        log.info("title: " + articleContents.get("title"));
        log.info("body: " + articleContents.get("body"));

        log.info("titleTag: " + articleContents.get("titleTag"));
        log.info("metaDescription: " + articleContents.get("metaDescription"));
        log.info("metaKeyword: " + articleContents.get("metaKeywordTag"));

        log.info("author: " + articleContents.get("author"));

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

        articleUrl = "http://www.skywordClient.com/" + HelperMethods.generateSlug((String) articleContents.get("title"));
        log.info("Publish Url: " + articleUrl);

        return articleUrl;
    }

    public void removeFromCMS(Map<String, Object> articleContents) throws Exception {
        log.info("Removing content with id: " + articleContents.get("contentRequestId"));
        log.info("MemberId: " + articleContents.get("memberId"));
    }

    /**
     * Parse command line arguments. Batch command line arguments must be of the form: -name value -name value -name
     * value
     * 
     * @param args
     * @return
     * @throws Exception
     */
    public static HashMap<String, String> parseCommandLine(String[] args) throws Exception {

        HashMap<String, String> parameters = new HashMap<String, String>();

        // There must be an even number of command line arguments!
        int i = 0;
        while (i < args.length) {
            String name = args[i].replaceAll("-", "");
            String value = args[i + 1];
            parameters.put(name, value);
            i += 2;
        }

        return parameters;

    }

    protected Map<String, String> getParamtersMap() {
        return parameters;
    }

    protected static String getParameter(String name) {
        return parameters.get(name);
    }

    protected void setParametersMap(Map<String, String> propertiesMap) {
        parameters = propertiesMap;
    }
}
