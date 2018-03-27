/*
 * Copyright 2018 Expedia, Inc.
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 *
 */
package com.expedia.www.haystack.pipes.secretDetector.com.expedia.www.haystack.pipes.secretDetector.actions;

import io.dataapps.chlorine.finder.Finder;
import io.dataapps.chlorine.pattern.RegexFinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
/*
 * Finds IP V4 addresses, but ignores those in the 10.0.0.0/8 and 192.168.0.0/16 ranges.
 */
public class NonLocalIpV4AddressFinder implements Finder {
    private final Finder ipvr4Finder = new RegexFinder("IPV4", "(?:[0-9]{1,3}\\.){3}[0-9]{1,3}");
    private final Finder tenDotFinder = new RegexFinder("IPV4", "(?:^10\\.)");
    private final Finder one92Dot168Finder = new RegexFinder("IPV4", "(?:^192\\.168\\.)");

    @Override
    public String getName() {
        return "NonLocalIpV4AddressFinder";
    }

    @Override
    public List<String> find(Collection<String> inputs) {
        final List<String> strings = new ArrayList<>();
        for (String input : inputs) {
            strings.addAll(find(input));
        }
        return strings;
    }

    @Override
    public List<String> find(String input) {
        final List<String> strings = ipvr4Finder.find(input);
        if(!strings.isEmpty()) {
            if(!tenDotFinder.find(input).isEmpty() || !one92Dot168Finder.find(input).isEmpty()) {
                return Collections.emptyList();
            }
        }
        return strings;
    }
}
