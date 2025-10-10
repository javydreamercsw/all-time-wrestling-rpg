package com.github.javydreamercsw.base.test;

import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

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

  protected static boolean isNotionTokenAvailable() {
    return EnvironmentVariableUtil.isNotionTokenAvailable();
  }
}
