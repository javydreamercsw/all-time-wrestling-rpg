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
package com.github.javydreamercsw.base.ai.notion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import notion.api.v1.model.pages.PageProperty;
import org.junit.jupiter.api.Test;

class NotionPropertyBuilderTest {

  @Test
  void testCreateRichTextPropertyLongContent() {
    // 2000 * 2 + 100 = 4100 characters
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 4100; i++) {
      sb.append("a");
    }
    String longContent = sb.toString();

    PageProperty property = NotionPropertyBuilder.createRichTextProperty(longContent);

    assertNotNull(property.getRichText());
    List<PageProperty.RichText> richTextList = property.getRichText();

    // Should be split into 3 chunks: 2000, 2000, 100
    assertEquals(3, richTextList.size());
    assertEquals(2000, richTextList.get(0).getText().getContent().length());
    assertEquals(2000, richTextList.get(1).getText().getContent().length());
    assertEquals(100, richTextList.get(2).getText().getContent().length());

    // Reconstruct and verify
    StringBuilder reconstructed = new StringBuilder();
    for (PageProperty.RichText rt : richTextList) {
      reconstructed.append(rt.getText().getContent());
    }
    assertEquals(longContent, reconstructed.toString());
  }

  @Test
  void testCreateTitlePropertyLongContent() {
    // 2000 + 500 = 2500 characters
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 2500; i++) {
      sb.append("b");
    }
    String longContent = sb.toString();

    PageProperty property = NotionPropertyBuilder.createTitleProperty(longContent);

    assertNotNull(property.getTitle());
    List<PageProperty.RichText> richTextList = property.getTitle();

    // Should be split into 2 chunks: 2000, 500
    assertEquals(2, richTextList.size());
    assertEquals(2000, richTextList.get(0).getText().getContent().length());
    assertEquals(500, richTextList.get(1).getText().getContent().length());

    // Reconstruct and verify
    StringBuilder reconstructed = new StringBuilder();
    for (PageProperty.RichText rt : richTextList) {
      reconstructed.append(rt.getText().getContent());
    }
    assertEquals(longContent, reconstructed.toString());
  }

  @Test
  void testCreateRichTextPropertyShortContent() {
    String content = "short content";
    PageProperty property = NotionPropertyBuilder.createRichTextProperty(content);

    assertNotNull(property.getRichText());
    List<PageProperty.RichText> richTextList = property.getRichText();

    assertEquals(1, richTextList.size());
    assertEquals(content, richTextList.get(0).getText().getContent());
  }

  @Test
  void testCreateRichTextPropertyEmptyContent() {
    String content = "";
    PageProperty property = NotionPropertyBuilder.createRichTextProperty(content);

    assertNotNull(property.getRichText());
    List<PageProperty.RichText> richTextList = property.getRichText();

    assertEquals(1, richTextList.size());
    assertEquals("", richTextList.get(0).getText().getContent());
  }
}
