package shardSelectionAlgorithms;

import abstractEntity.AbstractResourceSelection;
import abstractEntity.Resource;
import shardSelectionAlgorithms.interfaces.ReDDEInterface;
import utils.ScoredEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yashasvi
 */

public class ReDDE extends AbstractResourceSelection implements ReDDEInterface {

    protected int currentRankCutoff = -1;
    private static final double initialValue = 0.05;

    @Override
    protected <T> Map<Resource, Double> getResourceScores(
            List<ScoredEntity<T>> documents, List<Resource> resources, int cskTopN, int maxShard) {
        Map<Resource, Double> resourceScores = new HashMap<Resource, Double>();

        currentRankCutoff = sampleRankCutoff > 0 ? sampleRankCutoff :
                getCentralizedIndexingRank(documents, resources, completeRankCutoff);

        for (int i = 0; i < documents.size() && i < currentRankCutoff; i++) {
            Resource resource = resources.get(i);

            double score = resourceScores.containsKey(resource) ? resourceScores.get(resource) : 0;
            score += getScoreAtRank(documents.get(i).getScore(), i);

            resourceScores.put(resource, score);
        }
        analysisData(cskTopN + maxShard *10 + 300);

        for (Resource resource : resourceScores.keySet()) {
            double score = resourceScores.get(resource) * resource.getSize() / resource.getSampleSize();
            resourceScores.put(resource, score);
        }

        return resourceScores;
    }

    public double getInitialThreshold() {
        return initialValue;
    }

    @Override
    public <T> int getCentralizedIndexingRank(List<ScoredEntity<T>> documents,
                                              List<Resource> resources, int completeRank) {
        int rank = 0;
        for (int i = 0; i < documents.size(); i++) {
            rank += resources.get(i).getSize() / resources.get(i).getSampleSize();

            if (rank >= completeRank) {
                return i + 1;
            }
        }

        return rank;
    }


    /**
     * Returns a score that a resource receives
     * if its document appears at a given rank.
     * Can be overridden by subclasses.
     *
     * <p>
     * Both <code>score</code> and <code>rank</code>
     * are calculated based on a centralized sample index (CSI).
     * </p>
     *
     * @param score The document score.
     * @param rank  The document rank.
     * @return The score that a resource receives for a document
     * at a given rank.
     */
    protected double getScoreAtRank(double score, int rank) {
        return 1;
    }

}
