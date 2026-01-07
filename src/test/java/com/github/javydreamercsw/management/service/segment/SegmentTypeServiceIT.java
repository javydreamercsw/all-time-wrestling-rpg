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
package com.github.javydreamercsw.management.service.segment;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

class SegmentTypeServiceIT extends ManagementIntegrationTest {

  @Autowired private SegmentTypeService segmentTypeService;

  @SpyBean private SegmentTypeRepository segmentTypeRepository;

  @Test
  @DisplayName("Test that createSegmentType evicts cache")
  void testCreateSegmentTypeEvictsCache() {
    // First call, should hit the repository
    segmentTypeService.findAll();
    verify(segmentTypeRepository, times(1)).findAll();

    // Create a new segment type, should evict the cache
    segmentTypeService.createOrUpdateSegmentType("Test Segment Type", "Test Description");

    // Second call, should hit the repository again
    segmentTypeService.findAll();
    verify(segmentTypeRepository, times(2)).findAll();
  }
}
