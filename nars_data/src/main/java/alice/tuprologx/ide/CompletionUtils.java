package alice.tuprologx.ide;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.fife.ui.autocomplete.*;


import alice.tuprolog.event.TheoryEvent;
import alice.tuprolog.event.TheoryListener;

class CompletionUtils{

	public static final int THEORY_COMPLETIONS_RELEVANCE = 300;
	public static final int DOCS_COMPLETIONS_RELEVANCE = 200;
	public static final int TEMPLATE_COMPLETIONS_RELEVANCE = 100;
	
    public static DefaultCompletionProvider createCompletionProvider() {
    	DefaultCompletionProvider provider = new DefaultCompletionProvider();

    	// Keyword completions
        AddKeywordCompletion(provider, "fail", null);
        AddKeywordCompletion(provider, "halt", null);
        AddKeywordCompletion(provider, "true", null);

        // Built-ins and libraries
        try {
            InputStream builtinsDocs = CompletionUtils.class.getClassLoader().getResourceAsStream("alice/tuprologx/ide/docs.txt");
			createCompletionsFromDocs(provider, builtinsDocs);
		} catch (Exception e) {
			e.printStackTrace();
		}
        
        // Additional constructs
        TemplateCompletion ifThenElse = new TemplateCompletion(
        	provider, "if-then-else", "if-then-else [template]",
        	"( ${IfCondition} ->"
        	+ "\n\t${cursor}Then"
        	+ "\n;"
        	+ "\n\tElse"
        	+ "\n)",
        	null, "if-then-else statement");
        ifThenElse.setRelevance(TEMPLATE_COMPLETIONS_RELEVANCE);
        provider.addCompletion(ifThenElse);

        TemplateCompletion repeatUntil = new TemplateCompletion(
            	provider, "repeat-until", "repeat-until [template]",
            	"repeat,"
            	+ "\n\t${cursor}Action,"
            	+ "\n${Condition},",
            	null, "Repeat action until the condition holds.");
        repeatUntil.setRelevance(TEMPLATE_COMPLETIONS_RELEVANCE);
        provider.addCompletion(repeatUntil);

        return provider;
    }

    public static void createCompletionsFromDocs(AbstractCompletionProvider provider,
			InputStream builtinsDocs) {
		BufferedReader br = new BufferedReader(new InputStreamReader(builtinsDocs));
		String line;
		try {
			String library = null;
			String template = null;
			StringBuilder docText = new StringBuilder();
			
			HashMap<AbstractCompletion, String> addedCompletionsToLibraryName = new HashMap<AbstractCompletion, String>();
			
			while ((line = br.readLine()) != null) {
				if (!line.isEmpty()) {
					if (line.startsWith("Library: ")) {
						library = line.replace("Library: ", "");
						continue;
					}
					if (line.startsWith("Template: "))
						template = line.replace("Template: ", "").replace(" ", "");
					else
						docText.append(line);
				} else {
					if (template != null) {
						// Process and reset for the next doc
						String name = template.split("\\(")[0];
						String[] params = (template.length() > name.length()+2) ? // name() 
								template.replace(name + "(", "").replace(")", "").split(",")
								: new String[0];

						AbstractCompletion c = AddPredicateCompletion(provider, 
								name, library, docText.toString(), DOCS_COMPLETIONS_RELEVANCE, params);
						addedCompletionsToLibraryName.put(c, library);
						
						docText.setLength(0);
						template = null;
					}
				}
			}

			br.close();
			
			// Set relevance value based on library names to enforce sorting by library
			List<String> libraryNames = new ArrayList<String>(new HashSet<String>(addedCompletionsToLibraryName.values()));
			Collections.sort(libraryNames);
			Collections.reverse(libraryNames);
			for(Entry<AbstractCompletion, String> e : addedCompletionsToLibraryName.entrySet()){
				e.getKey().setRelevance(DOCS_COMPLETIONS_RELEVANCE + libraryNames.indexOf(e.getValue()));
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
    public static Completion AddKeywordCompletion(AbstractCompletionProvider provider, String name, String doc){
    	Completion completion = new BasicCompletion(provider, name, null, doc);
    	provider.addCompletion(completion);
    	
    	return completion;
    }
    
    public static AbstractCompletion AddPredicateCompletion(AbstractCompletionProvider provider, String name, String category, String doc, int relevance, String... params){
    	// Prepare the template
    	StringBuilder templateBuilder = new StringBuilder();
    	templateBuilder.append(name.replace("$", "$$"));
    	if(params.length > 0){
    		templateBuilder.append("(");
    		for(String param : params){
    			templateBuilder.append("${");
    			templateBuilder.append(param);
    			templateBuilder.append("}, ");
    		}
    		templateBuilder.setLength(templateBuilder.length()-2); // remove the last ", "
    		templateBuilder.append(")");
    	}
    	
		templateBuilder.append("${cursor}");

		String displayedCompletionText = String.format("%s/%d%s", 
				name, params.length, category == null ? "" : " [" + category +"]");
		TemplateCompletion completion = new TemplateCompletion(
    		provider, name, displayedCompletionText, templateBuilder.toString(), null,
    		doc.replace("Exception:", "<br><br>Exception:"));
    	
		completion.setRelevance(relevance);
		provider.addCompletion(completion);
		
    	return completion;
	}
}

class CompletionUpdateTheoryListener implements TheoryListener {
	private final LinkedList<Completion> addedTheoryCompletions = new LinkedList<Completion>();
	private final DefaultCompletionProvider commonCompletionProvider;

	CompletionUpdateTheoryListener(DefaultCompletionProvider commonCompletionProvider) {
		this.commonCompletionProvider = commonCompletionProvider;
	}

	@Override
	public void theoryChanged(TheoryEvent e) {
		// Simply remove old theory completions and add new ones - can be optimized by replacing deltas
		for(Completion oldCompletion : addedTheoryCompletions)
			commonCompletionProvider.removeCompletion(oldCompletion);
		
		addedTheoryCompletions.clear();
		
		// Create new completions: a naive implementation based on string processing for now...
		String newTheory = e.getNewTheory().toString();
		String[] lines = newTheory.split("\n");
		for(String line : lines){
			if(line.length()>0 && Character.isLetter(line.charAt(0)) && line.replace(" ", "").contains("):-")){
				String newTerm = line.replace(" ", "").split("\\)")[0];
				String name = newTerm.split("\\(")[0];
				// Skip commented lines
				if(name.contains("%"))
					continue;
				
				// Skip already added
				boolean alreadyAdded = false;
				for(Completion c : addedTheoryCompletions){
					if(c.getInputText().equals(name)){
						alreadyAdded = true;
						break;
					}
				}
				if(alreadyAdded)
					continue;
				
				String[] params = newTerm.replace(name + "(", "").replace(")", "").split(",");

				Completion newCompletion = CompletionUtils.AddPredicateCompletion(commonCompletionProvider, 
						name, null,	"User defined: " + newTerm + ")", CompletionUtils.THEORY_COMPLETIONS_RELEVANCE, params);

				addedTheoryCompletions.add(newCompletion);
			}
		}
	}
}