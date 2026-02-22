package com.lab.orchestrator;

import com.lab.orchestrator.model.CoreAllocation;
import com.lab.orchestrator.repository.CoreAllocationRepository;
import com.lab.orchestrator.repository.LabConfigRepository;
import com.lab.orchestrator.service.CoreAllocationService;
import java.util.List;
import org.junit.jupiter.api.Assertions;
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
    void testEvenDistribution() {
        coreAllocationService.initializeCores(30, List.of(0, 1, 2));

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
    void testCpuLimitCalculation() {
        coreAllocationService.initializeCores(10, List.of(0, 1));

        List<CoreAllocation> cores = coreAllocationRepository.findAll();
        Assertions.assertEquals(2, cores.size());
        for (CoreAllocation core : cores) {
            Assertions.assertEquals(0.2, core.getCpuLimit(), 1e-9);
        }
    }

    @Test
    void testReleaseCore() {
        coreAllocationService.initializeCores(1, List.of(0));
        CoreAllocation allocated = coreAllocationService.getNextAvailableCore();
        Assertions.assertEquals(1, allocated.getCurrentStudentCount());

        coreAllocationService.releaseCore(allocated.getCoreNumber());

        CoreAllocation afterRelease = coreAllocationRepository.findByCoreNumber(allocated.getCoreNumber()).orElseThrow();
        Assertions.assertEquals(0, afterRelease.getCurrentStudentCount());
    }

    // Test for spreading the load evenly across the cores
    @Test
    void testLeastCongestedCoreMaxDifferenceOne() {
        coreAllocationService.initializeCores(12, List.of(0, 1, 2));

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
    void testGetNextAvailableCoreThrowsWhenNoCoresInitialized() {
        Assertions.assertThrows(IllegalStateException.class, () -> coreAllocationService.getNextAvailableCore());
    }

    // Test for failing when at capacity
    @Test
    void testAllocationFailsWhenAtCapacity() {
        coreAllocationService.initializeCores(30, List.of(0, 1, 2));

        for (int i = 0; i < 30; i++) {
            coreAllocationService.getNextAvailableCore();
        }

        Assertions.assertThrows(IllegalStateException.class, () -> coreAllocationService.getNextAvailableCore());
    }

    @Test
    // Test for initializing cores and students and checking that no allocations have been made yet
    void testCoresAndStudentsInitializedNoAllocationsYet() {
        coreAllocationService.initializeCores(12, List.of(0, 1, 2));

        List<CoreAllocation> cores = coreAllocationRepository.findAll();
        Assertions.assertEquals(3, cores.size());
        for (CoreAllocation core : cores) {
            Assertions.assertEquals(0, core.getCurrentStudentCount());
        }
    }

    @Test
    void testGetNextAvailableCoreThrowsWhenCoresNotInitialized() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> coreAllocationService.initializeCores(10, List.of()));
    }

    @Test
    void testInitializeCoresThrowsWhenStudentsNotInitialized() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> coreAllocationService.initializeCores(0, List.of(0, 1)));
    }

    @Test
    void testGetNextAvailableCoreThrowsWhenBothNotInitialized() {
        Assertions.assertThrows(IllegalStateException.class, () -> coreAllocationService.getNextAvailableCore());
    }

    // Test for failing when lab config is missing for any reason 
    @Test
    void testGetNextAvailableCoreThrowsWhenLabConfigMissing() {
        coreAllocationService.initializeCores(10, List.of(0, 1, 2));
        labConfigRepository.deleteAll();

        Assertions.assertThrows(IllegalStateException.class, () -> coreAllocationService.getNextAvailableCore());
    }

    @Test
    void testBothInitializedEverythingWorks() {
        coreAllocationService.initializeCores(10, List.of(0, 1, 2));

        CoreAllocation allocated = coreAllocationService.getNextAvailableCore();
        Assertions.assertEquals(1, allocated.getCurrentStudentCount());

        List<CoreAllocation> cores = coreAllocationRepository.findAll();
        long totalAllocated = cores.stream().mapToInt(CoreAllocation::getCurrentStudentCount).sum();
        Assertions.assertEquals(1, totalAllocated);
    }

    @Test
    void testWorstCaseCpuLimitCalculationAtLimitSucceeds() {
        coreAllocationService.initializeCores(10, List.of(0, 1, 2));

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
    void testWorstCaseCpuLimitCalculationFailsWhenPastLimit() {
        coreAllocationService.initializeCores(10, List.of(0, 1, 2));

        for (int i = 0; i < 10; i++) {
            coreAllocationService.getNextAvailableCore();
        }

        Assertions.assertThrows(IllegalStateException.class, () -> coreAllocationService.getNextAvailableCore());
    }
}
