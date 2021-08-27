package test;

import abstractEntity.ResourceSelection;
import shardSelectionAlgorithms.HybridShardSelectionAlgorithm;
import shardSelectionAlgorithms.RankS;
import shardSelectionAlgorithms.ReDDE;
import shardSelectionAlgorithms.Sushi;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yashasvi
 */
public class TestMain {

    public static void main(String[] args) {

        ShardSelectionRequest shardSelectionRequest = new ShardSelectionRequest();
        shardSelectionRequest.setAlgorithm("Hybrid");
        shardSelectionRequest.setAlpha(20);
        shardSelectionRequest.setTotalShards(20);
        shardSelectionRequest.setIndexName("shardselections");
        shardSelectionRequest.setMaxShards(20);
        shardSelectionRequest.setRoutingFields(null);

        Map<String, Object> fieldMap = new HashMap<>() {{
            put("_id", "zz");
        }};

        Map<String, Object> matchMap = new HashMap<>() {{
            put("match", fieldMap);
        }};

        Map<String, Object> queryMap = new HashMap<>() {{
            put("query", matchMap);
        }};

        shardSelectionRequest.setSearch_query(queryMap);

        shardSelectionRequest.setSearch_query(queryMap);

        shardSelectionRequest.setTotalShards(20);

        ResourceSelection selection = getShardSelectionService(shardSelectionRequest.getAlgorithm());

        Map<String, Object> documentInfos = selection.getDocumentResponseScoreAndTime(shardSelectionRequest.getIndexName(),
                shardSelectionRequest.getSearch_query(), true, shardSelectionRequest.getMaxShards(), shardSelectionRequest.getTotalShards(), shardSelectionRequest.getAlpha(), shardSelectionRequest.getRoutingFields());

        System.out.println("documentInfos = " + documentInfos);


    }

    private static ResourceSelection getShardSelectionService(String algorithm) {
        if(algorithm.equalsIgnoreCase("Redde")){
            return new ReDDE();
        }
        else if(algorithm.equalsIgnoreCase("Sushi")) {
            return new Sushi();
        }
        else if(algorithm.equalsIgnoreCase("RankS")) {
            return new RankS();
        }
        else if(algorithm.equalsIgnoreCase("hybrid")) {
            return new HybridShardSelectionAlgorithm();
        }
        return null;
    }
}
