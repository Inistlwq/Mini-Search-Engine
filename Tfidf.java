package edu.asu.irs13;


import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.lucene.document.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

//import javax.swing.text.html.HTMLDocument.Iterator;
public class Tfidf {
public static String tfidf(String str) throws Exception{
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

		// This code is a test stub to get the term with lowest IDF value

		/*
		 * double minVal = Collections.min(idf_map.values()); for(Entry<String,
		 * Double> entry:idf_map.entrySet()){ if(entry.getValue() == minVal){
		 * System.out.println("Term with lowest idf is: "+entry.getKey()); } }
		 */

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

		// long end = System.currentTimeMillis();
		// double diff = end - start;
		// System.out.println("Time taken to compute norms(in seconds): " + diff
		// / 1000 + "\n");

		// get the query and find similarity with the documents
		HashMap<Integer, Double> similarity_map = new HashMap<Integer, Double>();
		Set<Entry<Integer, HashMap<String, Integer>>> entrySet1 = hmap.entrySet();
		java.util.Iterator<Entry<Integer, HashMap<String, Integer>>> it1 = entrySet1.iterator();
		
		
		
		HashMap<Integer, Double> dot_product_map = new HashMap<Integer, Double>();

		
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

			String result="";

			List<Integer> l1 = new ArrayList<Integer>();
			HashMap<Integer, Double> sim_map = sortMap(similarity_map, r);
			for(Integer i:sim_map.keySet()){
				l1.add(i);
				Document d1 = r.document(i);
				String url = d1.getFieldable("path").stringValue();
				//String snippet = getSnippet(hmap.get(i));
				url = url.replace("%%", "/");
				String snippet = Snippet.getContent(url,terms[0]); //read the content of url
				result +=/*i+" "+*/url+"\n";
				result +=snippet+"\n\n\n";
				//result += snippet+"\n\n";
			}
			return result;
			
}

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
			finalmap.put(entry.getKey(), entry.getValue());
			if (i == 10)
				break;
		}
		return finalmap;
	}


/*private static String getSnippet(HashMap<String, Integer> hashMap) {
	Set<Entry<String, Integer>> set = hashMap.entrySet();
	List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>(set);
	Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
		public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
			return o2.getValue().compareTo(o1.getValue());
		}
	});
	
	String snip= "";
	int i = 0;
	int j = 0;
	for (Entry<String, Integer> entry : list) {

		snip += entry.getKey()+" ";
		if(j++ == 10)
			snip += "\n";
		if (i++ == 20)
			break;
	}
	return snip;
}*/

	
}
