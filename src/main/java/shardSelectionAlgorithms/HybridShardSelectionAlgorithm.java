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


    @Override
    protected <T> Map<Resource, Double> getResourceScores(List<ScoredEntity<T>> documents, List<Resource> resources) {
        Map<Resource, Double> resourceScores = new HashMap<Resource, Double>();

        Map<Resource, Regression> resource2regression = adjustRank(documents, resources); //From Sushi

        int currentRankCutoff = sampleRankCutoff > 0 ? sampleRankCutoff :
                getCentralizedIndexingRank(documents, resources, completeRankCutoff); //From Reddie

        for (int i = 0; i < documents.size() && i < currentRankCutoff; i++) {

            Resource resource = resources.get(i);

            double score = resourceScores.containsKey(resource) ? resourceScores.get(resource) : 0;
            score += getRankVoting(documents.get(i).getScore());
            resourceScores.put(resource, score); //From RankS
        }

        for (Resource resource : resourceScores.keySet()) {
            double score = resourceScores.get(resource) * resource.getSize() / resource.getSampleSize();
            resourceScores.put(resource, score);
        }
        return resourceScores;
    }





    /** From ReDDe **/
    @Override
    public <T> int getCentralizedIndexingRank(List<ScoredEntity<T>> documents, List<Resource> resources, int completeRank) {
        ReDDE redde = new ReDDE();
        return redde.getCentralizedIndexingRank(documents, resources, completeRank);
    }

    /** From Sushi **/
    @Override
    public <T> Map<Resource, Regression> adjustRank(List<ScoredEntity<T>> documents, List<Resource> resources) {

        Sushi sushi = new Sushi();
        return sushi.adjustRank(documents, resources);
    }

    /** From RankS **/
    @Override
    public double getRankVoting(double score) {
        RankS rankS =  new RankS();
        return rankS.getRankVoting(score);
    }


}
