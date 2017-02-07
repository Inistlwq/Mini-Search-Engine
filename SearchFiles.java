package edu.asu.irs13;

import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.lucene.document.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;


public class SearchFiles {
	public static void main(String[] args) throws Exception {
		// the IndexReader object is the main handle that will give you
		// all the documents, terms and inverted index
		IndexReader r = IndexReader.open(FSDirectory.open(new File("index")));
		
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
				// System.out.println("Key is: "+mp.getKey() + " & " + " value
				// is: "+mp.getValue());
				square_sum += mp.getValue() * mp.getValue();
			}
			double root = Math.sqrt(square_sum);
			doc_magnitude.add(me.getKey(), root);
		}

		long end = System.currentTimeMillis();
		double diff = end - start;

		System.out.println("Time taken to compute norms(in seconds): " + diff / 1000 + "\n");
		
		TreeMap<Double, Integer> similarity_map = new TreeMap<Double, Integer>(Collections.reverseOrder()); 
		// get the query and find similarity with the documents
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
			double dotproduct;
			int cnt = 0;
			
			//this is test code to get time for restricting the size of the document set by ignoring any documents with id above a certain number 'n'.
			/*Scanner scanner = new Scanner(System.in);
			System.out.print("\nEnter max doc id :");
			int number = scanner.nextInt();*/
			long result_st = System.currentTimeMillis();
			//for each term get the documents containing teh corresponding term
			for (String word : terms) // foreach term in query
			{
				Term te = new Term("contents",word);
				TermDocs tdocs = r.termDocs(te);
				while(tdocs.next()/*&& tdocs.doc() < number*/){
					//Map.Entry<Integer, HashMap<String, Integer>> me = (Entry<Integer, HashMap<String, Integer>>) it1.next();
					double freq = hmap.get(tdocs.doc()).get(word);
					//System.out.println("value from hash map are : \n" + val);
					//val.get(word);
					Double temp = dot_product_map.get(tdocs.doc());
					if(temp == null){
						dot_product_map.put(tdocs.doc(),freq);
					}else{
						dot_product_map.put(tdocs.doc(),dot_product_map.get(tdocs.doc())+freq);
					}	
				}
				cnt++;
			}
			Set<Entry<Integer,Double>> se = dot_product_map.entrySet();
			java.util.Iterator<Entry<Integer,Double>> iter = se.iterator();
			while(iter.hasNext()){
				//Map.Entry<Integer, HashMap<String, Integer>> me = (Entry<Integer, HashMap<String, Integer>>) it1.next();
				Map.Entry<Integer, Double>	me =(Entry<Integer, Double>)iter.next();
				similarity_map.put(me.getValue()/(Math.sqrt(cnt) * doc_magnitude.get(me.getKey())),me.getKey());
			}

			// Now iterate over the arraylist to get the top urls in ranking
			// order
			Set<Entry<Double, Integer>> set = similarity_map.entrySet();
			java.util.Iterator<Map.Entry<Double, Integer>> itr = (java.util.Iterator<Entry<Double, Integer>>) set.iterator();
			int top = 0;
			Document d1;
			while (itr.hasNext() && top < 10) {
				Map.Entry<Double, Integer> me = (Map.Entry<Double, Integer>) itr.next();
				d1 = r.document(me.getValue());
				//String url1 = d1.getFieldable("path").stringValue();
				System.out.println(me.getValue() /*+ " " + url1.replace("%%", "/") + "  " + me.getKey()*/);
				top++;
			}
			long result_end = System.currentTimeMillis();
			double result_diff = result_end - result_st;
			System.out.println("Time taken to get results and sort(in seconds): " + result_diff / 1000 + "\n");
			System.out.print("query> ");

		}
	}
}
