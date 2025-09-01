package com.github.javydreamercsw.management.service.show.type;

import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class ShowTypeService {
  private final ShowTypeRepository showTypeRepository;
  private final Clock clock;

  ShowTypeService(ShowTypeRepository showTypeRepository, Clock clock) {
    this.showTypeRepository = showTypeRepository;
    this.clock = clock;
  }

  public List<ShowType> list(Pageable pageable) {
    return showTypeRepository.findAllBy(pageable).toList();
  }

  public long count() {
    return showTypeRepository.count();
  }

  public ShowType save(@NonNull ShowType showType) {
    showType.setCreationDate(clock.instant());
    return showTypeRepository.saveAndFlush(showType);
  }

  public List<ShowType> findAll() {
    return showTypeRepository.findAll();
  }

  /**
   * Find a show type by name.
   *
   * @param name The name of the show type
   * @return Optional containing the show type if found
   */
  public Optional<ShowType> findByName(@NonNull String name) {
    return showTypeRepository.findByName(name);
  }

  /**
   * Check if a show type exists by name.
   *
   * @param name The name to check
   * @return true if a show type with this name exists
   */
  public boolean existsByName(@NonNull String name) {
    return showTypeRepository.existsByName(name);
  }
}
