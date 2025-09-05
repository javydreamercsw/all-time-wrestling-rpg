package com.github.javydreamercsw.management.service.referee;

import com.github.javydreamercsw.management.domain.referee.Referee;
import com.github.javydreamercsw.management.domain.referee.RefereeRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RefereeService {

  @Autowired private RefereeRepository refereeRepository;

  public List<Referee> findAll() {
    return refereeRepository.findAll();
  }
}
