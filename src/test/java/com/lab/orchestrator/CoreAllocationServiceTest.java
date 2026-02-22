package com.lab.orchestrator;

import com.lab.orchestrator.model.CoreAllocation;
import com.lab.orchestrator.repository.CoreAllocationRepository;
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
}
