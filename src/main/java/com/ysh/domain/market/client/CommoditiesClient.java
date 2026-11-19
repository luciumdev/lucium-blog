package com.ysh.domain.market.client;

import com.ysh.domain.market.domain.Commodities;

/**
 * 원자재 가격 수집 클라이언트
 * 부분 실패 허용
 */
public interface CommoditiesClient {
    Commodities fetch();
}
