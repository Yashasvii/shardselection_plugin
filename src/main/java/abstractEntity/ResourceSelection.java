package abstractEntity;


import utils.ScoredEntity;

import java.util.List;
import java.util.Map;

/**
 * @author yashasvi
 */
public interface ResourceSelection {

    <T> List<ScoredEntity<Resource>> select(List<ScoredEntity<T>> documents,
                                            List<Resource> resources, int cskTopN);

    <T> Map<String, Object> getDocumentResponseScoreAndTime(Boolean executeInCluster, String indexName, Map query);
}
