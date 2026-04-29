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
package com.github.javydreamercsw.management.domain.show.export;

import org.springframework.stereotype.Component;

@Component
public class XShowCardFormatter extends AbstractSocialMediaFormatter {

  @Override
  public String getFormatName() {
    return "X (Twitter)";
  }

  @Override
  protected String getHashtags() {
    return "#ATW #Wrestling";
  }

  @Override
  protected String limitLength(String text) {
    if (text.length() > 280) {
      return text.substring(0, 277) + "...";
    }
    return text;
  }

  @Override
  public int getPriority() {
    return 30;
  }
}
