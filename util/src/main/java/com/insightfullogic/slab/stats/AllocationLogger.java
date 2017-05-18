package com.insightfullogic.slab.stats;

import com.insightfullogic.slab.Allocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AllocationLogger implements AllocationListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(AllocationLogger.class);

	@Override
    public void onAllocation(Allocator<?> by, int size, long sizeInBytes) {
		LOGGER.info("{} allocated", sizeInBytes);
	}

	/** @noinspection StringConcatenationArgumentToLogCall*/
	@Override
    public void onResize(Allocator<?> by, int size, long sizeInBytes) {
		LOGGER.info("resize {}", sizeInBytes);
	}

	@Override
    public void onFree(Allocator<?> by, int size, long sizeInBytes) {
		LOGGER.info("freed {}", sizeInBytes);
	}

}
