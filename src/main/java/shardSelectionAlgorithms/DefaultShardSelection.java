package shardSelectionAlgorithms;

import abstractEntity.AbstractResourceSelection;
import abstractEntity.Resource;
import utils.ScoredEntity;

import java.util.List;
import java.util.Map;

/**
 * @author yashasvi
 */
public class DefaultShardSelection extends AbstractResourceSelection {

    @Override
    protected <T> Map<Resource, Double> getResourceScores(List<ScoredEntity<T>> documents, List<Resource> resources, int cskTopN) {
        return null;
    }

}
