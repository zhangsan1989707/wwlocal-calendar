package com.wwlocal.calendar.service;

import com.wwlocal.calendar.domain.Entities.AttachmentEntity;
import com.wwlocal.calendar.dto.Requests.AttachmentRequest;
import com.wwlocal.calendar.repository.AttachmentRepository;
import org.springframework.stereotype.Service;

@Service
public class AttachmentService extends BaseCrudService<AttachmentEntity, AttachmentRequest> {
    public AttachmentService(AttachmentRepository repository) {
        super(repository);
    }
}
