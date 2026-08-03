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
package com.github.javydreamercsw.management.service.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

class CampaignChapterServiceLoadTest {

  private static final String FOLDER_PATTERN = "classpath*:campaigns/*/_chapter.json";
  private static final String FLAT_PATTERN = "classpath*:campaigns/*.json";

  private ObjectMapper objectMapper;
  private ResourcePatternResolver resolver;
  private CampaignChapterService service;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    FeatureDataService featureDataService =
        new FeatureDataService(objectMapper, mock(CampaignStateRepository.class));
    ExpansionService expansionService = mock(ExpansionService.class);
    when(expansionService.isExpansionEnabled(anyString())).thenReturn(true);
    resolver = mock(ResourcePatternResolver.class);
    service =
        new CampaignChapterService(objectMapper, featureDataService, expansionService, resolver);
  }

  // -------------------------------------------------------------------------
  // Folder-scan outer catch (lines ~108-110)
  // -------------------------------------------------------------------------

  @Test
  void folderScanThrows_doesNotCrash() throws IOException {
    when(resolver.getResources(FOLDER_PATTERN)).thenThrow(new IOException("disk error"));
    when(resolver.getResources(FLAT_PATTERN)).thenReturn(new Resource[0]);

    service.loadChapters();

    assertThat(service.getAllChapters()).isEmpty();
  }

  // -------------------------------------------------------------------------
  // Chapter InputStream inner catch (lines ~104-106)
  // -------------------------------------------------------------------------

  @Test
  void chapterInputStreamThrows_chapterSkipped() throws IOException {
    Resource chapterResource = mock(Resource.class);
    when(chapterResource.getURL()).thenReturn(new URL("file:/fake/campaigns/broken/_chapter.json"));
    when(chapterResource.getInputStream()).thenThrow(new IOException("chapter read error"));

    when(resolver.getResources(FOLDER_PATTERN)).thenReturn(new Resource[] {chapterResource});
    when(resolver.getResources(FLAT_PATTERN)).thenReturn(new Resource[0]);

    service.loadChapters();

    assertThat(service.getAllChapters()).isEmpty();
  }

  // -------------------------------------------------------------------------
  // Encounter InputStream inner catch (lines ~99-101)
  // -------------------------------------------------------------------------

  @Test
  void encounterInputStreamThrows_chapterLoadedWithNoEncounters() throws IOException {
    String chapterJson = "{\"id\":\"test-ch\",\"title\":\"Test Chapter\",\"staticEncounters\":[]}";
    Resource chapterResource = mock(Resource.class);
    URL chapterUrl = new URL("file:/fake/campaigns/test/_chapter.json");
    when(chapterResource.getURL()).thenReturn(chapterUrl);
    when(chapterResource.getInputStream())
        .thenReturn(new ByteArrayInputStream(chapterJson.getBytes(StandardCharsets.UTF_8)));

    Resource encounterResource = mock(Resource.class);
    when(encounterResource.getFilename()).thenReturn("01_broken.json");
    when(encounterResource.getInputStream()).thenThrow(new IOException("encounter read error"));

    String encounterPattern = "file:/fake/campaigns/test/encounters/*.json";
    when(resolver.getResources(FOLDER_PATTERN)).thenReturn(new Resource[] {chapterResource});
    when(resolver.getResources(encounterPattern)).thenReturn(new Resource[] {encounterResource});
    when(resolver.getResources(FLAT_PATTERN)).thenReturn(new Resource[0]);

    service.loadChapters();

    assertThat(service.getAllChapters()).hasSize(1);
    assertThat(service.getAllChapters().get(0).getId()).isEqualTo("test-ch");
    assertThat(service.getAllChapters().get(0).getStaticEncounters()).isEmpty();
  }

  // -------------------------------------------------------------------------
  // Flat-scan outer catch (lines ~123-125)
  // -------------------------------------------------------------------------

  @Test
  void flatScanThrows_doesNotCrash() throws IOException {
    when(resolver.getResources(FOLDER_PATTERN)).thenReturn(new Resource[0]);
    when(resolver.getResources(FLAT_PATTERN)).thenThrow(new IOException("flat scan error"));

    service.loadChapters();

    assertThat(service.getAllChapters()).isEmpty();
  }

  // -------------------------------------------------------------------------
  // Flat-file InputStream inner catch (lines ~119-121)
  // -------------------------------------------------------------------------

  @Test
  void flatFileInputStreamThrows_fileSkipped() throws IOException {
    Resource flatResource = mock(Resource.class);
    when(flatResource.getFilename()).thenReturn("broken.json");
    when(flatResource.getInputStream()).thenThrow(new IOException("flat file read error"));

    when(resolver.getResources(FOLDER_PATTERN)).thenReturn(new Resource[0]);
    when(resolver.getResources(FLAT_PATTERN)).thenReturn(new Resource[] {flatResource});

    service.loadChapters();

    assertThat(service.getAllChapters()).isEmpty();
  }

  // -------------------------------------------------------------------------
  // merged.isEmpty() warning path (lines ~127-129)
  // -------------------------------------------------------------------------

  @Test
  void bothScansReturnEmpty_noChaptersLoaded() throws IOException {
    when(resolver.getResources(FOLDER_PATTERN)).thenReturn(new Resource[0]);
    when(resolver.getResources(FLAT_PATTERN)).thenReturn(new Resource[0]);

    service.loadChapters();

    assertThat(service.getAllChapters()).isEmpty();
  }
}
