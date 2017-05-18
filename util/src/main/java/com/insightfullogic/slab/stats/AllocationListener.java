package com.insightfullogic.slab.stats;

import com.insightfullogic.slab.Allocator;


public interface AllocationListener {

	void onAllocation(Allocator<?> by, int size, long sizeInBytes);
	
	void onResize(Allocator<?> by, int size, long sizeInBytes);
	
	void onFree(Allocator<?> by, int size, long sizeInBytes);

}
