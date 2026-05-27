## [v2.0.0](https://github.com/javydreamercsw/all-time-wrestling-rpg/tree/v2.0.0) (2026-05-27)

[Full Changelog](https://github.com/javydreamercsw/all-time-wrestling-rpg/compare/v2.0.0-RC1...v2.0.0)

**Implemented enhancements:**

- feat: non-blocking AI narration and image generation \(ATW-44p\) [\#330](https://github.com/javydreamercsw/all-time-wrestling-rpg/pull/330) ([javydreamercsw](https://github.com/javydreamercsw))
- feat: granular CI path filters to skip E2E/integration tests on unrelated changes \(ATW-8rj\) [\#325](https://github.com/javydreamercsw/all-time-wrestling-rpg/pull/325) ([javydreamercsw](https://github.com/javydreamercsw))
- fix\(show-planning\): advisory MUST\_BOOK warnings, hard-block on STIPULATION\_REQUIRED, rivalry helpers, and test/doc coverage [\#317](https://github.com/javydreamercsw/all-time-wrestling-rpg/pull/317) ([javydreamercsw](https://github.com/javydreamercsw))
- feat: Flyway checksum safeguard, H2 migration integration test, and Vaadin 25.1.2 upgrade [\#316](https://github.com/javydreamercsw/all-time-wrestling-rpg/pull/316) ([javydreamercsw](https://github.com/javydreamercsw))
- test\(ATW-8a4\): add unit tests for missing Notion sync outgoing services [\#315](https://github.com/javydreamercsw/all-time-wrestling-rpg/pull/315) ([javydreamercsw](https://github.com/javydreamercsw))
-  feat\(ATW-nkx\): rivalry heat escalation and feud resolution pipeline [\#314](https://github.com/javydreamercsw/all-time-wrestling-rpg/pull/314) ([javydreamercsw](https://github.com/javydreamercsw))
- feat\(ATW-asl\): add unit tests for image generation services [\#311](https://github.com/javydreamercsw/all-time-wrestling-rpg/pull/311) ([javydreamercsw](https://github.com/javydreamercsw))
- test: add unit tests for AI narration provider services \(ATW-cyv\) [\#310](https://github.com/javydreamercsw/all-time-wrestling-rpg/pull/310) ([javydreamercsw](https://github.com/javydreamercsw))
- feat\(ATW-xz4\): replace condition HP penalty with wear-and-tear bump roll [\#309](https://github.com/javydreamercsw/all-time-wrestling-rpg/pull/309) ([javydreamercsw](https://github.com/javydreamercsw))
- feat\(ATW-aq5\): wire CampaignEffectContext stubs to CampaignState and featureData [\#308](https://github.com/javydreamercsw/all-time-wrestling-rpg/pull/308) ([javydreamercsw](https://github.com/javydreamercsw))
- feat\(ATW-46y\): wire per-universe expansion settings to runtime content filtering [\#307](https://github.com/javydreamercsw/all-time-wrestling-rpg/pull/307) ([javydreamercsw](https://github.com/javydreamercsw))
- feat\(ATW-38r\): enforce per-universe wrestler exclusions in findAllFiltered [\#306](https://github.com/javydreamercsw/all-time-wrestling-rpg/pull/306) ([javydreamercsw](https://github.com/javydreamercsw))

**Fixed bugs:**

- Fix/rivalry heat system [\#313](https://github.com/javydreamercsw/all-time-wrestling-rpg/pull/313) ([javydreamercsw](https://github.com/javydreamercsw))
- fix\(ATW-5f0\): preserve HTTP error codes in image generation services [\#312](https://github.com/javydreamercsw/all-time-wrestling-rpg/pull/312) ([javydreamercsw](https://github.com/javydreamercsw))

**Merged pull requests:**

- Build\(deps\): Bump org.openrewrite.recipe:rewrite-migrate-java from 3.34.0 to 3.35.0 [\#324](https://github.com/javydreamercsw/all-time-wrestling-rpg/pull/324) ([dependabot[bot]](https://github.com/apps/dependabot))
- Build\(deps-dev\): Bump org.openrewrite.maven:rewrite-maven-plugin from 6.38.0 to 6.40.0 [\#323](https://github.com/javydreamercsw/all-time-wrestling-rpg/pull/323) ([dependabot[bot]](https://github.com/apps/dependabot))
- Build\(deps\): Bump org.openrewrite.recipe:rewrite-static-analysis from 2.34.1 to 2.35.0 [\#322](https://github.com/javydreamercsw/all-time-wrestling-rpg/pull/322) ([dependabot[bot]](https://github.com/apps/dependabot))
- Build\(deps\): Bump com.github.oshi:oshi-core from 7.1.0 to 7.2.1 [\#321](https://github.com/javydreamercsw/all-time-wrestling-rpg/pull/321) ([dependabot[bot]](https://github.com/apps/dependabot))
- Add docs E2E coverage for undocumented views and add validate-videos CI job [\#319](https://github.com/javydreamercsw/all-time-wrestling-rpg/pull/319) ([javydreamercsw](https://github.com/javydreamercsw))
- fix\(qr\): use LAN IP in QR share URL when no HTTP request context is a… [\#318](https://github.com/javydreamercsw/all-time-wrestling-rpg/pull/318) ([javydreamercsw](https://github.com/javydreamercsw))
