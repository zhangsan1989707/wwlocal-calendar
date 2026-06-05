package com.wwlocal.calendar.controller;

import com.wwlocal.calendar.domain.Entities.BackupRestoreEntity;
import com.wwlocal.calendar.dto.Requests.BackupRestoreRequest;
import com.wwlocal.calendar.service.BackupRestoreService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/backup-restores")
public class BackupRestoreController extends CrudController<BackupRestoreEntity, BackupRestoreRequest> {
    public BackupRestoreController(BackupRestoreService service) {
        super(service);
    }
}
