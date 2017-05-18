package com.insightfullogic.slab.lib;

import com.insightfullogic.slab.Allocator;
import com.insightfullogic.slab.Cursor;

public class HashSlab<K, V extends Cursor> {

    public static <K, V extends Cursor> HashSlab<K, V> of(int size, Allocator<V> values) {
        return new HashSlab<>(size, values);
    }

    private final V values;
    private final int size;

    public HashSlab(int size, Allocator<V> values) {
        this.size = size;
        this.values = values.allocate(size);
    }

    // TODO: determine best collision avoidance strategy
    public V get(K key) {
        int index = key.hashCode() % size;
        values.move(index);
        return values;
    }

    public int size() {
        return size;
    }


    /**
     * from: https://raw.githubusercontent.com/langera/slab/master/src/main/java/com/yahoo/slab/Slab.java
     */
//    static class SlabAllocations<T> implements Iterable<T> {
//
//        private static class DirectAddressStrategy implements AddressStrategy<Bean> {
//
//            @Override
//            public long createKey(final long address, final Bean instance) {
//                return address;
//            }
//
//            @Override
//            public long getAddress(final long key) {
//                return key;
//            }
//
//            @Override
//            public long removeAddress(final long key) {
//                return key;
//            }
//
//            @Override
//            public long map(final long existingKey, final long newAddress) {
//                return newAddress;
//            }
//        }
//
//        public static final int INITIAL_CHUNKS_LIST_SIZE = 100;
//        private final AddressStrategy<T> addressStrategy;
//        private final SlabFlyweightFactory<T> factory;
//        private final int objectSize;
//        private final long chunkSize;
//        private final long maximumCapacity;
//        private final SlabStorageFactory storageFactory;
//
//        private List<SlabStorageChunk> storageChunks;
//        private long size = 0;
//
//        public Slab(final SlabStorageFactory storageFactory,
//                    final long chunkSize,
//                    final AddressStrategy<T> addressStrategy,
//                    final SlabFlyweightFactory<T> factory) {
//            this(storageFactory, chunkSize, addressStrategy, factory, INITIAL_CHUNKS_LIST_SIZE);
//        }
//
//        public Slab(final SlabStorageFactory storageFactory,
//                    final long chunkSize,
//                    final AddressStrategy<T> addressStrategy,
//                    final SlabFlyweightFactory<T> flyweightFactory,
//                    final int initialChunksListSize) {
//            if (!storageFactory.supportsCapacity(chunkSize)) {
//                throw new IllegalArgumentException("SlabStorage type does not support capacity [" + chunkSize + "]");
//            }
//            this.storageFactory = storageFactory;
//            this.objectSize = calculateObjectSize(storageFactory, flyweightFactory);
//            this.chunkSize = Math.max(objectSize, chunkSize - (chunkSize % objectSize));
//            this.maximumCapacity = calculateMaximumCapacity();
//            this.addressStrategy = addressStrategy;
//            this.factory = flyweightFactory;
//            this.storageChunks = new ArrayList<>(initialChunksListSize);
//            this.storageChunks.add(new SlabStorageChunk(storageFactory, this.chunkSize, 0));
//        }
//
//        public long add(final T instance) {
//            if (instance == null) {
//                throw new IllegalArgumentException("Cannot add null");
//            }
//            if (size == maximumCapacity) {
//                throw new IndexOutOfBoundsException("Slab reached maximum capacity of [" + maximumCapacity + "]");
//            }
//            size++;
//            long address = addToStorage(instance);
//            return addressStrategy.createKey(address, instance);
//        }
//
//        public T get(final long address) {
//            final SlabFlyweight<T> flyweight = factory.getInstance();
//            long realAddress = addressStrategy.getAddress(address);
//            SlabStorageChunk chunk = storageFor(realAddress);
//            realAddress = chunk.noOffsetAddress(realAddress);
//            final SlabStorage storage = chunk.getStorage();
//            flyweight.map(storage, realAddress);
//            return chunk.getFirstAvailableAddress() <= realAddress || flyweight.isNull() ? null : flyweight.asBean();
//        }
//
//        public void remove(final long address) {
//            final SlabFlyweight<T> flyweight = factory.getInstance();
//            long realAddress = addressStrategy.removeAddress(address);
//            final SlabStorageChunk chunk = storageFor(realAddress);
//            realAddress = chunk.noOffsetAddress(realAddress);
//            if (chunk.getFirstAvailableAddress() <= realAddress || flyweight.isNull(chunk.getStorage(), realAddress)) {
//                throw new IndexOutOfBoundsException("Address does not exist [" + address + "]");
//            } else {
//                chunk.decrementSize();
//                removeFromStorage(chunk, flyweight, realAddress);
//                size--;
//            }
//        }
//
//        public void compact(SlabCompactionEventHandler eventHandler) {
//            if (storageChunks.size() > 1) {
//                SlabStorageChunk lastChunk = storageChunks.get(storageChunks.size() - 1);
//                Iterator<SlabFlyweight<T>> iterator = new StorageChunkIterator(lastChunk, Direction.BACK);
//                while (iterator.hasNext() && canCompactToPreviousChunks(lastChunk)) {
//                    compact(eventHandler, lastChunk, iterator.next());
//                }
//                boolean lastChunkRemoved = false;
//                try {
//                    eventHandler.beforeCompactionOfStorage();
//                    if (lastChunk.size() == 0) {
//                        lastChunk.destroy();
//                        storageChunks.remove(lastChunk);
//                        lastChunkRemoved = true;
//                    }
//                } finally {
//                    eventHandler.afterCompactionOfStorage();
//                }
//                if (lastChunkRemoved) {
//                    compact(eventHandler);
//                }
//            }
//        }
//
//        @Override
//        public Iterator<T> iterator() {
//            return new SlabIterator(Direction.FORWARD);
//        }
//
//        public void destroy() {
//            for (SlabStorageChunk chunk : storageChunks) {
//                chunk.destroy();
//            }
//            storageChunks.clear();
//        }
//
//        public long size() {
//            return size;
//        }
//
//        public long availableCapacity() {
//            return (storageChunks.size() * chunkSize / objectSize) - size;
//        }
//
//        public long maximumCapacity() {
//            return maximumCapacity;
//        }
//
//        public long chunkSizeInBytes() {
//            return chunkSize;
//        }
//
//        public int chunkCapacity() {
//            return (int) (chunkSize / objectSize);
//        }
//
//        private boolean canCompactToPreviousChunks(final SlabStorageChunk lastChunk) {
//            return storageChunks.size() > 1 && size - lastChunk.size() < ((storageChunks.size() - 1) * chunkSize / objectSize);
//        }
//
//        private void compact(final SlabCompactionEventHandler eventHandler,
//                             final SlabStorageChunk chunk,
//                             final SlabFlyweight<T> flyweight) {
//            final long existingKey = chunk.offsetAddress(flyweight.getMappedAddress());
//            long newKey = -1;
//            try {
//                eventHandler.beforeCompactionMove(existingKey);
//                if (!flyweight.isNull()) {
//                    long newAddress = addToStorage(flyweight.asBean());
//                    newKey = addressStrategy.map(existingKey, newAddress);
//                    chunk.decrementSize();
//                    removeFromStorage(chunk, flyweight, flyweight.getMappedAddress());
//                }
//            } finally {
//                eventHandler.afterCompactionMove(existingKey, newKey);
//            }
//        }
//
//        private SlabStorageChunk storageFor(final long address) {
//            int index = (int) (address / chunkSize);
//            if (index < 0 || index >= storageChunks.size() || storageChunks.get(index) == null) {
//                throw new IndexOutOfBoundsException("Address does not exist [" + address + "]");
//            }
//            return storageChunks.get(index);
//        }
//
//        private SlabStorageChunk availableStorage() {
//            for (SlabStorageChunk chunk : storageChunks) {
//                if (chunk.isAvailableCapacity()) {
//                    return chunk;
//                }
//            }
//            SlabStorageChunk newChunk = new SlabStorageChunk(storageFactory, chunkSize, chunkSize * storageChunks.size());
//            storageChunks.add(newChunk);
//            return newChunk;
//        }
//
//        private long addToFreeEntry(final T instance, final SlabStorageChunk chunk) {
//            SlabFlyweight<T> flyweight = factory.getInstance();
//            final SlabStorage storage = chunk.getStorage();
//            long newAddress = chunk.getFreeListIndex();
//            chunk.setFreeListIndex(flyweight.getNextFreeAddress(storage, newAddress));
//            flyweight.dumpToStorage(instance, storage, newAddress);
//            chunk.addedUpTo(newAddress + objectSize);
//            return chunk.offsetAddress(newAddress);
//        }
//
//        private long addToLastIndex(final T instance, final SlabStorageChunk chunk) {
//            final long newAddress = chunk.getFirstAvailableAddress();
//            factory.getInstance().dumpToStorage(instance, chunk.getStorage(), newAddress);
//            chunk.addedUpTo(newAddress + objectSize);
//            return chunk.offsetAddress(newAddress);
//        }
//
//        private long addToStorage(final T instance) {
//            SlabStorageChunk chunk = availableStorage();
//            chunk.incrementSize();
//            return (chunk.getFreeListIndex() < 0) ? addToLastIndex(instance, chunk) : addToFreeEntry(instance, chunk);
//        }
//
//        private void removeFromStorage(final SlabStorageChunk chunk, final SlabFlyweight<T> flyweight, final long address) {
//            if (address == chunk.getFirstAvailableAddress() - objectSize) {
//                chunk.removedDownTo(address);
//            } else {
//                flyweight.setAsFreeAddress(chunk.getStorage(), address, chunk.getFreeListIndex());
//                chunk.setFreeListIndex(address);
//            }
//        }
//
//        private int calculateObjectSize(final SlabStorageFactory storageFactory, final SlabFlyweightFactory<T> flyweightFactory) {
//            final SlabStorage tempStorage = storageFactory.allocateStorage(0);
//            final int objectSize = flyweightFactory.getInstance().getStoredObjectSize(tempStorage);
//            tempStorage.freeStorage();
//            if (objectSize <= 0) {
//                throw new IllegalArgumentException("Object size must be a positive integer");
//            }
//            return objectSize;
//        }
//
//        private long calculateMaximumCapacity() {
//            BigInteger maximumChunksCanHold = BigInteger.valueOf(chunkSize).multiply(BigInteger.valueOf(Integer.MAX_VALUE));
//            final long maximumCapacityInBytes = (maximumChunksCanHold.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) ?
//                    Long.MAX_VALUE : maximumChunksCanHold.longValueExact();
//            return maximumCapacityInBytes / objectSize;
//        }
//
//        private enum Direction {
//            FORWARD {
//                @Override
//                long initialPtr(SlabStorageChunk chunk, long objectSize) {
//                    return -objectSize;
//                }
//
//                @Override
//                long advancePtr(final long objectSize) {
//                    return objectSize;
//                }
//
//                @Override
//                boolean done(final SlabStorageChunk chunk, final long ptr) {
//                    return ptr >= chunk.getFirstAvailableAddress();
//                }
//            }, BACK {
//                @Override
//                long initialPtr(SlabStorageChunk chunk, long objectSize) {
//                    return chunk.getFirstAvailableAddress();
//                }
//
//                @Override
//                long advancePtr(final long objectSize) {
//                    return -objectSize;
//                }
//
//                @Override
//                boolean done(final SlabStorageChunk chunk, final long ptr) {
//                    return ptr < 0;
//                }
//            };
//
//            abstract long initialPtr(SlabStorageChunk chunk, long objectSize);
//
//            abstract long advancePtr(long objectSize);
//
//            abstract boolean done(SlabStorageChunk chunk, long ptr);
//        }
//
//        private class SlabIterator implements Iterator<T> {
//
//            private long iterationCounter = size;
//            private final Direction direction;
//            private int chunkPtr = 0;
//            private StorageChunkIterator currentIterator;
//
//            private SlabIterator(final Direction direction) {
//                this.direction = direction;
//                currentIterator = new StorageChunkIterator(storageChunks.get(0), direction);
//            }
//
//            @Override
//            public boolean hasNext() {
//                return iterationCounter > 0;
//            }
//
//            @Override
//            public T next() {
//                if (currentIterator.hasNext()) {
//                    iterationCounter--;
//                    return currentIterator.next().asBean();
//                }
//                if (++chunkPtr >= storageChunks.size()) {
//                    throw new NoSuchElementException("Already iterated over [" + iterationCounter + "] elements");
//                }
//                currentIterator = new StorageChunkIterator(storageChunks.get(chunkPtr), direction);
//                return next();
//            }
//        }
//
//        private class StorageChunkIterator implements Iterator<SlabFlyweight<T>> {
//
//            private final SlabStorageChunk chunk;
//            private final Direction direction;
//            private final SlabStorage storage;
//
//            private long iterationCounter;
//            private long ptr;
//
//            private StorageChunkIterator(final SlabStorageChunk chunk, final Direction direction) {
//                this.chunk = chunk;
//                this.storage = chunk.getStorage();
//                this.direction = direction;
//                ptr = direction.initialPtr(chunk, objectSize);
//                iterationCounter = chunk.size();
//            }
//
//            @Override
//            public boolean hasNext() {
//                return iterationCounter > 0;
//            }
//
//            @SuppressWarnings("unchecked")
//            @Override
//            public SlabFlyweight<T> next() {
//                ptr += direction.advancePtr(objectSize);
//                SlabFlyweight<T> flyweight = factory.getInstance();
//                flyweight.map(storage, ptr);
//                while (!direction.done(chunk, ptr) && flyweight.isNull()) {
//                    ptr += direction.advancePtr(objectSize);
//                    flyweight.map(storage, ptr);
//                }
//                iterationCounter--;
//                return flyweight;
//            }
//        }
//
//        private static class SlabStorageChunk {
//
//            private final long offset;
//            private final SlabStorage storage;
//
//            private long size;
//            private long freeListIndex;
//            private long ptr;
//
//            SlabStorageChunk(SlabStorageFactory factory, final long capacity, final long offset) {
//                this.offset = offset;
//                this.storage = factory.allocateStorage(capacity);
//                this.freeListIndex = -1;
//                this.size = 0;
//                this.ptr = 0;
//
//            }
//
//            long offsetAddress(long address) {
//                return offset + address;
//            }
//
//            long noOffsetAddress(long address) {
//                final long noOffsetAddress = address - offset;
//                if (noOffsetAddress < 0) {
//                    throw new IndexOutOfBoundsException("Address does not exist [" + address + "]");
//                }
//                return noOffsetAddress;
//            }
//
//            long getFreeListIndex() {
//                return freeListIndex;
//            }
//
//            void setFreeListIndex(final long freeListIndex) {
//                this.freeListIndex = freeListIndex;
//            }
//
//            SlabStorage getStorage() {
//                return storage;
//            }
//
//            void destroy() {
//                storage.freeStorage();
//            }
//
//            boolean isAvailableCapacity() {
//                return freeListIndex > -1 || ptr < storage.capacity();
//            }
//
//            long size() {
//                return size;
//            }
//
//            void incrementSize() {
//                size++;
//            }
//
//            void decrementSize() {
//                size--;
//            }
//
//            long getFirstAvailableAddress() {
//                return ptr;
//            }
//
//            public void removedDownTo(final long offset) {
//                ptr = offset;
//            }
//
//            public void addedUpTo(final long offset) {
//                ptr = Math.max(ptr, offset);
//            }
//        }
//    }

}
