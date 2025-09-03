package com.github.javydreamercsw.management.service.referee;

import com.github.javydreamercsw.management.domain.referee.Referee;
import com.github.javydreamercsw.management.domain.referee.RefereeRepository;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class RefereeService {

  @Autowired private RefereeRepository refereeRepository;

  public Optional<Referee> findByName(@NonNull String name) {
    return refereeRepository.findByName(name);
  }

  @Transactional
  public Referee createReferee(@NonNull Referee referee) {
    log.info("Creating new referee: {}", referee.getName());
    return refereeRepository.save(referee);
  }
}
