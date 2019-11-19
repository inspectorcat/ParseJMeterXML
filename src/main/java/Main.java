import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
    private static String name;

    public static void main(String[] args) {
        fileName = args[0];
        // Get only name of file excluding path
        name = fileName
                .replaceAll("\\\\\\^\\\\", "")
                .replaceAll("\\\\\\^/", "");
        int s;
//        String osName = System.getProperty("os.name").toLowerCase();

        Process proc;
        try {
            // Clean logs
            proc = Runtime.getRuntime()
                    .exec("cd /DATA/Results/; rm –f ./*.jtl: rm –f ./*.csv");
            proc.waitFor();
            proc.destroy();

            changeTestType("single");

            // Start oneThread test
            proc = Runtime.getRuntime()
                    .exec("nohup jmeter -n -t " + name);
            proc.waitFor();
            proc.destroy();

            // Check logs for errors
            proc = Runtime.getRuntime()
                    .exec("cat nohup.out | egrep “Err:.+Active: 0” | tail –n 1 | awk '{print $15}'");
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()));
            s = Integer.parseInt(br.readLine());
            proc.waitFor();
            proc.destroy();

            // Run multiThread test
            if (s == 0) {
                changeTestType("multi");

                proc = Runtime.getRuntime()
                        .exec("nohup jmeter -n -t " + name + " -r &");
                proc.waitFor();
                proc.destroy();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void changeTestType(String testType) {

        if (testType.equals("single")) isSingle = true;
        else if (testType.equals("multi")) isSingle = false;
        else try {
                throw new Exception();
            } catch (Exception e) {
                e.printStackTrace();
            }

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(fileName);

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
                                String name1 = name.substring(0, name.length() - 4);
                                node1.setTextContent("/DATA/Results/${__strReplace(" + name1 + "_" + testType + ",&quot;.jmx&quot;,&quot;&quot;,)}.xml");
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
