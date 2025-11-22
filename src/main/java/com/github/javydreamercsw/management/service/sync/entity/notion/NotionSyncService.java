package com.github.javydreamercsw.management.service.sync.entity.notion;

import com.github.javydreamercsw.base.domain.AbstractEntity;

public interface NotionSyncService<T extends AbstractEntity<?>> {

  void syncToNotion(T entity);
}
