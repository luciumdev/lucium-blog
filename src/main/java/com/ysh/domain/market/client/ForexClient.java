package com.ysh.domain.market.client;

import com.ysh.domain.market.domain.ForexRates;

public interface ForexClient {
    ForexRates fetch();
}
