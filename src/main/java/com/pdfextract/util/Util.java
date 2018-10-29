package com.pdfextract.util;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.pdfextract.common.ExtractStrategy;
import com.pdfextract.common.Layout;
import com.pdfextract.common.LayoutExtractor;
import com.pdfextract.common.LayoutExtractorDetails;
import com.pdfextract.common.Section;
import com.pdfextract.common.TableDetail;
import com.pdfextract.common.regex.RegexCommonUtil;

import lombok.val;

public class Util {

	public static String extractCsvFromPdfExtract(InputStream in, List<String> tables, String layoutStr) throws Exception {
		try (PDDocument pdfDocument = PDDocument.load(in)) {
			return extractCsvFromPdfExtract(pdfDocument, tables, layoutStr);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	public static String extractCsvFromPdfExtract(PDDocument pdfDocument, List<String> tables, String layoutStr) throws Exception {
		try (StringWriter sw = new StringWriter();
				BufferedWriter writer = new BufferedWriter(sw);
				CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);) {
			csvData(pdfDocument, tables, layoutStr, csvPrinter);
			return sw.toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";

	}

	public static String extractJsonFromPdfExtract(InputStream in, List<String> tables, String layoutStr) throws Exception {
		try (PDDocument pdfDocument = PDDocument.load(in)) {
			return extractJsonFromPdfExtract(pdfDocument, tables, layoutStr);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	public static String extractJsonFromPdfExtract(PDDocument pdfDocument, List<String> tables, String layoutStr) throws Exception {
		JSONArray arr = new JSONArray();
		JSONObject root = new JSONObject();
		arr.add(root);
		root.put("extraction_method", "lattice");
		root.put("top", "506.75195");
		root.put("left", "72.06707");
		root.put("width", "221.339080810547");
		root.put("height", "64.64892578125");
		JSONArray data = jsonData(pdfDocument, tables, layoutStr);

		root.put("data", data);
		root.put("spec_index", "0");
		return arr.toJSONString(); 
		}

	private static String csvData(PDDocument pdfDocument, List tables, String layoutStr, CSVPrinter csvPrinter) throws Exception {
		try {
			val m = new ObjectMapper();
			val layout = m.readValue(layoutStr, Layout.class);

			val arrItem = new ArrayList<String>();

			List sections = getHeaders(tables, layout);

			for (Object columnHeader : sections) {
				arrItem.add((String) columnHeader);
			}

			csvPrinter.printRecord(arrItem);

			// get the data before RegEx
			List<String[]> ss1 = extractData(pdfDocument, tables, layout);

			// Apply RegEx IF ANY
			val ss = RegexCommonUtil.applyRegex(ss1, 0, layout);

			System.out.println("csvData ss.size()=" + ss.size() + "tables.size()=" + tables.size());

			for (int i = 0; i < ss.size(); i++) {
				val arrItem1 = new ArrayList<String>();
				String[] row = ss.get(i);
				System.out.println("row.length=" + row.length);
				for (int j = 0; j < row.length; j++) {
					if (row[j] != null && !row[j].trim().equals("")) {
						arrItem1.add(row[j].replace("\n", " ").replace("\r", " "));
					} else {
						arrItem1.add("no data");
					}
				}
				csvPrinter.printRecord(arrItem1);
			}
			csvPrinter.flush();
		} catch (IOException | InstantiationException | IllegalAccessException | ClassNotFoundException
				| ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private static JSONArray jsonData(PDDocument pdfDocument, List tables, String layoutStr) throws Exception {
		try {
			val m = new ObjectMapper();
			val layout = m.readValue(layoutStr, Layout.class);

			val data = new JSONArray();

			val arrItem = new JSONArray();

			List sections = getHeaders(tables, layout);

			for (Object columnHeader : sections) {
				JSONObject header = new JSONObject();
				header.put("top", "");
				header.put("left", "");
				header.put("width", "");
				header.put("height", "");
				header.put("text", columnHeader);
				arrItem.add(header);
			}

			data.add(arrItem);

			List<String[]> ss1 = extractData(pdfDocument, tables, layout);
			val ss = RegexCommonUtil.applyRegex(ss1, 0, layout);
			System.out.println("jsonData ss.size()=" + ss.size() + "tables.size()=" + tables.size());

			for (int i = 0; i < ss.size(); i++) {
				JSONArray arrItem1 = new JSONArray();
				String[] row = ss.get(i);
				for (int j = 0; j < row.length; j++) {
					JSONObject item = new JSONObject();
					item.put("top", "");
					item.put("left", "");
					item.put("width", "");
					item.put("height", "");
					if (row[j] != null && !row[j].trim().equals("")) {
						item.put("text", row[j].replace("\n", " ").replace("\r", " "));
					} else {
						item.put("text", "no data");
					}

					arrItem1.add(item);
				}
				data.add(arrItem1);
			}

			return data;
		} catch (IOException | ParseException | InstantiationException | IllegalAccessException
				| ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private static List getHeaders(List tables, Layout layout) throws ParseException {
		// TODO Auto-generated method stub
		if (layout.getHeaders() != null) {
			return Arrays.asList(layout.getHeaders());
		} else {
			List lst = new LinkedList();
			List tabList = Arrays.stream(layout.getSections()).filter(x -> x.getIsTabular() == true)
					.collect(Collectors.toList());

			if (tables.size() > 0 && tabList.size() > 0) {
				TableDetail td = new TableDetail((LinkedHashMap) tables.get(0));
				List columns = td.getColumns();
				for (Section s : layout.getSections()) {
					lst.add(s.getName());
					if (s.getIsTabular()) {
						lst.addAll(columns);
					}
				}
				return lst;
			} else {
				return Arrays.stream(layout.getSections()).map(Section::getName).collect(Collectors.toList());
			}
		}
	}

	private static List<String[]> extractData(PDDocument pdfDocument, List tables, Layout layout)
			throws Exception {
		System.out.println("*************************LOADING CHUNKS FROM DOCUMENT*************************");
		LayoutExtractor layoutExtractor = (LayoutExtractor) Class.forName("com.pdfextract.common.LapdfExtractStrategy").newInstance();
		val layoutSections = layoutExtractor.extractData(pdfDocument, layout);
		layoutSections.forEach(chunk -> {
			System.out.println(chunk);
		});
		val extractionStrategy = (ExtractStrategy) Class.forName(layout.getExtractStrategyDetails().getExtractStrategy())
				.newInstance();
		val sections = layout.getSections();
		val ss = extractionStrategy.extractData(layoutSections, layout);
		System.out.println("extractData ss.size()=" + ss.size() + "tables.size()=" + tables.size());

		int count = sections.length;

		if (tables.size() > 0) {
			count = getCount((LinkedHashMap) tables.get(0), sections);
		}

		val list = new LinkedList<String[]>();
		val tabList = new LinkedList<String[]>();

		for (int i = 0; i < ss.size(); i++) {
			String[] row = ss.get(i);
			val arrItem1 = new String[count];
			int index = 0;
			for (int j = 0; j < row.length; j++) {
				Section s = sections[j];

				if (row[j] != null) {
					arrItem1[index] = row[j].replace("\n", " ").replace("\r", " ");
				} else {
					arrItem1[index] = " ";
				}
				if (s.getIsTabular()) {
					// getJsonDataString(tables, i, arrItem1, j);
					if (i < tables.size()) {
						TableDetail td = new TableDetail((LinkedHashMap) tables.get(i));
						List<String[]> contents = td.getContent();
						int firstRow = 0;
						int indexOfTabularData = index + 1;
						for (String[] row1 : contents) {

							if (firstRow == 0) {
								for (int ii = 0; ii < row1.length; ii++) {
									arrItem1[indexOfTabularData++] = row1[ii];
								}
								indexOfTabularData = index + 1;
							} else {
								val arrItem2 = new String[count];
								for (int ii = 0; ii < row1.length; ii++) {
									arrItem2[indexOfTabularData++] = row1[ii];
								}
								tabList.add(arrItem2);
								indexOfTabularData = index + 1;
							}
							firstRow++;
						}
						index += td.getColumns().size();
					}
				}
				index++;
			}

			list.add(arrItem1);
			if (tabList.size() > 0) {
				list.addAll(tabList);
				tabList.clear();
			}
		}
		return list;
	}

	public static int getCount(LinkedHashMap tableData, Section[] sections) throws ParseException {

		TableDetail td = new TableDetail(tableData);

		int cnt = td.getColumns().size();
		int index = 0;

		for (int i = 0; i < sections.length; i++) {
			if (sections[i].getIsTabular()) {
				index += cnt;
			}
			index++;
		}

		return index;
	}
}
