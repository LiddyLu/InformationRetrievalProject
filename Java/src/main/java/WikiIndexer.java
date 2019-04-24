import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


/**
 * Created by yujia on 12/3/2019.
 */


public class WikiIndexer {
    private SolrClient solrClient;
    private static final String ELEMENT_PAGE = "page";
    private static final String ELEMENT_REVISION = "revision";
    private static final String ELEMENT_ID = "id";
    private static final String ELEMENT_TIMESTAMP = "timestamp";
    private static final String ELEMENT_TEXT = "text";
    private static final String ELEMENT_URL = "url";
    private static final String ELEMENT_TITLE = "title";


    public WikiIndexer(SolrClient solrClient) throws IOException, SolrServerException {
        this.solrClient = solrClient;
        // deleting existing documents from solr collection
        solrClient.deleteByQuery("*:*");
        solrClient.commit();
    }

    public void indexWikiPage(String dirPath) throws IOException, SolrServerException, ParserConfigurationException, SAXException, org.json.simple.parser.ParseException {
        File wikiFolder = new File(dirPath);
        File[] subFolders = wikiFolder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                // igonore hidden file
                return !pathname.isHidden();
            }
        });

        PrintWriter writer = new PrintWriter("index.txt", "UTF-8");
        // index each Wiki Page with id, url, title and text
        for (File subFolder : subFolders) {
            File[] files = subFolder.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    // igonore hidden file
                    return !pathname.isHidden();
                }
            });
            for (File file : files) {
                try {
                    JSONObject obj;
                    String line = null;
                    // FileReader reads text files in the default encoding.
                    FileReader fileReader = new FileReader(file);

                    // Always wrap FileReader in BufferedReader.
                    BufferedReader bufferedReader = new BufferedReader(fileReader);

                    while((line = bufferedReader.readLine()) != null) {
                        obj = (JSONObject) new JSONParser().parse(line);
                        String text = (String)obj.get(ELEMENT_TEXT);
                        System.out.println((String)obj.get("id")+":"+
                                (String)obj.get("title"));
                        writer.println((String)obj.get("id")+":"+
                                (String)obj.get("title"));
                        String content = text.substring(text.indexOf("\n\n") + 2).replace('\n', ' ');
                        String id = (String)obj.get(ELEMENT_ID);
                        String url = (String)obj.get(ELEMENT_URL);
                        String title = (String)obj.get(ELEMENT_TITLE);

                        SolrInputDocument solrDoc = new SolrInputDocument();
                        solrDoc.addField("id", id);
                        solrDoc.addField("url", url);
                        solrDoc.addField("title", title);
                        solrDoc.addField("content", content);
                        // initialize to 0 will update later
                        solrDoc.addField("revisioncount", 0);
                        UpdateResponse response = solrClient.add(solrDoc);

                    }
                    // Always close files.
                    bufferedReader.close();
                } catch (Exception e) {
                    // ignore exception
                    writer.close();
                    solrClient.commit();
                }
            }
            writer.close();
            solrClient.commit();
        }
    }


    public void indexRevisions(String file, String dumpDateStr) throws ParseException, IOException, XMLStreamException, SolrServerException {
        DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dumpDateTime = LocalDateTime.parse(dumpDateStr, inputFormat);

        XMLInputFactory factory = XMLInputFactory.newInstance();
        InputStream stream = new FileInputStream(file);

        XMLEventReader reader = factory.createXMLEventReader(stream);

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement() && event.asStartElement().getName()
                    .getLocalPart().equals(ELEMENT_PAGE)) {
                System.out.println("start of page");
                parsePageAndUpdateIndex(reader, dumpDateTime);
            }
            solrClient.commit();
        }
        solrClient.commit();
    }

    public void parsePageAndUpdateIndex(XMLEventReader reader, LocalDateTime dumpDateTime) throws XMLStreamException, IOException, SolrServerException {
        String id = null;
        String revisionTimeStr = null;
        int revisionCount = 0;
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            // end of a wiki page
            if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(ELEMENT_PAGE)) {
                System.out.println("end of page");
                break;
            }
            if (event.isStartElement()) {
                StartElement element = event.asStartElement();
                String elementName = element.getName().getLocalPart();
                // System.out.println("is start element: " + elementName);
                if (elementName.equals(ELEMENT_ID)) {
                    id = reader.getElementText();
                    // check if the document with id exists
                    SolrDocument solrDocument = solrClient.getById(id);
                    if (solrDocument == null) {
                        return;
                    }
                } else if (elementName.equals(ELEMENT_REVISION)) {
                    System.out.println("start of revision");
                    revisionTimeStr = parseRevision(reader);
                    LocalDateTime revisionDateTime = LocalDateTime.parse(revisionTimeStr.substring(0, revisionTimeStr.length() - 1));
                    // increment count if the revison is within 30 days
                    if (Duration.between(revisionDateTime, dumpDateTime).toDays() <= 90) {
                        revisionCount++;
                    }
                }
            }
        }
        if(revisionCount == 0) {
            return;
        }
        // update solr with revision count
        SolrInputDocument solrDoc = new SolrInputDocument();
        solrDoc.addField("id", id);
        Map<String,Object> fieldModifier = new HashMap<>(1);
        fieldModifier.put("set", revisionCount);
        solrDoc.addField("revisioncount", fieldModifier);

        System.out.println("id is: " + id + " revisioncount is: " + revisionCount);
        UpdateResponse response = solrClient.add(solrDoc);
    }

    public String parseRevision(XMLEventReader reader) throws XMLStreamException {
        String revisionTimeStr = null;
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            // end of a revision
            if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(ELEMENT_REVISION)) {
                System.out.println("end of revision");
                break;
            }
            if (event.isStartElement()) {
                StartElement element = event.asStartElement();
                String elementName = element.getName().getLocalPart();
                if (elementName.equals(ELEMENT_TIMESTAMP)) {
                    revisionTimeStr = reader.getElementText();
                }
            }
        }
        return revisionTimeStr;
    }
}
