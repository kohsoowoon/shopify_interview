package shopify.auto.complete;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class AutocompleteSystem {
    private final List<List<String>> tokenizedProductNames;
    private final List<String> productNames;
    private final Map<String, List<Integer>> prefixToProductIndices;

    public AutocompleteSystem(List<String> productNames) {
        this.productNames = productNames != null ? productNames : List.of();
        this.tokenizedProductNames = this.productNames.stream().map(this::normalizeString).toList();
        this.prefixToProductIndices = buildPrefixToProductIndices(this.tokenizedProductNames);
    }

    public List<String> getAutocompleteSuggestions(String prefix, int maxSuggestions) {
        if (productNames.isEmpty()) {
            return List.of();
        }
        if (prefix == null || prefix.isBlank()) {
            return List.of();
        }

        List<String> prefixTokens = normalizeString(prefix);
        Set<Integer> candidates = findCandidateProductIndices(prefixTokens);

        if (candidates.isEmpty()) {
            return List.of();
        }

        return selectTopKByScore(candidates, prefixTokens, maxSuggestions);
    }

    // Pre-computes every token prefix across all products at construction time.
    // Maps each prefix string → list of product indices containing a token with that prefix.
    // Pays the cost once so each query does O(1) lookup per prefix token instead of O(N×m).
    private Map<String, List<Integer>> buildPrefixToProductIndices(List<List<String>> tokenizedNames) {
        Map<String, List<Integer>> index = new HashMap<>();
        for (int i = 0; i < tokenizedNames.size(); i++) {
            for (String token : tokenizedNames.get(i)) {
                for (int len = 1; len <= token.length(); len++) {
                    index.computeIfAbsent(token.substring(0, len), k -> new ArrayList<>()).add(i);
                }
            }
        }
        return index;
    }

    // Returns the union of product indices matching any prefix token.
    // O(k) hash lookups — replaces the O(N×k×m) full scan.
    private Set<Integer> findCandidateProductIndices(List<String> prefixTokens) {
        Set<Integer> candidates = new HashSet<>();
        for (String prefixToken : prefixTokens) {
            List<Integer> matches = prefixToProductIndices.get(prefixToken);
            if (matches != null) {
                candidates.addAll(matches);
            }
        }
        return candidates;
    }

    // Scores each candidate exactly once, then uses a min-heap of size k to select the top-k
    // in O(R log k) rather than O(R log R). Since k is fixed at 5-10, log k is effectively constant.
    // The heap root is always the least desirable candidate kept so far:
    //   lowest score, or alphabetically latest name for equal scores.
    // When the heap exceeds k, the root is evicted, leaving only the top k survivors.
    private List<String> selectTopKByScore(Set<Integer> candidates, List<String> prefixTokens, int k) {
        Map<Integer, Double> scoreCache = new HashMap<>(candidates.size());
        for (int i : candidates) {
            scoreCache.put(i, scoreProductByTokens(tokenizedProductNames.get(i), prefixTokens));
        }

        Comparator<Integer> byScore = Comparator.comparingDouble((Integer i) -> scoreCache.get(i));
        Comparator<Integer> byScoreThenAlpha = byScore
                .thenComparing(i -> productNames.get(i), String.CASE_INSENSITIVE_ORDER.reversed());

        PriorityQueue<Integer> minHeap = new PriorityQueue<>(k + 1, byScoreThenAlpha);

        for (int candidateIndex : candidates) {
            minHeap.offer(candidateIndex);
            if (minHeap.size() > k) {
                minHeap.poll();
            }
        }

        // Heap drains worst-first; reverse so the final list is best-first.
        List<String> result = new ArrayList<>(minHeap.size());
        while (!minHeap.isEmpty()) {
            result.add(productNames.get(minHeap.poll()));
        }
        Collections.reverse(result);
        return result;
    }

    // Combines four signals into a single score that preserves their priority order:
    //   1. matchedCount    (×1000) — more prefix tokens matched always wins
    //   2. sequenceScore   (×100)  — in-order matches beat out-of-order at equal match count
    //   3. firstTokenBonus (×10)   — matching the first prefix token breaks remaining ties
    //   4. exactMatchCount (×1)    — exact token equality beats a prefix-only hit at equal standing
    private double scoreProductByTokens(List<String> productTokens, List<String> prefixTokens) {
        Map<Integer, Integer> prefixToProductMatch = matchPrefixTokensToProductTokens(productTokens, prefixTokens);

        int matchedCount = prefixToProductMatch.size();
        double sequenceScore = computeSequenceScore(prefixToProductMatch);
        double firstTokenBonus = prefixToProductMatch.containsKey(0) ? 1.0 : 0.0;
        int exactMatchCount = countExactTokenMatches(prefixToProductMatch, productTokens, prefixTokens);

        return matchedCount * 1000.0 + sequenceScore * 100.0 + firstTokenBonus * 10.0 + exactMatchCount;
    }

    // Counts how many matched prefix→product token pairs are an exact equality (not just startsWith).
    private int countExactTokenMatches(Map<Integer, Integer> prefixToProductMatch,
                                       List<String> productTokens, List<String> prefixTokens) {
        int count = 0;
        for (Map.Entry<Integer, Integer> entry : prefixToProductMatch.entrySet()) {
            if (productTokens.get(entry.getValue()).equals(prefixTokens.get(entry.getKey()))) {
                count++;
            }
        }
        return count;
    }

    // Greedily assigns each prefix token to the first unused product token it startsWith-matches.
    // Scanning all product tokens (not just forward from the last match) ensures out-of-order
    // matches are found, e.g. prefix ["iph","15"] against product ["15","iphone"] → {0→1, 1→0}.
    private Map<Integer, Integer> matchPrefixTokensToProductTokens(
            List<String> productTokens, List<String> prefixTokens) {
        Set<Integer> usedProductIndices = new HashSet<>();
        Map<Integer, Integer> prefixIndexToProductIndex = new LinkedHashMap<>();

        for (int pi = 0; pi < prefixTokens.size(); pi++) {
            for (int ti = 0; ti < productTokens.size(); ti++) {
                if (!usedProductIndices.contains(ti)
                        && productTokens.get(ti).startsWith(prefixTokens.get(pi))) {
                    prefixIndexToProductIndex.put(pi, ti);
                    usedProductIndices.add(ti);
                    break;
                }
            }
        }

        return prefixIndexToProductIndex;
    }

    // Returns a [0.0, 1.0] score representing how many consecutive matched pairs appear
    // in increasing product-index order. 1.0 means fully sequential; 0.0 means fully reversed.
    // A single match is treated as perfectly sequential (1.0).
    private double computeSequenceScore(Map<Integer, Integer> prefixToProductMatch) {
        if (prefixToProductMatch.size() < 2) {
            return 1.0;
        }

        List<Integer> matchedProductIndices = prefixToProductMatch.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();

        long inOrderPairs = 0;
        for (int i = 1; i < matchedProductIndices.size(); i++) {
            if (matchedProductIndices.get(i) > matchedProductIndices.get(i - 1)) {
                inOrderPairs++;
            }
        }

        return (double) inOrderPairs / (matchedProductIndices.size() - 1);
    }

    private List<String> normalizeString(String str) {
        if (str == null) {
            return null;
        }
        return Arrays.stream(str.trim().toLowerCase().split("\\s+")).toList();
    }
}
