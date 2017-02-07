package edu.asu.irs13;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Snippet {
	@SuppressWarnings("finally")
	public static String getContent(String url, String terms) {
		boolean ex = false;
		StringBuffer sb = new StringBuffer();
		try {

			Document doc = Jsoup.connect("http://"+url).get();
			Elements elements = doc.body().select("p");
			int i=0;
			for (Element element : elements) {
				if(element.ownText().contains(terms)){
					sb.append(element.ownText());
					break;
				}
				
			}

		} catch (Exception e) {
			ex = true;
		}finally{
			if(ex)return "Error 504";
			return sb.toString();
		}
	}
}