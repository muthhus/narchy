package com.insightfullogic.slab.implementation;

public class NullAllocationHandler implements AllocationHandler {

	@Override
    public void free() {}

	@Override
    public void resize(int size, long sizeInBytes) {}

}
