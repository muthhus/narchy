package com.insightfullogic.slab.stats;

import com.insightfullogic.slab.Allocator;


public class AllocationAdapter implements AllocationListener {

	@Override
    public void onAllocation(Allocator<?> by, int size, long sizeInBytes) {}

	@Override
    public void onResize(Allocator<?> by, int size, long sizeInBytes) {}

	@Override
    public void onFree(Allocator<?> by, int size, long sizeInBytes) {}

}
