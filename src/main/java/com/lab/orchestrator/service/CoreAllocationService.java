package com.lab.orchestrator.service;

import com.lab.orchestrator.model.CoreAllocation;
import com.lab.orchestrator.repository.CoreAllocationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoreAllocationService {

    private final CoreAllocationRepository coreAllocationRepository;

    public CoreAllocationService(CoreAllocationRepository coreAllocationRepository) {
        this.coreAllocationRepository = coreAllocationRepository;
    }

    @Transactional
    public void initializeCores(int totalStudents, List<Integer> coreNumbers) {
        coreAllocationRepository.deleteAll();

        if (coreNumbers.isEmpty()) {
            return;
        }

        double cpuLimit = 1.0 / ((double) totalStudents / coreNumbers.size());

        for (Integer coreNumber : coreNumbers) {
            CoreAllocation allocation = new CoreAllocation();
            allocation.setCoreNumber(coreNumber);
            allocation.setCurrentStudentCount(0);
            allocation.setCpuLimit(cpuLimit);
            coreAllocationRepository.save(allocation);
        }
    }

    @Transactional
    public CoreAllocation getNextAvailableCore() {
        List<CoreAllocation> cores = coreAllocationRepository.findAllByOrderByCurrentStudentCountAsc();
        if (cores.isEmpty()) {
            throw new IllegalStateException("No cores available. Call initializeCores first.");
        }
        CoreAllocation emptiest = cores.get(0);
        emptiest.setCurrentStudentCount(emptiest.getCurrentStudentCount() + 1);
        return coreAllocationRepository.save(emptiest);
    }

    @Transactional
    public void releaseCore(Integer coreNumber) {
        coreAllocationRepository.findByCoreNumber(coreNumber).ifPresent(allocation -> {
            allocation.setCurrentStudentCount(Math.max(0, allocation.getCurrentStudentCount() - 1));
            coreAllocationRepository.save(allocation);
        });
    }
}
