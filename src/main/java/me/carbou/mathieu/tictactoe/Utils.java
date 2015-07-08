/**
 * Copyright (C) 2015 Mathieu Carbou (mathieu@carbou.me)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.carbou.mathieu.tictactoe;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import java.util.List;
import java.util.Locale;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class Utils {

    public static Locale getBestLocale(ContainerRequestContext request) {
        String header = request.getHeaderString(HttpHeaders.ACCEPT_LANGUAGE);
        if (header == null) {
            header = Locale.US.toLanguageTag();
        }
        // IE10 can send: Accept-Language: en-ca,en-us;q=05
        header = header.replaceAll("(q=\\d)(\\d)", "$1.$2");
        List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(header);
        return Locale.forLanguageTag(ranges.get(0).getRange());
    }

}
