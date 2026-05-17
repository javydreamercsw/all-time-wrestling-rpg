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
package com.github.javydreamercsw.management.service.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.image.DefaultImageService;
import com.github.javydreamercsw.base.image.ImageCategory;
import com.github.javydreamercsw.base.image.ImageResolution;
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.domain.world.LocationRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LocationServiceTest {

  @Mock private LocationRepository repository;
  @Mock private DefaultImageService imageService;

  @InjectMocks private LocationService service;

  @Test
  void createLocation_savesAndReturns() {
    Location saved =
        Location.builder()
            .name("Tokyo Dome")
            .description("Iconic venue")
            .imageUrl("http://example.com/img.jpg")
            .culturalTags(Set.of("Japan"))
            .build();
    when(repository.save(any(Location.class))).thenReturn(saved);

    Location result =
        service.createLocation(
            "Tokyo Dome", "Iconic venue", "http://example.com/img.jpg", Set.of("Japan"));

    assertThat(result).isSameAs(saved);
    verify(repository).save(any(Location.class));
  }

  @Test
  void updateLocation_found_updatesAndReturns() {
    Location existing = Location.builder().name("Old Name").build();
    existing.setId(1L);
    when(repository.findById(1L)).thenReturn(Optional.of(existing));
    when(repository.save(existing)).thenReturn(existing);

    Optional<Location> result =
        service.updateLocation(1L, "New Name", "New desc", "http://new.com/img.jpg", Set.of("USA"));

    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("New Name");
    verify(repository).save(existing);
  }

  @Test
  void updateLocation_notFound_returnsEmpty() {
    when(repository.findById(99L)).thenReturn(Optional.empty());

    Optional<Location> result = service.updateLocation(99L, "Name", "Desc", null, Set.of());

    assertThat(result).isEmpty();
  }

  @Test
  void findById_found_returnsOptional() {
    Location location = Location.builder().name("Arena").build();
    when(repository.findById(1L)).thenReturn(Optional.of(location));

    Optional<Location> result = service.findById(1L);

    assertThat(result).isPresent().contains(location);
  }

  @Test
  void findById_notFound_returnsEmpty() {
    when(repository.findById(999L)).thenReturn(Optional.empty());

    Optional<Location> result = service.findById(999L);

    assertThat(result).isEmpty();
  }

  @Test
  void findAll_returnsList() {
    Location l1 = Location.builder().name("Arena 1").build();
    Location l2 = Location.builder().name("Arena 2").build();
    when(repository.findAll()).thenReturn(List.of(l1, l2));

    List<Location> result = service.findAll();

    assertThat(result).containsExactly(l1, l2);
  }

  @Test
  void list_delegatesToRepository() {
    Pageable pageable = PageRequest.of(0, 10);
    Location l = Location.builder().name("Arena").build();
    Page<Location> page = new PageImpl<>(List.of(l));
    when(repository.findAll(pageable)).thenReturn(page);

    Page<Location> result = service.list(pageable);

    assertThat(result.getContent()).containsExactly(l);
    verify(repository).findAll(pageable);
  }

  @Test
  void count_returnsCount() {
    when(repository.count()).thenReturn(7L);

    assertThat(service.count()).isEqualTo(7L);
  }

  @Test
  void deleteLocation_callsDeleteById() {
    service.deleteLocation(5L);

    verify(repository).deleteById(5L);
  }

  @Test
  void findByName_found() {
    Location location = Location.builder().name("Tokyo Dome").build();
    when(repository.findByName("Tokyo Dome")).thenReturn(Optional.of(location));

    Optional<Location> result = service.findByName("Tokyo Dome");

    assertThat(result).isPresent().contains(location);
  }

  @Test
  void findByName_notFound() {
    when(repository.findByName("Unknown")).thenReturn(Optional.empty());

    Optional<Location> result = service.findByName("Unknown");

    assertThat(result).isEmpty();
  }

  @Test
  void resolveLocationImage_withExistingUrl_returnsUrl() {
    Location location =
        Location.builder().name("Arena").imageUrl("http://example.com/arena.jpg").build();

    String result = service.resolveLocationImage(location);

    assertThat(result).isEqualTo("http://example.com/arena.jpg");
  }

  @Test
  void resolveLocationImage_withNullUrl_usesDefaultImage() {
    Location location = Location.builder().name("Mystery Arena").imageUrl(null).build();
    ImageResolution resolution = new ImageResolution("images/location_default.jpg", true);
    when(imageService.resolveImage(eq("Mystery Arena"), eq(ImageCategory.LOCATION)))
        .thenReturn(resolution);

    String result = service.resolveLocationImage(location);

    assertThat(result).isEqualTo("images/location_default.jpg");
    verify(imageService).resolveImage("Mystery Arena", ImageCategory.LOCATION);
  }
}
