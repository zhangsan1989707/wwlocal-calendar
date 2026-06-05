package com.wwlocal.calendar.controller;

import com.wwlocal.calendar.domain.Entities.AttachmentEntity;
import com.wwlocal.calendar.dto.Requests.AttachmentRequest;
import com.wwlocal.calendar.service.AttachmentService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/attachments")
public class AttachmentController extends CrudController<AttachmentEntity, AttachmentRequest> {
    public AttachmentController(AttachmentService service) {
        super(service);
    }
}
