package com.github.javydreamercsw.management.service.referee;

import com.github.javydreamercsw.management.domain.referee.Referee;
import com.github.javydreamercsw.management.domain.referee.RefereeRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class RefereeService {

  private final RefereeRepository refereeRepository;

  public RefereeService(RefereeRepository refereeRepository) {
    this.refereeRepository = refereeRepository;
  }

  public Optional<Referee> findByName(String name) {
    return refereeRepository.findByName(name);
  }

  @Transactional
  public Referee createReferee(Referee referee) {
    log.info("Creating new referee: {}", referee.getName());
    return refereeRepository.save(referee);
  }
}
