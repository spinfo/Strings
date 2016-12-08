package modules.matrix;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import common.parallelization.CallbackReceiver;
import modules.CharPipe;
import modules.InputPort;
import modules.ModuleImpl;
import modules.OutputPort;

public class AngAnalysisModule extends ModuleImpl {

	// Identifiers for the term to find contexts of
	public static final String PROPERTYKEY_CONTEXT_WINDOW = "context window";
	public static final String PROPERTYKEY_DELIMITER = "CSV delimiter";
	public static final String PROPERTYKEY_NMOSTFREQ = "n most frequent co-occs";

	// Identifiers for inputs and outputs
	public static final String INPUT_ID_CORPUS = "input corpus";
	public static final String INPUT_ID_TERMS = "input terms";
	public static final String OUTPUT_ID_MATRIX = "output matrix";
	public static final String OUTPUT_ID_META = "output meta data";
	
	private int contextWindow;
	private String delimiter;
	private int nMostFreq;

	public AngAnalysisModule(CallbackReceiver callbackReceiver, Properties properties) throws Exception {
		super(callbackReceiver, properties);

		// the module's name, description and category
		this.getPropertyDefaultValues().put(ModuleImpl.PROPERTYKEY_NAME, "AngAnalysisModule");
		this.setDescription("Runs special analysis methods for the ANG project.");

		// description and default value for the regular expression
		this.getPropertyDescriptions().put(PROPERTYKEY_CONTEXT_WINDOW, "Size of context window in one direction (5 = 10 words)");
		this.getPropertyDefaultValues().put(PROPERTYKEY_CONTEXT_WINDOW, "5");
		this.getPropertyDescriptions().put(PROPERTYKEY_DELIMITER, "CSV delimiter");
		this.getPropertyDefaultValues().put(PROPERTYKEY_DELIMITER, ";");
		this.getPropertyDescriptions().put(PROPERTYKEY_NMOSTFREQ, "use n most frequent co-occurrences (number)");
		this.getPropertyDefaultValues().put(PROPERTYKEY_DELIMITER, "30");

		// setup I/O
		InputPort inputCorpus = new InputPort(INPUT_ID_CORPUS, "Input corpus in text form", this);
		InputPort inputTerms = new InputPort(INPUT_ID_TERMS, "Input terms list, newline separated", this);
		OutputPort outputMatrix = new OutputPort(OUTPUT_ID_MATRIX, "Matrix of non-normalized co-occurrence vectors [CSV]", this);
		OutputPort outputMeta = new OutputPort(OUTPUT_ID_META, "Analysis meta data [plain text]", this);
		
		inputCorpus.addSupportedPipe(CharPipe.class);
		inputTerms.addSupportedPipe(CharPipe.class);
		outputMatrix.addSupportedPipe(CharPipe.class);
		outputMeta.addSupportedPipe(CharPipe.class);

		this.addInputPort(inputCorpus);
		this.addInputPort(inputTerms);
		this.addOutputPort(outputMatrix);
		this.addOutputPort(outputMeta);
	}

	@Override
	public boolean process() throws Exception {
		boolean result = true;

		// Input will be read line by line
		BufferedReader reader = null;
		String line = null;

		// Possible output ports
		OutputPort outMatrix = this.getOutputPorts().get(OUTPUT_ID_MATRIX);
		OutputPort outMeta = this.getOutputPorts().get(OUTPUT_ID_META);
		
		//read terms
		Set<String> terms = new HashSet<String>();
		reader = new BufferedReader(super.getInputPorts().get(INPUT_ID_TERMS).getInputReader());
		while ((line = reader.readLine()) != null)
			terms.add(line);
		
		////read and process corpus
		reader = new BufferedReader(super.getInputPorts().get(INPUT_ID_CORPUS).getInputReader());
		Map<String, Integer> countContexts = new HashMap<String, Integer>();
		LinkedHashMap<String, LinkedHashMap<String, Integer>> coOccMap = new LinkedHashMap<String, LinkedHashMap<String, Integer>>();
		for (String t : terms) countContexts.put(t, 0);
		
		//process corpus documents
		while ((line = reader.readLine()) != null){
			for (String t : terms){
				List<String[]> contexts = trimTextMulti(line, t, contextWindow, false); //get context coOccs
				countContexts.put(t, countContexts.get(t) + contexts.size()); //count contexts
				//add to coOccsMap
				for (String[] context : contexts){
					addToCoOccMap(t, context, coOccMap);
				}
			}
		}
		
		//sort and trim coOcc maps
		for (String key : coOccMap.keySet()){
			sortMapByValue(coOccMap.get(key), false);
			trimMap(coOccMap.get(key), nMostFreq);
		}
		
		//output matrix
		outMatrix.outputToAllCharPipes(coOccurrenceMapToCSV(coOccMap, delimiter.charAt(0)));
		
		//output meta
		outMeta.outputToAllCharPipes(writeContextCountMap(countContexts));
		
		return result;
	}
	

	@Override
	public void applyProperties() throws Exception {
		// Set defaults for properties not yet set
		super.setDefaultsIfMissing();

		// Apply own properties
		String cWindProp = this.getProperties().getProperty(PROPERTYKEY_CONTEXT_WINDOW,
				this.getPropertyDefaultValues().get(PROPERTYKEY_CONTEXT_WINDOW));
		this.contextWindow = cWindProp == null ? 5 : Integer.parseInt(cWindProp);
		
		this.delimiter = this.getProperties().getProperty(PROPERTYKEY_DELIMITER,
				this.getPropertyDefaultValues().get(PROPERTYKEY_DELIMITER));
		
		String nMostFreqProp = this.getProperties().getProperty(PROPERTYKEY_NMOSTFREQ,
				this.getPropertyDefaultValues().get(PROPERTYKEY_NMOSTFREQ));
		this.nMostFreq = nMostFreqProp == null ? 30 : Integer.parseInt(nMostFreqProp);
		
		super.applyProperties();
	}
	
	
	private void addToCoOccMap(String term, String[] coOccs, Map<String, LinkedHashMap<String, Integer>> coOccMap){
		for (String c : coOccs){
			if (coOccs.equals(term))
				continue;
			if (coOccMap.get(term).get(c) == null)
				coOccMap.get(term).put(c, 0);
			//add
			coOccMap.get(term).put(c, coOccMap.get(term).get(c) + 1);
		}
	}
	
	
	private static List<String[]> trimTextMulti(String text, String around, int contextNrOfWords, boolean useSubstrings) {
		List<String[]> out = new ArrayList<String[]>();
		String[] tokens = text.replaceAll("\\P{L}", " ").replaceAll("\\s+", " ").split(" ");

		around = around.toUpperCase();
		int min;
		int max;
		int ind = -1;

		for (int i = ind + 1; i < tokens.length; i++) {
			if (useSubstrings && !tokens[i].toUpperCase().contains(around))
				continue;
			if (!useSubstrings && !tokens[i].equalsIgnoreCase(around))
				continue;
			ind = i;
			min = Math.max(ind - contextNrOfWords, 0);
			max = Math.min(ind + contextNrOfWords + 1, tokens.length);
			out.add(Arrays.copyOfRange(tokens, min, max));
		}

		return out;
	}
	
	
	private String coOccurrenceMapToCSV(LinkedHashMap<String, LinkedHashMap<String, Integer>> map, char delimiter){
		
		////construct joined map
		Map<String, Integer> coOccs = new LinkedHashMap<String, Integer>();
		Map<String, LinkedHashMap<String, Integer>> joined = new LinkedHashMap<String, LinkedHashMap<String, Integer>>();
		//collect all coOccs
		for (Entry<String, LinkedHashMap<String, Integer>> entryTerm : map.entrySet()){
			for (Entry<String, Integer> entryCoOccs : entryTerm.getValue().entrySet()){
				coOccs.put(entryCoOccs.getKey(), 0);
			}
		}
		//prepare joined map
		for (Entry<String, LinkedHashMap<String, Integer>> entryTerm : map.entrySet()){
			joined.put(entryTerm.getKey(), new LinkedHashMap<String,Integer>(coOccs));
		}
		//join
		for (Entry<String, LinkedHashMap<String, Integer>> entryTerm : map.entrySet()){
			for (Entry<String, Integer> entryCoOccs : entryTerm.getValue().entrySet()){
				joined.get(entryTerm.getKey()).put(entryCoOccs.getKey(), entryCoOccs.getValue());
			}
		}
		
		//cleanup
		map = null;
		
		////construct CSV
		StringBuilder sb = new StringBuilder();
		//header
		for (Entry<String,Integer> entrycoOccs : coOccs.entrySet())
			sb.append(delimiter + entrycoOccs.getKey());
		sb.append("\n");
		
		//data
		for (Entry<String, LinkedHashMap<String, Integer>> joinedTerm : joined.entrySet()){
			sb.append(joinedTerm.getKey());
			for (Entry<String, Integer> joinedCoOccs : joinedTerm.getValue().entrySet()){
				sb.append(delimiter + joinedCoOccs.getValue());
			}
			sb.append("\n");
		}
		sb.deleteCharAt(sb.length() - 1);
		
		return sb.toString();
	}
	
	
	private <K, V extends Comparable<? super V>> Map<K, V> sortMapByValue(Map<K, V> map, final boolean ascending) {
		List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			@Override
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				if (ascending)
					return (o1.getValue()).compareTo(o2.getValue());
				else
					return (o2.getValue()).compareTo(o1.getValue());
			}
		});

		Map<K, V> result = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
	
	
	private LinkedHashMap<String, Integer> trimMap(LinkedHashMap<String, Integer> map, int n){
		int count = 0;
		for (Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator(); it.hasNext(); ) {
			it.next();
			if (count < n) {
				count++;
			} else {
				it.remove();
			}
		}
		return map;
	}
	
	
	private String writeContextCountMap(Map<String, Integer> map) {
		StringBuilder sb = new StringBuilder();
		for (String string : map.keySet())
			sb.append(string + " \t " + map.get(string) + "\n");
		return sb.toString();
	}
	
}
