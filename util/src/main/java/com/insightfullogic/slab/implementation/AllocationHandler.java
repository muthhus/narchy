package com.insightfullogic.slab.implementation;


public interface AllocationHandler {

	void free();

	void resize(int size, long sizeInBytes);

}
