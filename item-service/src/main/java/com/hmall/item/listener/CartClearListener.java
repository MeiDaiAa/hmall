package com.hmall.item.listener;

import com.hmall.common.constant.MqConstant;
import com.hmall.item.domain.dto.OrderDetailDTO;
import com.hmall.item.service.IItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author meidaia
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CartClearListener {
    private final IItemService itemService;
    @RabbitListener(bindings = @QueueBinding(value = @Queue(name = MqConstant.CART_CLEAR_QUEUE, durable = "true"),
    exchange = @Exchange(name = MqConstant.CART_CLEAR_EXCHANGE),
    key = MqConstant.CART_CLEAR_KEY))
    public void listenOrderCreat(List<OrderDetailDTO> detailDtos) {
        log.info("订单创建成功，准备扣减库存：{}", detailDtos);
        itemService.deductStock(detailDtos);
    }
}
