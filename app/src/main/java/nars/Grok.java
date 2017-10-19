/*
 * Copyright 2016 ruckc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nars;

import jdk.nashorn.api.scripting.URLReader;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;


/**
 * {@code Grok} parse arbitrary text and structure it.<br>
 * <p>
 * {@code Grok} is simple API that allows you to easily parse logs
 * and other files (single line). With {@code Grok},
 * you can turn unstructured log and event data into structured data (JSON).
 * <br>
 * example:<br>
 * <pre>
 *  Grok grok = Grok.create("patterns/patterns");
 *  grok.compile("%{USER}");
 *  Match gm = grok.match("root");
 *  gm.captures();
 * </pre>
 *
 * @author anthonycorbacho
 * @since 0.0.1
 */
public class Grok implements Serializable {

    public static void main(String[] args) throws IOException {



        Grok g = Grok.withThe("patterns", "linux-syslog");
        BufferedReader br = new BufferedReader(new FileReader(
                //"/var/log/popularity-contest"
                "/var/log/alternatives.log"
        ));
        br.lines().forEach(line -> {
            System.out.println(line);

            String data = g.discover(line);

            System.out.println(data);
        });

    }

    private static final Logger LOG = LoggerFactory.getLogger(Grok.class);
    /**
     * Named regex of the originalGrokPattern.
     */
    private String namedRegex;
    /**
     * Map of the named regex of the originalGrokPattern
     * with id = namedregexid and value = namedregex.
     */
    private Map<String, String> namedRegexCollection;
    /**
     * Original {@code Grok} pattern (expl: %{IP}).
     */
    private String originalGrokPattern;
    /**
     * Pattern of the namedRegex.
     */
    private Pattern compiledNamedRegex;
    /**
     * {@code Grok} discovery.
     */
    private Discovery disco;
    /**
     * {@code Grok} patterns definition.
     */
    private Map<String, String> grokPatternDefinition;

    /**
     * only use in grok discovery.
     */
    private String savedPattern;


    /**
     * automatic conversion of values
     */
    private boolean automaticConversionEnabled = true;

    /**
     * Create Empty {@code Grok}.
     */
    public static final Grok EMPTY = new Grok();

    /**
     * Create a new <i>empty</i>{@code Grok} object.
     */
    public Grok() {
        originalGrokPattern = StringUtils.EMPTY;
        disco = null;
        namedRegex = StringUtils.EMPTY;
        compiledNamedRegex = null;
        grokPatternDefinition = new TreeMap<String, String>();
        namedRegexCollection = new TreeMap<String, String>();
        savedPattern = StringUtils.EMPTY;
    }

    String getSaved_pattern() {
        return savedPattern;
    }

    void setSaved_pattern(String savedpattern) {
        this.savedPattern = savedpattern;
    }

    /**
     * Create a {@code Grok} instance with the given patterns file and
     * a {@code Grok} pattern.
     *
     * @param grokPatternPath Path to the pattern file
     * @param grokExpression  - <b>OPTIONAL</b> - Grok pattern to compile ex: %{APACHELOG}
     * @return {@code Grok} instance
     * @throws RuntimeException runtime expt
     */
    public static Grok withFile(String grokPatternPath/*, String grokExpression*/)
            throws RuntimeException, FileNotFoundException {
        Grok g = new Grok();
        g.addPatternFrom(grokPatternPath);
        return g;
    }
    public static Grok withReader(Reader reader)
            throws RuntimeException, FileNotFoundException {
        Grok g = new Grok();
        g.addPatterns(reader);
        return g;
    }
    /**
     * Add custom pattern to grok in the runtime.
     *
     * @param name    : Pattern Name
     * @param pattern : Regular expression Or {@code Grok} pattern
     * @throws RuntimeException runtime expt
     **/
    public void addPattern(String name, String pattern) throws RuntimeException {
        if (StringUtils.isBlank(name)) {
            throw new RuntimeException("Invalid Pattern name");
        }
        if (StringUtils.isBlank(pattern)) {
            throw new RuntimeException("Invalid Pattern");
        }
        grokPatternDefinition.put(name, pattern);
    }

    /**
     * Copy the given Map of patterns (pattern name, regular expression) to {@code Grok},
     * duplicate element will be override.
     *
     * @param cpy : Map to copy
     * @throws RuntimeException runtime expt
     **/
    public void addPatterns(Map<String, String> cpy) throws RuntimeException {
        if (cpy == null) {
            throw new RuntimeException("Invalid Patterns");
        }

        if (cpy.isEmpty()) {
            throw new RuntimeException("Invalid Patterns");
        }
        for (Map.Entry<String, String> entry : cpy.entrySet()) {
            grokPatternDefinition.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Get the current map of {@code Grok} pattern.
     *
     * @return Patterns (name, regular expression)
     */
    public Map<String, String> patterns() {
        return grokPatternDefinition;
    }

    /**
     * Get the named regex from the {@code Grok} pattern. <br>
     * See {@link #compile(String)} for more detail.
     *
     * @return named regex
     */
    public String namedRegex() {
        return namedRegex;
    }

    /**
     * Add patterns to {@code Grok} from the given file.
     *
     * @param file : Path of the grok pattern
     * @throws RuntimeException runtime expt
     */
    public Grok addPatternFrom(String file) throws RuntimeException, FileNotFoundException {
        return addPatterns(new FileReader(file));
    }

    /**
     * Add patterns to {@code Grok} from a Reader.
     *
     * @param r : Reader with {@code Grok} patterns
     * @throws RuntimeException runtime expt
     */
    public Grok addPatterns(Reader r) throws RuntimeException {
        BufferedReader br = new BufferedReader(r);
        String line;
        // We dont want \n and commented line
        Pattern pattern = Pattern.compile("^([A-z0-9_]+)\\s+(.*)$");
        try {
            while ((line = br.readLine()) != null) {
                Matcher m = pattern.matcher(line);
                if (m.matches()) {
                    this.addPattern(m.group(1), m.group(2));
                }
            }
            br.close();
        } catch (IOException | RuntimeException e) {
            throw new RuntimeException(e.getMessage());
        }
        return this;
    }

    /**
     * Disable automatic conversion of values
     */
    public void disableAutomaticConversion() {
        this.automaticConversionEnabled = false;
    }

    public boolean isAutomaticConversionEnabled() {
        return automaticConversionEnabled;
    }

    /**
     * Match the given <tt>log</tt> with the named regex.
     * And return the json representation of the matched element
     *
     * @param text : log to match
     * @return json representation og the log
     */
    public Match capture(String text, boolean flattened) {
        Match match = match(text);
        match.captures(flattened);
        return match;
    }



    /**
     * Match the given <tt>text</tt> with the named regex
     * {@code Grok} will extract data from the string and get an extence of {@link Match}.
     *
     * @param text : Single line of log
     * @return Grok Match
     */
    public Match match(String text) {
        if (compiledNamedRegex == null || StringUtils.isBlank(text)) {
            return Match.EMPTY;
        }

        Matcher m = compiledNamedRegex.matcher(text);
        Match match = new Match();
        if (m.find()) {
            match.setSubject(text);
            match.setGrok(this);
            match.setMatch(m);
            match.setStart(m.start(0));
            match.setEnd(m.end(0));
        }
        return match;
    }

    /**
     * Compile the {@code Grok} pattern to named regex pattern.
     *
     * @param pattern : Grok pattern (ex: %{IP})
     * @throws RuntimeException runtime expt
     */
    public void compile(String pattern) throws RuntimeException {
        compile(pattern, false);
    }

    /**
     * Compile the {@code Grok} pattern to named regex pattern.
     *
     * @param pattern   : Grok pattern (ex: %{IP})
     * @param namedOnly : Whether to capture named expressions only or not (i.e. %{IP:ip} but not ${IP})
     * @throws RuntimeException runtime expt
     */
    public void compile(String pattern, boolean namedOnly) throws RuntimeException {

        if (StringUtils.isBlank(pattern)) {
            throw new RuntimeException("{pattern} should not be empty or null");
        }

        namedRegex = pattern;
        originalGrokPattern = pattern;
        int index = 0;
        /** flag for infinite recurtion */
        int iterationLeft = 1000;
        Boolean continueIteration = true;

        // Replace %{foo} with the regex (mostly groupname regex)
        // and then compile the regex
        while (continueIteration) {
            continueIteration = false;
            if (iterationLeft <= 0) {
                throw new RuntimeException("Deep recursion pattern compilation of " + originalGrokPattern);
            }
            iterationLeft--;

            Matcher m = GROK_PATTERN.matcher(namedRegex);
            // Match %{Foo:bar} -> pattern name and subname
            // Match %{Foo=regex} -> add new regex definition 
            if (m.find()) {
                continueIteration = true;
                Map<String, String> group = namedGroups(m, m.group());
                if (group.get("definition") != null) {
                    try {
                        addPattern(group.get("pattern"), group.get("definition"));
                        group.put("name", group.get("name") + "=" + group.get("definition"));
                    } catch (RuntimeException e) {
                        throw new RuntimeException(e);
                    }
                }
                int count = StringUtils.countMatches(namedRegex, "%{" + group.get("name") + "}");
                for (int i = 0; i < count; i++) {
                    String definitionOfPattern = grokPatternDefinition.get(group.get("pattern"));
                    if (definitionOfPattern == null) {
                        throw new RuntimeException(format("No definition for key '%s' found, aborting",
                                group.get("pattern")));
                    }
                    String replacement = String.format("(?<name%d>%s)", index, definitionOfPattern);
                    if (namedOnly && group.get("subname") == null) {
                        replacement = String.format("(?:%s)", definitionOfPattern);
                    }
                    namedRegexCollection.put("name" + index,
                            (group.get("subname") != null ? group.get("subname") : group.get("name")));
                    namedRegex =
                            StringUtils.replace(namedRegex, "%{" + group.get("name") + "}", replacement, 1);
                    // System.out.println(_expanded_pattern);
                    index++;
                }
            }
        }

        if (namedRegex.isEmpty()) {
            throw new RuntimeException("Pattern not fount");
        }
        // Compile the regex
        compiledNamedRegex = Pattern.compile(namedRegex);
    }

    /**
     * {@code Grok} will try to find the best expression that will match your input.
     * {@link Discovery}
     *
     * @param input : Single line of log
     * @return the Grok pattern
     */
    public String discover(String input) {

        if (disco == null) {
            disco = new Discovery(this);
        }
        return disco.discover(input);
    }

    /**
     * Original grok pattern used to compile to the named regex.
     *
     * @return String Original Grok pattern
     */
    public String getOriginalGrokPattern() {
        return originalGrokPattern;
    }

    /**
     * Get the named regex from the given id.
     *
     * @param id : named regex id
     * @return String of the named regex
     */
    public String getNamedRegexCollectionById(String id) {
        return namedRegexCollection.get(id);
    }

    /**
     * Get the full collection of the named regex.
     *
     * @return named RegexCollection
     */
    public Map<String, String> getNamedRegexCollection() {
        return namedRegexCollection;
    }

    public static Grok withThe(String... patternLibs) throws FileNotFoundException {
        Grok g = new Grok();
        for (String s : patternLibs) {
            g.addPatterns(new URLReader(Grok.class.getClassLoader().getResource("patterns/" + s)));
        }
        return g;
    }

    /**
     * {@code Discovery} try to find the best pattern for the given string.
     *
     * @author anthonycorbacho
     * @since 0.0.2
     */
    static class Discovery {

        private Grok grok;

        /**
         * Create a new {@code Discovery} object.
         *
         * @param grok instance of grok
         */
        public Discovery(Grok grok) {
            this.grok = grok;
        }

        /**
         * Sort by regex complexity.
         *
         * @param groks Map of the pattern name and grok instance
         * @return the map sorted by grok pattern complexity
         */
        private Map<String, Grok> sort(Map<String, Grok> groks) {

            List<Grok> groky = new ArrayList<Grok>(groks.values());
            Map<String, Grok> mGrok = new LinkedHashMap<String, Grok>();
            Collections.sort(groky, new Comparator<Grok>() {

                public int compare(Grok g1, Grok g2) {
                    return (this.complexity(g1.namedRegex()) < this.complexity(g2.namedRegex())) ? 1
                            : 0;
                }

                private int complexity(String expandedPattern) {
                    int score = 0;
                    score += expandedPattern.split("\\Q" + "|" + "\\E", -1).length - 1;
                    score += expandedPattern.length();
                    return score;
                }
            });

            for (Grok g : groky) {
                mGrok.put(g.getSaved_pattern(), g);
            }
            return mGrok;

        }

        /**
         * @param expandedPattern regex string
         * @return the complexity of the regex
         */
        private int complexity(String expandedPattern) {
            int score = 0;

            score += expandedPattern.split("\\Q" + "|" + "\\E", -1).length - 1;
            score += expandedPattern.length();

            return score;
        }

        /**
         * Find a pattern from a log.
         *
         * @param text     witch is the representation of your single
         * @param patterns
         * @return Grok pattern %{Foo}...
         */
        public String discover(String text) {
            if (text == null) {
                return "";
            }

            Map<String, Grok> groks = new TreeMap<String, Grok>();
            Map<String, String> gPatterns = grok.patterns();
            // Boolean done = false;
            String texte = text;

            // Compile the pattern
            Iterator<Map.Entry<String, String>> it = gPatterns.entrySet().iterator();
            while (it.hasNext()) {
                @SuppressWarnings("rawtypes")
                Map.Entry pairs = (Map.Entry) it.next();
                String key = pairs.getKey().toString();
                Grok g = new Grok();

                // g.patterns.putAll( gPatterns );
                try {
                    g.addPatterns(gPatterns);
                    g.setSaved_pattern(key);
                    g.compile("%{" + key + "}");
                    groks.put(key, g);
                } catch (RuntimeException e) {
                    // Add logger
                    continue;
                }

            }

            // Sort patterns by complexity
            Map<String, Grok> patterns = this.sort(groks);

            // while (!done){
            // done = true;
            Iterator<Map.Entry<String, Grok>> pit = patterns.entrySet().iterator();
            while (pit.hasNext()) {
                @SuppressWarnings("rawtypes")
                Map.Entry pairs = (Map.Entry) pit.next();
                String key = pairs.getKey().toString();
                Grok value = (Grok) pairs.getValue();

                // We want to search with more complex pattern
                // We avoid word, small number, space....
                if (this.complexity(value.namedRegex()) < 20) {
                    continue;
                }

                Match m = value.match(text);
                if (m.isNull()) {
                    continue;
                }
                // get the part of the matched text
                String part = getPart(m, text);

                // we skip boundary word
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(".\\b.");
                Matcher ma = pattern.matcher(part);
                if (!ma.find()) {
                    continue;
                }

                // We skip the part that already include %{Foo}
                java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile("%\\{[^}+]\\}");
                Matcher ma2 = pattern2.matcher(part);

                if (ma2.find()) {
                    continue;
                }
                texte = StringUtils.replace(texte, part, "%{" + key + "}");
            }
            // }

            return texte;
        }

        /**
         * Get the substring that match with the text.
         *
         * @param m    Grok Match
         * @param text text
         * @return string
         */
        private String getPart(Match m, String text) {

            if (m == null || text == null) {
                return "";
            }

            return text.substring(m.getStart(), m.getEnd());
        }
    }

    /**
     * Convert String argument to the right type.
     *
     * @author anthonyc
     */
    static class Converter {

        public static Map<String, IConverter<?>> converters = new HashMap<String, IConverter<?>>();
        public static Locale locale = Locale.ENGLISH;

        static {
            converters.put("byte", new ByteConverter());
            converters.put("boolean", new BooleanConverter());
            converters.put("short", new ShortConverter());
            converters.put("int", new IntegerConverter());
            converters.put("long", new LongConverter());
            converters.put("float", new FloatConverter());
            converters.put("double", new DoubleConverter());
            converters.put("date", new DateConverter());
            converters.put("datetime", new DateConverter());
            converters.put("string", new StringConverter());

        }

        private static IConverter getConverter(String key) throws Exception {
            IConverter converter = converters.get(key);
            if (converter == null) {
                throw new Exception("Invalid data type :" + key);
            }
            return converter;
        }

        public static KeyValue convert(String key, Object value) {
            String[] spec = key.split(";|:", 3);
            try {
                if (spec.length == 1) {
                    return new KeyValue(spec[0], value);
                } else if (spec.length == 2) {
                    return new KeyValue(spec[0], getConverter(spec[1]).convert(String.valueOf(value)));
                } else if (spec.length == 3) {
                    return new KeyValue(spec[0], getConverter(spec[1]).convert(String.valueOf(value), spec[2]));
                } else {
                    return new KeyValue(spec[0], value, "Unsupported spec :" + key);
                }
            } catch (Exception e) {
                return new KeyValue(spec[0], value, e.toString());
            }
        }
    }


//
// KeyValue
//

    static class KeyValue {

        private String key = null;
        private Object value = null;
        private String grokFailure = null;

        public KeyValue(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        public KeyValue(String key, Object value, String grokFailure) {
            this.key = key;
            this.value = value;
            this.grokFailure = grokFailure;
        }

        public boolean hasGrokFailure() {
            return grokFailure != null;
        }

        public String getGrokFailure() {
            return this.grokFailure;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }


    //
// Converters
//
    @FunctionalInterface  static interface IConverter<T> {

        default T convert(String value, String informat) throws Exception {
            return null;
        }

        public T convert(String value) throws Exception;
    }


    static class ByteConverter implements IConverter<Byte> {
        @Override
        public Byte convert(String value) throws Exception {
            return Byte.parseByte(value);
        }
    }


    static class BooleanConverter implements IConverter<Boolean> {
        @Override
        public Boolean convert(String value) throws Exception {
            return Boolean.parseBoolean(value);
        }
    }


    static class ShortConverter implements IConverter<Short> {
        @Override
        public Short convert(String value) throws Exception {
            return Short.parseShort(value);
        }
    }


    static class IntegerConverter implements IConverter<Integer> {
        @Override
        public Integer convert(String value) throws Exception {
            return Integer.parseInt(value);
        }
    }


    static class LongConverter implements IConverter<Long> {
        @Override
        public Long convert(String value) throws Exception {
            return Long.parseLong(value);
        }
    }


    static class FloatConverter implements IConverter<Float> {
        @Override
        public Float convert(String value) throws Exception {
            return Float.parseFloat(value);
        }
    }


    static class DoubleConverter implements IConverter<Double> {
        @Override
        public Double convert(String value) throws Exception {
            return Double.parseDouble(value);
        }
    }


    static class StringConverter implements IConverter<String> {
        @Override
        public String convert(String value) throws Exception {
            return value;
        }
    }


    static class DateConverter implements IConverter<Date> {
        @Override
        public Date convert(String value) throws Exception {
            return DateFormat.getDateTimeInstance(DateFormat.SHORT,
                    DateFormat.SHORT,
                    Converter.locale).parse(value);
        }

        @Override
        public Date convert(String value, String informat) throws Exception {
            SimpleDateFormat formatter = new SimpleDateFormat(informat, Converter.locale);
            return formatter.parse(value);
        }

    }

    static class Match {

        private String subject; // texte
        private Map<String, Object> capture;
        private Garbage garbage;
        private Grok grok;
        private Matcher match;
        private int start;
        private int end;

        /**
         * For thread safety.
         */
        private static ThreadLocal<Match> matchHolder = ThreadLocal.withInitial(Match::new);

        /**
         * Create a new {@code Match} object.
         */
        public Match() {
            subject = "Nothing";
            grok = null;
            match = null;
            capture = new TreeMap<String, Object>();
            garbage = new Garbage();
            start = 0;
            end = 0;
        }

        /**
         * Create Empty grok matcher.
         */
        public static final Match EMPTY = new Match();

        public void setGrok(Grok grok) {
            if (grok != null) {
                this.grok = grok;
            }
        }

        public Matcher getMatch() {
            return match;
        }

        public void setMatch(Matcher match) {
            this.match = match;
        }

        public int getStart() {
            return start;
        }

        public void setStart(int start) {
            this.start = start;
        }

        public int getEnd() {
            return end;
        }

        public void setEnd(int end) {
            this.end = end;
        }

        /**
         * Singleton.
         *
         * @return instance of Match
         */
        public static Match getInstance() {
            return matchHolder.get();
        }

        /**
         * Set the single line of log to parse.
         *
         * @param text : single line of log
         */
        public void setSubject(String text) {
            if (text == null) {
                return;
            }
            if (text.isEmpty()) {
                return;
            }
            subject = text;
        }

        /**
         * Retrurn the single line of log.
         *
         * @return the single line of log
         */
        public String getSubject() {
            return subject;
        }


        /**
         * Match to the <tt>subject</tt> the <tt>regex</tt> and save the matched element into a map
         * <p>
         * Multiple values to the same key are flattened to one value: the sole non-null value will be captured.
         * Should there be multiple non-null values a RuntimeException is being thrown.
         * <p>
         * This can be used in cases like: (foo (.*:message) bar|bar (.*:message) foo) where the regexp guarantees that only
         * one value will be captured.
         * <p>
         * See also {@link #captures} which returns multiple values of the same key as list.
         */
        public void capturesFlattened() {
            captures(true);
        }

        @SuppressWarnings("unchecked")
        private void captures(boolean flattened) {
            if (match == null) {
                return;
            }
            capture.clear();
            boolean automaticConversionEnabled = true; //grok.isAutomaticConversionEnabled();


            // _capture.put("LINE", this.line);
            // _capture.put("LENGTH", this.line.length() +"");

            Map<String, String> mappedw = namedGroups(this.match, this.subject);
            Iterator<Map.Entry<String, String>> it = mappedw.entrySet().iterator();
            while (it.hasNext()) {

                @SuppressWarnings("rawtypes")
                Map.Entry pairs = (Map.Entry) it.next();
                String key = null;
                Object value = null;
                if (this.grok.getNamedRegexCollectionById(pairs.getKey().toString()) == null) {
                    key = pairs.getKey().toString();
                } else if (!this.grok.getNamedRegexCollectionById(pairs.getKey().toString()).isEmpty()) {
                    key = this.grok.getNamedRegexCollectionById(pairs.getKey().toString());
                }
                if (pairs.getValue() != null) {
                    value = pairs.getValue().toString();


                    if (automaticConversionEnabled) {
                        KeyValue keyValue = Converter.convert(key, value);

                        // get validated key
                        key = keyValue.getKey();

                        // resolve value
                        if (keyValue.getValue() instanceof String) {
                            value = cleanString((String) keyValue.getValue());
                        } else {
                            value = keyValue.getValue();
                        }

//                        // set if grok failure
//                        if (keyValue.hasGrokFailure()) {
//                            capture.put(key + "_grokfailure", keyValue.getGrokFailure());
//                        }
                    }
                }

                if (capture.containsKey(key)) {
                    Object currentValue = capture.get(key);

                    if (flattened) {
                        if (currentValue == null && value != null) {
                            capture.put(key, value);
                        }
                        if (currentValue != null && value != null) {
                            throw new RuntimeException(
                                    format("key '%s' has multiple non-null values, this is not allowed in flattened mode, values:'%s', '%s'",
                                            key,
                                            currentValue,
                                            value));
                        }
                    } else {
                        if (currentValue instanceof List) {
                            ((List<Object>) currentValue).add(value);
                        } else {
                            List<Object> list = new ArrayList<Object>();
                            list.add(currentValue);
                            list.add(value);
                            capture.put(key, list);
                        }
                    }
                } else {
                    capture.put(key, value);
                }

                it.remove(); // avoids a ConcurrentModificationException
            }
        }

        /**
         * remove from the string the quote and double quote.
         *
         * @param value string to pure: "my/text"
         * @return unquoted string: my/text
         */
        private String cleanString(String value) {
            if (value == null) {
                return null;
            }
            if (value.isEmpty()) {
                return value;
            }
            char[] tmp = value.toCharArray();
            if (tmp.length == 1 && (tmp[0] == '"' || tmp[0] == '\'')) {
                value = "";//empty string
            } else if ((tmp[0] == '"' && tmp[value.length() - 1] == '"')
                    || (tmp[0] == '\'' && tmp[value.length() - 1] == '\'')) {
                value = value.substring(1, value.length() - 1);
            }
            return value;
        }


//        /**
//         * Get the json representation of the matched element.
//         * <p>
//         * example: map [ {IP: 127.0.0.1}, {status:200}] will return {"IP":"127.0.0.1", "status":200}
//         * </p>
//         * If pretty is set to true, json will return prettyprint json string.
//         *
//         * @return Json of the matched element in the text
//         */
//        public String toJson(Boolean pretty) {
//            if (capture == null) {
//                return "{}";
//            }
//            if (capture.isEmpty()) {
//                return "{}";
//            }
//
//            this.cleanMap();
//            Gson gs;
//            if (pretty) {
//                gs = PRETTY_GSON;
//            } else {
//                gs = GSON;
//            }
//            return gs.toJson(/* cleanMap( */capture/* ) */);
//        }

//        /**
//         * Get the json representation of the matched element.
//         * <p>
//         * example: map [ {IP: 127.0.0.1}, {status:200}] will return {"IP":"127.0.0.1", "status":200}
//         * </p>
//         *
//         * @return Json of the matched element in the text
//         */
//        public String toJson() {
//            return toJson(false);
//        }

        /**
         * Get the map representation of the matched element in the text.
         *
         * @return map object from the matched element in the text
         */
        public Map<String, Object> toMap() {
            this.cleanMap();
            return capture;
        }

        /**
         * Remove and rename the unwanted elelents in the matched map.
         */
        private void cleanMap() {
            garbage.rename(capture);
            garbage.remove(capture);
        }

        /**
         * Util fct.
         *
         * @return boolean
         */
        public Boolean isNull() {
            return this.match == null;
        }

    }

    /**
     * The Leon the professional of {@code Grok}.<br>
     * Garbage is use by grok to remove or rename elements before getting the final output
     *
     * @author anthonycorbacho
     * @since 0.0.2
     */
    static class Garbage {

        private List<String> toRemove;
        private Map<String, Object> toRename;

        /**
         * Create a new {@code Garbage} object.
         */
        public Garbage() {

            toRemove = new ArrayList<String>();
            toRename = new TreeMap<String, Object>();
            /** this is a default value to remove */
            toRemove.add("UNWANTED");
        }

        /**
         * Set a new name to be change when exporting the final output.
         *
         * @param origin : original field name
         * @param value  : New field name to apply
         */
        public void addToRename(String origin, Object value) {
            if (origin == null || value == null) {
                return;
            }

            if (!origin.isEmpty() && !value.toString().isEmpty()) {
                toRename.put(origin, value);
            }
        }

        /**
         * Set a field to be remove when exporting the final output.
         *
         * @param name of the field to remove
         */
        public void addToRemove(String name) {
            if (name == null) {
                return;
            }

            if (!name.isEmpty()) {
                toRemove.add(name);
            }
        }

        /**
         * Set a list of field name to be remove when exporting the final output.
         *
         * @param lst list of elem to remove
         */
        public void addToRemove(List<String> lst) {
            if (lst == null) {
                return;
            }

            if (!lst.isEmpty()) {
                toRemove.addAll(lst);
            }
        }

        /**
         * Remove from the map the unwilling items.
         *
         * @param map to clean
         * @return nb of deleted item
         */
        public int remove(Map<String, Object> map) {
            int item = 0;

            if (map == null) {
                return item;
            }

            if (map.isEmpty()) {
                return item;
            }

            for (Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Object> entry = it.next();
                for (int i = 0; i < toRemove.size(); i++) {
                    if (entry.getKey().equals(toRemove.get(i))) {
                        it.remove();
                        item++;
                    }
                }
            }
            return item;
        }

        /**
         * Rename the item from the map.
         *
         * @param map elem to rename
         * @return nb of renamed items
         */
        public int rename(Map<String, Object> map) {
            int item = 0;

            if (map == null) {
                return item;
            }

            if (map.isEmpty() || toRename.isEmpty()) {
                return item;
            }

            for (Iterator<Map.Entry<String, Object>> it = toRename.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Object> entry = it.next();
                if (map.containsKey(entry.getKey())) {
                    Object obj = map.remove(entry.getKey());
                    map.put(entry.getValue().toString(), obj);
                    item++;
                }
            }
            return item;
        }

    }

    /**
     * Extract Grok patter like %{FOO} to FOO, Also Grok pattern with semantic.
     */
    public static final java.util.regex.Pattern GROK_PATTERN = java.util.regex.Pattern.compile(
            "%\\{" +
                    "(?<name>" +
                    "(?<pattern>[A-z0-9]+)" +
                    "(?::(?<subname>[A-z0-9_:;\\/\\s\\.]+))?" +
                    ")" +
                    "(?:=(?<definition>" +
                    "(?:" +
                    "(?:[^{}]+|\\.+)+" +
                    ")+" +
                    ")" +
                    ")?" +
                    "\\}");

    public static final java.util.regex.Pattern NAMED_REGEX = java.util.regex.Pattern
            .compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>");

    private static Set<String> getNameGroups(String regex) {
        Set<String> namedGroups = new LinkedHashSet<String>();
        Matcher m = NAMED_REGEX.matcher(regex);
        while (m.find()) {
            namedGroups.add(m.group(1));
        }
        return namedGroups;
    }

    public static Map<String, String> namedGroups(Matcher matcher,
                                                  String namedRegex) {
        Set<String> groupNames = getNameGroups(matcher.pattern().pattern());
        Matcher localMatcher = matcher.pattern().matcher(namedRegex);
        Map<String, String> namedGroups = new LinkedHashMap<String, String>();
        if (localMatcher.find()) {
            for (String groupName : groupNames) {
                String groupValue = localMatcher.group(groupName);
                namedGroups.put(groupName, groupValue);
            }
        }
        return namedGroups;
    }
}