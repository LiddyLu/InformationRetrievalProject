import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;

import java.io.IOException;
import java.util.Collections;

/**
 * Created by yujia on 15/4/2019.
 */
public class SolrUtility {
    public static void createCore(SolrClient solrClient, String core) throws IOException, SolrServerException {
        CoreAdminResponse response = CoreAdminRequest.getStatus(core, solrClient);
        // only create core if it does not exists
        if (response.getCoreStatus(core).size() < 1) {
            CoreAdminRequest.Create aCreateRequest = new CoreAdminRequest.Create();
            aCreateRequest.setCoreName(core);
            aCreateRequest.setInstanceDir(core);
            aCreateRequest.process(solrClient);
        }
    }

    public static void deleteCore(SolrClient solrClient, String core) throws Exception {
        CoreAdminResponse response = CoreAdminRequest.getStatus(core, solrClient);
        // only delete core if it exits
        if (response.getCoreStatus(core).size() != 0) {
            CoreAdminRequest.unloadCore(core, true, true, solrClient);
        }
    }

    public static void clearSolrDocs(SolrClient solrClient) throws Exception {
        UpdateRequest req = new UpdateRequest();
        req.setDeleteQuery(Collections.singletonList("*:*"));
        solrClient.request(req);
        solrClient.commit();
    }
}
