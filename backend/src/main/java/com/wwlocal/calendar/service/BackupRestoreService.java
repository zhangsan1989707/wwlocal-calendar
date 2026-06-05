package com.wwlocal.calendar.service;

import com.wwlocal.calendar.domain.Entities.BackupRestoreEntity;
import com.wwlocal.calendar.dto.Requests.BackupRestoreRequest;
import com.wwlocal.calendar.repository.BackupRestoreRepository;
import org.springframework.stereotype.Service;

@Service
public class BackupRestoreService extends BaseCrudService<BackupRestoreEntity, BackupRestoreRequest> {
    public BackupRestoreService(BackupRestoreRepository repository) {
        super(repository);
    }
}
