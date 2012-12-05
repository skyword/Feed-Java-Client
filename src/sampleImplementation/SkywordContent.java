package sampleImplementation;

import java.util.Map;
import java.util.StringTokenizer;

import skywordFeedClient.SkywordPull;

public class SkywordContent extends SkywordPull {

    public String saveToOurCMS(Map<String, String> articleContents) throws Exception {

        String articleUrl = null;

        log.info("Publishing content with id: " + articleContents.get("id"));

        log.info("title: " + articleContents.get("title"));
        log.info("body: " + articleContents.get("body"));

        log.info("titleTag: " + articleContents.get("titleTag"));
        log.info("metaDescription: " + articleContents.get("metaDescription"));
        log.info("metaKeyword: " + articleContents.get("metaKeywordTag"));

        if (articleContents.get("tags") != null) {
            StringTokenizer st = new StringTokenizer(articleContents.get("tags"), ",");
            while (st.hasMoreElements()) {
                String token = (String) st.nextElement();
                if (token != null && token.length() > 255) {
                    token = token.substring(0, 255);
                }
                token = token.trim();
                log.info("tag: " + token);
            }
        }

        return articleUrl;
    }
}
