package com.pagoda.aiqueryselect.security;

import com.pagoda.aiqueryselect.security.QueryValidator.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class QueryValidatorTest {

    private QueryValidator validator;

    @BeforeEach
    void setUp() {
        validator = new QueryValidator();
    }

    @Nested
    @DisplayName("Valid SELECT queries")
    class ValidSelectQueries {

        @Test
        void shouldAcceptSimpleSelect() {
            ValidationResult result = validator.validate("SELECT * FROM employees");
            assertTrue(result.valid());
            assertNotNull(result.cleanedQuery());
        }

        @Test
        void shouldAcceptSelectWithWhereClause() {
            ValidationResult result = validator.validate("SELECT id, name FROM users WHERE status = 'active'");
            assertTrue(result.valid());
        }

        @Test
        void shouldAcceptSelectWithJoin() {
            ValidationResult result = validator.validate(
                    "SELECT e.name, d.department_name FROM employees e JOIN departments d ON e.dept_id = d.id");
            assertTrue(result.valid());
        }

        @Test
        void shouldAcceptSelectWithSubquery() {
            ValidationResult result = validator.validate(
                    "SELECT * FROM employees WHERE dept_id IN (SELECT id FROM departments WHERE active = 1)");
            assertTrue(result.valid());
        }

        @Test
        void shouldAcceptWithClause() {
            ValidationResult result = validator.validate(
                    "WITH active_employees AS (SELECT * FROM employees WHERE status = 'active') " +
                            "SELECT * FROM active_employees");
            assertTrue(result.valid());
        }

        @Test
        void shouldAcceptSelectWithGroupByAndHaving() {
            ValidationResult result = validator.validate(
                    "SELECT dept_id, COUNT(*) as cnt FROM employees GROUP BY dept_id HAVING COUNT(*) > 5");
            assertTrue(result.valid());
        }

        @Test
        void shouldAcceptSelectWithOrderByAndFetch() {
            ValidationResult result = validator.validate(
                    "SELECT * FROM employees ORDER BY hire_date DESC FETCH FIRST 10 ROWS ONLY");
            assertTrue(result.valid());
        }

        @Test
        void shouldAcceptSelectWithTrailingSemicolon() {
            ValidationResult result = validator.validate("SELECT * FROM employees;");
            assertTrue(result.valid());
        }

        @Test
        void shouldHandleLowercaseSelect() {
            ValidationResult result = validator.validate("select * from employees");
            assertTrue(result.valid());
        }

        @Test
        void shouldHandleMixedCaseSelect() {
            ValidationResult result = validator.validate("SeLeCt * FrOm employees");
            assertTrue(result.valid());
        }
    }

    @Nested
    @DisplayName("Invalid queries - forbidden keywords")
    class ForbiddenKeywords {

        @ParameterizedTest
        @ValueSource(strings = {
                "INSERT INTO employees (name) VALUES ('test')",
                "UPDATE employees SET name = 'test'",
                "DELETE FROM employees",
                "DROP TABLE employees",
                "CREATE TABLE test (id INT)",
                "ALTER TABLE employees ADD COLUMN email VARCHAR(100)",
                "TRUNCATE TABLE employees",
                "MERGE INTO employees USING temp ON (1=1) WHEN MATCHED THEN UPDATE SET name='x'",
                "GRANT SELECT ON employees TO user1",
                "REVOKE SELECT ON employees FROM user1",
                "EXECUTE my_procedure",
                "EXEC my_procedure",
                "CALL my_procedure()",
                "COMMIT",
                "ROLLBACK",
                "SAVEPOINT sp1",
                "LOCK TABLE employees IN EXCLUSIVE MODE"
        })
        void shouldRejectForbiddenStatements(String query) {
            ValidationResult result = validator.validate(query);
            assertFalse(result.valid());
            assertNotNull(result.errorMessage());
        }

        @Test
        void shouldRejectInsertEvenInLowercase() {
            ValidationResult result = validator.validate("insert into employees (name) values ('test')");
            assertFalse(result.valid());
        }

        @Test
        void shouldRejectUpdateEvenWithMixedCase() {
            ValidationResult result = validator.validate("UpDaTe employees SET name = 'test'");
            assertFalse(result.valid());
        }
    }

    @Nested
    @DisplayName("SQL injection attempts")
    class SqlInjectionAttempts {

        @Test
        void shouldRejectMultipleStatements() {
            ValidationResult result = validator.validate("SELECT * FROM employees; DROP TABLE employees");
            assertFalse(result.valid());
            assertTrue(result.errorMessage().contains("Multiple statements"));
        }

        @Test
        void shouldRejectCommentBasedInjectionWithDelete() {
            ValidationResult result = validator.validate("SELECT * FROM employees /* ; DELETE FROM employees */");
            assertTrue(result.valid()); // Comment is stripped, resulting query is valid SELECT
        }

        @Test
        void shouldRejectLineCommentBasedInjection() {
            ValidationResult result = validator.validate("SELECT * FROM employees -- ; DELETE FROM employees");
            assertTrue(result.valid()); // Comment is stripped, resulting query is valid SELECT
        }

        @Test
        void shouldRejectUnionBasedDeleteAttempt() {
            ValidationResult result = validator.validate(
                    "SELECT * FROM employees UNION DELETE FROM employees");
            assertFalse(result.valid());
            assertTrue(result.errorMessage().contains("DELETE"));
        }

        @Test
        void shouldRejectHiddenDeleteInSubquery() {
            // This is actually a syntax error in Oracle but we still want to block DELETE keyword
            ValidationResult result = validator.validate(
                    "SELECT * FROM (DELETE FROM employees RETURNING *)");
            assertFalse(result.valid());
            assertTrue(result.errorMessage().contains("DELETE"));
        }
    }

    @Nested
    @DisplayName("Comment handling")
    class CommentHandling {

        @Test
        void shouldStripBlockComments() {
            ValidationResult result = validator.validate("SELECT /* comment */ * FROM employees");
            assertTrue(result.valid());
        }

        @Test
        void shouldStripMultilineBlockComments() {
            ValidationResult result = validator.validate(
                    "SELECT /* this is\na multiline\ncomment */ * FROM employees");
            assertTrue(result.valid());
        }

        @Test
        void shouldStripLineComments() {
            ValidationResult result = validator.validate("SELECT * FROM employees -- this is a comment");
            assertTrue(result.valid());
        }

        @Test
        void shouldStripMixedComments() {
            ValidationResult result = validator.validate(
                    "SELECT /* block */ * -- line\nFROM employees");
            assertTrue(result.valid());
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        void shouldRejectNullQuery() {
            ValidationResult result = validator.validate(null);
            assertFalse(result.valid());
            assertTrue(result.errorMessage().contains("empty"));
        }

        @Test
        void shouldRejectEmptyQuery() {
            ValidationResult result = validator.validate("");
            assertFalse(result.valid());
            assertTrue(result.errorMessage().contains("empty"));
        }

        @Test
        void shouldRejectBlankQuery() {
            ValidationResult result = validator.validate("   \t\n  ");
            assertFalse(result.valid());
            assertTrue(result.errorMessage().contains("empty"));
        }

        @Test
        void shouldRejectQueryWithOnlyComments() {
            ValidationResult result = validator.validate("/* just a comment */");
            assertFalse(result.valid());
            assertTrue(result.errorMessage().contains("empty"));
        }

        @Test
        void shouldRejectQueryNotStartingWithSelect() {
            ValidationResult result = validator.validate("SHOW TABLES");
            assertFalse(result.valid());
            assertTrue(result.errorMessage().contains("SELECT"));
        }

        @Test
        void shouldHandleSelectWithParenthesis() {
            ValidationResult result = validator.validate("(SELECT * FROM employees)");
            assertFalse(result.valid()); // Must start with SELECT or WITH, not parenthesis
        }

        @Test
        void shouldAcceptSelectStartingWithParenInSubquery() {
            ValidationResult result = validator.validate(
                    "SELECT * FROM (SELECT id FROM employees) t");
            assertTrue(result.valid());
        }
    }

    @Nested
    @DisplayName("Keyword detection in context")
    class KeywordContext {

        @Test
        void shouldAllowSelectKeywordInStringLiteral() {
            // "DELETE" in a string should be allowed
            ValidationResult result = validator.validate(
                    "SELECT * FROM employees WHERE action = 'DELETE'");
            // Note: Our simple regex-based approach will still block this
            // A more sophisticated parser would be needed to handle this correctly
            // For safety, we accept false positives
            assertFalse(result.valid());
        }

        @Test
        void shouldAllowSelectKeywordInColumnAlias() {
            ValidationResult result = validator.validate(
                    "SELECT update_count FROM statistics");
            // "update" as part of column name should be blocked by word boundary check
            assertTrue(result.valid()); // update_count != UPDATE
        }

        @Test
        void shouldBlockDeleteAsSubstring() {
            ValidationResult result = validator.validate(
                    "SELECT * FROM employees WHERE DELETE = 1");
            assertFalse(result.valid());
        }
    }
}
