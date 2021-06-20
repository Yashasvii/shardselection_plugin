package shardSelectionAlgorithms.interfaces;

import abstractEntity.Resource;
import utils.ScoredEntity;

import java.util.List;

/**
 * @author yashasvi
 */
public interface ReDDEInterface  {


    <T> int getCentralizedIndexingRank(List<ScoredEntity<T>> documents,
                                       List<Resource> resources, int completeRank);
}
