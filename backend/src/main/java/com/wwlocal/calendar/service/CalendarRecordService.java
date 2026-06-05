package com.wwlocal.calendar.service;

import com.wwlocal.calendar.domain.Entities.CalendarEntity;
import com.wwlocal.calendar.dto.Requests.CalendarRequest;
import com.wwlocal.calendar.repository.CalendarRepository;
import org.springframework.stereotype.Service;

@Service
public class CalendarRecordService extends BaseCrudService<CalendarEntity, CalendarRequest> {
    public CalendarRecordService(CalendarRepository repository) {
        super(repository);
    }
}
