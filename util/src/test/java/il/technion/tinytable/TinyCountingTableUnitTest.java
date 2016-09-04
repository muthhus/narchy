package il.technion.tinytable;

import junit.framework.Assert;

import org.junit.Test;

public class TinyCountingTableUnitTest {
	
	@Test
	public void testStoreBasic()
	{
		int itemSize =6; 
		int bucketCapacity =40;
		int nrBuckets=1;
		TinyCountingTable tt = new TinyCountingTable(itemSize, bucketCapacity, nrBuckets);
		// requires 2 counter items. 
		tt.set("Magic", 100);
		Assert.assertTrue(tt.get("Magic") ==100);
		// require 3 counter items. 
		tt.set("Magic", 8000);
		Assert.assertTrue(tt.get("Magic") ==8000);
		// require 1 counter item. 
		tt.set("Magic", 20);
		Assert.assertTrue(tt.get("Magic") ==20);


	}
	@Test
	public void testStoreAdvanced()
	{
		int itemSize =6; 
		int bucketCapacity =40;
		int nrBuckets=5;
		TinyCountingTable tt = new TinyCountingTable(itemSize, bucketCapacity, nrBuckets);
		
		int nrItems = 40;
		for (int i =0; i<nrItems; i++)
		{
			for(int j=0; j<i;j++)
			{

				Assert.assertTrue(tt.get("Magic" + j)==j);
			}
			tt.set("Magic" +i, i);

			Assert.assertTrue(tt.get("Magic_")==0);

		}



	}

}
