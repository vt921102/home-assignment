package com.toanlv.flashsale.flashsale.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toanlv.flashsale.flashsale.dto.FlashSaleItemDto;
import com.toanlv.flashsale.flashsale.dto.SessionDto;
import com.toanlv.flashsale.flashsale.repository.FlashSaleSessionItemRepository;
import com.toanlv.flashsale.flashsale.repository.FlashSaleSessionRepository;
import com.toanlv.flashsale.flashsale.service.IFlashSaleQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FlashSaleQueryService implements IFlashSaleQueryService {

  private final FlashSaleSessionItemRepository itemRepository;
  private final FlashSaleSessionRepository sessionRepository;
  private final Clock clock;

  /**
   * List all products currently available in active flash sale sessions.
   *
   * <p>Cached with TTL 2s — high-traffic endpoint, stale by at most 2 seconds. Correctness (stock
   * levels) is enforced at purchase time, not here.
   */
  @Override
  @Cacheable(value = "current-flash-sale", key = "'current'")
  @Transactional(readOnly = true)
  public List<FlashSaleItemDto> getCurrentItems() {
    var today = LocalDate.now(clock);
    var now = LocalTime.now(clock);
    return itemRepository.findActiveItems(today, now).stream().map(FlashSaleItemDto::from).toList();
  }

  /** List all sessions for a given date — admin use. */
  @Override
  @Transactional(readOnly = true)
  public List<SessionDto> getSessionsByDate(LocalDate date) {
    return sessionRepository.findBySaleDateOrderByStartTimeAsc(date).stream()
        .map(SessionDto::from)
        .toList();
  }

  /** Invalidate the current flash sale cache. Called when session config changes via admin API. */
  @Override
  @CacheEvict(value = "current-flash-sale", allEntries = true)
  public void invalidateCurrentCache() {
    // Cache eviction handled by Spring
  }
}
