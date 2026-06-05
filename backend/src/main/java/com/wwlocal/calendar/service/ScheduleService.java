package com.wwlocal.calendar.service;

import com.wwlocal.calendar.domain.Entities.ScheduleEntity;
import com.wwlocal.calendar.dto.Requests.ScheduleRequest;
import com.wwlocal.calendar.repository.ScheduleRepository;
import org.springframework.stereotype.Service;

@Service
public class ScheduleService extends BaseCrudService<ScheduleEntity, ScheduleRequest> {
    public ScheduleService(ScheduleRepository repository) {
        super(repository);
    }
}
