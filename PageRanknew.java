package edu.asu.irs13;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.FSDirectory;

public class PageRanknew {
	static LinkAnalysis L;

	public static void main(String[] args) throws Exception {

		LinkAnalysis.numDocs = 25054;
		LinkAnalysis l = new LinkAnalysis();
		L = l;

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

		long end = System.currentTimeMillis();
		double diff = end - start;

		// System.out.println("Time taken to compute norms(in seconds): " + diff
		// / 1000 + "\n");
		HashMap<Integer, Double> similarity_map_new = new HashMap<Integer, Double>();

		// get the query and find similarity with the documents

		Set<Entry<Integer, HashMap<String, Integer>>> entrySet1 = hmap.entrySet();
		java.util.Iterator<Entry<Integer, HashMap<String, Integer>>> it1 = entrySet1.iterator();
		Scanner sc = new Scanner(System.in);
		String str = "";
		System.out.print("query> ");
		HashMap<Integer, Double> dot_product_map = new HashMap<Integer, Double>();

		// String[] queries = { "campus tour", "transcripts", "admissions",
		// "employee benefits", "parking decal", "src" };
		//double[] W = { 0.4 };
		// for (int q = 0; q < queries.length; q++) {

		// str = queries[q];
		// System.out.println("query is "+str);
		// System.out.println("--------------------------------------:");
		//for (int p = 0; p < W.length; p++) {

			//double w = W[p];
		double w = 0.4;
			//System.out.println("weight is " + w);

			while (!(str = sc.nextLine()).equals("quit")) {
				dot_product_map.clear();
				// similarity_map.clear();
				similarity_map_new.clear();
				String[] terms = str.split("\\s+");
				int cnt = 0;
				long result_st = System.currentTimeMillis();

				// for each term get the documents containing the corresponding
				// term
				for (String word : terms) // foreach term in query
				{
					Term te = new Term("contents", word);
					TermDocs tdocs = r.termDocs(te);
					while (tdocs.next()) {
						// Map.Entry<Integer, HashMap<String, Integer>> me =
						// (Entry<Integer, HashMap<String, Integer>>)
						// it1.next();
						double freq = hmap.get(tdocs.doc()).get(word);
						Double temp = dot_product_map.get(tdocs.doc());
						if (temp == null) {
							dot_product_map.put(tdocs.doc(), freq * idf_map.get(word));
						} else {
							dot_product_map.put(tdocs.doc(),
									dot_product_map.get(tdocs.doc()) + freq * idf_map.get(word));
						}
					}
					cnt++;
				}

				Set<Entry<Integer, Double>> se = dot_product_map.entrySet();
				java.util.Iterator<Entry<Integer, Double>> iter = se.iterator();
				while (iter.hasNext()) {
					Map.Entry<Integer, Double> me = (Entry<Integer, Double>) iter.next();
					similarity_map_new.put(me.getKey(),
							me.getValue() / (Math.sqrt(cnt) * doc_magnitude.get(me.getKey())));
				}

				// List<Integer> rootset = new ArrayList<Integer>();

				HashMap<Integer, Double> rankmap = new HashMap<Integer, Double>();

				// Normalise the map
				double sum = 0;
				for (int i : similarity_map_new.keySet()) {
					sum += similarity_map_new.get(i);
				}

				for (int i : similarity_map_new.keySet()) {
					double temp = similarity_map_new.get(i) / sum;
					similarity_map_new.put(i, temp);
				}

				HashMap<Integer, Double> sim_map = sortMap(similarity_map_new, r);

				double[] d_arr = { 0.85 };
				for (double d : d_arr) {
					//System.out.println("damping factor is " + d);
					HashMap<Integer, Double> pgrank = computePageRank(d);

					// code to get the document with highest page rank

					/*
					 * System.out.println("document with highest page rank :");
					 * HashMap<Integer, Double> sort_pgrank= sortMap(pgrank);
					 */
					// double w=0.4;

					/*
					 * this code computes the similarity and page rank for the
					 * top 10 documents with highest similarity. Note:Here only
					 * the top 10 vector similar documents are considered.
					 */

					for (int pg_id : sim_map.keySet()) {
						double rank = pgrank.get(pg_id);
						double sim = sim_map.get(pg_id);
						// System.out.println("Page rank for "+pg_id+" is
						// "+rank+" similarity is "+sim);
						rankmap.put(pg_id, (1 - w) * sim + w * rank);
					}

					System.out.println("Final ranked pages using page rank and similarity measures ");
					HashMap<Integer, Double> sorted_rankmap = sortMap(rankmap, r);

					// long result_end = System.currentTimeMillis();
					// double result_diff = result_end - result_st;
					// System.out.println("Time taken to get results and sort(in
					// seconds): " + result_diff / 1000);
					 System.out.print("query> ");
					// }
				}
			}
		}
	//}

	/*
	 * This function implements page rank algorithm. 
	 * Reference is http://www.ccs.neu.edu/course/cs6200f13/proj1.html
	 */
	public static HashMap<Integer, Double> computePageRank(double d) {
		
		//Runtime r = Runtime.getRuntime();
		//long t1 = r.totalMemory();

		double threshold = Math.pow(10, -9);
		LinkAnalysis l = L;
		int docs = 25054;

		HashMap<Integer, Double> pgrank = new HashMap<Integer, Double>();
		HashMap<Integer, Double> n_pgrank = new HashMap<Integer, Double>();

		// get the sink nodes
		List<Integer> sink = new ArrayList<Integer>();
		for (int i = 0; i < docs; i++) {
			// int[] links = l.getLinks(i);
			if (l.getLinks(i).length == 0)
				sink.add(i);
		}
		// System.out.println("Sink nodes size is :"+sink.size());
		
		//Initialising the page rank for each page as 1/#documents
		for (int i = 0; i < docs; i++) {
			pgrank.put(i, 1.00 / docs);
			n_pgrank.put(i, 0.00);
		}

		/*
		 * This loop runs until the matrix converge which means that until a stable state is reached.
		 */
		int iter = 0;
		while (true) {
			iter++;
			float sinkpr = 0;
			for (int pg : sink) {
				sinkpr += pgrank.get(pg);
			}
			for (int pg = 0; pg < docs; pg++) {
				n_pgrank.put(pg, (1.00 - d) / docs);
				double temp1 = d * sinkpr / docs;
				double temp2 = n_pgrank.get(pg);
				n_pgrank.put(pg, temp1 + temp2);
				int[] cit = l.getCitations(pg);
				for (int q : cit) {
					double temp3 = n_pgrank.get(pg);
					double temp4;
					int[] link = l.getLinks(q);
					if (link.length > 0) {
						temp4 = (d * pgrank.get(q)) / link.length;
						n_pgrank.put(pg, temp3 + temp4);
					}
				}
			}

			boolean stop = true;
			// double sum = 0.00;
			for (int pg = 0; pg < docs; pg++) {

				if (Math.abs(n_pgrank.get(pg) - pgrank.get(pg)) > threshold) {
					stop = false;
				}
			}

			for (int i = 0; i < docs; i++) {
				double temp = n_pgrank.get(i);
				pgrank.put(i, temp);
			}
			if (stop == true || iter > 1500) {
				// System.out.println("\n Stability reached after " + iter +
				// "iterations.\n");
				break;
			}

		}

		/* Runtime r = Runtime.getRuntime(); */
		//long t2 = r.totalMemory();
		//long diff = t2 - t1 / (1024 * 1024);
		//System.out.println("Total memory is: " + diff);
		return pgrank;

	}

	/*
	 * This code sorts the given hashmap based on the value and prints the 
	 * top 10 tuples based on the value
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
			String url = r.document(entry.getKey()).getFieldable("path").stringValue().replace("%%", "/");
			System.out.println(entry.getKey() + "   " + url
					+ "     "/* + entry.getValue() */);
			finalmap.put(entry.getKey(), entry.getValue());
			if (i == 10)
				break;
		}
		return finalmap;
	}
}
