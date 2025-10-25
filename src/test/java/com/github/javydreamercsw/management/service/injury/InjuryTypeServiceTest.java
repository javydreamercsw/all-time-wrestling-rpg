package com.github.javydreamercsw.management.service.injury;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.injury.InjuryType;
import com.github.javydreamercsw.management.domain.injury.InjuryTypeRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InjuryTypeServiceTest {

  @Mock private InjuryTypeRepository injuryTypeRepository;
  @InjectMocks private InjuryTypeService injuryTypeService;

  private InjuryType injuryType;

  @BeforeEach
  void setUp() {
    injuryType = new InjuryType();
    injuryType.setId(1L);
    injuryType.setInjuryName("Sprain");
    injuryType.setHealthEffect(-2);
    injuryType.setStaminaEffect(-1);
    injuryType.setCardEffect(0);
    injuryType.setSpecialEffects("None");
  }

  @Test
  void testCreateInjuryType_success() {
    when(injuryTypeRepository.existsByInjuryName("Sprain")).thenReturn(false);
    when(injuryTypeRepository.saveAndFlush(any(InjuryType.class))).thenReturn(injuryType);
    InjuryType result = injuryTypeService.createInjuryType("Sprain", -2, -1, 0, "None");
    assertThat(result.getInjuryName()).isEqualTo("Sprain");
  }

  @Test
  void testCreateInjuryType_duplicateName() {
    when(injuryTypeRepository.existsByInjuryName("Sprain")).thenReturn(true);
    assertThatThrownBy(() -> injuryTypeService.createInjuryType("Sprain", -2, -1, 0, "None"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void testUpdateInjuryType_success() {
    when(injuryTypeRepository.findById(1L)).thenReturn(Optional.of(injuryType));
    when(injuryTypeRepository.saveAndFlush(any(InjuryType.class))).thenReturn(injuryType);
    Optional<InjuryType> result =
        injuryTypeService.updateInjuryType(1L, "Sprain", -2, -1, 0, "None");
    assertThat(result).isPresent();
    assertThat(result.get().getInjuryName()).isEqualTo("Sprain");
  }

  @Test
  void testUpdateInjuryType_nameConflict() {
    InjuryType otherType = new InjuryType();
    otherType.setId(2L);
    otherType.setInjuryName("Fracture");
    when(injuryTypeRepository.findById(1L)).thenReturn(Optional.of(injuryType));
    when(injuryTypeRepository.existsByInjuryName("Fracture")).thenReturn(true);
    assertThatThrownBy(() -> injuryTypeService.updateInjuryType(1L, "Fracture", -2, -1, 0, "None"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void testUpdateInjuryType_notFound() {
    when(injuryTypeRepository.findById(99L)).thenReturn(Optional.empty());
    Optional<InjuryType> result =
        injuryTypeService.updateInjuryType(99L, "Unknown", 0, 0, 0, "None");
    assertThat(result).isEmpty();
  }

  @Test
  void testUpdateInjuryType_entity_success() {
    when(injuryTypeRepository.existsById(1L)).thenReturn(true);
    when(injuryTypeRepository.saveAndFlush(injuryType)).thenReturn(injuryType);
    InjuryType result = injuryTypeService.updateInjuryType(injuryType);
    assertThat(result.getId()).isEqualTo(1L);
  }

  @Test
  void testUpdateInjuryType_entity_notFound() {
    when(injuryTypeRepository.existsById(1L)).thenReturn(false);
    assertThatThrownBy(() -> injuryTypeService.updateInjuryType(injuryType))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not found");
  }

  @Test
  void testDeleteInjuryType_success() {
    when(injuryTypeRepository.existsById(1L)).thenReturn(true);
    doNothing().when(injuryTypeRepository).deleteById(1L);
    boolean result = injuryTypeService.deleteInjuryType(1L);
    assertThat(result).isTrue();
  }

  @Test
  void testDeleteInjuryType_notFound() {
    when(injuryTypeRepository.existsById(99L)).thenReturn(false);
    boolean result = injuryTypeService.deleteInjuryType(99L);
    assertThat(result).isFalse();
  }

  @Test
  void testDeleteInjuryType_exception() {
    when(injuryTypeRepository.existsById(1L)).thenReturn(true);
    doThrow(new RuntimeException("DB error")).when(injuryTypeRepository).deleteById(1L);
    assertThatThrownBy(() -> injuryTypeService.deleteInjuryType(1L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Cannot delete injury type");
  }
}
