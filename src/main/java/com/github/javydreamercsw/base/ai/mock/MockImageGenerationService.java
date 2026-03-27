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
package com.github.javydreamercsw.base.ai.mock;

import com.github.javydreamercsw.base.ai.image.ImageGenerationService;

/** Mock implementation of {@link ImageGenerationService} for testing. */
public class MockImageGenerationService implements ImageGenerationService {

  @Override
  public String generateImage(ImageRequest request) {
    return "mock-image-url.png";
  }

  @Override
  public String getProviderName() {
    return "MockProvider";
  }

  @Override
  public boolean isAvailable() {
    return true;
  }
}
