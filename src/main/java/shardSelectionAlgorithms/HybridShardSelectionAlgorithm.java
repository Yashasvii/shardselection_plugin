package shardSelectionAlgorithms;

import abstractEntity.AbstractResourceSelection;
import abstractEntity.Regression;
import abstractEntity.Resource;
import shardSelectionAlgorithms.interfaces.RanKSInterface;
import shardSelectionAlgorithms.interfaces.ReDDEInterface;
import shardSelectionAlgorithms.interfaces.SushiInterface;
import utils.ScoredEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yashasvi
 */
public class HybridShardSelectionAlgorithm extends AbstractResourceSelection implements RanKSInterface, ReDDEInterface, SushiInterface {

    private static final int  rankThreshold = 1000;
    private static final double initialValue = 0.45;


    @Override
    protected <T> Map<Resource, Double> getResourceScores(List<ScoredEntity<T>> documents, List<Resource> resources, int cskTopN, int maxShard) {
        Map<Resource, Double> resourceScores = new HashMap<Resource, Double>();

        double resourcetoScore = 0.00;
        Map<Resource, Regression> resource2regression = adjustRank(documents, resources); //From Sushi

            for (Map.Entry<Resource, Regression> res2regr : resource2regression.entrySet()) {
                Regression regression = res2regr.getValue();
                for (int i = 1; i <= rankThreshold; i++) {
                    resourcetoScore += regression.predict(i);
                }
            }

        int currentRankCutoff = sampleRankCutoff > 0 ? sampleRankCutoff :
                getCentralizedIndexingRank(documents, resources, completeRankCutoff); //From Reddie

        for (int i = 0; i < documents.size() && i < currentRankCutoff; i++) {

            Resource resource = resources.get(i);

            double score = resourceScores.containsKey(resource) ? resourceScores.get(resource) : 0;
            score += getRankVoting(documents.get(i).getScore());  //From RankS
            resourceScores.put(resource, score+resourcetoScore);
        }

        analysisData(cskTopN+maxShard *10);

        for (Resource resource : resourceScores.keySet()) {
            double score = resourceScores.get(resource) * resource.getSize() / resource.getSampleSize();
            resourceScores.put(resource, score+resourcetoScore);
        }
        return resourceScores;
    }


    /**
     * From ReDDe
     **/
    @Override
    public <T> int getCentralizedIndexingRank(List<ScoredEntity<T>> documents, List<Resource> resources, int completeRank) {
        ReDDE redde = new ReDDE();
        return redde.getCentralizedIndexingRank(documents, resources, completeRank);
    }

    /**
     * From Sushi
     **/
    @Override
    public <T> Map<Resource, Regression> adjustRank(List<ScoredEntity<T>> documents, List<Resource> resources) {

        Sushi sushi = new Sushi();
        return sushi.adjustRank(documents, resources);
    }

    /**
     * From RankS
     **/
    @Override
    public double getRankVoting(double score) {
        RankS rankS = new RankS();
        return rankS.getRankVoting(score);
    }

    @Override
    public double getInitialThreshold() {
        return initialValue;
    }


}
