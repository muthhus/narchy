package il.technion.tinytable;

import junit.framework.Assert;

import org.junit.Test;

public class TinyTableUnitTest {
	
	@Test
	public void TestBasicAdd()
	{
		int fingerprintSize = 6;
		int bucketCapacity = 40; 
		int nrBuckets = 1; 
		
		TinyTable tt = new TinyTable(fingerprintSize, bucketCapacity, nrBuckets);
		
		tt.add("TinyTable1");
		tt.add("TinyTable2");
		tt.add("TinyTable3");

		Assert.assertTrue(tt.contains("TinyTable1"));
		Assert.assertTrue(tt.contains("TinyTable2"));
		Assert.assertTrue(tt.contains("TinyTable3"));

		
	}
	@Test
	public void TestBasicRemove()
	{
		int fingerprintSize = 6;
		int bucketCapacity = 40; 
		int nrBuckets = 1; 
		
		TinyTable tt = new TinyTable(fingerprintSize, bucketCapacity, nrBuckets);
		
		tt.add("TinyTable1");
		tt.add("TinyTable2");
		tt.add("TinyTable3");

		Assert.assertTrue(tt.contains("TinyTable1"));
		Assert.assertTrue(tt.contains("TinyTable2"));
		Assert.assertTrue(tt.contains("TinyTable3"));
		tt.remove("TinyTable1");
		tt.remove("TinyTable2");
		tt.remove("TinyTable3");

		Assert.assertFalse(tt.contains("TinyTable1"));
		Assert.assertFalse(tt.contains("TinyTable2"));
		Assert.assertFalse(tt.contains("TinyTable3"));
	}
	
	/**
	 *  Fills TinyTable until capacity, verify that all items remain in table. 
	 */
	@Test
	public void AdvancedTestAdd()
	{
		int fingerprintSize = 6;
		int bucketCapacity = 40; 
		int nrBuckets = 100; 
		
		TinyTable tt = new TinyTable(fingerprintSize, bucketCapacity, nrBuckets);
		int totalItems = bucketCapacity*nrBuckets -1;
		for(int i =0; i<totalItems; i++)
		{
			tt.add(i);
			for(int j=0; j<i; j++)
			{
				Assert.assertTrue(tt.contains(i));
			}
		}

	}
	@Test
	public void AdvancedTestRemove()
	{
		int fingerprintSize = 6;
		int bucketCapacity = 40; 
		int nrBuckets = 100; 
		
		TinyTable tt = new TinyTable(fingerprintSize, bucketCapacity, nrBuckets);
		int totalItems = bucketCapacity*nrBuckets -1;
		for(int i =0; i<totalItems; i++)
		{
			tt.add(i);
			for(int j=0; j<i; j++)
			{
				Assert.assertTrue(tt.contains(i));
			}
		}
		// remove all the items...
		for(int i =totalItems-1; i>=0; i--)
		{
			
			// check that all the stuff we did not remove is still there!. 
			for(int j =0; j<=i;j++){
				Assert.assertTrue(tt.contains(j));
			}
			
			// remove another one! - false positives may cause this one to be contained... 
			tt.remove(i);
		}
		// we removed all items now lets check that they are all gone. 
		for(int i =0; i<totalItems; i++)
		{
			Assert.assertFalse(tt.contains(i));
		}


	}
	

}
