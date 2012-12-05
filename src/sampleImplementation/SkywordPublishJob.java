package sampleImplementation;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SkywordPublishJob {

    protected static Log log = LogFactory.getLog(SkywordPublishJob.class);

    protected Integer processActivityId;

    protected static Map<String, String> parameters;

    protected static String hostname = null;
    protected static String ipAddress = null;

    /**
     * Default constructor.
     */
    public SkywordPublishJob() {
    }

    public static void main(String[] args) throws Exception {

        // Parse the command line
        parameters = parseCommandLine(args);

        String key = "1rs5nqvbq3be2sacxeor";
        String overrideUrl = null;

        try {
            hostname = InetAddress.getLocalHost().getHostName();
            log.info("Host from localaddress = " + hostname);
            if (hostname == null || "localhost".equalsIgnoreCase(hostname)) {
                hostname = System.getenv("HOST");
            }

            ipAddress = InetAddress.getByName(hostname).getHostAddress();

            if (getParameter("key") != null) {
                key = getParameter("key");
            }

            if (getParameter("url") != null) {
                overrideUrl = getParameter("url");
            }

            SkywordContent sc = new SkywordContent();
            sc.setKey(key);
            if (overrideUrl != null) {
                sc.setBaseUrl(overrideUrl);
            }

            sc.getSkywordContent();
            log.info("All work committed successfuly");

        } catch (Exception e) {
            log.error("Fatal Exception Raised!", e);
        }

        return;

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
        this.parameters = propertiesMap;
    }
}
