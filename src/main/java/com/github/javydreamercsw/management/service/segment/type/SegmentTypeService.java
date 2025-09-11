package com.github.javydreamercsw.management.service.segment.type;

import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SegmentTypeService {

  private final SegmentTypeRepository segmentTypeRepository;

  public SegmentTypeService(@NonNull SegmentTypeRepository segmentTypeRepository) {
    this.segmentTypeRepository = segmentTypeRepository;
  }

  public Optional<SegmentType> findByName(@NonNull String name) {
    return segmentTypeRepository.findByName(name);
  }

  public List<SegmentType> findAll() {
    return segmentTypeRepository.findAll();
  }

  @Transactional
  public SegmentType createSegmentType(@NonNull SegmentType segmentType) {
    log.info("Creating new segment type: {}", segmentType.getName());
    return segmentTypeRepository.save(segmentType);
  }

  @Transactional
  public SegmentType createOrUpdateSegmentType(@NonNull String name, @NonNull String description) {
    Optional<SegmentType> existingOpt = segmentTypeRepository.findByName(name);
    SegmentType segmentType;
    if (existingOpt.isPresent()) {
      segmentType = existingOpt.get();
      log.debug("Updating existing segment type: {}", name);
    } else {
      segmentType = new SegmentType();
      log.info("Creating new segment type: {}", name);
    }
    segmentType.setName(name);
    segmentType.setDescription(description);
    return segmentTypeRepository.save(segmentType);
  }

  @Transactional
  public void deleteSegmentType(@NonNull Long id) {
    if (segmentTypeRepository.existsById(id)) {
      segmentTypeRepository.deleteById(id);
      log.info("Deleted segment type with ID: {}", id);
    } else {
      log.warn("Segment type with ID {} not found for deletion.", id);
      throw new IllegalArgumentException("Segment type not found with ID: " + id);
    }
  }
}
