package abstractEntity;

import helperClasses.MinMax;
import utils.ScoredEntity;

import java.util.*;

/**
 * @author yashasvi
 */
public abstract class AbstractResourceSelection implements ResourceSelection {

    /**
     * A rank at which a <i>complete</i> ranking of documents is truncated.
     * All documents above the complete rank cutoff are considered by SD resource selection
     * and all documents bellow the cutoff are discarded.
     *
     * <p>
     * The complete rank cutoff is used by default.
     * </p>
     */
    protected int completeRankCutoff = 1000;
    private static final double initialValue = 0.00;

    protected int sampleRankCutoff = -1;

    /**
     * Sets the complete rank cutoff.
     * The complete rank cutoff should be positive.
     *
     * <p>
     * If set, {@link #completeRankCutoff} is used,
     * while {@link #sampleRankCutoff} is ignored.
     * </p>
     *
     * @param completeRankCutoff The complete rank cutoff.
     * @throws IllegalArgumentException if <code>completeRankCutoff</code> is less than or equal to zero.
     * @see #completeRankCutoff
     */
    public void setCompleteRankCutoff(int completeRankCutoff) {
        if (completeRankCutoff <= 0) {
            throw new IllegalArgumentException("The complete rank cutoff is not positive: " + completeRankCutoff);
        }
        this.completeRankCutoff = completeRankCutoff;
        this.sampleRankCutoff = -1;
    }

    @Override
    public <T> List<ScoredEntity<Resource>> select(
            List<ScoredEntity<T>> documents, List<Resource> resources) {
        if (documents == null) {
            throw new NullPointerException("The list of scored documents is null.");
        }
        if (resources == null) {
            throw new NullPointerException("The list of resources is null.");
        }
        if (documents.size() != resources.size()) {
            throw new IllegalArgumentException("The list of scored documents and the list of resources are of different size: " +
                    documents.size() + " != " + resources.size());
        }

        List<ScoredEntity<T>> sortedDocuments = new ArrayList<ScoredEntity<T>>();
        sortedDocuments.addAll(documents);

        List<Resource> sortedResources = new ArrayList<Resource>();
        sortedResources.addAll(resources);

        if (!checkSorting(sortedDocuments)) {
            sort(sortedDocuments, sortedResources);
        }

        Map<Resource, Double> resource2score = getResourceScores(sortedDocuments, sortedResources);
        List<ScoredEntity<Resource>> scoredResources = getScoredResourceList(resource2score);
        scoredResources = ScoredEntity.sort(scoredResources, false);
        addZeroScoredResources(sortedResources, scoredResources);

        return scoredResources;
    }

    @Override
    public <T> double getDocumentScore(List<ScoredEntity<T>> documents, List<Resource> resources) {

        List<ScoredEntity<Resource>> scoredResources = select(documents, resources);
        List<ScoredEntity<Resource>> normResources = new MinMax().normalize(scoredResources);

        double score = 0.00;
        for (ScoredEntity<Resource> normResource : normResources) {
               score += normResource.getScore();
        }
        return getScoreByFactor(score + getInitialThreshold(),3) ;
    }

    public double getScoreByFactor(double score, int factor) {
        return score * factor;
    }

    public double getInitialThreshold() {
        return initialValue;
    }

    /**
     * Calculates scores for resources in <code>resources</code>
     * based on the ranking of scored documents in <code>documents</code>.
     * Returns a mapping between resources and their scores.
     *
     * <p>
     * At this point scored documents must be sorted descending
     * with respect to their scores.
     * </p>
     *
     * <p>
     * This method must be overridden by subclasses.
     * </p>
     *
     * @param documents The list of scored documents.
     * @param resources The list of corresponding resources.
     * @return The mapping between resources and their scores.
     */
    protected abstract <T> Map<Resource, Double> getResourceScores(
            List<ScoredEntity<T>> documents, List<Resource> resources);

    /**
     * Checks if a given list of scored documents
     * is sorted descending with respect to document scores.
     * Returns <code>true</code> if and only if it is sorted
     * and <code>false</code> otherwise.
     *
     * @param documents The list of scored documents.
     * @return <code>true</code> if and only if the list of scored documents
     * is sorted descending with respect to document scores
     * and <code>false</code> otherwise.
     */
    protected <T> boolean checkSorting(List<ScoredEntity<T>> documents) {
        for (int i = 0; i < documents.size() - 1; i++) {
            if (documents.get(i).getScore() < documents.get(i + 1).getScore()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Calculates what rank in a sample list of documents
     * corresponds to a given complete rank.
     * For example, if a document is from a resource with the size <code>R</code>
     * and the size of its corresponding sample is <code>S</code>,
     * then the complete rank of such document is estimated as
     * <code>rank_of_previous_doc + R / S</code>.
     *
     * @param documents    The list of scored documents.
     * @param resources    The list of corresponding resources.
     * @param completeRank The complete rank.
     * @return The sample rank corresponding to the given complete rank.
     */
    protected <T> int getSampleRank(List<ScoredEntity<T>> documents,
                                    List<Resource> resources, int completeRank) {
        int rank = 0;
        for (int i = 0; i < documents.size(); i++) {
            rank += resources.get(i).getSize() / resources.get(i).getSampleSize();

            if (rank >= completeRank) {
                return i + 1;
            }
        }

        return resources.size();
    }

    /**
     * For each distinct resource in <code>resources</code>
     * extracts a list of corresponding documents from <code>documents</code>.
     * Returns the obtained mapping between resources and documents.
     *
     * @param documents The list of scored documents.
     * @param resources The list of corresponding resources.
     * @return The mapping between resources and documents.
     */
    protected <T> Map<Resource, List<ScoredEntity<T>>> getDocument2Resource(List<ScoredEntity<T>> documents, List<Resource> resources) {
        Map<Resource, List<ScoredEntity<T>>> doc2res = new HashMap<Resource, List<ScoredEntity<T>>>();

        for (int i = 0; i < documents.size(); i++) {
            Resource resource = resources.get(i);

            List<ScoredEntity<T>> resourceDocs = doc2res.get(resource) != null ?
                    doc2res.get(resource) : new ArrayList<ScoredEntity<T>>();
            resourceDocs.add(documents.get(i));

            doc2res.put(resource, resourceDocs);
        }

        return doc2res;
    }

    /**
     * Sorts documents in descending order with respect to their scores.
     * Reorders resources to maintain the original correspondence.
     *
     * @param documents The list of scored documents to sort.
     * @param resources The list of corresponding resources.
     */
    protected <T> void sort(List<ScoredEntity<T>> documents, List<Resource> resources) {
        sort(documents, resources, documents.size());
    }

    /**
     * Sorts first <code>top</code> documents in descending order with respect to their scores.
     * Reorders resources to maintain the original correspondence.
     *
     * @param documents The list of scored documents to sort.
     * @param resources The list of corresponding resources.
     * @param top       Indicates how many documents should be sorted.
     */
    protected <T> void sort(List<ScoredEntity<T>> documents, List<Resource> resources, int top) {
        for (int i = 0; i < documents.size() && i < top; i++) {
            double maxScore = documents.get(i).getScore();
            int index = i;

            for (int j = i + 1; j < documents.size(); j++) {
                if (documents.get(j).getScore() > maxScore) {
                    maxScore = documents.get(j).getScore();
                    index = j;
                }
            }

            if (index != i) {
                Collections.swap(documents, i, index);
                Collections.swap(resources, i, index);
            }
        }
    }

    /**
     * Converts a mapping between resources and their scores
     * to a list of scored resources.
     *
     * @param resource2score The mapping between resources and their scores.
     * @return The list of scored resources.
     */
    private List<ScoredEntity<Resource>> getScoredResourceList(Map<Resource, Double> resource2score) {
        List<ScoredEntity<Resource>> scoredResources = new ArrayList<ScoredEntity<Resource>>();

        for (Map.Entry<Resource, Double> res2score : resource2score.entrySet()) {
            scoredResources.add(new ScoredEntity<Resource>(res2score.getKey(), res2score.getValue()));
        }

        return scoredResources;
    }

    /**
     * For all resources from <code>resources</code> that do not appear in <code>scoredResources</code>
     * adds them to <code>scoredResources</code> with zero score.
     *
     * @param resources       The list of resources.
     * @param scoredResources The list of scored resources.
     */
    private void addZeroScoredResources(List<Resource> resources, List<ScoredEntity<Resource>> scoredResources) {
        for (Resource resource : resources) {
            ScoredEntity<Resource> scoredResource = new ScoredEntity<Resource>(resource, 0);
            if (!scoredResources.contains(scoredResource)) {
                scoredResources.add(scoredResource);
            }
        }
    }
}
