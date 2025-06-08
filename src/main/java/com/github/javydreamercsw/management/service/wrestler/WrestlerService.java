package com.github.javydreamercsw.management.service.wrestler;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Clock;
import java.util.List;
import lombok.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class WrestlerService {

  private final WrestlerRepository wrestlerRepository;
  private final Clock clock;

  WrestlerService(WrestlerRepository wrestlerRepository, Clock clock) {
    this.wrestlerRepository = wrestlerRepository;
    this.clock = clock;
  }

  public void createCard(@NonNull String name) {
    Wrestler wrestler = new Wrestler();
    wrestler.setName(name);
    // Set default values
    wrestler.setDeckSize(15);
    wrestler.setStartingHealth(15);
    wrestler.setLowHealth(0);
    wrestler.setStartingStamina(0);
    wrestler.setLowStamina(0);
    save(wrestler);
  }

  public List<Wrestler> list(Pageable pageable) {
    return wrestlerRepository.findAllBy(pageable).toList();
  }

  public long count() {
    return wrestlerRepository.count();
  }

  public Wrestler save(@NonNull Wrestler wrestler) {
    wrestler.setCreationDate(clock.instant());
    return wrestlerRepository.saveAndFlush(wrestler);
  }

  public void delete(@NonNull Wrestler wrestler) {
    wrestlerRepository.delete(wrestler);
  }

  public List<Wrestler> findAll() {
    return wrestlerRepository.findAll();
  }
}
