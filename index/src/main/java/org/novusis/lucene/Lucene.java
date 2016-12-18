//package org.novusis.lucene;
//
//import org.apache.lucene.analysis.Analyzer;
//import org.apache.lucene.analysis.standard.StandardAnalyzer;
//import org.apache.lucene.document.Document;
//import org.apache.lucene.document.Field;
//import org.apache.lucene.document.FieldType;
//import org.apache.lucene.document.StoredField;
//import org.apache.lucene.index.*;
//import org.apache.lucene.search.*;
//import org.apache.lucene.store.Directory;
//import org.apache.lucene.store.FSDirectory;
//import org.apache.lucene.store.SimpleFSDirectory;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
//import java.io.IOException;
//import java.nio.file.Paths;
//import java.util.*;
//
//public class Lucene {
//
//	//https://github.com/nakicdragan/luceneshowcase/blob/master/src/org/novusis/lucene/Lucene.java
//
//	private static String INDEX_PATH = "/tmp/lucene";
//	private static String LUCENE_RESOURCES = "/tmp/corpus.txt";
//	private static String LUCINE_FIELD = "LUCINE_FIELD";
//	private static int LIMIT_SEARCH = 100;
//
//	//Search texts
//	private static String prefixSeachText = "schw";
//	private static String wildcardSearchText = "epid";
//	private static String querySearchText = "muscle";
//	private static String fuzzySearchText = "miscle";
//
//	private IndexSearcher searcher = null;
//	public static void main(String[] args) throws IOException {
//		Lucene lucene = new Lucene();
//		lucene.initializeLuceneIndexes();
//		lucene.initilializeIndexSearcher(INDEX_PATH);
//		lucene.prefixSearch(prefixSeachText);
//		lucene.wildcardSearch(wildcardSearchText);
//		//lucene.querySearch(querySearchText);
//		lucene.fuzzySearch(fuzzySearchText);
//	}
//
//
//	public void prefixSearch(String prefix){
//		System.out.println("EXECUTED PREFIX SEARCH. LIMIT TO "+LIMIT_SEARCH+" RESULTS. SEARCH TEXT = "+prefix);
//		Query query = new PrefixQuery(new Term(LUCINE_FIELD, prefix));
//		this.executeSearch(query);
//	}
//
//	public void wildcardSearch(String wildcardSeachText){
//		System.out.println("EXECUTED WILDCARD SEARCH. LIMIT TO "+LIMIT_SEARCH+" RESULTS. SEARCH TEXT = "+wildcardSeachText);
//		Query query = new WildcardQuery(new Term(LUCINE_FIELD, wildcardSeachText+"*"));
//		this.executeSearch(query);
//	}
//
////	public void querySearch(String queryString){
////		System.out.println("EXECUTED QUERY SEARCH. LIMIT TO "+LIMIT_SEARCH+" RESULTS. SEARCH TEXT = "+queryString);
////		try {
////			QueryParser parser = new QueryParser(Version.LUCENE_36, LUCINE_FIELD ,new StandardAnalyzer(Version.LUCENE_36));
////        	Query query = parser.parse(queryString);
////        	executeSearch(query);
////        } catch (ParseException e) {
////			e.printStackTrace();
////		}
////	}
////
//	public void fuzzySearch(String fuzzySearchText){
//		System.out.println("EXECUTED FUZZY SEARCH. LIMIT TO "+LIMIT_SEARCH+" RESULTS. SEARCH TEXT = "+fuzzySearchText);
//		Term term = new Term(LUCINE_FIELD, fuzzySearchText);
//		FuzzyQuery fuzzyQuery = new FuzzyQuery(term, fuzzySearchText.length()/5);
//		executeSearch(fuzzyQuery);
//	}
//
//	public List<String> executeSearch(Query query) {
//		try{
//			System.err.println("===***==="+searcher.explain(query, LIMIT_SEARCH));
//			TopDocs results = searcher.search(query,LIMIT_SEARCH);
//			//searcher.close();
//			ScoreDoc[] hits = results.scoreDocs;
//	        List<String> searchResults = new ArrayList<String>();
//	        int count = 1;
//	        for (ScoreDoc hit : hits) {
//	        	Document doc = searcher.doc(hit.doc);
//	        	String res = doc.get(LUCINE_FIELD);
//	            searchResults.add(doc.get(LUCINE_FIELD));
//	            System.err.println(count+"  "+res+", "+hit.score);
//	            count++;
//	        }
//	        Collections.sort(searchResults);
//			return searchResults;
//		}catch(Exception e){
//			e.printStackTrace();
//			return null;
//		}
//	}
//
//	private void initializeLuceneIndexes(){
//		try {
//			Analyzer analyzer = new StandardAnalyzer();
//			IndexWriterConfig config = new IndexWriterConfig(analyzer);
//			IndexWriter indexWriter = new IndexWriter(FSDirectory.open(Paths.get(INDEX_PATH)), config);
//			indexWriter.deleteAll();
//			BufferedReader br = new BufferedReader(new FileReader(new File(LUCENE_RESOURCES)));
//			String line;
//			Map<String, Boolean> inserted = new HashMap<String, Boolean>();
//
//			FieldType fieldType = new FieldType();
//			fieldType.setStored(true);
//			fieldType.setTokenized(true);
//
//
//			while ((line = br.readLine()) != null) {
//				if(!inserted.containsKey(line.toLowerCase())){
//					Document doc = new Document();
//					doc.add(new Field(LUCINE_FIELD, line.toLowerCase(), Field.Store.YES));
//
//				 	indexWriter.addDocument(doc);
//				 	inserted.put(line.toLowerCase(), true);
//				}
//			}
//			br.close();
//			indexWriter.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	private void initilializeIndexSearcher(String folderName)  {
//		try {
//			Directory directory = new SimpleFSDirectory(Paths.get(folderName));
//		    IndexReader reader = DirectoryReader.open(directory);
//
//		    this.searcher = new IndexSearcher(reader);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//}