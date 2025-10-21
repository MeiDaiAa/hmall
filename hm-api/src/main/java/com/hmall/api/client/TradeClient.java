package com.hmall.api.client;

import com.hmall.api.domain.po.Order;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value="trade-service")
public interface TradeClient {
    @PutMapping("/orders")
    void updateById(@RequestBody Order order);
}
