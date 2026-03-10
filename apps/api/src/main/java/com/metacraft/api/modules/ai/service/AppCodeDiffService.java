package com.metacraft.api.modules.ai.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class AppCodeDiffService {

    private static final Pattern DIFF_BLOCK_PATTERN = Pattern.compile(
            "<<<<<<< SEARCH\\R(.*?)\\R=======\\R(.*?)\\R>>>>>>> REPLACE",
            Pattern.DOTALL);

    public String applyDiff(String originalContent, String diffContent, String fileLabel) {
        if (originalContent == null || diffContent == null || diffContent.isBlank()) {
            return originalContent;
        }

        String normalizedOriginal = normalizeNewlines(originalContent);
        String normalizedDiff = normalizeNewlines(diffContent).trim();
        Matcher matcher = DIFF_BLOCK_PATTERN.matcher(normalizedDiff);
        String updatedContent = normalizedOriginal;
        int previousMatchEnd = 0;
        boolean foundBlock = false;

        while (matcher.find()) {
            validateInterBlockText(normalizedDiff.substring(previousMatchEnd, matcher.start()), fileLabel);

            String searchSnippet = matcher.group(1);
            String replaceSnippet = matcher.group(2);
            if (searchSnippet.isEmpty()) {
                throw new IllegalArgumentException(fileLabel + " diff contains an empty SEARCH block");
            }

            int matchCount = countOccurrences(updatedContent, searchSnippet);
            if (matchCount == 0) {
                throw new IllegalArgumentException(fileLabel + " diff SEARCH block not found in source");
            }
            if (matchCount > 1) {
                throw new IllegalArgumentException(fileLabel + " diff SEARCH block matched multiple locations");
            }

            int matchIndex = updatedContent.indexOf(searchSnippet);
            updatedContent = updatedContent.substring(0, matchIndex)
                    + replaceSnippet
                    + updatedContent.substring(matchIndex + searchSnippet.length());

            previousMatchEnd = matcher.end();
            foundBlock = true;
        }

        if (!foundBlock) {
            throw new IllegalArgumentException(fileLabel + " diff did not contain any valid SEARCH/REPLACE blocks");
        }

        validateInterBlockText(normalizedDiff.substring(previousMatchEnd), fileLabel);
        return updatedContent;
    }

    private void validateInterBlockText(String contentBetweenBlocks, String fileLabel) {
        if (!contentBetweenBlocks.isBlank()) {
            throw new IllegalArgumentException(fileLabel + " diff contains unexpected text outside SEARCH/REPLACE blocks");
        }
    }

    private int countOccurrences(String source, String target) {
        int count = 0;
        int currentIndex = 0;

        while ((currentIndex = source.indexOf(target, currentIndex)) >= 0) {
            count++;
            currentIndex += target.length();
        }

        return count;
    }

    private String normalizeNewlines(String content) {
        return content.replace("\r\n", "\n").replace('\r', '\n');
    }
}