import org.junit.Test;
import utils.ScoredEntity;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author yashasvi
 */

public class ScoredEntityTests {
    private static final double DOUBLE_DELTA = 1e-5;


    @Test
    public void testHashCode() {
        String entity = "test entity";
        ScoredEntity<String> scoredEntity = new ScoredEntity<String>(entity, 0);
        assertEquals(entity.hashCode(), scoredEntity.hashCode());
    }

    @Test(expected = NullPointerException.class)
    public void testScoredEntity() {
        new ScoredEntity<Object>(null, 0);
    }


    @Test
    public void testGetEntity() {
        Integer entity = 42;
        ScoredEntity<Integer> scoredEntity = new ScoredEntity<Integer>(entity, 0);
        assertEquals(entity, scoredEntity.getEntity());
    }


    @Test
    public void testGetScore() {
        double score = 42.42;
        ScoredEntity<Object> scoredEntity = new ScoredEntity<Object>(new Object(), score);
        assertEquals(score, scoredEntity.getScore(), DOUBLE_DELTA);
    }

    @Test
    public void testEqualsObject() {
        String entity = "test entity";
        ScoredEntity<String> scoredEntity1 = new ScoredEntity<String>(entity, 42);
        ScoredEntity<String> scoredEntity2 = new ScoredEntity<String>(entity, 0);
        assertTrue(scoredEntity1.equals(scoredEntity2));
    }


    @Test
    public void testCompareToNotEqual() {
        Object entity1 = new Object();
        ScoredEntity<Object> scoredEntity1 = new ScoredEntity<Object>(entity1, -42);

        String entity2 = "test entity";
        ScoredEntity<Object> scoredEntity2 = new ScoredEntity<Object>(entity2, 42);

        assertEquals(-1, scoredEntity1.compareTo(scoredEntity2));
    }

    @Test
    public void testCompareToEquals() {
        Object entity1 = new Object();
        ScoredEntity<Object> scoredEntity1 = new ScoredEntity<Object>(entity1, 23.542);

        String entity2 = "test entity";
        ScoredEntity<Object> scoredEntity2 = new ScoredEntity<Object>(entity2, 23.542);

        assertEquals(0, scoredEntity1.compareTo(scoredEntity2));
    }


    @Test(expected = NullPointerException.class)
    public void testNPE() {
        ScoredEntity.sort(null, false);
    }


    @Test
    public void testSorting() {
        Random random = new Random();

        List<ScoredEntity<Object>> entities = new LinkedList<ScoredEntity<Object>>();
        int numEntities = 173;
        for (int i = 0; i < numEntities; i++) {
            entities.add(new ScoredEntity<Object>(new Object(), random.nextInt(1000)));
        }

        List<ScoredEntity<Object>> sortedEntities = ScoredEntity.sort(entities, false);
        for (int i = 0; i < sortedEntities.size() - 1; i++) {
            assertTrue(sortedEntities.get(i).getScore() >= sortedEntities.get(i + 1).getScore());
        }
    }

}
