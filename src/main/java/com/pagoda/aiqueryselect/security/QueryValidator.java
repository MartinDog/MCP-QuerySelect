package com.pagoda.aiqueryselect.security;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

@Component
public class QueryValidator {

    private static final Set<String> FORBIDDEN_KEYWORDS = Set.of(
            "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER", "TRUNCATE",
            "MERGE", "GRANT", "REVOKE", "EXECUTE", "EXEC", "CALL",
            "COMMIT", "ROLLBACK", "SAVEPOINT", "LOCK", "UNLOCK"
    );

    private static final Set<String> ALLOWED_STARTS = Set.of("SELECT", "WITH");

    private static final Pattern COMMENT_PATTERN = Pattern.compile(
            "/\\*.*?\\*/|--[^\\r\\n]*", Pattern.DOTALL
    );

    private static final Pattern SEMICOLON_PATTERN = Pattern.compile(";");

    public ValidationResult validate(String query) {
        if (query == null || query.isBlank()) {
            return ValidationResult.invalid("Query cannot be empty");
        }

        String cleanedQuery = removeComments(query).trim();

        if (cleanedQuery.isEmpty()) {
            return ValidationResult.invalid("Query cannot be empty after removing comments");
        }

        // Check for multiple statements
        String[] statements = SEMICOLON_PATTERN.split(cleanedQuery);
        int nonEmptyStatements = 0;
        for (String stmt : statements) {
            if (!stmt.trim().isEmpty()) {
                nonEmptyStatements++;
            }
        }
        if (nonEmptyStatements > 1) {
            return ValidationResult.invalid("Multiple statements are not allowed");
        }

        // Remove trailing semicolon for further validation
        cleanedQuery = cleanedQuery.replaceAll(";\\s*$", "").trim();

        String upperQuery = cleanedQuery.toUpperCase();

        // Check if query starts with allowed keywords
        boolean startsWithAllowed = ALLOWED_STARTS.stream()
                .anyMatch(keyword -> upperQuery.startsWith(keyword + " ") || upperQuery.startsWith(keyword + "("));

        if (!startsWithAllowed) {
            return ValidationResult.invalid("Query must start with SELECT or WITH");
        }

        // Check for forbidden keywords
        for (String forbidden : FORBIDDEN_KEYWORDS) {
            Pattern pattern = Pattern.compile(
                    "\\b" + forbidden + "\\b",
                    Pattern.CASE_INSENSITIVE
            );
            if (pattern.matcher(upperQuery).find()) {
                return ValidationResult.invalid("Forbidden keyword detected: " + forbidden);
            }
        }

        return ValidationResult.valid(cleanedQuery);
    }

    private String removeComments(String query) {
        return COMMENT_PATTERN.matcher(query).replaceAll(" ");
    }

    public record ValidationResult(boolean valid, String cleanedQuery, String errorMessage) {
        public static ValidationResult valid(String cleanedQuery) {
            return new ValidationResult(true, cleanedQuery, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, null, errorMessage);
        }
    }
}
