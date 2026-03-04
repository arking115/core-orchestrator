package com.lab.orchestrator.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lab.orchestrator.model.LabConfig;
import com.lab.orchestrator.repository.LabConfigRepository;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DockerServiceTest {

    @Mock
    private CommandExecutionService commandExecutionService;

    @Mock
    private LabConfigRepository labConfigRepository;

    @InjectMocks
    private DockerService dockerService;

    @Test
    @DisplayName("startContainer builds exact docker run command (ports + volume) and ensures directory first")
    void startContainer_buildsExpectedCommand_andEnsuresDirectoryFirst() {
        ReflectionTestUtils.setField(dockerService, "basePath", "/opt/lab-data");

        LabConfig config = new LabConfig();
        config.setImageName("ubuntu-rt-base-custom");
        when(labConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(commandExecutionService.executeCommand(eq("mkdir -p /opt/lab-data/student1"))).thenReturn("ok");
        when(commandExecutionService.executeCommand(eq(
                "docker run -d --name student1 --cpuset-cpus=\"2\" --cpus=\"1.5\" -p 2222:22 -v /opt/lab-data/student1:/home/student --cap-add=SYS_NICE --privileged ubuntu-rt-base-custom"
        ))).thenReturn("container-id");

        dockerService.startContainer("student1", 2, 1.5, 2222);

        InOrder inOrder = inOrder(commandExecutionService, labConfigRepository);
        inOrder.verify(commandExecutionService).executeCommand("mkdir -p /opt/lab-data/student1");
        inOrder.verify(labConfigRepository).findById(1L);
        inOrder.verify(commandExecutionService).executeCommand(
                "docker run -d --name student1 --cpuset-cpus=\"2\" --cpus=\"1.5\" -p 2222:22 -v /opt/lab-data/student1:/home/student --cap-add=SYS_NICE --privileged ubuntu-rt-base-custom"
        );
    }

    @Test
    @DisplayName("If remote server is down (SSH fails), the exception is propagated and docker run is not attempted")
    void startContainer_remoteServerDown_throws_andDoesNotRunDocker() {
        ReflectionTestUtils.setField(dockerService, "basePath", "/opt/lab-data");
        when(commandExecutionService.executeCommand(eq("mkdir -p /opt/lab-data/student1")))
                .thenThrow(new RuntimeException("Failed to execute SSH command"));

        assertThrows(RuntimeException.class, () -> dockerService.startContainer("student1", 2, 1.5, 2222));

        verify(commandExecutionService).executeCommand("mkdir -p /opt/lab-data/student1");
        verify(commandExecutionService, never()).executeCommand(org.mockito.ArgumentMatchers.startsWith("docker run"));
        verifyNoInteractions(labConfigRepository);
    }

    @Test
    @DisplayName("If student already has a running container, docker run error is propagated")
    void startContainer_containerNameAlreadyInUse_throws() {
        ReflectionTestUtils.setField(dockerService, "basePath", "/opt/lab-data");

        when(labConfigRepository.findById(1L)).thenReturn(Optional.empty());
        when(commandExecutionService.executeCommand(eq("mkdir -p /opt/lab-data/student1"))).thenReturn("ok");
        when(commandExecutionService.executeCommand(eq(
                "docker run -d --name student1 --cpuset-cpus=\"2\" --cpus=\"1.5\" -p 2222:22 -v /opt/lab-data/student1:/home/student --cap-add=SYS_NICE --privileged ubuntu-rt-base"
        ))).thenThrow(new RuntimeException("Conflict. The container name \"student1\" is already in use."));

        assertThrows(RuntimeException.class, () -> dockerService.startContainer("student1", 2, 1.5, 2222));

        InOrder inOrder = inOrder(commandExecutionService, labConfigRepository);
        inOrder.verify(commandExecutionService).executeCommand("mkdir -p /opt/lab-data/student1");
        inOrder.verify(labConfigRepository).findById(1L);
        inOrder.verify(commandExecutionService).executeCommand(
                "docker run -d --name student1 --cpuset-cpus=\"2\" --cpus=\"1.5\" -p 2222:22 -v /opt/lab-data/student1:/home/student --cap-add=SYS_NICE --privileged ubuntu-rt-base"
        );
    }

    @ParameterizedTest(name = "invalid args: studentId={0}, core={1}, cpu={2}, port={3}")
    @MethodSource("invalidStartContainerArgs")
    @DisplayName("Invalid inputs (null/blank/0) fail fast without any remote calls")
    void startContainer_invalidInputs_throwsIllegalArgumentException(
            String studentId,
            int coreNumber,
            double cpuLimit,
            int assignedPort
    ) {
        ReflectionTestUtils.setField(dockerService, "basePath", "/opt/lab-data");

        assertThrows(IllegalArgumentException.class,
                () -> dockerService.startContainer(studentId, coreNumber, cpuLimit, assignedPort));

        verifyNoInteractions(commandExecutionService);
        verifyNoInteractions(labConfigRepository);
    }

    private static Stream<Arguments> invalidStartContainerArgs() {
        return Stream.of(
                Arguments.of(null, 2, 1.0, 2222),
                Arguments.of("   ", 2, 1.0, 2222),
                Arguments.of("student1", 0, 1.0, 2222),
                Arguments.of("student1", 2, 0.0, 2222),
                Arguments.of("student1", 2, 1.0, 0)
        );
    }
}

