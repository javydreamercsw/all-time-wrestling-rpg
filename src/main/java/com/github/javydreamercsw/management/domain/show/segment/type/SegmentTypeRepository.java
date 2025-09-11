package com.github.javydreamercsw.management.domain.show.segment.type;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SegmentTypeRepository extends JpaRepository<SegmentType, Long> {

  Optional<SegmentType> findByName(String name);
}
