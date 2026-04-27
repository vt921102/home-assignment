package com.toanlv.flashsale.flashsale.service;


import com.toanlv.flashsale.common.exception.BusinessException;
import com.toanlv.flashsale.common.exception.ErrorCode;
import com.toanlv.flashsale.flashsale.domain.FlashSaleSession;
import com.toanlv.flashsale.flashsale.domain.FlashSaleSessionItem;
import com.toanlv.flashsale.flashsale.dto.AddSessionItemRequest;
import com.toanlv.flashsale.flashsale.dto.CreateSessionRequest;
import com.toanlv.flashsale.flashsale.dto.FlashSaleItemDto;
import com.toanlv.flashsale.flashsale.dto.SessionDto;
import com.toanlv.flashsale.flashsale.repository.FlashSaleSessionItemRepository;
import com.toanlv.flashsale.flashsale.repository.FlashSaleSessionRepository;
import com.toanlv.flashsale.product.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SessionAdminService {

    private final FlashSaleSessionRepository     sessionRepository;
    private final FlashSaleSessionItemRepository itemRepository;
    private final ProductService                 productService;
    private final FlashSaleQueryService          queryService;

    public SessionAdminService(
            FlashSaleSessionRepository sessionRepository,
            FlashSaleSessionItemRepository itemRepository,
            ProductService productService,
            FlashSaleQueryService queryService) {
        this.sessionRepository = sessionRepository;
        this.itemRepository    = itemRepository;
        this.productService    = productService;
        this.queryService      = queryService;
    }

    @Transactional
    public SessionDto createSession(CreateSessionRequest request) {
        if (request.endTime().isBefore(request.startTime())
                || request.endTime().equals(request.startTime())) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "End time must be after start time");
        }

        var session = FlashSaleSession.create(
                request.name(),
                request.saleDate(),
                request.startTime(),
                request.endTime());

        var saved = sessionRepository.save(session);
        queryService.invalidateCurrentCache();
        return SessionDto.from(saved);
    }

    @Transactional
    public FlashSaleItemDto addItem(
            UUID sessionId,
            AddSessionItemRequest request) {

        var session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.SESSION_NOT_FOUND));

        // Validate product exists and is active
        var product = productService.findActiveProduct(request.productId());

        var item = FlashSaleSessionItem.create(
                session,
                product,
                request.salePrice(),
                request.totalQuantity(),
                request.perUserLimit());

        var saved = itemRepository.save(item);
        queryService.invalidateCurrentCache();
        return FlashSaleItemDto.from(saved);
    }

    @Transactional
    public SessionDto deactivateSession(UUID sessionId) {
        var session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.SESSION_NOT_FOUND));

        session.deactivate();
        var saved = sessionRepository.save(session);
        queryService.invalidateCurrentCache();
        return SessionDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<FlashSaleItemDto> getItemsBySession(UUID sessionId) {
        return itemRepository.findBySessionId(sessionId)
                .stream()
                .map(FlashSaleItemDto::from)
                .toList();
    }
}
