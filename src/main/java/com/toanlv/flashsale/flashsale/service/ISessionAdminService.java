package com.toanlv.flashsale.flashsale.service;

import java.util.List;
import java.util.UUID;

import com.toanlv.flashsale.flashsale.dto.AddSessionItemRequest;
import com.toanlv.flashsale.flashsale.dto.CreateSessionRequest;
import com.toanlv.flashsale.flashsale.dto.FlashSaleItemDto;
import com.toanlv.flashsale.flashsale.dto.SessionDto;

public interface ISessionAdminService {
  SessionDto createSession(CreateSessionRequest request);

  FlashSaleItemDto addItem(UUID sessionId, AddSessionItemRequest request);

  SessionDto deactivateSession(UUID sessionId);

  List<FlashSaleItemDto> getItemsBySession(UUID sessionId);
}
