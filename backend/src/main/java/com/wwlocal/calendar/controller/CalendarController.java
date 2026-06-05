package com.wwlocal.calendar.controller;

import com.wwlocal.calendar.domain.Entities.CalendarEntity;
import com.wwlocal.calendar.dto.Requests.CalendarRequest;
import com.wwlocal.calendar.service.CalendarRecordService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/calendars")
public class CalendarController extends CrudController<CalendarEntity, CalendarRequest> {
    public CalendarController(CalendarRecordService service) {
        super(service);
    }
}
