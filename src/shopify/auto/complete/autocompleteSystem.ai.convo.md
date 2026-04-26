# AutocompleteSystem — AI Conversation Log

---

## 1. Enriching the scoring algorithm

**User:** I want to enrich the `getAutocompleteSuggestion` method in `AutocompleteSystem` of the `shopify.auto.complete` package. I want to weigh the matching score by prefix tokenized values and each token matching to the productName tokens. For example, `iph 15` should rank results as: `iphone 15`, `15 iphone`, `iphone`, `15` — so that the most matching by order of each token, frequency of the token, then the first token has the max value (in case of `iphone` and `15 cents`, iphone should come before 15 cents). Give me your implementation plan exactly how you will update the code. Consider human readability — if methods are too long, make them shorter with a clear role for each method, and make sure the method names are obvious.

**AI:** Explored the codebase. Current implementation does a raw `startsWith` on the whole product name string, ignores tokens entirely, never uses `tokenizedProductNames`, and ignores `maxSuggestions`.

Proposed decomposition into five methods, each with one role:

- `getAutocompleteSuggestions` — entry point: tokenize prefix, filter, score-sort, limit
- `hasAnyTokenMatch` — filter predicate: exclude products with zero token overlap
- `matchPrefixTokensToProductTokens` — greedy assignment of prefix tokens to product tokens, scanning all product tokens (not just forward) to allow out-of-order detection
- `computeSequenceScore` — returns 0.0–1.0 for how many consecutive matched pairs are in increasing order
- `scoreProductByTokens` — combines three signals: `matchedCount × 100 + sequenceScore × 10 + firstTokenBonus`

Verified the scoring against the example:

| Product | Score |
|---|---|
| `iphone 15` | 211 |
| `15 iphone` | 201 |
| `iphone` | 111 |
| `15` | 110 |

Result order: `iphone 15` → `15 iphone` → `iphone` → `15` ✓

---

## 2. Writing tests and implementing

**User:** Write the test cases we discussed, plus major happy path and null/blank scenarios in a new class `AutocompleteSuggestionTest.java` with an executable static method. Also: readable clean method names, and all tests must execute regardless of failures. Then implement the enriched `AutocompleteSystem`. Then run the tests.

**AI:** Confirmed the existing pattern from `shopify.shopping.cart.Main` — plain Java `main`-based runner, no JUnit, uses `runTest` / `assertEquals` / `assertThrows` / `ThrowingRunnable`. Followed the same pattern.

Wrote three test sections:
- `testNullAndBlankPrefixValidation` — null, empty string, whitespace-only prefix
- `testBasicTokenMatching` — no match, single match, multiple match, `maxSuggestions` cap
- `testTokenWeightedScoring` — the four scoring scenarios from the plan

Implemented the enriched `AutocompleteSystem`. All 11 tests passed.

---

## 3. Null and empty product list guard

**User:** There's no test case for when the list of product names is null or empty. In this case, just make sure we don't do any operation but return an empty list to save operation cost. Add this test case.

**AI:** Added `testNullAndEmptyProductList` as a new first section. Added two guards in the implementation: constructor converts null to `List.of()` to prevent NPE during tokenization; `getAutocompleteSuggestions` returns early on `productNames.isEmpty()` to skip all filtering and scoring. 13/13 passed.

---

## 4. Mid-token substring match

**User:** I don't see a test case for when `iph` is found in the middle of a token — for example, prefix `iph` and product name `Giphy 10`.

**AI:** Added a test asserting that `Giphy 10` is excluded and `iPhone 15` is the only result. The current `startsWith` logic correctly excludes mid-token substrings — `giphy` does not start with `iph`. The test documents this as an intentional constraint: token-prefix matching only, not substring matching. 14/14 passed.

---

## 5. Three-token prefix and exact match scoring

**User:** Add test cases for: (1) matching score with 3 prefix tokens and product names with up to 4 tokens — prefix `iph 15 ref` against `iphone 15 refurbished`, `iphone 15 open box`, `iphone 14 refurbished`, `iphone 15`. (2) Full token match vs prefix-only match — if prefix is `iphone` and products are `iphone` and `iphone15`, `iphone` should score higher. Add the tests and run to see if this rule lives today.

**AI:** Added both tests and ran them. Result:
- 3-token test: passed — existing match count dominance was already sufficient
- Exact match test: failed — `iphone` and `iphone15` scored identically under the existing formula

Fixed by adding a fourth scoring signal (`countExactTokenMatches`) and rescaling the weights to preserve the priority hierarchy:

| Signal | Old weight | New weight |
|---|---|---|
| `matchedCount` | ×100 | ×1000 |
| `sequenceScore` | ×10 | ×100 |
| `firstTokenBonus` | ×1 | ×10 |
| `exactMatchCount` | — | ×1 |

The ×10 gaps between each level ensure no lower signal can outweigh a higher one. 16/16 passed.

---

## 6. Alphabetical tiebreaker

**User:** When scores tie, I want the final sort to be alphabetical. Prefix `iph`, products `iphone 13`, `iphone 15`, `iphone 19`, `iphon`, `iphone`, `iphone app` should come out as: `iphon`, `iphone`, `iphone 13`, `iphone 15`, `iphone 19`, `iphone app`. Does this seem reasonable?

**AI:** Yes, reasonable — alphabetical tiebreaker is standard and predictable. One clarifying question: should the comparison be case-insensitive to stay consistent with the normalization already in place?

**User:** Your assumption is correct — case-insensitive alphabetical. Add the test and make the corresponding code change.

**AI:** Added `testTiedScoresFallBackToCaseInsensitiveAlphabeticalOrder`. Code change was a single `.thenComparing` chained onto the existing comparator using `String.CASE_INSENSITIVE_ORDER` — a built-in JDK comparator. 17/17 passed.

---

## 7. Performance discussion — 100k to 1M product names

**User:** We iterate prefix tokens and product tokens for all existing products if they have at least one match, which seems expensive. Do you have any suggestions to reduce time complexity for 100k or 1M product names?

**AI:** Identified the root problem: no index — every query is a full table scan.

Current complexity per query: O(N × k × m) for filtering, O(R × k × m) for scoring, O(R log R) for sorting.

Recommended: **pre-expand all token prefixes into a `HashMap` at construction time** — maps each prefix string directly to the list of product indices containing a token with that prefix. Query becomes O(k) hash lookups instead of O(N × k × m) for the filter step.

Explained why this fits the stated constraints:
- Maintainable: zero change to scoring logic, flat structure, no trie nodes or tree traversal
- Pre-computation: all prefix expansion happens once at construction, never at query time
- Time over space: trades memory (~21M entries for 1M products) for near-instant candidate lookup

---

## 8. TreeMap vs pre-computed HashMap — SLA analysis

**User:** Can you compare the time complexity of the TreeMap approach vs pre-computing the product names? Can we use both to maximize performance? If this method needs a 100ms SLA for 1M product names, would it make any difference?

**AI:** Side-by-side comparison:

| | TreeMap | Pre-computed HashMap |
|---|---|---|
| Construction | O(N × m × log V) | O(N × m × L̄) |
| Query — find candidates | O(k × (log V + R_tokens)) | O(k) — one hash lookup per prefix token |
| Space | O(N × m) | O(N × m × L̄) — ~7× more |

Key difference: `TreeMap.subMap()` still iterates over every unique token starting with the prefix to collect product indices. For a common prefix like `i` or `a`, `R_tokens` could be thousands. The HashMap collapses this to a single O(1) lookup because index lists are pre-merged per prefix at construction.

Using both: no value — they solve the same sub-problem. Pick one.

SLA analysis with concrete estimates (JVM, ~10ns per string op):
- **Naive approach**: 1M × 3 × 3 = 9M `startsWith` calls ≈ **360ms — fails the 100ms SLA**
- **TreeMap**: ~0.2ms total — well within SLA
- **HashMap**: ~0.2ms total — well within SLA

Difference matters under concurrent load, not single-query latency. HashMap has a flatter access pattern and fewer cache misses at high throughput. Recommendation: implement the HashMap pre-computation.

---

## 9. Priority queue for top-k selection

**User:** Would a priority queue help on sorting and getting top k if k is reasonably small? Realistically we are looking at 5 to 10 max for k.

**AI:** Yes, but the gain is targeted — it only improves the sort/selection step, not the scoring step.

| Step | After HashMap | After HashMap + heap |
|---|---|---|
| Find candidates | O(k) | O(k) |
| Score candidates | O(R × k × m) | O(R × k × m) — unchanged |
| Sort / select top k | O(R log R) | O(R log k) ≈ O(R) since k is constant |

For R=1000, k=10: sort step goes from ~10,000 to ~3,300 operations — roughly 3× on that step alone. The HashMap index is the high-leverage change (~1000×); the heap is a real but smaller improvement (~3×) on a step that is no longer the bottleneck.

Worth doing: k being fixed at 5–10 is exactly the use case a min-heap is designed for, the implementation is straightforward with Java's `PriorityQueue`, and it fits naturally alongside the HashMap change.

---

## 10. Implementation — HashMap index + min-heap

**User:** Yes — HashMap index + min-heap.

**AI:** Implemented three focused changes, all scoring methods left untouched:

- `buildPrefixToProductIndices` — pre-computes at construction, O(N × m × L̄) one-time cost
- `findCandidateProductIndices` — O(k) hash lookups, replaces the O(N × k × m) full scan; removed `hasAnyTokenMatch`
- `selectTopKByScore` — min-heap of size k

Two deliberate decisions in `selectTopKByScore`:
1. Score cache: each candidate scored exactly once before the heap — without this, the comparator would invoke `scoreProductByTokens` O(log k) times per candidate during heap sift operations
2. Comparator split into a named `Comparator<Integer>` variable — required to preserve generic type information through the `.thenComparing` chain

The heap comparator puts the least desirable candidate at the root (lowest score; alphabetically later for equal scores) so `poll()` always evicts the weakest. Heap drains worst-first so `Collections.reverse()` produces the final best-first order.

All 17 tests passed.
