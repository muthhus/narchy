package il.technion.tinytable;

import il.technion.tinytable.hash.RankIndexing;
import junit.framework.Assert;
import il.technion.tinytable.hash.FingerPrint;

import org.junit.Test;

public class RankIndexingTechnqiueUnitTest {


	@Test
	public void TestAdd()
	{
		long[] I0 = new long[1];
		long[] IStar =  new long[1];
		byte[] offsets = new byte[64];
		byte[] chain = new byte[64];
		
		
		FingerPrint fpaux = new FingerPrint(0, 4, 1);
		
		RankIndexing.addItem(fpaux,I0,IStar,offsets,chain);
		
		Assert.assertTrue(RankIndexing.chainExist(I0[0], 4));

		fpaux.chainId = 5;
		RankIndexing.addItem(fpaux,I0,IStar,offsets,chain);
		Assert.assertTrue(RankIndexing.chainExist(I0[0], fpaux.chainId));
		RankIndexing.getChainAndUpdateOffsets(fpaux, I0, IStar, offsets, chain);
		Assert.assertTrue(chain[0]==1);
		fpaux.chainId = 4;
		RankIndexing.getChainAndUpdateOffsets(fpaux, I0, IStar, offsets, chain);
		Assert.assertTrue(chain[0]==0);
	}
	@Test
	public void TestRemove()
	{
		long[] I0 = new long[1];
		long[] IStar =  new long[1];
		byte[] offsets = new byte[64];
		byte[] chain = new byte[64];
		
		
		FingerPrint fpaux = new FingerPrint(0, 4, 1);
		//add an item!
		RankIndexing.addItem(fpaux,I0,IStar,offsets,chain);
		// check that item chain exists
		Assert.assertTrue(RankIndexing.chainExist(I0[0], 4));
		// get the chain
		int chainSize = RankIndexing.getChainAndUpdateOffsets(fpaux, I0, IStar, offsets, chain);
		// assert that it is of size 1. 
		Assert.assertTrue(chainSize == 1);
		// remove the item - update the chain size. 
		RankIndexing.RemoveItem(4,I0,IStar,0, offsets,chain,chainSize-1);
		// verify that the chain does not exist anymore. 
		chainSize = RankIndexing.getChainAndUpdateOffsets(fpaux, I0, IStar, offsets, chain);
		Assert.assertTrue(chainSize ==0);
		Assert.assertTrue(chain[0]==-1);


	}
	
	
	
	
}
