package evaluations;

import abstractEntity.AbstractResourceSelection;
import abstractEntity.Resource;
import abstractEntity.ResourceSelection;
import shardSelectionAlgorithms.ReDDE;
import utils.ScoredEntity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * @author yashasvi
 */
public class ShardSelectionMainTester {


    /**
     * The resource selection method.
     */
    private final ResourceSelection selection;

    /**
     * The score normalization method.
     */


    public ShardSelectionMainTester(ResourceSelection selection) {
        if (selection == null) {
            throw new NullPointerException("The resource selection method is null.");
        }

        this.selection = selection;
    }

    /**
     * The main executable method.
     *
     * @param args The external parameters of the method.
     */
    public static void main(String[] args) {
        // Check parameters
        if (args.length < 1) {
            System.exit(1);
        }

        String inputFolder = args[0];
        File inputFolderDir = new File(inputFolder);
        if (!inputFolderDir.exists() || !inputFolderDir.isDirectory()) {
            System.out.println("The directory " + inputFolder + " containing chunk(shards) of files doesn't exists");
            System.exit(1);
        }
        String queryFile = args[1];
        File queryFileObject = new File(queryFile);
        if (!queryFileObject.exists() || !queryFileObject.isFile()) {
            System.out.println("The file " + queryFile + " containing the query doesn't exists");
            System.exit(1);
        }

        // Initialize resource selection
        ReDDE redde = new ReDDE();
        int kParam = 1000;
        redde.setCompleteRankCutoff(kParam);



    }
    /**
     * Returns a list of resources
     * (assumes that there are 4 resources with ids from 1 to 4).
     * The resource and sample sizes are generated randomly (for example purpose only!).
     */
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
     * Returns a mapping between resources and their searchers.
     * Assumes that for a given resource
     * its TREC-formatted results file, located in <code>resultsDir</code>,
     * has a name equal to resource id.
     */
    private static Map<Resource, FileSearcher> getResourceSearchers(List<Resource> resources,
                                                                    String resultsDir) {
        Map<Resource, FileSearcher> resourceSearchers = new HashMap<Resource, FileSearcher>();
        int resourceTopN = 100;
        for (Resource resource : resources) {
            File resourceResultsFile = new File(resultsDir + File.separator + resource.getResourceId());
            FileSearcher resourceSearcher = new FileSearcher(resourceResultsFile, resourceTopN);
            resourceSearchers.put(resource, resourceSearcher);
        }
        return resourceSearchers;
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
     * Runs the example.
     *
     * @param csiSearcher       The CSI searcher.
     * @param resourceSearchers The mapping between resources and their corresponding searchers.
     * @param doc2resource      The mapping between CSI documents and their corresponding resources.
     * @throws NullPointerException if <code>csiSearcher</code>, or <code>resourceSearchers</code>,
     *                              or <code>doc2resource</code> is <code>null</code>.
     */
    public void run(FileSearcher csiSearcher,
                    Map<Resource, FileSearcher> resourceSearchers,
                    Map<String, Resource> doc2resource) {
        if (csiSearcher == null) {
            throw new NullPointerException("The CSI searcher is null.");
        }
        if (doc2resource == null) {
            throw new NullPointerException("The mapping between documents and resources is null.");
        }

        // Process queries (assumes that there are 5 queries with ids from 1 to 5)
        for (int queryId = 1; queryId <= 5; queryId++) {
            System.out.println("Processing query " + queryId);
            List<ScoredEntity<String>> mergedResult = new ArrayList<ScoredEntity<String>>();

            // Obtain a CSI ranking of documents and a list of corresponding sources
            List<ScoredEntity<String>> csiDocs = csiSearcher.search(Integer.toString(queryId));
            List<Resource> resources = getResources(csiDocs, doc2resource);

            // Run resource selection and normalize resource scores
            List<ScoredEntity<Resource>> scoredResources = selection.select(csiDocs, resources);


            System.out.println("\tDocument ranking: " + mergedResult);
        }
    }

    /**
     * For a given list of documents returns a list of their corresponding resources.
     */
    private List<Resource> getResources(List<ScoredEntity<String>> documents,
                                        Map<String, Resource> doc2resource) {
        List<Resource> resources = new ArrayList<Resource>(documents.size());
        List<ScoredEntity<String>> filteredDocs = new ArrayList<ScoredEntity<String>>(documents.size());
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
}
