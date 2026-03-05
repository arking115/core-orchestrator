package com.lab.orchestrator;

import com.lab.orchestrator.model.CoreAllocation;
import com.lab.orchestrator.model.LabConfig;
import com.lab.orchestrator.repository.CoreAllocationRepository;
import com.lab.orchestrator.repository.LabConfigRepository;
import com.lab.orchestrator.service.CoreAllocationService;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CoreAllocationServiceTest {

    @Autowired
    private CoreAllocationService coreAllocationService;

    @Autowired
    private CoreAllocationRepository coreAllocationRepository;

    @Autowired
    private LabConfigRepository labConfigRepository;

    @Test
    @DisplayName("When many students are allocated, the load is evenly distributed across all cores")
    void evenDistribution_multipleCores_spreadsLoadEvenly() {
        coreAllocationService.initializeCores(30, List.of(1, 2, 3), "test-image");

        for (int i = 0; i < 30; i++) {
            coreAllocationService.getNextAvailableCore();
        }

        List<CoreAllocation> cores = coreAllocationRepository.findAll();
        Assertions.assertEquals(3, cores.size());
        for (CoreAllocation core : cores) {
            Assertions.assertEquals(10, core.getCurrentStudentCount());
        }
    }

    @Test
    @DisplayName("CPU limit per core is computed from total students and number of cores")
    void cpuLimitCalculation_derivedFromStudentsAndCores() {
        coreAllocationService.initializeCores(10, List.of(1, 2), "test-image");

        List<CoreAllocation> cores = coreAllocationRepository.findAll();
        Assertions.assertEquals(2, cores.size());
        for (CoreAllocation core : cores) {
            Assertions.assertEquals(0.2, core.getCpuLimit(), 1e-9);
        }
    }

    @Test
    @DisplayName("Releasing a core decrements its student count back toward zero")
    void releaseCore_decrementsCurrentStudentCount() {
        coreAllocationService.initializeCores(1, List.of(1), "test-image");
        CoreAllocation allocated = coreAllocationService.getNextAvailableCore();
        Assertions.assertEquals(1, allocated.getCurrentStudentCount());

        coreAllocationService.releaseCore(allocated.getCoreNumber());

        CoreAllocation afterRelease = coreAllocationRepository.findByCoreNumber(allocated.getCoreNumber()).orElseThrow();
        Assertions.assertEquals(0, afterRelease.getCurrentStudentCount());
    }

    // Test for spreading the load evenly across the cores
    @Test
    @DisplayName("Least-congested-core algorithm keeps max difference between cores at most 1")
    void leastCongestedCore_doesNotOverloadAnySingleCore() {
        coreAllocationService.initializeCores(12, List.of(1, 2, 3), "test-image");

        for (int i = 0; i < 5; i++) {
            coreAllocationService.getNextAvailableCore();
        }

        List<CoreAllocation> cores = coreAllocationRepository.findAll();
        Assertions.assertEquals(3, cores.size());
        int min = cores.stream().mapToInt(CoreAllocation::getCurrentStudentCount).min().orElseThrow();
        int max = cores.stream().mapToInt(CoreAllocation::getCurrentStudentCount).max().orElseThrow();
        Assertions.assertTrue(max - min <= 1,
                "Least Congested Core Algorithm should keep max difference between cores at most 1, got min=" + min + " max=" + max);
    }

    @Test
    @DisplayName("getNextAvailableCore throws when no cores have been initialized")
    void getNextAvailableCore_noCoresInitialized_throwsIllegalStateException() {
        coreAllocationRepository.deleteAll();
        labConfigRepository.deleteAll();
        Assertions.assertThrows(IllegalStateException.class, () -> coreAllocationService.getNextAvailableCore());
    }

    // Test for failing when at capacity
    @Test
    @DisplayName("Allocations past configured capacity fail with an exception")
    void allocation_atCapacity_throwsIllegalStateException() {
        coreAllocationService.initializeCores(30, List.of(1, 2, 3), "test-image");

        for (int i = 0; i < 30; i++) {
            coreAllocationService.getNextAvailableCore();
        }

        Assertions.assertThrows(IllegalStateException.class, () -> coreAllocationService.getNextAvailableCore());
    }

    @Test
    // Test for initializing cores and students and checking that no allocations have been made yet
    @DisplayName("After initialization, cores exist but have zero allocations")
    void initializeCores_createsCoresWithZeroStudents() {
        coreAllocationService.initializeCores(12, List.of(1, 2, 3), "test-image");

        List<CoreAllocation> cores = coreAllocationRepository.findAll();
        Assertions.assertEquals(3, cores.size());
        for (CoreAllocation core : cores) {
            Assertions.assertEquals(0, core.getCurrentStudentCount());
        }
    }

    @Test
    @DisplayName("imageName passed to initializeCores is stored in LabConfig with ID 1L")
    void testInitializeCoresSavesImageToConfig() {
        String imageName = "teacher-specified-image:latest";
        coreAllocationService.initializeCores(5, List.of(1, 2), imageName);

        LabConfig config = labConfigRepository.findById(1L).orElseThrow();
        Assertions.assertEquals(imageName, config.getImageName());
    }

    @Test
    void initializeCores_emptyCoreList_throwsIllegalArgumentException() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> coreAllocationService.initializeCores(10, List.of(), "test-image"));
    }

    @Test
    @DisplayName("initializeCores rejects a non-positive totalStudents value")
    void initializeCores_zeroStudents_throwsIllegalArgumentException() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> coreAllocationService.initializeCores(0, List.of(1, 2), "test-image"));
    }

    @Test
    @DisplayName("getNextAvailableCore throws when both cores and lab config are missing")
    void getNextAvailableCore_coresAndConfigMissing_throwsIllegalStateException() {
        coreAllocationRepository.deleteAll();
        labConfigRepository.deleteAll();
        Assertions.assertThrows(IllegalStateException.class, () -> coreAllocationService.getNextAvailableCore());
    }

    // Test for failing when lab config is missing for any reason 
    @Test
    @DisplayName("getNextAvailableCore throws when LabConfig has been deleted")
    void getNextAvailableCore_labConfigMissing_throwsIllegalStateException() {
        coreAllocationService.initializeCores(10, List.of(1, 2, 3), "test-image");
        labConfigRepository.deleteAll();

        Assertions.assertThrows(IllegalStateException.class, () -> coreAllocationService.getNextAvailableCore());
    }

    @Test
    @DisplayName("With cores and config initialized, a single allocation succeeds and is tracked correctly")
    void initializeAndAllocate_once_incrementsTotalAllocatedStudents() {
        coreAllocationService.initializeCores(10, List.of(1, 2, 3), "test-image");

        CoreAllocation allocated = coreAllocationService.getNextAvailableCore();
        Assertions.assertEquals(1, allocated.getCurrentStudentCount());

        List<CoreAllocation> cores = coreAllocationRepository.findAll();
        long totalAllocated = cores.stream().mapToInt(CoreAllocation::getCurrentStudentCount).sum();
        Assertions.assertEquals(1, totalAllocated);
    }

    @Test
    @DisplayName("Worst-case CPU limit still allows allocations up to the configured student maximum")
    void worstCaseCpuLimit_atLimit_allAllocationsSucceed() {
        coreAllocationService.initializeCores(10, List.of(1, 2, 3), "test-image");

        List<CoreAllocation> cores = coreAllocationRepository.findAll();
        Assertions.assertEquals(3, cores.size());
        for (CoreAllocation core : cores) {
            Assertions.assertEquals(0.25, core.getCpuLimit(), 1e-9);
        }

        for (int i = 0; i < 10; i++) {
            coreAllocationService.getNextAvailableCore();
        }

        List<CoreAllocation> afterAllocations = coreAllocationRepository.findAll();
        int totalAllocated = afterAllocations.stream().mapToInt(CoreAllocation::getCurrentStudentCount).sum();
        Assertions.assertEquals(10, totalAllocated);
    }

    @Test
    @DisplayName("Allocations beyond the worst-case CPU limit fail with an exception")
    void worstCaseCpuLimit_beyondLimit_allocationFails() {
        coreAllocationService.initializeCores(10, List.of(1, 2, 3), "test-image");

        for (int i = 0; i < 10; i++) {
            coreAllocationService.getNextAvailableCore();
        }

        Assertions.assertThrows(IllegalStateException.class, () -> coreAllocationService.getNextAvailableCore());
    }

    @Test
    @DisplayName("initializeCores rejects invalid core number (Core 0) with IllegalArgumentException")
    void initializeCores_invalidCoreNumberZero_throwsIllegalArgumentException() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> coreAllocationService.initializeCores(10, List.of(0), "test-image"));
    }

    @Test
    @DisplayName("initializeCores rejects core 0 and negative core numbers")
    void initializeCores_coreZeroOrNegative_throwsIllegalArgumentException() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> coreAllocationService.initializeCores(10, List.of(0, 1, 2), "test-image"));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> coreAllocationService.initializeCores(10, List.of(-1, 1, 2), "test-image"));
    }
}
