package helperClasses;

import utils.ScoredEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yashasvi
 */
public final class MinMax extends LinearScoreNormalization {


	protected <T> List<ScoredEntity<T>> doNormalization(List<ScoredEntity<T>> unnormScoredDocs) {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		
		for (int i = 0; i < unnormScoredDocs.size(); i++) {
			min = Math.min(min, unnormScoredDocs.get(i).getScore());
			max = Math.max(max, unnormScoredDocs.get(i).getScore());
		}
		
		double norm = max - min;
		if (norm == 0) {
			norm = 1;
		}

		List<ScoredEntity<T>> normScoredDocs = new ArrayList<ScoredEntity<T>>(unnormScoredDocs.size());
		for (int i = 0; i < unnormScoredDocs.size(); i++) {
			ScoredEntity<T> normScoredDoc = new ScoredEntity<T>(
					unnormScoredDocs.get(i).getEntity(),
					(unnormScoredDocs.get(i).getScore() - min) / norm);
			normScoredDocs.add(normScoredDoc);
		}
		
		return normScoredDocs;
	}
}
