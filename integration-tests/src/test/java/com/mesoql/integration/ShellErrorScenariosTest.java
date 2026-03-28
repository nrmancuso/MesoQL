package com.mesoql.integration;

import com.mesoql.integration.support.ShellExtension;
import com.mesoql.integration.support.ShellResult;
import com.mesoql.integration.support.ShellSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ShellExtension.class)
class ShellErrorScenariosTest {

    private static final String PROMPT = "mesoql> ";

    @Test
    @DisplayName("shows an error for malformed syntax and keeps the shell alive")
    void rejectsMalformedSyntax(ShellSession shell) {
        final String expectedOutput = """
            MesoQL (type \\q to quit)

            mesoql> SEARCH WHERE
            ERROR: line 1:7 missing {'storm_events', 'forecast_discussions'} at 'WHERE'
            %s
            """.formatted(PROMPT).stripTrailing();
        assertFailsAndRecovers(shell, "SEARCH WHERE", expectedOutput);
    }

    @Test
    @DisplayName("shows an error when the semantic clause is missing")
    void rejectsQueryWithoutSemanticClause(ShellSession shell) {
        final String expectedOutput = """
            MesoQL (type \\q to quit)

            mesoql> SEARCH storm_events WHERE state IN ("TX")
            ERROR: line 1:26 mismatched input 'state' expecting 'semantic'
            %s
            """.formatted(PROMPT).stripTrailing();
        assertFailsAndRecovers(shell,
            "SEARCH storm_events WHERE state IN (\"TX\")",
            expectedOutput);
    }

    @Test
    @DisplayName("shows an error for an unknown field")
    void rejectsUnknownField(ShellSession shell) {
        final String expectedOutput = """
            MesoQL (type \\q to quit)

            mesoql> SEARCH storm_events WHERE SEMANTIC("test") AND magnitude >= 5
            ERROR: Unknown field 'magnitude' for source 'storm_events'
            %s
            """.formatted(PROMPT).stripTrailing();
        assertFailsAndRecovers(shell,
            "SEARCH storm_events WHERE SEMANTIC(\"test\") AND magnitude >= 5",
            expectedOutput);
    }

    @Test
    @DisplayName("shows an error for an unknown slash command")
    void rejectsUnknownSlashCommand(ShellSession shell) {
        final String expectedOutput = """
            MesoQL (type \\q to quit)

            mesoql> \\does-not-exist
            ERROR: line 1:0 token recognition error at: '\\'
            %s
            """.formatted(PROMPT).stripTrailing();
        assertFailsAndRecovers(shell, "\\does-not-exist", expectedOutput);
    }

    private static void assertFailsAndRecovers(ShellSession shell, String query, String expectedOutput) {
        final ShellResult result = shell.sendLine(query);
        assertEquals(expectedOutput, result.text());
        assertTrue(result.text().contains("ERROR:"), result.text());
        assertTrue(result.promptSeenAgain(), result.text());
    }
}
