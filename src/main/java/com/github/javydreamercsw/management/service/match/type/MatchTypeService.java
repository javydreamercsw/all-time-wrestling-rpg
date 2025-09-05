package com.github.javydreamercsw.management.service.match.type;

import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
import com.github.javydreamercsw.management.domain.show.match.type.MatchTypeRepository;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class MatchTypeService {

  private final MatchTypeRepository matchTypeRepository;

  public MatchTypeService(MatchTypeRepository matchTypeRepository) {
    this.matchTypeRepository = matchTypeRepository;
  }

  public Optional<MatchType> findByName(String name) {
    return matchTypeRepository.findByName(name);
  }

  public List<MatchType> findAll() {
    return matchTypeRepository.findAll();
  }

  @Transactional
  public MatchType createMatchType(MatchType matchType) {
    log.info("Creating new match type: {}", matchType.getName());
    return matchTypeRepository.save(matchType);
  }

  @Transactional
  public MatchType createOrUpdateMatchType(@NonNull String name, @NonNull String description) {
    Optional<MatchType> existingOpt = matchTypeRepository.findByName(name);
    MatchType matchType;
    if (existingOpt.isPresent()) {
      matchType = existingOpt.get();
      log.debug("Updating existing match type: {}", name);
    } else {
      matchType = new MatchType();
      log.info("Creating new match type: {}", name);
    }
    matchType.setName(name);
    matchType.setDescription(description);
    return matchTypeRepository.save(matchType);
  }
}
