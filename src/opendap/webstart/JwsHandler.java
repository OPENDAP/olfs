package opendap.webstart;

import org.jdom.Element;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public abstract class JwsHandler {

    public abstract void init(Element config, String resourcesDirectory);

    public abstract boolean datasetCanBeViewed(String localId, String query);

    public abstract String getJnlpForDataset(String query);

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