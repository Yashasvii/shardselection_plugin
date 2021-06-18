package shardSelectionAlgorithms;

/**
 * @author yashasvi
 */
public final class ReDDETop extends ReDDE {

	@Override
	protected double getScoreAtRank(double score, int rank) {
		return score;
	}

}
