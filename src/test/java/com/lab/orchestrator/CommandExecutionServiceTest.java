package com.lab.orchestrator.service;

import com.lab.orchestrator.service.CommandExecutionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CommandExecutionServiceTest {

    @Autowired
    private CommandExecutionService commandExecutionService;

    @Test
    void testSshConnection() {
        String result = commandExecutionService.executeCommand("echo %USERNAME%");

        System.out.println("SSH command result: " + result);

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isBlank(), "SSH command result should not be empty");
    }

    @Test
    void testInvalidCommandThrowsRuntimeException() {
        Assertions.assertThrows(RuntimeException.class,
                () -> commandExecutionService.executeCommand("this_command_should_fail_123"));
    }

    @Test
    void testEmptyCommandThrowsIllegalArgumentException() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> commandExecutionService.executeCommand("   "));
    }
}

