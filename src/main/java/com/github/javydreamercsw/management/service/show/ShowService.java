package com.github.javydreamercsw.management.service.show;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import java.time.Clock;
import java.util.List;
import lombok.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class ShowService {
  private final ShowRepository showRepository;
  private final Clock clock;

  ShowService(ShowRepository showRepository, Clock clock) {
    this.showRepository = showRepository;
    this.clock = clock;
  }

  public List<Show> list(Pageable pageable) {
    return showRepository.findAllBy(pageable).toList();
  }

  public long count() {
    return showRepository.count();
  }

  public Show save(@NonNull Show showType) {
    showType.setCreationDate(clock.instant());
    return showRepository.saveAndFlush(showType);
  }

  public List<Show> findAll() {
    return showRepository.findAll();
  }
}
