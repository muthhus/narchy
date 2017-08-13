/*
 * Copyright 2016 Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>.
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
package br.ufpr.gres.util;

/**
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class StringUtils {

    public static String join(final Iterable<String> strings,
            final String separator) {
        final StringBuilder sb = new StringBuilder();
        String sep = "";
        for (final String s : strings) {
            sb.append(sep).append(s);
            sep = separator;
        }
        return sb.toString();
    }

    public static String newLine() {
        return System.getProperty("line.separator");
    }

    public static String separatorLine(final char c) {
        return repeat(c, 80);
    }

    public static String separatorLine() {
        return repeat('-', 80);
    }

    public static String repeat(final char c, final int n) {
        return new String(new char[n]).replace('\0', c);
    }

    public static void escapeBasicHtmlChars(final String s,
            final StringBuilder out) {

        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if ((c < 32) || (c > 127) || (c == 38) || (c == 39) || (c == 60)
                    || (c == 62) || (c == 34)) {
                out.append('&');
                out.append('#');
                out.append((int) c);
                out.append(';');
            } else {
                out.append(c);
            }
        }
    }

}
