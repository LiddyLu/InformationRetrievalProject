import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;


/**
 * Created by yujia on 14/4/2019.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        String docDirPath = args[0];

        final String SOLR_CORE_URL = "http://localhost:8983/solr/wiki_core";
        final String DUMP_DATE_STR = "2019-03-20 22:46:13";

        final SolrClient solrCoreClient = new HttpSolrClient.Builder(SOLR_CORE_URL)
                .withConnectionTimeout(10000)
                .withSocketTimeout(60000)
                .build();

        SolrSchemaCreator schemaCreator = new SolrSchemaCreator(solrCoreClient);
        schemaCreator.createFields();
        System.out.println("created schema fields");

        String metaDataPath = args[1];
        WikiIndexer indexer = new WikiIndexer(solrCoreClient);

        indexer.indexWikiPage(docDirPath);
        indexer.indexRevisions(metaDataPath, DUMP_DATE_STR);
        solrCoreClient.close();

    }
}
