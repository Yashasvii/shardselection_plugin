package shardSelectionAlgorithms;

import abstractEntity.AbstractResourceSelection;
import abstractEntity.Resource;
import shardSelectionAlgorithms.interfaces.RanKSInterface;
import utils.ScoredEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yashasvi
 */
public class RankS extends AbstractResourceSelection implements RanKSInterface {

    private static final double THRESHOLD = 30.00;
    private static final double VOTING_BIAS = 5.00;
    private static final double initialValue = 0.15;


    @Override
    protected <T> Map<Resource, Double> getResourceScores(List<ScoredEntity<T>> documents, List<Resource> resources,  int cskTopN) {

        Map<Resource, Double> resourceScores = new HashMap<Resource, Double>();

        int currentRankCutoff = sampleRankCutoff > 0 ? sampleRankCutoff :
                getSampleRank(documents, resources, completeRankCutoff);

        for (int i = 0; i < documents.size() && i < currentRankCutoff; i++) {
            Resource resource = resources.get(i);

            double score = resourceScores.containsKey(resource) ? resourceScores.get(resource) : 0;
            score += getRankVoting(documents.get(i).getScore());

            resourceScores.put(resource, score);
        }

        analysisData(cskTopN);

        for (Resource resource : resourceScores.keySet()) {
            double score = resourceScores.get(resource) * resource.getSize() / resource.getSampleSize();
            resourceScores.put(resource, score);
        }

        return resourceScores;
    }

    @Override
    public double getRankVoting(double score) {
        return votingCriteria(score);
    }

    private double votingCriteria(double score) {

        if (score < THRESHOLD) {
            return THRESHOLD;
        }
        return THRESHOLD + VOTING_BIAS;
    }

    @Override
    public double getInitialThreshold() {
        return initialValue;
    }
}
