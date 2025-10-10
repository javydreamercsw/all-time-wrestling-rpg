package com.github.javydreamercsw.management.service.show.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;

@SpringBootTest
class ShowTemplateServiceTest {
  @Autowired private ShowTemplateRepository repository;
  @Autowired private ShowTemplateService service;

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
    st.setName("Test Show");
    ShowTemplate result = service.save(st);
    assertNotNull(result);
    assertEquals(st.getName(), result.getName());
    repository.delete(result);
  }
}
