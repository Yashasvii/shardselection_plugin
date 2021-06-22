package helperClasses;


import utils.ScoredEntity;

import java.util.List;

/**
 * @author yashasvi
 */

public interface ScoreNormalization {
	
	/**
	 * Returns a list of normalized document scores
	 * for a given list of unnormalized scores.
	 * 
	 * @param unnormScoredDocs The list of document scores to be normalized.
	 * 
	 * @throws NullPointerException
	 * 		If <code>unnormScoredDocs</code> is <code>null</code>.
	 * 
	 * @return The list of normalized document scores.
	 */
	<T> List<ScoredEntity<T>> normalize(List<ScoredEntity<T>> unnormScoredDocs);
}
