/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.ui.view.ranking;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import org.junit.jupiter.api.Test;

public class ChampionshipImageTest {

  @Test
  void testChampionshipImagesExist() {
    String[] imageNames = {
      "atw-world.png",
      "atw-extreme.png",
      "atw-intertemporal.png",
      "atw-tag-team.png",
      "time-vault.png"
    };

    for (String imageName : imageNames) {
      String resourcePath = "META-INF/resources/images/championships/" + imageName;
      InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
      assertNotNull(stream, "Image should be found in classpath: " + resourcePath);
    }
  }
}
