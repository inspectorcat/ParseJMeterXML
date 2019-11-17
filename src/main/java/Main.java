import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Main {
    private static boolean isSingle;
    private static String fileName;

    public static void main(String[] args) {
        fileName = args[0];
        if (args[1].equals("single")) isSingle = true;
        else if (args[1].equals("multi")) isSingle = false;
        else try {
                throw new Exception();
            } catch (Exception e) {
                e.printStackTrace();
            }

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(fileName);

            Node jmeterTestPlan = doc.getFirstChild();

            Node hashTree = doc.getElementsByTagName("hashTree").item(1);

            NodeList list = hashTree.getChildNodes();

            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);

                if ("ThreadGroup".equals(node.getNodeName())) {
                    changeAttr(node, isSingle);
                } else if ("kg.apc.jmeter.threads.UltimateThreadGroup".equals(node.getNodeName())) {
                    changeAttr(node, !isSingle);
                } else if ("BackendListener".equals(node.getNodeName())) {
                    changeAttr(node, !isSingle);
                } else if ("ResultCollector".equals(node.getNodeName())) {
                    NamedNodeMap attr = node.getAttributes();
                    Node nodeattr = attr.getNamedItem("testname");
                    if (nodeattr.getNodeValue().equals("View Results Tree")) {
                        NodeList list1 = node.getChildNodes();
                        for (int j = 0; j < list1.getLength(); j++) {
                            Node node1 = list1.item(j);
                            if ("stringProp".equals(node1.getNodeName())) {
                                String name = fileName
                                        .replaceAll("\\\\\\^\\\\","")
                                        .replaceAll("\\\\\\^/","");
                                String name1 = name.substring(0, name.length() - 4);
                                node1.setTextContent("/DATA/Results/${__strReplace(" + name1 + "_" + args[1] + ",&quot;.jmx&quot;,&quot;&quot;,)}.xml");
                            }
                        }
                    }
                }
            }

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(fileName));
            transformer.transform(source, result);

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (SAXException sae) {
            sae.printStackTrace();
        }
    }

    private static void changeAttr(Node node, boolean isSingle) {
        NamedNodeMap attr = node.getAttributes();
        Node nodeattr = attr.getNamedItem("enabled");
        nodeattr.setTextContent(String.valueOf(isSingle));
    }
}
