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

public class AuthoritiesandHubs {
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
		
		//This code is a test stub to get the term with lowest IDF value
		
		/*double minVal = Collections.min(idf_map.values());
		for(Entry<String, Double> entry:idf_map.entrySet()){
			if(entry.getValue() == minVal){
				System.out.println("Term with lowest idf is: "+entry.getKey());
			}
		}*/

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

		//long end = System.currentTimeMillis();
		//double diff = end - start;
		//System.out.println("Time taken to compute norms(in seconds): " + diff / 1000 + "\n");

		
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
			
			//for each term get the documents containing the corresponding term
			for (String word : terms) // foreach term in query
			{
				Term te = new Term("contents",word);
				TermDocs tdocs = r.termDocs(te);
				while(tdocs.next() /*&& tdocs.doc() < number*/){
					//Map.Entry<Integer, HashMap<String, Integer>> me = (Entry<Integer, HashMap<String, Integer>>) it1.next();
					double freq = hmap.get(tdocs.doc()).get(word);
					Double temp = dot_product_map.get(tdocs.doc());
					if(temp == null){
						dot_product_map.put(tdocs.doc(),freq*idf_map.get(word));
					}else{
						dot_product_map.put(tdocs.doc(),dot_product_map.get(tdocs.doc())+freq*idf_map.get(word));
					}	
				}
				cnt++;
			}
			Set<Entry<Integer,Double>> se = dot_product_map.entrySet();
			java.util.Iterator<Entry<Integer,Double>> iter = se.iterator();
			while(iter.hasNext()){
				Map.Entry<Integer, Double>	me =(Entry<Integer, Double>)iter.next();
				similarity_map.put(me.getKey(), me.getValue()/(Math.sqrt(cnt) * doc_magnitude.get(me.getKey())));
			}
			
			//Normalise the map 
			double sum=0;
			for(int i:similarity_map.keySet()){
				sum+=similarity_map.get(i);
			}
			
			for(int i:similarity_map.keySet()){
				double temp = similarity_map.get(i)/sum;
				similarity_map.put(i,temp);
			}

			// Now get the top K documents and add it to the rootset.
			
			HashMap<Integer, Double> sim_map = sortMap(similarity_map,r);
			List<Integer> rootset = new ArrayList<Integer>();
			for (int i : sim_map.keySet()) {
				rootset.add(i);
			}

			t2 = System.nanoTime();
			double rset_diff = t2 - t1;
			System.out.println("Time taken to get root set(in nanoseconds): " + rset_diff);
			
			computeHubandAuthorityScores(rootset,r);
			//System.out.println("Root set is "+rootset);
			//long result_end = System.currentTimeMillis();
			//double result_diff = result_end - result_st;
			//System.out.println("Time taken to get results and sort(in seconds): " + result_diff / 1000);
			System.out.print("query> ");

		}
		
	}
	
	/*This function does computation of Authorities and Hub scores
	 * 
	 */
	
	public static void computeHubandAuthorityScores(List<Integer> rootset, IndexReader r)throws Exception{
		
		LinkAnalysis.numDocs = 25054;
		LinkAnalysis l = new LinkAnalysis();
		double threshold = Math.pow(10,-9);
		long bset_st = System.nanoTime();
		
		//bset contains the base set.
		List<Integer> bset = new ArrayList<Integer>();
	
		//get the citations and links and add it to bset which is base set. 
		//Note:bset is an ArrayList
		for(int i:rootset){
			int[] links = l.getLinks(i);
			int[] cit = l.getCitations(i);
			for(int j:links){
				if(!bset.contains(j))bset.add(j);
			}
			for(int k:cit){
				if(!bset.contains(k))bset.add(k);
			}
			if(!bset.contains(i))bset.add(i);
		}
		
		long bset_end = System.nanoTime();

		double bset_diff = bset_end - bset_st;
		System.out.println("Time taken to get base set(in nanoseconds): " + bset_diff);
		
		//compute the adjacency matrix
		int size = bset.size();
		double adj[][] = new double[size][size];
		for(int i=0;i<size;i++){
			int[] i1 = l.getLinks(bset.get(i));
			for(int j=0;j<size;j++){
				for(int k=0;k<i1.length;k++){
					if(i1[k] == bset.get(j)){
						adj[i][j]=1;
					}
				}
			}
		}
		
		//long adj_end = System.nanoTime();
		//double adj_diff = adj_end - adj_st;
		//System.out.println("Time taken to compute adjacent matrix(in nanoseconds): " + adj_diff);
		
		//code to print adj matrix
		/*System.out.println("Adjacency matrix is : "+"\n");
		*/
		
		//compute transpose
		double[][] transpose= new double[size][size];
		for(int i=0;i<size;i++){
			for(int j=0;j<size;j++){
				transpose[j][i] = adj[i][j];
			}	
		}
		
		//long init_st = System.nanoTime();
		
		//initialise the hub and auth vectors to 1
		double[][] authorityvec0 = new double[size][1];
		double[][] hubvec0 = new double[size][1];
		
		for(int i=0;i<size;i++){
			authorityvec0[i][0] = 1.00;
			hubvec0[i][0] = 1.00;
		}
		
		//long init_end = System.nanoTime();
		//double init_diff = init_end - init_st;
		//System.out.println("Time taken to intialize hub and authority vectors(in nanoseconds): " + init_diff);
		
		//code to print hub and auth initial vectors
		/*System.out.println("Auth vec 0 is :"+"\n");
		for(int i=0;i<size;i++){
			System.out.println(authorityvec0[i][0]+"\n");
		}*/
		
		//long itr_st = System.nanoTime();
		
		//do power iteration until the matrix converge
		int iter_cnt=0;
		double[][] authorityvec =  multiply(transpose,hubvec0);
		double[][] hubvec = multiply(adj,authorityvec);
		
		double norm1 = magnitude(authorityvec);
		double[][] norm_authorityvec = normalize(authorityvec, norm1);
		
		double norm2 = magnitude(hubvec);
		double[][] norm_hubvec = normalize(hubvec, norm2);
		
		double[][] prev_auth = authorityvec0;
		double[][] prev_hub = hubvec0;
		boolean stop = true;
		while(true){
			iter_cnt++;
			stop = true;
			double[][] diff = subtract(norm_authorityvec,prev_auth);
			for(int i=0;i<size;i++){
				if(diff[i][0] > threshold){
					stop=false;break;
				}
			}
			if(stop == true){
				double[][] diff2 = subtract(norm_hubvec,prev_hub);
				for(int i=0;i<size;i++){
					if(diff2[i][0] > threshold){
						stop=false;break;
					}
				}
				if(stop == true){
					break;
				}
			}
			if(stop == false){
				
				prev_auth = norm_authorityvec;
				prev_hub = norm_hubvec;
				
				//long I_st = System.nanoTime();
				authorityvec =  multiply(transpose,prev_hub);
				//long I_end = System.nanoTime();
				//I_sum += (I_end - I_st);
				
				//long O_st = System.nanoTime();
				hubvec = multiply(adj,authorityvec);
				//long O_end = System.nanoTime();
				//O_sum += (O_end - O_st);
				
				//long N_st = System.nanoTime();
				norm1 = magnitude(authorityvec);
				norm2 = magnitude(hubvec);
				norm_authorityvec = normalize(authorityvec, norm1);
				norm_hubvec = normalize(hubvec, norm2);
				//long N_end = System.nanoTime();
				//N_sum += (N_end - N_st);
			}
			iter_cnt++;
		}
		
		//System.out.println("Time taken for I operation(in nanoseconds): " + I_sum / iter_cnt);
		//System.out.println("Time taken for O operation(in nanoseconds): " + O_sum / iter_cnt);
		//System.out.println("Time taken for N operation(in nanoseconds): " + N_sum / iter_cnt);

		//long itr_end = System.nanoTime();
		//double itr_diff = itr_end - itr_st;
		//System.out.println("Time taken to do power iterations(in nanoseconds): " + itr_diff / iter_cnt);
		
		//System.out.println("No of iterations done are :" + iter_cnt);
		
		//long rank_st = System.nanoTime();
		
		//now put the pages and corresponding auth/hub scores in a map
		HashMap<Integer,Double> hmap1 = new HashMap<Integer,Double>();
		HashMap<Integer,Double> hmap2 = new HashMap<Integer,Double>();
		for(int i=0;i<norm_hubvec.length;i++){
			hmap1.put(bset.get(i),norm_hubvec[i][0]);
			hmap2.put(bset.get(i),norm_authorityvec[i][0]);
		}
		
		
		System.out.println("Top 10 hubs are :");
		sortMap(hmap1,r);
		
		System.out.println("Top 10 authorities are :");
		sortMap(hmap2,r);
		
		//long rank_end = System.nanoTime();
		//double rank_diff = rank_end - rank_st;
		//System.out.println("Time taken to get the sorted hubs and authority vectors(in nanoseconds): " + rank_diff);
	
	}
	
	/*This code does multiplication of matrices
	 */
	 public static double[][] multiply(double[][] A, double[][] B) {
	        int mA = A.length;
	        int nA = A[0].length;
	        int mB = B.length;
	        int nB = B[0].length;
	        if (nA != mB) throw new RuntimeException("Illegal matrix dimensions.");
	        double[][] C = new double[mA][nB];
	        for (int i = 0; i < mA; i++)
	            for (int j = 0; j < nB; j++)
	                for (int k = 0; k < nA; k++)
	                    C[i][j] += A[i][k] * B[k][j];
	        return C;
	    }
	 
	 /*This code does subtraction of matrices
	  */
	 public static double[][] subtract(double[][] A, double[][] B) {
	        int m = A.length;
	        int n = A[0].length;
	        double[][] C = new double[m][n];
	        for (int i = 0; i < m; i++)
	            for (int j = 0; j < n; j++)
	                C[i][j] = Math.abs(A[i][j] - B[i][j]);
	        return C;
	    }
	 
	 /*This code computes L2 norm of given vector
	  */
	 public static double magnitude(double[][] A){
		 if(A.length == 0) return 0;
		 double sum=0;
		 for(int i=0;i<A.length;i++){
			 sum+=Math.pow(A[i][0],2);
		 }
		 return Math.sqrt(sum);
	 }
	 
	 /*This code normalizes the given vector
	  */
	 public static double[][] normalize(double[][] A, double norm){
		 for(int i=0;i<A.length;i++){
			 A[i][0] = A[i][0]/norm;
		 }
		 return A; 
	 }
	 
	 /*
	  * This code prints out the given matrix
	  */
	 public static void print(double[][] A){
		 for(int i=0;i<A.length;i++){
				for(int j=0;j<A[0].length;j++){
					System.out.print(A[i][j] +" ");	
				}
				System.out.print("\n");
			}
	 }
	 
	/*
	 * This code sorts the given hashmap based on the value and prints the 
	 * top 10 tuples based on the value
	 */
	 public static HashMap<Integer, Double> sortMap(HashMap<Integer, Double> map, IndexReader r)throws Exception  {
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
				if(i<=10) 
					System.out.println(i + " " + entry.getKey() + "   " + /*entry.getValue()*/ url);
				finalmap.put(entry.getKey(), entry.getValue());
				if (i == 10)
					break;
			}
			return finalmap;
		}
}
