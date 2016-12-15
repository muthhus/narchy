/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.util;

import com.carrotsearch.randomizedtesting.generators.RandomNumbers;
import com.carrotsearch.randomizedtesting.generators.RandomPicks;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.DocValuesFormat;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.asserting.AssertingCodec;
import org.apache.lucene.index.*;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NoLockFactory;
import org.junit.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * General utility methods for Lucene unit tests.
 */
public final class TestUtil extends TestUtil0 {

    /**
     * Checks some basic behaviour of an AttributeImpl
     *
     * @param reflectedValues contains a map with "AttributeClass#key" as values
     */
    public static <T> void assertAttributeReflection(final AttributeImpl att, Map<String, T> reflectedValues) {
        final Map<String, Object> map = new HashMap<>();
        att.reflectWith(new AttributeReflector() {
            @Override
            public void reflect(Class<? extends Attribute> attClass, String key, Object value) {
                map.put(attClass.getName() + '#' + key, value);
            }
        });
        Assert.assertEquals("Reflection does not produce same map", reflectedValues, map);
    }

    public static void assertEquals(TopDocs expected, TopDocs actual) {
        Assert.assertEquals("wrong total hits", expected.totalHits, actual.totalHits);
        Assert.assertEquals("wrong maxScore", expected.getMaxScore(), actual.getMaxScore(), 0.0);
        Assert.assertEquals("wrong hit count", expected.scoreDocs.length, actual.scoreDocs.length);
        for (int hitIDX = 0; hitIDX < expected.scoreDocs.length; hitIDX++) {
            final ScoreDoc expectedSD = expected.scoreDocs[hitIDX];
            final ScoreDoc actualSD = actual.scoreDocs[hitIDX];
            Assert.assertEquals("wrong hit docID", expectedSD.doc, actualSD.doc);
            Assert.assertEquals("wrong hit score", expectedSD.score, actualSD.score, 0.0);
            if (expectedSD instanceof FieldDoc) {
                Assert.assertTrue(actualSD instanceof FieldDoc);
                Assert.assertArrayEquals("wrong sort field values",
                        ((FieldDoc) expectedSD).fields,
                        ((FieldDoc) actualSD).fields);
            } else {
                Assert.assertFalse(actualSD instanceof FieldDoc);
            }
        }
    }

    /**
     * If failFast is true, then throw the first exception when index corruption is hit, instead of moving on to other fields/segments to
     * look for any other corruption.
     */
    public static CheckIndex.Status checkIndex(Directory dir, boolean crossCheckTermVectors, boolean failFast, ByteArrayOutputStream output) throws IOException {
        if (output == null) {
            output = new ByteArrayOutputStream(1024);
        }
        // TODO: actually use the dir's locking, unless test uses a special method?
        // some tests e.g. exception tests become much more complicated if they have to close the writer
        try (CheckIndex checker = new CheckIndex(dir, NoLockFactory.INSTANCE.obtainLock(dir, "bogus"))) {
            checker.setCrossCheckTermVectors(crossCheckTermVectors);
            checker.setFailFast(failFast);
            checker.setInfoStream(new PrintStream(output, false, IOUtils.UTF_8), false);
            CheckIndex.Status indexStatus = checker.checkIndex(null);

            if (indexStatus == null || indexStatus.clean == false) {
                System.out.println("CheckIndex failed");
                System.out.println(output.toString(IOUtils.UTF_8));
                throw new RuntimeException("CheckIndex failed");
            } else {
                if (LuceneTestCase.INFOSTREAM) {
                    System.out.println(output.toString(IOUtils.UTF_8));
                }
                return indexStatus;
            }
        }
    }

    /**
     * Return a Codec that can read any of the
     * default codecs and formats, but always writes in the specified
     * format.
     */
    public static Codec alwaysDocValuesFormat(final DocValuesFormat format) {
        // TODO: we really need for docvalues impls etc to announce themselves
        // (and maybe their params, too) to infostream on flush and merge.
        // otherwise in a real debugging situation we won't know whats going on!
        if (LuceneTestCase.VERBOSE) {
            System.out.println("TestUtil: forcing docvalues format to:" + format);
        }
        return new AssertingCodec() {
            @Override
            public DocValuesFormat getDocValuesFormatForField(String field) {
                return format;
            }
        };
    }

    public static String randomSubString(Random random, int wordLength, boolean simple) {
        if (wordLength == 0) {
            return "";
        }

        int evilness = nextInt(random, 0, 20);

        StringBuilder sb = new StringBuilder();
        while (sb.length() < wordLength) {
            if (simple) {
                sb.append(random.nextBoolean() ? randomSimpleString(random, wordLength) : randomHtmlishString(random, wordLength));
            } else {
                if (evilness < 10) {
                    sb.append(randomSimpleString(random, wordLength));
                } else if (evilness < 15) {
                    assert sb.length() == 0; // we should always get wordLength back!
                    sb.append(randomRealisticUnicodeString(random, wordLength, wordLength));
                } else if (evilness == 16) {
                    sb.append(randomHtmlishString(random, wordLength));
                } else if (evilness == 17) {
                    // gives a lot of punctuation
                    sb.append(randomRegexpishString(random, wordLength));
                } else {
                    sb.append(randomUnicodeString(random, wordLength));
                }
            }
        }
        if (sb.length() > wordLength) {
            sb.setLength(wordLength);
            if (Character.isHighSurrogate(sb.charAt(wordLength - 1))) {
                sb.setLength(wordLength - 1);
            }
        }

        if (random.nextInt(17) == 0) {
            // mix up case
            String mixedUp = randomlyRecaseCodePoints(random, sb.toString());
            assert mixedUp.length() == sb.length();
            return mixedUp;
        } else {
            return sb.toString();
        }
    }

    public static String randomAnalysisString(Random random, int maxLength, boolean simple) {
        assert maxLength >= 0;

        // sometimes just a purely random string
        if (random.nextInt(31) == 0) {
            return randomSubString(random, random.nextInt(maxLength), simple);
        }

        // otherwise, try to make it more realistic with 'words' since most tests use MockTokenizer
        // first decide how big the string will really be: 0..n
        maxLength = random.nextInt(maxLength);
        int avgWordLength = nextInt(random, 3, 8);
        StringBuilder sb = new StringBuilder();
        while (sb.length() < maxLength) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            int wordLength = -1;
            while (wordLength < 0) {
                wordLength = (int) (random.nextGaussian() * 3 + avgWordLength);
            }
            wordLength = Math.min(wordLength, maxLength - sb.length());
            sb.append(randomSubString(random, wordLength, simple));
        }
        return sb.toString();
    }


    /** start and end are BOTH inclusive */
    public static int nextInt(Random r, int start, int end) {
        return RandomNumbers.randomIntBetween(r, start, end);
    }


    /** Return a Codec that can read any of the
     *  default codecs and formats, but always writes in the specified
     *  format. */
    public static Codec alwaysPostingsFormat(final PostingsFormat format) {
        // TODO: we really need for postings impls etc to announce themselves
        // (and maybe their params, too) to infostream on flush and merge.
        // otherwise in a real debugging situation we won't know whats going on!
        if (LuceneTestCase.VERBOSE) {
            System.out.println("forcing postings format to:" + format);
        }
        return new AssertingCodec() {
            @Override
            public PostingsFormat getPostingsFormatForField(String field) {
                return format;
            }
        };
    }


    /** This runs the CheckIndex tool on the index in.  If any
     *  issues are hit, a RuntimeException is thrown; else,
     *  true is returned. */
    public static CheckIndex.Status checkIndex(Directory dir) throws IOException {
        return checkIndex(dir, true);
    }

    public static CheckIndex.Status checkIndex(Directory dir, boolean crossCheckTermVectors) throws IOException {
        return checkIndex(dir, crossCheckTermVectors, false, null);
    }

    /** This runs the CheckIndex tool on the Reader.  If any
     *  issues are hit, a RuntimeException is thrown */
    public static void checkReader(IndexReader reader) throws IOException {
        for (LeafReaderContext context : reader.leaves()) {
            checkReader(context.reader(), true);
        }
    }

    public static void checkReader(LeafReader reader, boolean crossCheckTermVectors) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
        PrintStream infoStream = new PrintStream(bos, false, IOUtils.UTF_8);

        final CodecReader codecReader;
        if (reader instanceof CodecReader) {
            codecReader = (CodecReader) reader;
            reader.checkIntegrity();
        } else {
            codecReader = SlowCodecReaderWrapper.wrap(reader);
        }
        CheckIndex.testLiveDocs(codecReader, infoStream, true);
        CheckIndex.testFieldInfos(codecReader, infoStream, true);
        CheckIndex.testFieldNorms(codecReader, infoStream, true);
        CheckIndex.testPostings(codecReader, infoStream, false, true);
        CheckIndex.testStoredFields(codecReader, infoStream, true);
        CheckIndex.testTermVectors(codecReader, infoStream, false, crossCheckTermVectors, true);
        CheckIndex.testDocValues(codecReader, infoStream, true);
        CheckIndex.testPoints(codecReader, infoStream, true);

        // some checks really against the reader API
        checkReaderSanity(reader);

        if (LuceneTestCase.INFOSTREAM) {
            System.out.println(bos.toString(IOUtils.UTF_8));
        }

        LeafReader unwrapped = FilterLeafReader.unwrap(reader);
        if (unwrapped instanceof SegmentReader) {
            SegmentReader sr = (SegmentReader) unwrapped;
            long bytesUsed = sr.ramBytesUsed();
            if (sr.ramBytesUsed() < 0) {
                throw new IllegalStateException("invalid ramBytesUsed for reader: " + bytesUsed);
            }
            assert Accountables.toString(sr) != null;
        }
    }



}
