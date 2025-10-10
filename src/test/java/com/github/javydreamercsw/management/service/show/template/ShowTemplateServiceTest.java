package com.github.javydreamercsw.management.service.show.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;

@SpringBootTest
class ShowTemplateServiceTest {
  @Autowired private ShowTemplateRepository repository;
  @Autowired private ShowTemplateService service;
  @Autowired private ShowTypeService showTypeService;

  /** Test of list method, of class ShowTemplateService. */
  @Test
  void testList() {
    Pageable pageable = Pageable.ofSize(10);
    List<ShowTemplate> result = service.list(pageable);
    assertNotNull(result);
  }

  /** Test of save method, of class ShowTemplateService. */
  @Test
  void testSave() {
    ShowTemplate st = new ShowTemplate();
    Optional<ShowType> type = showTypeService.findByName("Weekly");
    assertTrue(type.isPresent());
    st.setName("Test Show");
    st.setShowType(type.get());
    ShowTemplate result = service.save(st);
    assertNotNull(result);
    assertEquals(st.getName(), result.getName());
    repository.delete(result);
  }
}
