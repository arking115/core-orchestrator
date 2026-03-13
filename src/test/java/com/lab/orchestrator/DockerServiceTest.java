package com.lab.orchestrator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void setStoragePaths() {
        ReflectionTestUtils.setField(dockerService, "basePath", "/opt/lab-data");
        ReflectionTestUtils.setField(dockerService, "containerPath", "/home/student");
    }

    @Test
    @DisplayName("startContainer builds exact docker run command (ports + volume) and ensures directory first")
    void startContainer_buildsExpectedCommand_andEnsuresDirectoryFirst() {
        LabConfig config = new LabConfig();
        config.setImageName("ubuntu-rt-base-custom");
        when(labConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(commandExecutionService.executeCommand(eq("mkdir -p /opt/lab-data/ubuntu-rt-base-custom/student1"))).thenReturn("ok");
        when(commandExecutionService.executeCommand(eq(
                "docker run -d --name student1 --cpuset-cpus=\"2\" --cpus=\"1.5\" -p 2222:22 -v /opt/lab-data/ubuntu-rt-base-custom/student1:/home/student --cap-add=SYS_NICE --privileged ubuntu-rt-base-custom"
        ))).thenReturn("container-id");

        dockerService.startContainer("student1", 2, 1.5, 2222);

        InOrder inOrder = inOrder(commandExecutionService, labConfigRepository);
        inOrder.verify(labConfigRepository).findById(1L);
        inOrder.verify(commandExecutionService).executeCommand("mkdir -p /opt/lab-data/ubuntu-rt-base-custom/student1");
        inOrder.verify(commandExecutionService).executeCommand(
                "docker run -d --name student1 --cpuset-cpus=\"2\" --cpus=\"1.5\" -p 2222:22 -v /opt/lab-data/ubuntu-rt-base-custom/student1:/home/student --cap-add=SYS_NICE --privileged ubuntu-rt-base-custom"
        );
    }

    @Test
    @DisplayName("When no LabConfig exists, startContainer falls back to default ubuntu-rt-base image")
    void startContainer_noConfig_usesDefaultImage() {
        when(labConfigRepository.findById(1L)).thenReturn(Optional.empty());
        when(commandExecutionService.executeCommand(eq("mkdir -p /opt/lab-data/ubuntu-rt-base/student1"))).thenReturn("ok");
        when(commandExecutionService.executeCommand(eq(
                "docker run -d --name student1 --cpuset-cpus=\"2\" --cpus=\"1.5\" -p 2222:22 -v /opt/lab-data/ubuntu-rt-base/student1:/home/student --cap-add=SYS_NICE --privileged ubuntu-rt-base"
        ))).thenReturn("container-id");

        dockerService.startContainer("student1", 2, 1.5, 2222);

        InOrder inOrder = inOrder(commandExecutionService, labConfigRepository);
        inOrder.verify(labConfigRepository).findById(1L);
        inOrder.verify(commandExecutionService).executeCommand("mkdir -p /opt/lab-data/ubuntu-rt-base/student1");
        inOrder.verify(commandExecutionService).executeCommand(
                "docker run -d --name student1 --cpuset-cpus=\"2\" --cpus=\"1.5\" -p 2222:22 -v /opt/lab-data/ubuntu-rt-base/student1:/home/student --cap-add=SYS_NICE --privileged ubuntu-rt-base"
        );
    }

    @Test
    @DisplayName("If remote server is down (SSH fails), the exception is propagated and docker run is not attempted")
    void startContainer_remoteServerDown_throws_andDoesNotRunDocker() {
        when(labConfigRepository.findById(1L)).thenReturn(Optional.empty());
        when(commandExecutionService.executeCommand(eq("mkdir -p /opt/lab-data/ubuntu-rt-base/student1")))
                .thenThrow(new RuntimeException("Failed to execute SSH command"));

        assertThrows(RuntimeException.class, () -> dockerService.startContainer("student1", 2, 1.5, 2222));

        verify(labConfigRepository).findById(1L);
        verify(commandExecutionService).executeCommand("mkdir -p /opt/lab-data/ubuntu-rt-base/student1");
        verify(commandExecutionService, never()).executeCommand(org.mockito.ArgumentMatchers.startsWith("docker run"));
    }

    @Test
    @DisplayName("If student already has a running container, docker run error is propagated")
    void startContainer_containerNameAlreadyInUse_throws() {
        when(labConfigRepository.findById(1L)).thenReturn(Optional.empty());
        when(commandExecutionService.executeCommand(eq("mkdir -p /opt/lab-data/ubuntu-rt-base/student1"))).thenReturn("ok");
        when(commandExecutionService.executeCommand(eq(
                "docker run -d --name student1 --cpuset-cpus=\"2\" --cpus=\"1.5\" -p 2222:22 -v /opt/lab-data/ubuntu-rt-base/student1:/home/student --cap-add=SYS_NICE --privileged ubuntu-rt-base"
        ))).thenThrow(new RuntimeException("Conflict. The container name \"student1\" is already in use."));

        assertThrows(RuntimeException.class, () -> dockerService.startContainer("student1", 2, 1.5, 2222));

        InOrder inOrder = inOrder(commandExecutionService, labConfigRepository);
        inOrder.verify(labConfigRepository).findById(1L);
        inOrder.verify(commandExecutionService).executeCommand("mkdir -p /opt/lab-data/ubuntu-rt-base/student1");
        inOrder.verify(commandExecutionService).executeCommand(
                "docker run -d --name student1 --cpuset-cpus=\"2\" --cpus=\"1.5\" -p 2222:22 -v /opt/lab-data/ubuntu-rt-base/student1:/home/student --cap-add=SYS_NICE --privileged ubuntu-rt-base"
        );
    }

    @Test
    @DisplayName("stopContainer calls docker stop before docker rm for the same student")
    void stopContainer_callsStopBeforeRemove_inOrder() {
        String studentId = "student1";

        dockerService.stopContainer(studentId);

        InOrder inOrder = inOrder(commandExecutionService);
        inOrder.verify(commandExecutionService).executeCommand("docker stop student1 || true");
        inOrder.verify(commandExecutionService).executeCommand("docker rm student1 || true");
    }

    @Test
    @DisplayName("If remote server is down during stop (SSH fails), the exception is propagated and docker rm is not attempted")
    void stopContainer_remoteServerDown_throws_andDoesNotRemoveContainer() {
        String studentId = "student1";

        when(commandExecutionService.executeCommand("docker stop student1 || true"))
                .thenThrow(new RuntimeException("Failed to execute SSH command during stop"));

        assertThrows(RuntimeException.class, () -> dockerService.stopContainer(studentId));

        verify(commandExecutionService).executeCommand("docker stop student1 || true");
        verify(commandExecutionService, never()).executeCommand("docker rm student1 || true");
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

    @Test
    @DisplayName("Image name with registry and tag is sanitized for directory path")
    void startContainer_imageNameWithSpecialChars_sanitizesForPath() {
        LabConfig config = new LabConfig();
        config.setImageName("myregistry.io/lab/ubuntu-rt:v2.0");
        when(labConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(commandExecutionService.executeCommand(eq("mkdir -p /opt/lab-data/myregistry.io_lab_ubuntu-rt_v2.0/student1"))).thenReturn("ok");
        when(commandExecutionService.executeCommand(eq(
                "docker run -d --name student1 --cpuset-cpus=\"2\" --cpus=\"1.5\" -p 2222:22 -v /opt/lab-data/myregistry.io_lab_ubuntu-rt_v2.0/student1:/home/student --cap-add=SYS_NICE --privileged myregistry.io/lab/ubuntu-rt:v2.0"
        ))).thenReturn("container-id");

        dockerService.startContainer("student1", 2, 1.5, 2222);

        InOrder inOrder = inOrder(commandExecutionService, labConfigRepository);
        inOrder.verify(labConfigRepository).findById(1L);
        inOrder.verify(commandExecutionService).executeCommand("mkdir -p /opt/lab-data/myregistry.io_lab_ubuntu-rt_v2.0/student1");
        inOrder.verify(commandExecutionService).executeCommand(
                "docker run -d --name student1 --cpuset-cpus=\"2\" --cpus=\"1.5\" -p 2222:22 -v /opt/lab-data/myregistry.io_lab_ubuntu-rt_v2.0/student1:/home/student --cap-add=SYS_NICE --privileged myregistry.io/lab/ubuntu-rt:v2.0"
        );
    }

    @ParameterizedTest(name = "sanitizeForPath(\"{0}\") = \"{1}\"")
    @MethodSource("sanitizeForPathTestCases")
    @DisplayName("sanitizeForPath replaces slashes, colons, and spaces with underscores")
    void sanitizeForPath_replacesSpecialChars(String input, String expected) {
        String result = ReflectionTestUtils.invokeMethod(dockerService, "sanitizeForPath", input);
        assertEquals(expected, result);
    }

    private static Stream<Arguments> sanitizeForPathTestCases() {
        return Stream.of(
                Arguments.of("ubuntu-rt-base", "ubuntu-rt-base"),
                Arguments.of("image:latest", "image_latest"),
                Arguments.of("registry/image", "registry_image"),
                Arguments.of("registry.io/org/image:v1.0", "registry.io_org_image_v1.0"),
                Arguments.of("image with spaces", "image_with_spaces"),
                Arguments.of("a/b:c d", "a_b_c_d")
        );
    }
}

