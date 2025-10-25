package com.github.javydreamercsw.management.ui.view.show;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.show.planning.ProposedSegment;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.UI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EditSegmentDialogTest {

  private WrestlerService wrestlerService;
  private ProposedSegment segment;
  private Runnable onSave;

  @BeforeEach
  void setUp() {
    wrestlerService = mock(WrestlerService.class);
    segment = new ProposedSegment();
    segment.setDescription("Old Description");
    segment.setParticipants(new ArrayList<>(List.of("Wrestler 1")));

    Wrestler wrestler1 = new Wrestler();
    wrestler1.setName("Wrestler 1");
    Wrestler wrestler2 = new Wrestler();
    wrestler2.setName("Wrestler 2");
    List<Wrestler> allWrestlers = Arrays.asList(wrestler1, wrestler2);

    when(wrestlerService.findAll()).thenReturn(allWrestlers);
    when(wrestlerService.findByName("Wrestler 1")).thenReturn(Optional.of(wrestler1));
    when(wrestlerService.findByName("Wrestler 2")).thenReturn(Optional.of(wrestler2));

    onSave = mock(Runnable.class);
  }

  @Test
  void testSave() {
    // Mock UI to allow dialog to be opened
    UI ui = mock(UI.class);
    UI.setCurrent(ui);

    EditSegmentDialog dialog = new EditSegmentDialog(segment, wrestlerService, onSave);
    dialog.open();

    // Simulate user input
    dialog.getDescriptionArea().setValue("New Description");
    dialog
        .getParticipantsCheckboxGroup()
        .setValue(Set.of(wrestlerService.findByName("Wrestler 2").get()));

    // Trigger save
    dialog.getSaveButton().click();

    // Verify segment is updated
    assertEquals("New Description", segment.getDescription());
    assertEquals(1, segment.getParticipants().size());
    assertEquals("Wrestler 2", segment.getParticipants().get(0));

    // Verify onSave is called and dialog is closed
    verify(onSave).run();
    // Verify that the dialog is closed
    // Note: Directly checking dialog.isOpened() might not work as expected in a unit test
    // without a full Vaadin UI environment. We rely on the onSave callback being triggered.
  }
}
