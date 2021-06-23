package abstractEntity;


import utils.ScoredEntity;

import java.util.List;

/**
 * @author yashasvi
 */
public interface ResourceSelection {

    <T> List<ScoredEntity<Resource>> select(List<ScoredEntity<T>> documents,
                                            List<Resource> resources);

    <T> double getDocumentScore(int csiTopN);
}
