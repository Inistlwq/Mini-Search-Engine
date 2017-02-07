package edu.asu.irs13;


import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.lucene.document.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

//import javax.swing.text.html.HTMLDocument.Iterator;
//get the most freq terms 

public class Extracredit {

	public static void main(String[] args) throws Exception {
		// the IndexReader object is the main handle that will give you
		// all the documents, terms and inverted index
		IndexReader r = IndexReader.open(FSDirectory.open(new File("index")));

		TreeMap<Integer, String> doc_freq_map = new TreeMap<Integer, String>(Collections.reverseOrder());
		
		TermEnum t0 = r.terms(); 
		while(t0.next()) {
			Term te0 = new Term("contents", t0.term().text());
			doc_freq_map.put(r.docFreq(te0),t0.term().text());
		}
		System.out.println("Doc_freq_map is " + doc_freq_map);
	}
}
