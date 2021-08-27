package test;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author yashasvi
 */

@Data
public class ShardSelectionRequest {

    private String algorithm;
    private Map<String,Object> search_query;
    private String indexName;
    private Integer maxShards;
    private Integer totalShards;
    private Integer alpha;
    private List<String> routingFields;
}
