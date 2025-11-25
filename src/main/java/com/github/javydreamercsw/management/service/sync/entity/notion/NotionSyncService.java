package com.github.javydreamercsw.management.service.sync.entity.notion;

import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import lombok.NonNull;

public interface NotionSyncService {

  /**
   * Sync entities to Notion
   *
   * @param operationId Optional operation ID for progress tracking
   * @return Result of the sync.
   */
  SyncResult syncToNotion(@NonNull String operationId);
}
