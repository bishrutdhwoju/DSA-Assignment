package assignment;

import java.util.*;

// Word Break: finds all ways to segment a string into valid dictionary words using DFS + memoization
public class WordBreakAllSentences {

    // Returns all valid segmentations of the input string
    public static List<String> wordBreak(String inputString, List<String> wordList) {
        Set<String> dictionary = new HashSet<>(wordList);
        Map<Integer, List<String>> memoCache = new HashMap<>();
        return findAllSegmentations(0, inputString, dictionary, memoCache);
    }

    // DFS with memoization: finds all valid sentences from startIndex to end
    private static List<String> findAllSegmentations(int startIndex, String fullString,
                                                      Set<String> dictionary, Map<Integer, List<String>> memoCache) {
        if (memoCache.containsKey(startIndex)) return memoCache.get(startIndex);

        List<String> validSentences = new ArrayList<>();

        // Base case: reached end of string
        if (startIndex == fullString.length()) {
            validSentences.add("");
            memoCache.put(startIndex, validSentences);
            return validSentences;
        }

        // Try every possible word starting at current index
        for (int endIndex = startIndex + 1; endIndex <= fullString.length(); endIndex++) {
            String candidateWord = fullString.substring(startIndex, endIndex);
            if (!dictionary.contains(candidateWord)) continue;

            // Recursively get all segmentations for the remaining string
            List<String> remainingSegmentations = findAllSegmentations(endIndex, fullString, dictionary, memoCache);

            // Combine current word with each remaining segmentation
            for (String remainingSentence : remainingSegmentations) {
                if (remainingSentence.isEmpty()) {
                    validSentences.add(candidateWord);
                } else {
                    validSentences.add(candidateWord + " " + remainingSentence);
                }
            }
        }

        memoCache.put(startIndex, validSentences);
        return validSentences;
    }

    public static void main(String[] args) {
        // Expected: [nepal trekking guide, nepaltrekking guide]
        System.out.println(wordBreak("nepaltrekkingguide",
                Arrays.asList("nepal", "trekking", "guide", "nepaltrekking")));
    }
}