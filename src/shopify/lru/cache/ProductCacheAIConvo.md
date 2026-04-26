# ProductCache AI Conversation Log

## Topic
Implementing an LRU cache for product details in Java, incrementally from design to tested implementation.

---

## Q: What is the common data structure for an LRU cache?

**A:** Two structures combined:
- **HashMap** — O(1) key lookup
- **Doubly Linked List** — O(1) eviction (tail) and promotion (head)

Java's `LinkedHashMap` provides this out of the box. The conversation confirmed `LinkedHashMap` was the target.

---

## Q: Set up ProductCache class with LinkedHashMap and get/put stubs

**Decision:** Use `LinkedHashMap<String, ProductDetail>` where the key is `productId`.

**Dependency resolution path:**
- Started with `JsonNode` (Jackson) — unresolved, no build file
- Tried `org.json.JSONObject` — also unresolved, plain IntelliJ module with no Maven/Gradle
- Settled on `Object` initially, then defined a proper `ProductDetail` inner class

**ProductDetail fields:**
- `String productId`
- `Object productMetadata` — kept flexible since no concrete requirement existed at the time

---

## Q: Define all null/empty guard cases — what should happen?

**Agreed behavior:**
- `get` with null or blank productId → return `null`
- `get` on missing productId → return `null`
- `put` with null or blank productId → do nothing
- `put` with null productDetail → do nothing

**Test file created:** `ProductCacheEmptyTest.java`

---

## Q: Test runner setup — all tests must run regardless of pass/fail

**Solution:** Wrapped each test in a try-catch on `AssertionError` inside a shared `TestRunner.run()` utility. Output prints `PASS` or `FAIL` with message per test.

---

## Q: Separate test entry point and test classes

**Structure agreed:**
- `TestRunner.java` — shared `run(name, Runnable)` utility
- Each test class exposes a static `runAll()` method
- `ProductCacheTestMain.java` — single entry point, calls each class's `runAll()` in sequence

---

## Q: Implementation plan for get and put

**Plan summary:**

| Step | Detail |
|---|---|
| Constructor | `LinkedHashMap(capacity, 0.75f, true)` — `accessOrder=true` enables automatic LRU promotion on access |
| `get` | Null/blank guard, then `cache.get()` — side effect promotes entry to MRU |
| `put` | Null/blank/null-detail guards; if key exists: overwrite + promote to MRU via remove+put; if at capacity: evict LRU via `iterator().next()`, then insert |
| Thread safety | `synchronized` on both methods — `get` mutates internal order so read lock is insufficient; `synchronized` is correct and simpler |

---

## Q: Create ProductCacheGetTest and ProductCachePutTest

**ProductCacheGetTest cases:**
1. Returns product detail when productId exists
2. Returns null when productId does not exist
3. `get` updates access order — accessed product is not evicted first

**ProductCachePutTest cases (initial):**
1. Adds product when below capacity
2. Does not overwrite existing product detail *(later corrected — see below)*
3. Updates access order for existing productId
4. Evicts LRU when at capacity
5. Evicts correctly when capacity is 1
6. Fills to capacity without eviction

---

## Requirement clarification: put should overwrite with latest data

**Original assumption:** put on existing key is a no-op (keep original).
**Corrected requirement:** put on existing key should overwrite with the latest `productDetail` AND promote to MRU.

**Impact:**
- `testPutDoesNotOverwriteExistingProductDetail` was incorrect — replaced with `testPutOverwritesExistingProductDetailWithLatestData`
- Implementation updated: existing-key branch now does `remove` + `put` with new value (overwrites and promotes)

---

## Q: Merge redundant eviction tests, break out access order and capacity tests

**Final ProductCachePutTest structure (4 tests):**
1. `testPutAddsProductWhenCacheIsBelowCapacity` — basic add
2. `testPutOverwritesExistingProductDetailWithLatestData` — content overwrite
3. `testPutOnExistingProductIdUpdatesAccessOrder` — put promotes to MRU, survives next eviction
4. `testPutEvictsLruAndCacheSizeNeverExceedsCapacity` — LRU eviction correctness + capacity 1 edge case

---

## Final Test Run

All 13 tests passed:

```
=== ProductCacheEmptyTest ===
PASS: testGetOnEmptyCache
PASS: testGetWithNullProductId
PASS: testGetWithBlankProductId
PASS: testPutWithNullProductId
PASS: testPutWithBlankProductId
PASS: testPutWithNullProductDetail

=== ProductCacheGetTest ===
PASS: get returns product detail when productId exists in cache
PASS: get returns null when productId does not exist in cache
PASS: get updates access order so accessed product is not evicted first

=== ProductCachePutTest ===
PASS: put adds product to cache when cache is below capacity
PASS: put overwrites existing product detail with latest data for same productId
PASS: put on existing productId promotes it to most recently used so it is not evicted first
PASS: put evicts least recently used product and cache size never exceeds capacity
```

---

## Files Produced

| File | Purpose |
|---|---|
| `ProductCache.java` | Cache implementation with `ProductDetail` inner class |
| `TestRunner.java` | Shared test utility |
| `ProductCacheEmptyTest.java` | Null/empty guard test cases |
| `ProductCacheGetTest.java` | get behavior test cases |
| `ProductCachePutTest.java` | put behavior test cases |
| `ProductCacheTestMain.java` | Single test entry point |
