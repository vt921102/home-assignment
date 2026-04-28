package com.toanlv.flashsale.flashsale.service;

import java.time.LocalDate;
import java.util.List;

import com.toanlv.flashsale.flashsale.dto.FlashSaleItemDto;
import com.toanlv.flashsale.flashsale.dto.SessionDto;

public interface IFlashSaleQueryService {
  List<FlashSaleItemDto> getCurrentItems();

  List<SessionDto> getSessionsByDate(LocalDate date);

  void invalidateCurrentCache();
}
