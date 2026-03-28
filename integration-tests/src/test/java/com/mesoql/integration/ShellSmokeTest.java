package com.mesoql.integration;

import com.mesoql.integration.support.ShellExtension;
import com.mesoql.integration.support.ShellResult;
import com.mesoql.integration.support.ShellSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(ShellExtension.class)
class ShellSmokeTest {

    private static final String PROMPT = "mesoql> ";

    @Test
    @DisplayName("quits cleanly with \\q")
    void quitsWithBackslashQ(ShellSession shell) {
        final ShellResult result = shell.sendLine("\\q");
        final String expectedOutput = """
            MesoQL (type \\q to quit)

            mesoql> \\q
            """.stripTrailing();
        assertEquals(expectedOutput, result.text());
        assertNotNull(result.exitCode());
        assertEquals(0, result.exitCode());
    }

    @Test
    @DisplayName("quits cleanly with exit")
    void quitsWithExit(ShellSession shell) {
        final ShellResult result = shell.sendLine("exit");
        final String expectedOutput = """
            MesoQL (type \\q to quit)

            mesoql> exit
            """.stripTrailing();
        assertEquals(expectedOutput, result.text());
        assertNotNull(result.exitCode());
        assertEquals(0, result.exitCode());
    }

    @Test
    @DisplayName("quits cleanly with quit")
    void quitsWithQuit(ShellSession shell) {
        final ShellResult result = shell.sendLine("quit");
        final String expectedOutput = """
            MesoQL (type \\q to quit)

            mesoql> quit
            """.stripTrailing();
        assertEquals(expectedOutput, result.text());
        assertNotNull(result.exitCode());
        assertEquals(0, result.exitCode());
    }

    @Test
    @DisplayName("starts and accepts an empty line")
    void startsAtPrompt(ShellSession shell) {
        final ShellResult result = shell.sendLine("");
        final String expectedOutput = """
            MesoQL (type \\q to quit)

            %s
            %s
            """.formatted(PROMPT, PROMPT).stripTrailing();
        assertEquals(expectedOutput, result.text());
    }
}
