package com.toanlv.flashsale.common.util;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(
    value = {"pageable", "sort"},
    ignoreUnknown = true)
public class PageImplBean<T> extends PageImpl<T> {

  @JsonCreator
  public PageImplBean(
      @JsonProperty("content") List<T> content,
      @JsonProperty("number") int number,
      @JsonProperty("size") int size,
      @JsonProperty("totalElements") long totalElements) {
    super(content, PageRequest.of(number, Math.max(size, 1)), totalElements);
  }

  public PageImplBean(Page<T> page) {
    super(page.getContent(), page.getPageable(), page.getTotalElements());
  }
}
