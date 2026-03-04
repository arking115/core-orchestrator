package com.lab.orchestrator.service;

import com.lab.orchestrator.model.CoreAllocation;
import com.lab.orchestrator.model.LabConfig;
import com.lab.orchestrator.repository.CoreAllocationRepository;
import com.lab.orchestrator.repository.LabConfigRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoreAllocationService {

    private final CoreAllocationRepository coreAllocationRepository;
    private final LabConfigRepository labConfigRepository;

    public CoreAllocationService(CoreAllocationRepository coreAllocationRepository,
                                 LabConfigRepository labConfigRepository) {
        this.coreAllocationRepository = coreAllocationRepository;
        this.labConfigRepository = labConfigRepository;
    }

    @Transactional
    public void initializeCores(int totalStudents, List<Integer> coreNumbers, String imageName) {
        if (totalStudents <= 0) {
            throw new IllegalArgumentException("totalStudents must be positive and non-zero.");
        }
        if (coreNumbers == null || coreNumbers.isEmpty()) {
            throw new IllegalArgumentException("At least one core must be provided for allocation.");
        }
        boolean hasInvalidCore = coreNumbers.stream()
                .anyMatch(core -> core == null || core <= 0);
        if (hasInvalidCore) {
            throw new IllegalArgumentException("All core numbers must be positive and non-zero (Core 0 is reserved).");
        }

        coreAllocationRepository.deleteAll();
        labConfigRepository.deleteAll();

        LabConfig config = new LabConfig();
        config.setId(1L);
        config.setMaxStudents(totalStudents);
        config.setImageName(imageName);
        labConfigRepository.save(config);

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
        LabConfig config = labConfigRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("No lab config. Call initializeCores first."));
        Integer total = coreAllocationRepository.getTotalStudentCount();
        int currentTotal = total != null ? total : 0;
        if (currentTotal + 1 > config.getMaxStudents()) {
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
