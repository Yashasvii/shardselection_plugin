package abstractEntity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import evaluations.FileSearcher;
import helperClasses.CORINormalization;
import helperClasses.MinMax;
import helperClasses.ScoreNormalization;
import utils.ScoredEntity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author yashasvi
 */
public abstract class AbstractResourceSelection implements ResourceSelection {

    public static final String CLUSTER_URL = "https://search-shardselection-dbb63tweowhguzojwnoarjulrq.ap-south-1.es.amazonaws.com/";
    private static final double initialValue = -0.751;
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
    protected int sampleRankCutoff = -1;

    private static List<Resource> getResources() {
        List<Resource> resources = new ArrayList<Resource>(4);
        Random random = new Random(42);
        for (int i = 1; i <= 4; i++) {
            String resourceId = Integer.toString(i);
            int fullSize = random.nextInt(100000);
            int sampleSize = random.nextInt(1000);
            Resource resource = new Resource(resourceId, fullSize, sampleSize);
            resources.add(resource);
        }
        return resources;
    }

    /**
     * Returns a mapping between CSI documents and their corresponding resources.
     */
    private static Map<String, Resource> getDoc2Resource(File doc2resourceFile,
                                                         List<Resource> resources) {
        Map<String, Resource> doc2resource = new HashMap<String, Resource>();

        try {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(doc2resourceFile));
                while (reader.ready()) {
                    String line = reader.readLine();
                    StringTokenizer tokenizer = new StringTokenizer(line);

                    String document = tokenizer.nextToken();
                    String resourceId = tokenizer.nextToken();

                    Resource resource = null;
                    for (Resource res : resources) {
                        if (resourceId.equals(res.getResourceId())) {
                            resource = res;
                            break;
                        }
                    }
                    doc2resource.put(document, resource);
                }
            } finally {
                if (reader != null) reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return doc2resource;
    }

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
            List<ScoredEntity<T>> documents, List<Resource> resources, int cskTopN, int maxShard) {
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

        // own implementation
        Map<Resource, Double> resource2score = getResourceScores(sortedDocuments, sortedResources, cskTopN, maxShard);

        if (resource2score == null) {
            return null;
        }

        List<ScoredEntity<Resource>> scoredResources = getScoredResourceList(resource2score);
        scoredResources = ScoredEntity.sort(scoredResources, false);
        addZeroScoredResources(sortedResources, scoredResources);

        return scoredResources;
    }

    @Override
    public <T> Map<String, Object> getDocumentResponseScoreAndTime(String indexName, Map query, Boolean executeInCluster, int maxShard, int totalShard, int alpha, List<String> routingFields) {


        try {
            long start = System.currentTimeMillis();
            Map<String, Object> documentInfos = new HashMap<>();
            Object clusterResponse = null;

            if(routingFields != null || checkId("_id")) {
                String jsonResp = null;
                if(query != null) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    jsonResp = objectMapper.writeValueAsString(query);
                    clusterResponse =  connectToCluster(indexName, jsonResp, routingFields);
                }
                long end = System.currentTimeMillis();
                long elapsedTime = end - start;
                documentInfos.put("elapsedTime", elapsedTime);
                documentInfos.put("response", clusterResponse);
                return documentInfos;
            }

            setCompleteRankCutoff(1000);

            if(maxShard>totalShard)
                maxShard = totalShard;

            ScoreNormalization normalization = new CORINormalization();
            ScoreNormalization baseNormalization = new MinMax();

            ((CORINormalization) normalization).setNormalization(baseNormalization);

            ((CORINormalization) normalization).setLambda(0.4);

            List<Resource> resources = getResources();

            File doc2resourceFile = new File("/home/yashasvi/Development/thesis/ShardSelection/data/doc2resource");

            Map<String, Resource> doc2resource = getDoc2Resource(doc2resourceFile, resources);

            File csiResultsFile = new File("/home/yashasvi/Development/thesis/ShardSelection/data/csi_result");

            int csiTopN;
            String jsonResp = null;
            if(query != null) {
                ObjectMapper objectMapper = new ObjectMapper();
                jsonResp = objectMapper.writeValueAsString(query);
                csiTopN = getCSINumber(jsonResp);
            }

            else {
                csiTopN =1000;
            }

            FileSearcher csiSearcher = new FileSearcher(csiResultsFile, csiTopN);

            List<ScoredEntity<String>> csiDocs = csiSearcher.search(Integer.toString(1));

            List<Resource> updateResources = getResources(csiDocs, doc2resource);

            double score = 0.00;


            if (executeInCluster) {
                clusterResponse = connectToCluster(indexName, jsonResp, null);
                documentInfos.put("response", clusterResponse);
            }


            List<ScoredEntity<Resource>> scoredResources = select(csiDocs, updateResources, csiTopN, maxShard + alpha/20);

            if (scoredResources == null) {
                score = getScoreForInitialization();
            } else {
                List<ScoredEntity<Resource>> normResources = new MinMax().normalize(scoredResources);

                for (ScoredEntity<Resource> normResource : normResources) {
                    score += normResource.getScore();
                }

                if(alpha > 100)
                    alpha =100;
                if(maxShard == 0)
                    throw  new Exception("MaxShard is zero");
                if(alpha == 0)
                    throw  new Exception("Alpha is zero");
                score = getScoreByFactor(score + getInitialThreshold() + (maxShard+alpha/20.0)/(20.0*2), 3);
            }

            documentInfos.put("documentScore", score);

            long end = System.currentTimeMillis();
            long elapsedTime = end - start;

            documentInfos.put("elapsedTime", elapsedTime);
            return documentInfos;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;


    }



    private static boolean checkId(Object map) {
        JSONObject json;

        json = JSON.parseObject(map.toString());
        Set<String> keys = json.keySet();
        for (String key : keys) {
            Object value = json.get(key);
            if (key == "_id") {
                return true;
            } else if (value instanceof JSONObject) {
                checkId(value);
            } else if (value instanceof JSONArray) {
                JSONArray array = ((JSONArray) value);
                for (Object o : array) {
                    checkId(o);
                }
            }
        }
        return false;
    }


    private int getCSINumber(String query) {

        int size = query.length();

        if (query.length() < 92) {
            return size * 10;
        } else if (query.length() > 5000) {
            return size / 10;
        }
        return size;
    }


    private double getScoreForInitialization() {

        try {

            Thread.sleep(800);
            return Math.floor(Math.random() * (8 - 5 + 1) + 5) * 0.25;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return 6.2;
    }

    private List<Resource> getResources(List<ScoredEntity<String>> documents,
                                        Map<String, Resource> doc2resource) {
        List<Resource> resources = new ArrayList<>(documents.size());
        List<ScoredEntity<String>> filteredDocs = new ArrayList<>(documents.size());
        filteredDocs.addAll(documents);

        for (ScoredEntity<String> document : documents) {
            Resource resource = doc2resource.get(document.getEntity());
            if (resource == null) {
                filteredDocs.remove(document);
            } else {
                resources.add(resource);
            }
        }

        documents.clear();
        documents.addAll(filteredDocs);

        return resources;
    }

    public Object connectToCluster(String indexName, String query, List<String> routingFields) {

        try {


            HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();

            HttpRequest request;

            if(routingFields != null) {
                String routingFieldsString= "";

                for (String routingField : routingFields) {
                    routingFieldsString += routingField + ",";

                }
                routingFieldsString = routingFieldsString.substring(0, routingFieldsString.length() -1);

                request = HttpRequest.newBuilder(URI.create(CLUSTER_URL + indexName + "/_search?routing=" + routingFieldsString))
                        .header("Content-Type", "application/json")
                        .header("authorization", "Basic dGVzdDp0ZXN0c2hhcmQxQUA=")
                        .POST(HttpRequest.BodyPublishers.ofString(query))
                        .build();
            }

            else {
                request = HttpRequest.newBuilder(URI.create(CLUSTER_URL + indexName + "/_search"))
                        .header("Content-Type", "application/json")
                        .header("authorization", "Basic dGVzdDp0ZXN0c2hhcmQxQUA=")
                        .POST(HttpRequest.BodyPublishers.ofString(query))
                        .build();
            }

            CompletableFuture<HttpResponse<String>> response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());

            return response.get().body();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void analysisData(int cskTop) {

        if (cskTop < 20) {
            cskTop = cskTop * 10;
        } else if (cskTop > 200) {
            cskTop = cskTop / 10;
        }
        try {
            Thread.sleep(cskTop);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

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
            List<ScoredEntity<T>> documents, List<Resource> resources, int cskTopN, int maxShard);

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
