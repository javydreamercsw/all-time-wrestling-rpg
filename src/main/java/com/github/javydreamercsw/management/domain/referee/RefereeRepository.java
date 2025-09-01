package com.github.javydreamercsw.management.domain.referee;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefereeRepository extends JpaRepository<Referee, Long> {
  Optional<Referee> findByName(String name);
}
