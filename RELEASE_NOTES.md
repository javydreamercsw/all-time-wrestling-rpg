## [v2.12.0](https://github.com/javydreamercsw/all-time-wrestling-rpg/tree/v2.12.0) (2026-10-26)
### Dependencies

- parallelize unit tests with surefire forks (ATW-5jhs) (#627)
- **deps:** Bump @vaadin/aura from 25.3.2 to 25.3.4 (#622)
- **deps:** Bump @vaadin/number-field from 25.3.3 to 25.3.4 (#623)
- **deps:** Bump @vaadin/confirm-dialog from 25.3.2 to 25.3.4 (#624)
- **deps:** Bump @vaadin/text-field from 25.3.3 to 25.3.4 (#625)
- **deps:** Bump @vaadin/notification from 25.3.2 to 25.3.4 (#621)
- **deps:** Bump com.github.oshi:oshi-core from 7.7.1 to 7.7.2 (#617)
- **deps:** Bump com.github.oshi:oshi-core from 7.7.1 to 7.7.2 (#617)
- **deps-dev:** Bump org.graalvm.buildtools:native-maven-plugin (#616)
- **deps-dev:** Bump the npm_and_yarn group across 2 directories with 1 update (#614)
- **deps-dev:** Bump the npm_and_yarn group across 2 directories with 1 update (#601)
- **deps-dev:** Bump vite (#600)
- **deps-dev:** Bump vite (#599)
- **deps-dev:** Bump com.diffplug.spotless:spotless-maven-plugin (#595)
- **deps-dev:** Bump org.codehaus.mojo:versions-maven-plugin (#596)
### Documentation

- Regenerate Game Guide for v2.12.0
- Regenerate Game Guide for v2.12.0
### Fixed bugs

- **inbox:** dedup update notifications and clear stale ones after upgrade (ATW-rap2)
- **sync:** top up missing seed entries under skip-if-not-empty gate (ATW-dn45)
- align vite to flow-server's bundled 8.3.1 — stop the upgrade fight (ATW-qzaj)
- sweep stale activeWrestlerId when a wrestler is reassigned between accounts
- **docs:** escape <tournament> placeholder in docs feature description (ATW-h09t) (#628)
### Miscellaneous

- ignore vite npm bumps while flow-server pins 8.3.1 (ATW-qzaj)
### Other

- keep vite 8.3.5 (flow-server 25.3.1 bundled default 8.3.1 churns it back via rewrite hook)
- Update version
[Full Changelog](https://github.com/javydreamercsw/all-time-wrestling-rpg/compare/v2.11.3...v2.12.0)
