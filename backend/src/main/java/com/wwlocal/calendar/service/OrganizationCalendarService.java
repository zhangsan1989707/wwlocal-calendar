package com.wwlocal.calendar.service;

import com.wwlocal.calendar.domain.Entities.OrganizationCalendarEntity;
import com.wwlocal.calendar.dto.Requests.OrganizationCalendarRequest;
import com.wwlocal.calendar.repository.OrganizationCalendarRepository;
import org.springframework.stereotype.Service;

@Service
public class OrganizationCalendarService extends BaseCrudService<OrganizationCalendarEntity, OrganizationCalendarRequest> {
    public OrganizationCalendarService(OrganizationCalendarRepository repository) {
        super(repository);
    }
}
