package edu.asu.irs13;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.FSDirectory;

public class KMeansNew {
	private static int itr;

	public static void main(String[] args) throws Exception {

		// the IndexReader object is the main handle that will give you
		// all the documents, terms and inverted index
		IndexReader r = IndexReader.open(FSDirectory.open(new File("index")));

		// this code takes care of calculating idf for each term
		HashMap<String, Double> idf_map = new HashMap<String, Double>();
		TermEnum t = r.terms();
		double docs_count = r.maxDoc();
		while (t.next()) {
			Term te = new Term("contents", t.term().text());
			double idf = Math.log(docs_count / r.docFreq(te));
			idf_map.put(t.term().text(), idf);
		}

		long t1, t2;
		long start = System.currentTimeMillis();

		// code to get |d| for every document

		HashMap<Integer, HashMap<String, Integer>> hmap = new HashMap<Integer, HashMap<String, Integer>>();
		TermEnum trm = r.terms();
		while (trm.next()) {
			Term te1 = new Term("contents", trm.term().text());
			TermDocs td1 = r.termDocs(te1);

			while (td1.next()) {
				HashMap<String, Integer> a1 = hmap.get(td1.doc());
				if (a1 == null) {
					a1 = new HashMap<String, Integer>();
					a1.put(trm.term().text(), td1.freq());
					hmap.put(td1.doc(), a1);
				} else {
					a1.put(trm.term().text(), td1.freq());
				}
			}
		}

		// compute magnitude of each document vector and store it in array

		ArrayList<Double> doc_magnitude = new ArrayList<Double>();
		HashMap<String, Integer> val = new HashMap<String, Integer>();
		Set<Entry<Integer, HashMap<String, Integer>>> entrySet = hmap.entrySet();

		java.util.Iterator<Entry<Integer, HashMap<String, Integer>>> it = entrySet.iterator();
		while (it.hasNext()) { // foreach document id
			double square_sum = 0.0;
			Map.Entry<Integer, HashMap<String, Integer>> me = (Entry<Integer, HashMap<String, Integer>>) it.next();
			val = me.getValue();
			Set<Map.Entry<String, Integer>> entry = val.entrySet();
			java.util.Iterator<Map.Entry<String, Integer>> iterator = entry.iterator();
			while (iterator.hasNext()) { // foreach term
				Map.Entry<String, Integer> mp = iterator.next();
				square_sum += mp.getValue() * mp.getValue();
			}
			double root = Math.sqrt(square_sum);
			doc_magnitude.add(me.getKey(), root);
		}

		// get the query and find similarity with the documents
		HashMap<Integer, Double> similarity_map = new HashMap<Integer, Double>();
		Set<Entry<Integer, HashMap<String, Integer>>> entrySet1 = hmap.entrySet();
		java.util.Iterator<Entry<Integer, HashMap<String, Integer>>> it1 = entrySet1.iterator();
		Scanner sc = new Scanner(System.in);
		String str = "";
		System.out.print("query> ");
		HashMap<Integer, Double> dot_product_map = new HashMap<Integer, Double>();

		while (!(str = sc.nextLine()).equals("quit")) {
			dot_product_map.clear();
			similarity_map.clear();

			String[] terms = str.split("\\s+");
			int cnt = 0;

			long result_st = System.currentTimeMillis();
			t1 = System.nanoTime();

			// for each term get the documents containing the corresponding term
			for (String word : terms) // foreach term in query
			{
				Term te = new Term("contents", word);
				TermDocs tdocs = r.termDocs(te);
				while (tdocs.next() /* && tdocs.doc() < number */) {
					// Map.Entry<Integer, HashMap<String, Integer>> me =
					// (Entry<Integer, HashMap<String, Integer>>) it1.next();
					double freq = hmap.get(tdocs.doc()).get(word);
					Double temp = dot_product_map.get(tdocs.doc());
					if (temp == null) {
						dot_product_map.put(tdocs.doc(), freq * idf_map.get(word));
					} else {
						dot_product_map.put(tdocs.doc(), dot_product_map.get(tdocs.doc()) + freq * idf_map.get(word));
					}
				}
				cnt++;
			}
			Set<Entry<Integer, Double>> se = dot_product_map.entrySet();
			java.util.Iterator<Entry<Integer, Double>> iter = se.iterator();
			while (iter.hasNext()) {
				Map.Entry<Integer, Double> me = (Entry<Integer, Double>) iter.next();
				similarity_map.put(me.getKey(), me.getValue() / (Math.sqrt(cnt) * doc_magnitude.get(me.getKey())));
			}

			// Normalize the map
			double sum = 0;
			for (int i : similarity_map.keySet()) {
				sum += similarity_map.get(i);
			}

			for (int i : similarity_map.keySet()) {
				double temp = similarity_map.get(i) / sum;
				similarity_map.put(i, temp);
			}

			// Now get the top K documents and add it to the rootset.

			HashMap<Integer, Double> sim_map = sortMap(similarity_map, r);

			// This array list contain the top 50 doc ids for the given query.
			ArrayList<Integer> top_doc_ids = new ArrayList<Integer>();
			for (Integer i : sim_map.keySet()) {
				top_doc_ids.add(i);
			}

			// get the terms present in the top 50 docs.
			Set<String> top_terms = getTerms(hmap, top_doc_ids);
			
			//this contains <doc-id,<terms_in_top50_docs,tf*idf of each term>>
			HashMap<Integer, HashMap<String, Double>> docs = new HashMap<Integer, HashMap<String, Double>>();
			
			//populate docs 
			for (Integer i : top_doc_ids) { // for each doc
				int max_freq = Collections.max(hmap.get(i).values());
				HashMap<String, Double> a1 = new HashMap<String, Double>();

				for (String s : top_terms) { // for each term in the top 50 docs
					if (hmap.get(i).containsKey(s)) { // if the term is already
														// present in the doc
														// get the frequency of
														// term

						double freq = (double) hmap.get(i).get(s);
						double val1;
						if (idf_map.get(s) != null) { // for the term get its
														// idf value
							val1 = (freq / max_freq) * idf_map.get(s);
							a1.put(s, val1);
						} else {
							a1.put(s, 0.0);
						}
					} else { // the term is not present.so add the term with 0
								// as value
						a1.put(s, 0.0);
					}
				}
				docs.put(i, a1);
			}

			//Take K as input
			System.out.println("K: ");
			int K = Integer.parseInt(sc.nextLine());

		//int []arr = {3,4,5,6,7,8,9,10};
		//for(int K:arr){
			//System.out.println("For k= "+K);
			long st = System.nanoTime();
			List<List<Integer>> clusters = AlgorithmKMeans(docs, top_doc_ids, top_terms, K);
			long end = System.nanoTime();
			
			long diff = end-st;
			//System.out.println("for k = "+K+"Time taken(in nano seconds) for Kmeans algorithm is "+diff);
			
			//Display the clusters
			for (int i = 0; i < K; i++){
				List<Integer> l = clusters.get(i);
				System.out.println("Cluster#"+(i+1)+" is :");
				System.out.println("--------------------------");
				int k=1;
				for(Integer doc:l){
					String url = r.document(doc).getFieldable("path").stringValue().replace("%%","/");
					System.out.println(k+"-"+doc+"-"+url);k++;
				}
				//System.out.println(Arrays.asList(clusters.get(i)));
			}
		//}
			
		//}
			System.out.print("query> ");
		}
		
	}

	/*This function returns a set containing terms present in the given list of arguments 
		hmap is the HashMap of the form <doc_id,<terms,frequency_of_term>>
		top_doc_ids contains doc_ids of top 50 documents similar to the given query 
	*/
	public static Set<String> getTerms(HashMap<Integer, HashMap<String, Integer>> hmap,
			ArrayList<Integer> top_doc_ids) {
		if (hmap == null)
			return null;
		Set<String> terms = new HashSet<String>(); // set to contain all the
													// terms in the top 50 docs
		for (Integer i : top_doc_ids) {
			if (hmap.get(i) != null) {
				terms.addAll(hmap.get(i).keySet());
			} else {
				System.out.println("Something went wrong with the top_doc_ids");
			}
		}
		return terms;
	}

	/*This code is the implementation of K-means algorithm
	 * docs is the HashMap of the form <doc_id,<terms,tf*idf of term>>
	   top_doc_ids contains doc_ids of top 50 documents similar to the given query
	   top_terms contains terms present in top 50 documents
	   K = no of clusters to be formed 
	 * */
	public static List<List<Integer>> AlgorithmKMeans(HashMap<Integer, HashMap<String, Double>> docs,
			ArrayList<Integer> top_doc_ids, Set<String> top_terms, int K) {

		// these array lists are the initial clusters
		List<List<Integer>> clusters = new ArrayList<List<Integer>>();
		List<List<Integer>> new_clusters;

		List<Map<String, Double>> cent;
		for (int x = 0; x < K; x++) {
			clusters.add(new ArrayList<Integer>());
		}

		int ii = 0;
		for (Integer doc : top_doc_ids) {
			clusters.get(ii++ % K).add(doc);
		}

		//this is for How does the similarity of the document to the centroid of the cluster change?
		int rand_doc = top_doc_ids.get(0);
		int final_clust = -1;

		itr = 0;
		while (true) {
			new_clusters = new ArrayList<List<Integer>>();
			for (int x = 0; x < K; x++) {
				new_clusters.add(new ArrayList<Integer>());
			}
			cent = new ArrayList<Map<String, Double>>();
			
			//compute the centroids of clusters
			for (int x = 0; x < K; x++) {
				Map<String, Double> m = Centroid(docs, clusters.get(x), top_terms);
				cent.add(m);
			}

			// calculate the distance between centroids and the documents
			for (Integer i : top_doc_ids) { // for all the documents
				HashMap<String, Double> map1 = docs.get(i);
				double max = Integer.MIN_VALUE;
				int index = 0;

				for (int x = 0; x < K; x++) {  //calculate the similarity for each document with the given centroids
					// double distance = EuclideanDistance(map1, cent.get(x));
					double distance = cos_similarity(map1, cent.get(x), top_terms);
					if (distance > max) {
						max = distance;
						index = x;
					}
				}

				//add to the cluster list in the index for which the similarity is large 
				List<Integer> l;
				if (new_clusters.size() == 0 || new_clusters.get(index) == null) {
					l = new ArrayList<Integer>();
					new_clusters.add(index, l);
				} else
					l = new_clusters.get(index);
				l.add(i);
			}
			if (clusters.equals(new_clusters)) { //if the clusters converge stop
				
				//code to find cluster similarity to the given document
				/*for(int i=0;i<clusters.size();i++){
					List<Integer> l = clusters.get(i);
					if(l.contains(rand_doc)) final_clust=i;
				}
				
				double sim = cos_similarity(docs.get(rand_doc),cent.get(final_clust),top_terms);*/
				//System.out.println("for K = "+K+" Similarity values is "+sim);
				return clusters;
			} else {
				clusters = new_clusters;
			}

			itr++;

		}
		
	}

	/*public static boolean isConverged(List<List<Integer>> cluster1, List<List<Integer>> cluster2) {
		if (cluster1.size() != cluster2.size())
			return false;

		for (int x = 0; x < cluster1.size(); x++) {
			if (cluster1.get(x).size() != cluster2.get(x).size())
				return false;
		}

		for (int x = 0; x < cluster1.size(); x++) {
			for (int y = 0; y < cluster1.get(x).size(); y++) {
				if (cluster1.get(x).get(y) != cluster2.get(x).get(y))
					return false;
			}
		}
		return true;
	}*/
	
	/*
	 * This code sorts the given hashmap based on the value and prints the top
	 * 10 tuples based on the value
	 */
	public static HashMap<Integer, Double> sortMap(HashMap<Integer, Double> map, IndexReader r) throws Exception {
		Set<Entry<Integer, Double>> set = map.entrySet();
		List<Entry<Integer, Double>> list = new ArrayList<Entry<Integer, Double>>(set);
		Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
			public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
		HashMap<Integer, Double> finalmap = new HashMap<Integer, Double>();
		int i = 0;
		for (Entry<Integer, Double> entry : list) {
			++i;
			// String url =
			// r.document(entry.getKey()).getFieldable("path").stringValue().replace("%%",
			// "/");

			// System.out.println(i + " " + entry.getKey() + " " +
			// /*entry.getValue()*/ url);
			// if (i <= 50)
			// System.out.println(entry.getKey() /* + " " +entry.getValue() */);
			finalmap.put(entry.getKey(), entry.getValue());
			if (i == 50)
				break;
		}
		return finalmap;
	}

	/* This function computes centroid of the given cluster of documents
	 * */
	public static Map<String, Double> Centroid(HashMap<Integer, HashMap<String, Double>> docs, List<Integer> list,
			Set<String> top_terms) {

		Map<String, Double> cent = new HashMap<String, Double>();
		if (docs == null)
			return null;
		for (String s : top_terms) {
			double sum = 0.0;
			for (Integer i : list) {
				if (docs.get(i).containsKey(s))
					sum += docs.get(i).get(s);
			}
			double temp = sum / list.size();
			cent.put(s, temp);
		}
		return cent;
	}

	public static double EuclideanDistance(HashMap<String, Double> m1, Map<String, Double> m2) {
		if (m1 == null || m2 == null)
			return 0.0;
		double dist = 0.0;

		for (String s : m1.keySet()) {
			if (m2.containsKey(s)) {
				dist += Math.pow(Math.abs(m1.get(s) - m2.get(s)), 2);
			} else {
				System.out.println("Something is wrong in computing distance");
				return -1;
			}
		}
		return Math.sqrt(dist);
	}

	/*
	 * This function computes the cosine similarity between the given documents represented as maps
	 */
	public static double cos_similarity(HashMap<String, Double> map1, Map<String, Double> map2, Set<String> top_terms) {
		double sim = 0.0, distance;
		double map1_mag = 0.0, map2_mag = 0.0;
		for (String s : top_terms) {
			double temp1 = map1.get(s);
			double temp2 = map2.get(s);
			sim += (temp1 * temp2);
			map1_mag = map1_mag + Math.pow(temp1, 2);
			map2_mag = map1_mag + Math.pow(temp2, 2);
		}

		map1_mag = Math.sqrt(map1_mag);
		map1_mag = Math.sqrt(map2_mag);

		if (map1_mag == 0 || map1_mag == 0)
			return 0.0;

		distance = sim / (map1_mag * map1_mag);
		return distance;
	}
}
