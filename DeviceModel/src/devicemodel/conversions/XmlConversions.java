package devicemodel.conversions;

import devicemodel.DeviceNode;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 *
 * @author root
 */
public class XmlConversions {

    public static Element nodeToXml(DeviceNode node) {
        Element elem = new Element(node.getName());
        if (node.getAttributes().size() > 0) {
            Set<String> keySet = node.getAttributes().keySet();
            List<Attribute> attributes = new ArrayList<>();
            for (String key : keySet) {
                attributes.add(new Attribute(key, node.getAttribute(key)));
            }
            elem.setAttributes(attributes);
        }

        if (node.getValue() != null) {
            elem.setText(node.getValue().toString());
        }
        
        if (node.getChildren().size() > 0) {
            List<DeviceNode> children = node.getChildrenSorted();
            for (DeviceNode child : children) {
                elem.getChildren().add(nodeToXml(node.getChild(child)));
            }
        }

        return elem;
    }

    public static DeviceNode xmlToNode(Element e) {
        DeviceNode node = xmlToNode(e, "");
        return node;
    }

    public static DeviceNode xmlToNode(Element e, String id) {
        String[] ids = new String[]{""};
        if (e.getAttribute("ids") != null) {
            ids = e.getAttributeValue("ids").split(",");
            //e.removeAttribute("ids");
        }

        DeviceNode node = new DeviceNode(e.getName() + id);
        node.setValue(e.getTextTrim());

        for (Attribute a : e.getAttributes()) {
            // Skip the ids attributes.
            if (a.getName().equalsIgnoreCase("ids")) continue;

            node.addAttribute(a.getName(), a.getValue());
        }

        int idAttr = 0;
        for (String cid : ids) {
            for (Element c : e.getChildren()) {
                // Determine if we have an id for these children.
                if (cid.trim().length() > 0) {
                    // Use the cid.
                    try {
                        node.addChild(xmlToNode(c, cid));
                    } catch (Exception ex) {}
                }
                else {
                    // Check to see if the children already have _id attributes.
                    if (c.getAttribute("_id") != null) {
                        // Again, good to go.
                        try {
                            node.addChild(xmlToNode(c, ""));
                        } catch (Exception ex) {}
                    }
                    else {
                        // Check to see how many siblings this child
                        // has with the same name.
                        if (e.getChildren(c.getName()).size() > 1) {
                            // We need to provide an id for them.
                            try {
                                c.setAttribute("_id", String.valueOf(idAttr));
                                node.addChild(xmlToNode(c, ""));
                                idAttr++;
                            } catch (Exception ex) {}
                        }
                        else {
                            // Only child, so good to go.
                            try {
                                node.addChild(xmlToNode(c, ""));
                            } catch (Exception ex) {}
                        }
                    }
                }
            }
        }

        return node;
    }

    public static DeviceNode xmlToNode(File f) throws IOException, JDOMException {
        SAXBuilder docBuilder = new SAXBuilder();
        Document doc = docBuilder.build(f);

        return xmlToNode(doc.getRootElement());
    }

    public static DeviceNode xmlToNode(String xml) throws IOException, JDOMException {
        SAXBuilder docBuilder = new SAXBuilder();
        Document doc = docBuilder.build(new StringReader(xml));

        return xmlToNode(doc.getRootElement());
    }

    public static String nodeToXmlString(DeviceNode node) throws IOException {
        return element2XmlString(nodeToXml(node));
    }

    public static String document2XmlStringNoHeader(final Document doc) throws IOException {
        final StringWriter stringWriter = new StringWriter();
        final XMLOutputter xmlOutput = new XMLOutputter();
        final Format plainFormat = Format.getPrettyFormat();
        plainFormat.setOmitDeclaration(true);
        xmlOutput.setFormat(plainFormat);
        xmlOutput.output(doc, stringWriter);

        return stringWriter.toString();
    }

    public static String element2XmlString(final Element element) throws IOException {
        return document2XmlStringNoHeader(new Document(element.clone()));
    }

    public static Element xmlString2Element(String string) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(new StringReader(string));
        return document.getRootElement();
    }
}
