package helperClasses;

import utils.ScoredEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yashasvi
 */
public class CORINormalization implements ResultsMerging {
	

	private double resultListRelevance = 1d;
	
	/**
	 * CORI parameter that controls
	 * how much weight is given to the relevance of a result list.
	 */
	private double lambda = 0.4;

	private ScoreNormalization normalization = new MinMax();

	public void setResultListRelevance(double resultListRelevance) {
		if (resultListRelevance < 0 || resultListRelevance > 1) {
			throw new IllegalArgumentException("The relevance of a result list to be normalized " +
					"is outside the range [0, 1]: " + resultListRelevance);
		}
		this.resultListRelevance = resultListRelevance;
	}

	/**
	 * Returns the value of the CORI parameter lambda.
	 * 
	 * @return The value of the CORI parameter lambda.
	 * 
	 * @see #lambda
	 */
	public double getLambda() {
		return lambda;
	}

	/**
	 * Sets the value of the CORI parameter lambda.
	 * 
	 * @param lambda The value of the parameter to set.
	 * 
	 * @throws IllegalArgumentException
	 * 		if <code>lambda</code> is negative.
	 * 
	 * @see #lambda
	 */
	public void setLambda(double lambda) {
		if (lambda < 0) {
			throw new IllegalArgumentException("The CORI parameter lambda is negative: " + lambda);
		}
		this.lambda = lambda;
	}

	/**
	 * Returns the basic normalization algorithm.
	 * 
	 * @return The basic normalization algorithm.
	 * 
	 * @see #normalization
	 */
	public ScoreNormalization getNormalization() {
		return normalization;
	}

	/**
	 * Sets the basic normalization algorithm.
	 * 
	 * @param normalization The normalization algorithm to set.
	 * 
	 * @throws NullPointerException
	 * 		if <code>normalization</code> is <code>null</code>.
	 * 
	 * @see #normalization
	 */
	public void setNormalization(ScoreNormalization normalization) {
		if (normalization == null) {
			throw new NullPointerException("The basic normalization algorithm is null.");
		}
		this.normalization = normalization;
	}

	@Override
	public <T> List<ScoredEntity<T>> normalize(List<ScoredEntity<T>> unnormScoredDocs) {
		if (unnormScoredDocs == null) {
			throw new NullPointerException("The list of scored documents is null.");
		}
		if (unnormScoredDocs.size() == 0) {
			return new ArrayList<ScoredEntity<T>>();
		}
		
		List<ScoredEntity<T>> normScoredDocs = normalization.normalize(unnormScoredDocs);
		List<ScoredEntity<T>> weightedNormScoredDocs = new ArrayList<ScoredEntity<T>>(unnormScoredDocs.size());
		
		for (int i = 0; i < unnormScoredDocs.size(); i++) {
			double score = getNormalizedScore(normScoredDocs.get(i).getScore(), resultListRelevance);
			ScoredEntity<T> normScoredDoc = new ScoredEntity<T>(unnormScoredDocs.get(i).getEntity(), score);
			weightedNormScoredDocs.add(normScoredDoc);
		}
		
		reset();
		return weightedNormScoredDocs;
	}
	
	/**
	 * Calculates the CORI normalized document score
	 * based on <code>docScore</code> and <code>resultListRelevance</code>.
	 * 
	 * <p>
	 * This method can be overridden by subclasses
	 * in order to change the calculation of the normalized
	 * document score.
	 * </p>
	 *  
	 * @param docScore The document score.
	 * @param resultListRelevance The relevance of a corresponding result list.
	 * 
	 * @return The CORI normalized document score.
	 */
	protected double getNormalizedScore(double docScore, double resultListRelevance) {
		return docScore * (1 + lambda * resultListRelevance) / (1 + lambda);
	}

	/**
	 * Resets {@link #resultListRelevance}.
	 */
	private void reset() {
		resultListRelevance = 1;
	}
}
