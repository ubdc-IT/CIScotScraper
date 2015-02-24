/*
 * scraperCIScot.java
 * 
 * Copyright 2015 Michael Comerford (UBDC) <michael.comerford@glasgow.ac.uk>
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 * 
 * 
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class scraperCIScot {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// get date stamp for output file
		String stamp = new SimpleDateFormat("yyyyMMddhhmm").format(new Date());
		// headers for details
		String headers = "\"Service Number\", \"Service Name\",\"Registration Date\","
				+ "\"Service Type\",\"Service Type Detail\",\"Service Address\","
				+ "\"Town\",\"Postcode\",\"Telephone\",\"Fax\",\"Manager's Name\""
				+ ",\"Manager's Tel.\",\"Manager's Email\",\"Intending to Cancel\""
				+ ",\"Inspector\",\"Inspector Office\"\n";
		// counter for service details
		int sD = 0;
		// counter for quality grades
		int qG = 0;

		// set up print writer for output file for details
		PrintWriter q = null;
		File out = new File("ScrapedCIScotDetails-"+stamp+".csv");
		try {
			q = new PrintWriter(new FileOutputStream(out),
					true);
		} catch (FileNotFoundException e1) {
			// file not found
			System.out.println("Error: file "+out+" not found");
			e1.printStackTrace();
		}
		// add headers to file
		q.print(headers);

		// set up print writer for output file for Quality Grades
		PrintWriter w = null;
		File out2 = new File("ScrapedCIScotQGrades-"+stamp+".csv");
		try {
			w = new PrintWriter(new FileOutputStream(out2),
					true);
		} catch (FileNotFoundException e2) {
			// file not found
			System.out.println("Error: file "+out2+" not found");
			e2.printStackTrace();
		}
		// connect to page and get cookies
		Response res = null;
		try {
			res = Jsoup
					.connect("http://www.careinspectorate.com/")
					.method(Method.GET)
					.execute();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Map<String, String> cookies = res.cookies();

		// grab the top level page
		Document doc = null;
		try {
			doc = Jsoup.connect("http://www.careinspectorate.com/index.php"
					+ "?option=com_content&view=article&id=7644&Itemid=489")
					.data("bereNextPageId", "ServiceList_action.php")
					.data("serviceType", "")
					.data("serviceTypeCat", "")
					.data("serviceName", "")
					.data("serviceNumber", "")
					.data("providerName", "")
					.data("providerNo", "")
					.data("town", "")
					.data("postcode", "")
					.data("town", "")
					.data("distance", "1")
					.data("whithinDistancePostcode", "")
					.data("action", "searchService")
					.data("option", "com_content")
					.data("task", "")
					.data("id", "7644")
					.data("Itemid", "489")
					.cookies(cookies)
					.post();
			//System.out.println(doc.outerHtml());
		} catch (IOException e) {
			// can't connect to url
			System.out.println("Error: Could not conect to "+"http://www.careinspectorate.com/");
			e.printStackTrace();
		}

		// find the format for page links 
		Elements pages = doc.getElementsByClass("divPaging");
		//System.out.println(pages);
		Element plink = pages.get(0).select("a").first();
		String p = plink.absUrl("href").substring(0, (plink.absUrl("href").length()-2));
		//System.out.println(p);
		Document page = null;
		int j = 0;
		// we know it paginates in 10s and we know the total number, so... 14050
		while (j<=14050) {
			try {
				page = Jsoup.connect("http://www.careinspectorate.com/index.php?"
						+ "option=com_content&view=article&id=7644"
						+ "&Itemid=489&bereNextPageId=ServiceList_action.php"
						+ "&action=movePaging&offset="+j)
						.cookies(cookies)
						.get();
			} catch (IOException e) {
				// couldn't connect to url
				System.out.println("Error: Could not connect to url \""+p.concat(String.valueOf(j))+"\"");
				e.printStackTrace();
			}
			// grab all service links
			//System.out.println(page.outerHtml());
			Elements serviceLinks = page.getElementsByClass("listElement");
			//System.out.println(serviceLinks);
			ArrayList<Element> serviceURLS = new ArrayList<Element>(); 
			for (int i=0; i< serviceLinks.size();i++) {
				if (serviceLinks.get(i).getElementsByTag("a").first()!= null) {
					serviceURLS.add(serviceLinks.get(i).getElementsByTag("a").first());
				}
			}
			Element link;
			// go to the service page serviceURLS.size()
			for (int i=0;i<serviceURLS.size();i++) {
				link = serviceURLS.get(i);
				//System.out.println(link);
				try {
					// connect to service info page
					Document service = Jsoup.connect(link.absUrl("href")).cookies(cookies).get();
					sD++; //move on counter
					// extract table
					Elements detailsTable = service.getElementsByClass("itemDetails");
					//System.out.println(detailsTable);
					Elements tableRows = detailsTable.get(0).getElementsByTag("tr");
					//System.out.println(tableRows);
					// extract service number
					String serviceNo = tableRows.get(0).text()
							.substring(tableRows.get(0).text()
									.indexOf(":")+1, tableRows.get(0).text().length()).trim();
					// extract name
					String Name = tableRows.get(1).text()
							.substring(tableRows.get(1).text()
									.indexOf(":")+1, tableRows.get(1).text().length()).trim();
					// extract Registration Date
					String regDate = tableRows.get(3).text()
							.substring(tableRows.get(3).text()
									.indexOf(":")+1, tableRows.get(3).text().length()).trim();
					// extract Service Type
					String serviceType = tableRows.get(5).text()
							.substring(tableRows.get(5).text()
									.indexOf(":")+1, tableRows.get(5).text().length()).trim();
					// extract Service Type Detail
					String serviceTypeD = tableRows.get(6).text()
							.substring(tableRows.get(6).text()
									.indexOf(":")+1, tableRows.get(6).text().length()).trim();
					// extract Service Address
					String serviceAddr = tableRows.get(8).text()
							.substring(tableRows.get(8).text()
									.indexOf(":")+1, tableRows.get(8).text().length()).trim();
					// extract Town
					String town = tableRows.get(9).text()
							.substring(tableRows.get(9).text()
									.indexOf(":")+1, tableRows.get(9).text().length()).trim();
					// extract postcode
					String postcode = tableRows.get(10).text()
							.substring(tableRows.get(10).text()
									.indexOf(":")+1, tableRows.get(10).text().length()).trim();
					// extract Telephone
					String telephone = tableRows.get(12).text()
							.substring(tableRows.get(12).text()
									.indexOf(":")+1, tableRows.get(12).text().length()).trim();
					// extract Fax
					String fax = tableRows.get(13).text()
							.substring(tableRows.get(13).text()
									.indexOf(":")+1, tableRows.get(13).text().length()).trim();
					// extract Telephone
					String managersName = tableRows.get(15).text()
							.substring(tableRows.get(15).text()
									.indexOf(":")+1, tableRows.get(15).text().length()).trim();
					// extract Managers Tel
					String managersTel = tableRows.get(16).text()
							.substring(tableRows.get(16).text()
									.indexOf(":")+1, tableRows.get(16).text().length()).trim();
					// extract Managers Email
					String managersEmail = tableRows.get(17).text()
							.substring(tableRows.get(17).text()
									.indexOf(":")+1, tableRows.get(17).text().length()).trim();
					// extract Intending to Cancel
					String intendingCancel = tableRows.get(19).text()
							.substring(tableRows.get(19).text()
									.indexOf(":")+1, tableRows.get(19).text().length()).trim();
					// extract Inspector
					String inspector = tableRows.get(21).text()
							.substring(tableRows.get(21).text()
									.indexOf(":")+1, tableRows.get(21).text().length()).trim();
					// extract Inspectors Office
					String inspectorsOffice = tableRows.get(22).text()
							.substring(tableRows.get(22).text()
									.indexOf(":")+1, tableRows.get(22).text().length()).trim();

					try {
						q.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\","
								+ "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\","
								+ "\"%s\",\"%s\",\"%s\",\"%s\",\n",
								serviceNo,Name,regDate,serviceType,
								serviceTypeD,serviceAddr,town,postcode,
								telephone,fax,managersName,managersTel,
								managersEmail,intendingCancel,inspector,
								inspectorsOffice);
					} catch (Exception e) {
						// couldn't write to file
						System.out.println("Error: Could not write to output file");
					}

					// follow link to Quality grades and scrape table if it exists
					Element qlink = service.getElementById("Quality Grades");
					//System.out.println(qlink.absUrl("href"));
					Document qualityGrades = Jsoup.connect(qlink.absUrl("href")).cookies(cookies).get();
					//System.out.println(qualityGrades.outerHtml());
					Element qg;
					if ((qg = qualityGrades.getElementById("QAFTable")) != null) {
						if (qG == 0) {
							// add name and no. headers
							w.printf("\"%s\",\"%s\",", "Service Number"," Service Name");
							// get table headers
							Element qtableHeader = qg.getElementsByTag("thead").get(0);
							Elements qtableHeaders = qtableHeader.getElementsByTag("th");
							for (int k = 0;k<qtableHeaders.size();k++) {
								if (k==0) {
									w.printf("\"%s\"", qtableHeaders.get(k).text().trim());
								} else {
									w.printf(",\"%s\"", qtableHeaders.get(k).text().trim());
								}
							}
							w.print("\n");
						}
						// get table body
						Element tableBody = qg.getElementsByTag("tbody").first();
						//System.out.print(tableBody.outerHtml());
						// get table rows
						Elements qtableRows = tableBody.getElementsByTag("tr");
						// get data items
						Elements qdata;
						for (int k = 0;k<qtableRows.size();k++) {
							w.printf("\"%s\",\"%s\",",serviceNo,Name);
							qdata = qtableRows.get(k).getElementsByTag("td");
							for (int l =0;l<qdata.size();l++) {
								if (l==0) {
									w.printf("\"%s\"", qdata.get(l).text().trim());
								} else {
									w.printf(",\"%s\"", qdata.get(l).text().trim());
								}
							}
							w.print("\n");
						}
						qG++;
					} 
				} catch (IOException e) {
					// couldn't connect full story url
					System.out.println("Error: Could not connect to url \""+link.absUrl("href")+"\"");
					e.printStackTrace();
				}
			}
			j = j+10;
		}
		// close printwriter
		w.close();
		q.close();
		System.out.printf("Service Details: %d\tQuality Grades: %d\n",sD,qG);
		System.out.println("Scrape successful");
	}
}
