package opendap.webstart;

import org.jdom.Document;
import org.jdom.Element;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public abstract class JwsHandler {

    public abstract void init(Element config, String resourcesDirectory);
    
    public abstract String getApplicationName();
    public abstract String getServiceId();

    public abstract boolean datasetCanBeViewed(Document ddx);

    public abstract String getJnlpForDataset(String datasetUrl);

    public static String readFileAsString(String pathname) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        Scanner scanner = new Scanner(new File(pathname));

        try {
            while (scanner.hasNextLine()) {
                stringBuilder.append(scanner.nextLine() + "\n");
            }
        } finally {
            scanner.close();
        }
        return stringBuilder.toString();
    }


}