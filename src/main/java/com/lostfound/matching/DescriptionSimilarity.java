
package com.lostfound.matching;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DescriptionSimilarity {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "is", "was", "were", "and", "or",
            "of", "in", "on", "at", "to", "for", "with", "my",
            "this", "that", "it", "i", "found", "lost"
    );

    public double calculateSimilarity(String first, String second) {

        Set<String> firstWords = getWords(first);
        Set<String> secondWords = getWords(second);

        if (firstWords.isEmpty() && secondWords.isEmpty()) {
            return 0.0;
        }

        if (firstWords.isEmpty() || secondWords.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(firstWords);
        intersection.retainAll(secondWords);

        Set<String> union = new HashSet<>(firstWords);
        union.addAll(secondWords);

        return (double) intersection.size() / union.size();
    }

    private Set<String> getWords(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(
                        text.toLowerCase()
                                .replaceAll("[^a-z0-9\\s]", " ")
                                .trim()
                                .split("\\s+"))
                .filter(word -> !word.isBlank())
                .filter(word -> !STOP_WORDS.contains(word))
                .collect(Collectors.toSet());
    }
}
