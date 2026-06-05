package com.wwlocal.calendar.controller;

import com.wwlocal.calendar.domain.Entities.OrganizationCalendarEntity;
import com.wwlocal.calendar.dto.Requests.OrganizationCalendarRequest;
import com.wwlocal.calendar.service.OrganizationCalendarService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organization-calendars")
public class OrganizationCalendarController extends CrudController<OrganizationCalendarEntity, OrganizationCalendarRequest> {
    public OrganizationCalendarController(OrganizationCalendarService service) {
        super(service);
    }
}
