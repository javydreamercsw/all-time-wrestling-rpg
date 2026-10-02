/*
* Copyright (C) 2026 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.domain.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class WellKnownTournamentTest {

  @Test
  void enumAndJsonCodesAreInSync() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    List<Map<String, Object>> dtos =
        mapper.readValue(
            new ClassPathResource("tournaments.json").getInputStream(), new TypeReference<>() {});

    Set<String> jsonCodes =
        dtos.stream()
            .map(d -> (String) d.get("code"))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Set<String> enumCodes =
        Arrays.stream(WellKnownTournament.values())
            .map(WellKnownTournament::getCode)
            .collect(Collectors.toSet());

    assertThat(enumCodes)
        .as(
            "WellKnownTournament constants not present in tournaments.json — "
                + "add the missing code(s) to the JSON or remove the obsolete enum constant(s)")
        .isSubsetOf(jsonCodes);

    assertThat(jsonCodes)
        .as(
            "tournaments.json code(s) not registered in WellKnownTournament enum — "
                + "add the missing constant(s) to WellKnownTournament")
        .isSubsetOf(enumCodes);
  }

  @Test
  void matchesAndFromCodeBehave() {
    Tournament tournament = new Tournament();
    tournament.setCode("deadly_combat");

    assertThat(WellKnownTournament.DEADLY_COMBAT.matches(tournament)).isTrue();
    assertThat(WellKnownTournament.DEADLY_COMBAT.matches(null)).isFalse();
    Tournament other = new Tournament();
    other.setCode("something_else");
    assertThat(WellKnownTournament.DEADLY_COMBAT.matches(other)).isFalse();

    assertThat(WellKnownTournament.fromCode("deadly_combat"))
        .contains(WellKnownTournament.DEADLY_COMBAT);
    assertThat(WellKnownTournament.fromCode("unknown_code")).isEmpty();
    assertThat(WellKnownTournament.fromCode(null)).isEmpty();
  }
}
