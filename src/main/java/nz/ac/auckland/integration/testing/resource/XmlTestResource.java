package nz.ac.auckland.integration.testing.resource;

import nz.ac.auckland.integration.testing.utility.XmlUtilities;
import nz.ac.auckland.integration.testing.utility.XPathSelector;
import org.w3c.dom.Document;

import java.io.File;
import java.net.URL;

/**
 * Provides a mechanism for retrieving XML values from a file/URL/String and also
 * validating a response from a target service.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class XmlTestResource extends StaticTestResource<Document> {

    private XPathSelector xpathSelector;
    private XmlUtilities xmlUtilities = new XmlUtilities();

    public XmlTestResource(Document value) {
        super(value);
    }

    public XmlTestResource(File file) {
        super(file);
    }

    public XmlTestResource(URL url) {
        super(url);
    }

    public XmlTestResource(Document value, XPathSelector xpathSelector) {
        super(value);
        this.xpathSelector = xpathSelector;
    }

    public XmlTestResource(File file, XPathSelector xpathSelector) {
        super(file);
        this.xpathSelector = xpathSelector;
    }

    public XmlTestResource(URL url, XPathSelector xpathSelector) {
        super(url);
        this.xpathSelector = xpathSelector;
    }

    public XmlUtilities getXmlUtilities() {
        return xmlUtilities;
    }

    /**
     * In case you have any special XML requirements
     */
    public void setXmlUtilities(XmlUtilities xmlUtilities) {
        this.xmlUtilities = xmlUtilities;
    }

    /**
     * @return The XML document from the resource as a String
     * @throws Exception XPathEvaluationException thrown if the xpath could not be evaluated correctly
     */
    protected Document getResource(File file) throws Exception {
        return xmlUtilities.getXmlAsDocument(file);
    }

    /**
     * @return The test resource in the appropriate format
     * @throws java.io.IOException
     */
    @Override
    public Document getValue() throws Exception {
        Document value = super.getValue();
        if (xpathSelector != null)
            return xpathSelector.evaluate(value);
        else
            return value;
    }

    public XPathSelector getXpathSelector() {
        return this.xpathSelector;
    }
}
