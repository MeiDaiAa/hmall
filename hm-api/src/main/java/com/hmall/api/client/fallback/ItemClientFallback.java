package com.hmall.api.client.fallback;

import com.hmall.api.client.ItemClient;
import com.hmall.api.domain.dto.ItemDTO;
import com.hmall.api.domain.dto.OrderDetailDTO;
import com.hmall.common.exception.BizIllegalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collection;
import java.util.List;

@Slf4j
public class ItemClientFallback implements FallbackFactory<ItemClient> {
    @Override
    public ItemClient create(Throwable cause) {
        return new ItemClient() {
            @Override
            public List<ItemDTO> queryItemByIds(Collection<Long> ids) {
                log.error("查询商品信息失败：{}", ids, cause);
                return List.of();
            }

            @Override
            public void deductStock(Collection<OrderDetailDTO> details) {
                // 库存扣减业务需要触发事务回滚，查询失败，抛出异常
                throw new BizIllegalException(cause);
            }
        };
    }
}
