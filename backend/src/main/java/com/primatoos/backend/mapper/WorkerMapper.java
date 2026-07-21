package com.primatoos.backend.mapper;

import com.primatoos.backend.dto.worker.WorkerResponse;
import com.primatoos.backend.model.Worker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkerMapper {

    private final UserMapper userMapper;

    public WorkerResponse toResponse(Worker worker) {
        return new WorkerResponse(
                worker.getId(),
                worker.getName(),
                worker.getFunction(),
                worker.getPhone(),
                worker.isActive(),
                userMapper.toSummary(worker.getUser()),
                worker.getCreatedAt()
        );
    }
}
