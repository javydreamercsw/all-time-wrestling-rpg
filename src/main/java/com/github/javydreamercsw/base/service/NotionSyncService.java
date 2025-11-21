package com.github.javydreamercsw.base.service;

import com.github.javydreamercsw.base.domain.AbstractEntity;

public interface NotionSyncService<T extends AbstractEntity<?>> {

  void syncToNotion(T entity);
}
