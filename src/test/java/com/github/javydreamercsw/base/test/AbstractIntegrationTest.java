package com.github.javydreamercsw.base.test;

import com.github.javydreamercsw.Application;
import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeudRepository;
import com.github.javydreamercsw.management.domain.inbox.InboxRepository;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public abstract class AbstractIntegrationTest {

  protected static InboxRepository inboxRepository; // Added static InboxRepository
  protected static WrestlerRepository wrestlerRepository;
  protected static MultiWrestlerFeudService multiWrestlerFeudService;
  protected static SeasonRepository seasonRepository;
  protected static SegmentService segmentService;
  protected static SeasonService seasonService;
  protected static RivalryService rivalryService;
  protected static TitleService titleService;
  protected static ShowService showService;
  protected static SegmentTypeService segmentTypeService;
  protected static SegmentRuleService segmentRuleService;
  protected static SegmentRepository segmentRepository;
  protected static MultiWrestlerFeudRepository multiWrestlerFeudRepository;
  protected static ShowRepository showRepository;
  protected static ShowTypeRepository showTypeRepository;
  protected static SegmentTypeRepository segmentTypeRepository;
  protected static FactionService factionService;
  protected static WrestlerService wrestlerService;
  protected static ShowTemplateRepository showTemplateRepository;
  private static final ConfigurableApplicationContext context;
  protected static int serverPort;

  static {
    serverPort = Integer.parseInt(System.getProperty("server.port", "9090"));
    String[] args = {
      "--server.port=" + serverPort, "--spring.profiles.active=test",
    };
    log.info("Attempting to start Spring Boot application for E2E tests on port {}", serverPort);
    context = SpringApplication.run(Application.class, args);
    inboxRepository = context.getBean(InboxRepository.class);
    wrestlerRepository = context.getBean(WrestlerRepository.class);
    multiWrestlerFeudService = context.getBean(MultiWrestlerFeudService.class);
    seasonRepository = context.getBean(SeasonRepository.class);
    segmentService = context.getBean(SegmentService.class);
    seasonService = context.getBean(SeasonService.class);
    rivalryService = context.getBean(RivalryService.class);
    titleService = context.getBean(TitleService.class);
    showService = context.getBean(ShowService.class);
    segmentTypeService = context.getBean(SegmentTypeService.class);
    segmentRuleService = context.getBean(SegmentRuleService.class);
    segmentRepository = context.getBean(SegmentRepository.class);
    multiWrestlerFeudRepository = context.getBean(MultiWrestlerFeudRepository.class);
    showRepository = context.getBean(ShowRepository.class);
    showTypeRepository = context.getBean(ShowTypeRepository.class);
    segmentTypeRepository = context.getBean(SegmentTypeRepository.class);
    factionService = context.getBean(FactionService.class);
    wrestlerService = context.getBean(WrestlerService.class);
    showTemplateRepository = context.getBean(ShowTemplateRepository.class);
    log.info("Spring Boot application started for E2E tests.");
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  log.info("Shutting down Spring Boot application for E2E tests.");
                  context.close();
                }));
  }

  protected SegmentNarrationService.SegmentNarrationContext createCustomSegmentContext() {
    SegmentNarrationService.SegmentNarrationContext context =
        new SegmentNarrationService.SegmentNarrationContext();

    // Match Type
    SegmentNarrationService.SegmentTypeContext matchType =
        new SegmentNarrationService.SegmentTypeContext();
    matchType.setSegmentType("Hell in a Cell");
    matchType.setStipulation("King of the Ring 1998");
    matchType.setRules(java.util.Arrays.asList("No Disqualification", "Falls Count Anywhere"));
    context.setSegmentType(matchType);

    // Venue
    SegmentNarrationService.VenueContext venue = new SegmentNarrationService.VenueContext();
    venue.setName("Civic Arena");
    venue.setLocation("Pittsburgh, Pennsylvania");
    venue.setType("Indoor Arena");
    venue.setCapacity(17_000);
    venue.setDescription("Historic venue for legendary matches");
    venue.setAtmosphere("Intense and foreboding");
    venue.setSignificance("Site of the most famous Hell in a Cell segment");
    context.setVenue(venue);

    // Wrestlers
    SegmentNarrationService.WrestlerContext undertaker =
        new SegmentNarrationService.WrestlerContext();
    undertaker.setName("The Undertaker");
    undertaker.setDescription("The Deadman - Phenom of WWE");

    com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext mankind =
        new com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext();
    mankind.setName("Mankind");
    mankind.setDescription("Hardcore legend Mick Foley");

    context.setWrestlers(java.util.Arrays.asList(undertaker, mankind));

    // Context
    context.setAudience("Shocked and horrified crowd of 17,000");
    context.setDeterminedOutcome(
        "The Undertaker wins after Mankind is thrown off the Hell in a Cell");

    return context;
  }
}
