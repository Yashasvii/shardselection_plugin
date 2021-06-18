package shardSelectionAlgorithms;

import abstractEntity.AbstractResourceSelection;
import abstractEntity.Resource;
import utils.ScoredEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yashasvi
 */

public class ReDDE extends AbstractResourceSelection {

	protected int currentRankCutoff = -1;

	@Override
	protected <T> Map<Resource, Double> getResourceScores(
			List<ScoredEntity<T>> documents, List<Resource> resources)
	{
		Map<Resource, Double> resourceScores = new HashMap<Resource, Double>();

		currentRankCutoff = sampleRankCutoff > 0 ? sampleRankCutoff :
			getSampleRank(documents, resources, completeRankCutoff);
		
		for (int i = 0; i < documents.size() && i < currentRankCutoff; i++) {
			Resource resource = resources.get(i);
			
			double score = resourceScores.containsKey(resource) ? resourceScores.get(resource) : 0;
			score += getScoreAtRank(documents.get(i).getScore(), i);
			
			resourceScores.put(resource, score);
		}
		
		for (Resource resource : resourceScores.keySet()) {
			double score = resourceScores.get(resource) * resource.getSize() / resource.getSampleSize();
			resourceScores.put(resource, score);
		}
		
		return resourceScores;
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
	 * @param rank The document rank.
	 * 
	 * @return The score that a resource receives for a document
	 * 		at a given rank.
	 */
	protected double getScoreAtRank(double score, int rank) {
		return 1;
	}

}