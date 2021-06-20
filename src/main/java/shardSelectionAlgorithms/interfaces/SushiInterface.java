package shardSelectionAlgorithms.interfaces;

import abstractEntity.Regression;
import abstractEntity.Resource;
import utils.ScoredEntity;

import java.util.List;
import java.util.Map;

/**
 * @author yashasvi
 */
public interface SushiInterface {

    <T> Map<Resource, Regression> adjustRank(List<ScoredEntity<T>> documents, List<Resource> resources);
}
