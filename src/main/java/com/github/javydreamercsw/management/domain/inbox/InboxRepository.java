package com.github.javydreamercsw.management.domain.inbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface InboxRepository
    extends JpaRepository<InboxItem, Long>, JpaSpecificationExecutor<InboxItem> {}
