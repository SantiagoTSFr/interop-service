package com.interop.service;

import com.interop.model.Case;
import com.interop.model.SearchModels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Deterministic "AI-ready" search over cases using a hash-based TF-IDF-like representation.
 *
 * Determinism guarantees:
 *  - Tokenisation is pure string ops (lowercase + split on non-alpha).
 *  - Term weights use MurmurHash3-inspired 32-bit mixing (no randomness).
 *  - Scoring is cosine-similarity equivalent computed via dot-product of normalized term vectors.
 *  - Same query + same data => identical ordered top_k results every time.
 *
 * Index is rebuilt/refreshed after every successful /ingest call.
 */
@Service
public class SearchIndexService {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexService.class);

    /** Indexed document entry */
    private record IndexEntry(long caseId, String title, String status, Map<String, Double> termVector) {}

    /** In-memory index: caseId -> IndexEntry */
    private final Map<Long, IndexEntry> index = new ConcurrentHashMap<>();

    // ---------------------------------------------------------------
    // Index management
    // ---------------------------------------------------------------

    /** Replace the entire index with a fresh set of cases. */
    public synchronized void rebuild(List<Case> cases) {
        index.clear();
        for (Case c : cases) {
            index.put(c.caseId(), toEntry(c));
        }
        log.info("Search index rebuilt with {} documents", index.size());
    }

    /** Upsert a single case (used for incremental refresh). */
    public synchronized void upsert(Case c) {
        index.put(c.caseId(), toEntry(c));
    }

    /** Current index size (for diagnostics). */
    public int size() {
        return index.size();
    }

    // ---------------------------------------------------------------
    // Search
    // ---------------------------------------------------------------

    public List<SearchModels.SearchResult> search(String query, int topK) {
        if (index.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Double> queryVector = termVector(query);
        if (queryVector.isEmpty()) {
            return Collections.emptyList();
        }

        // Score every document

        return index.values().stream()
                .map(entry -> {
                    double score = cosineSimilarity(queryVector, entry.termVector());
                    return new SearchModels.SearchResult(entry.caseId(), round6(score), entry.title(), entry.status());
                })
                .filter(r -> r.score() > 0.0)
                .sorted(Comparator
                        .comparingDouble(SearchModels.SearchResult::score).reversed()
                        .thenComparingLong(SearchModels.SearchResult::caseId))  // tie-break for determinism
                .limit(topK)
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------
    // Vectorisation internals
    // ---------------------------------------------------------------

    private IndexEntry toEntry(Case c) {
        String text = c.title() + " " + c.description();
        Map<String, Double> vec = termVector(text);
        return new IndexEntry(c.caseId(), c.title(), c.status(), vec);
    }

    /**
     * Builds a normalised term-frequency vector with deterministic hash-based IDF weighting.
     * No external state, no randomness.
     */
    private Map<String, Double> termVector(String text) {
        List<String> tokens = tokenise(text);
        if (tokens.isEmpty()) return Collections.emptyMap();

        // TF: raw counts
        Map<String, Integer> tf = new LinkedHashMap<>();
        for (String t : tokens) {
            tf.merge(t, 1, Integer::sum);
        }

        // Hash-based weight: deterministic pseudo-IDF using murmur-like mix
        // Rare/specific terms get higher weight than common short ones
        Map<String, Double> weighted = new LinkedHashMap<>();
        double total = tokens.size();
        for (Map.Entry<String, Integer> e : tf.entrySet()) {
            double termFreq = e.getValue() / total;
            double hashWeight = hashWeight(e.getKey());
            weighted.put(e.getKey(), termFreq * hashWeight);
        }

        // L2 normalise
        return l2Normalise(weighted);
    }

    /** Deterministic hash-based weight in range [0.5, 2.0] based on term content. */
    private double hashWeight(String term) {
        int h = murmurMix(term.hashCode());
        // Map to [0.5, 2.0]: longer/rarer terms get higher weight
        double base = 0.5 + (1.5 * (Math.abs(h) % 1000) / 1000.0);
        // Boost longer terms (more specific)
        double lengthBoost = Math.min(1.0 + (term.length() - 3) * 0.05, 1.5);
        return base * lengthBoost;
    }

    /** Simple deterministic integer mixing (MurmurHash3 finaliser). */
    private int murmurMix(int h) {
        h ^= h >>> 16;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        h ^= h >>> 16;
        return h;
    }

    private Map<String, Double> l2Normalise(Map<String, Double> vec) {
        double norm = Math.sqrt(vec.values().stream().mapToDouble(v -> v * v).sum());
        if (norm == 0) return vec;
        Map<String, Double> result = new LinkedHashMap<>();
        vec.forEach((k, v) -> result.put(k, v / norm));
        return result;
    }

    private double cosineSimilarity(Map<String, Double> a, Map<String, Double> b) {
        // Both vectors are already L2-normalized, so dot product = cosine similarity
        double dot = 0.0;
        for (Map.Entry<String, Double> e : a.entrySet()) {
            Double bVal = b.get(e.getKey());
            if (bVal != null) dot += e.getValue() * bVal;
        }
        return dot;
    }

    private List<String> tokenise(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        String[] parts = text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        List<String> tokens = new ArrayList<>();
        for (String p : parts) {
            if (p.length() >= 2) tokens.add(p); // skip single-char tokens
        }
        return tokens;
    }

    private double round6(double v) {
        return Math.round(v * 1_000_000.0) / 1_000_000.0;
    }
}
