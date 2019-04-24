import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yujia on 14/4/2019.
 */
public class SolrSchemaCreator {

    private SolrClient solrClient;

    public SolrSchemaCreator(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    public void createFields() throws Exception {
        SolrUtility.clearSolrDocs(solrClient);
        addField("url", "string");
        addField("title", "text_en");
        addField("content", "text_en");
        addField("revisioncount", "pint");
    }

    private void addField(String name, String type) throws IOException, SolrServerException {
        removeIfFieldExists(name);
        Map<String, Object> fieldAttributes = new LinkedHashMap<>();
        fieldAttributes.put("name", name);
        fieldAttributes.put("type", type);
        fieldAttributes.put("stored", true);
        fieldAttributes.put("indexed", true);
        fieldAttributes.put("multiValued", false);

        SchemaRequest.AddField schemaRequest = new SchemaRequest.AddField(fieldAttributes);
        SchemaResponse.UpdateResponse response =  schemaRequest.process(solrClient);
    }

    private void removeIfFieldExists(String name) throws IOException, SolrServerException {
        SchemaRequest.Fields fieldSchemaRequest = new SchemaRequest.Fields();
        SchemaResponse.FieldsResponse fieldResponse = fieldSchemaRequest.process(solrClient);
        List<Map<String, Object>> allFields = fieldResponse.getFields();
        for (Map<String, Object> field : allFields) {
            String fieldName = (String) field.get("name");
            if (fieldName.equals(name)) {
                SchemaRequest.DeleteField deleteFieldRequest = new SchemaRequest.DeleteField(name);
                SchemaResponse.UpdateResponse deleteFieldResponse = deleteFieldRequest.process(solrClient);
                return;
            }
        }
    }
}
