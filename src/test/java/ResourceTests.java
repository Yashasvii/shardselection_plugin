import abstractEntity.Resource;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author yashasvi
 */
public class ResourceTests {
	private static final String resourceId = "test resource";
	private static final int size = 1234;
	private static final int sampleSize = 300;
	private static final Resource resource = new Resource(resourceId, size, sampleSize);
	

	@Test
	public void testHashCode() {
		assertEquals(resourceId.hashCode(), resource.hashCode());
	}


	@Test(expected=NullPointerException.class)
	public void testResourceNPE() {
		new Resource(null, size, sampleSize);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testResourceNegativeSize() {
		new Resource(resourceId, -1, sampleSize);
	}
	

	@Test(expected=IllegalArgumentException.class)
	public void testResourceNegativeSampleSize() {
		new Resource(resourceId, size, -5);
	}

	@Test
	public void testGetResourceId() {
		assertEquals(resourceId, resource.getResourceId());
	}

	@Test
	public void testGetSize() {
		assertEquals(size, resource.getSize());
	}

	@Test
	public void testGetSampleSize() {
		assertEquals(sampleSize, resource.getSampleSize());
	}

	@Test
	public void testEqualsObject() {
		Resource otherResource = new Resource(resourceId, 100, 10);
		assertTrue(resource.equals(otherResource));
	}

}
