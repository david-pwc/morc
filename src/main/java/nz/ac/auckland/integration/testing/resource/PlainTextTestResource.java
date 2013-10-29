package nz.ac.auckland.integration.testing.resource;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URL;

/**
 * A simple mechanism for sending and comparing plain text values
 * using the Java equals mechanism
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class PlainTextTestResource extends StaticTestResource<String> {

    public PlainTextTestResource(String value) {
        super(value);
    }

    public PlainTextTestResource(File file) {
        super(file);
    }

    public PlainTextTestResource(URL url) {
        super(url);
    }

    /**
     * @return The plain text from an external resource as a standard Java String
     * @throws Exception
     */
    protected String getResource(File file) throws Exception {
        return FileUtils.readFileToString(file);
    }
}
