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
        if (totalStudents <= 0) {
            throw new IllegalArgumentException("totalStudents must be positive and non-zero.");
        }
        coreAllocationRepository.deleteAll();

        if (coreNumbers == null || coreNumbers.isEmpty()) {
            throw new IllegalArgumentException("At least one core must be provided for allocation.");
        }

        // Pessimistic resource allocation, calculation of the limit based on the worst case core.
        int maxStudentsPerCore = (int) Math.ceil((double) totalStudents / coreNumbers.size());

        double cpuLimit = 1.0 / maxStudentsPerCore;

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
        int currentTotal = cores.stream()
                .mapToInt(CoreAllocation::getCurrentStudentCount)
                .sum();
        double cpuLimit = cores.get(0).getCpuLimit();
        // reverse math to find the max total number of students that can be allocated
        int maxTotal = (int) Math.round(cores.size() / cpuLimit);
        if (currentTotal + 1 > maxTotal) {
            throw new IllegalStateException("At capacity: cannot allocate more students.");
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
