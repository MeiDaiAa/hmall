package com.hmall.trade.listener;

import com.hmall.api.client.PayClient;
import com.hmall.api.domain.dto.PayOrderDTO;
import com.hmall.api.domain.po.Order;
import com.hmall.common.constant.MqConstant;
import com.hmall.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDelayMessageListener {
    private final PayClient payClient;
    private final IOrderService orderService;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstant.ORDER_DELAY_QUEUE, durable = "true"),
            exchange = @Exchange(name = MqConstant.ORDER_DELAY_EXCHANGE, delayed = "true"),
            key = MqConstant.ORDER_DELAY_KEY
    ))
    public void listenOrderDelayMessage(Long id) {
        log.info("订单延迟队列监听订单：" + id);

        // 查询支付订单状态
        Order order = orderService.getById(id);

        if (order == null || order.getStatus() != 1) {
            // 订单不存在或者已经支付，不需要处理
            log.info("订单不存在或者已经支付，不需要处理");
            return;
        }
        // 需要处理
        PayOrderDTO payOrder = payClient.queryPayOrderByBizOrderNo(id);
        if (payOrder == null || payOrder.getStatus() != 3) {
            // TODO 用户未支付, 取消订单，释放库存
            log.info("用户未支付, 取消订单，释放库存");
            return;
        }
        // 订单已支付
        orderService.updateById(new Order().setId(id).setStatus(2));
        log.info("支付成功, 订单已修改：" + id);
    }
}
