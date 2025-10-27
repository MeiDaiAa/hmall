package com.hmall.common.constant;

/**
 * @author meidaia
 */
public class MqConstant {
    // 减少库存
    public static final String CART_CLEAR_QUEUE = "trade.clear.queue";
    public static final String CART_CLEAR_EXCHANGE = "trade.topic";
    public static final String CART_CLEAR_KEY = "order.create";

    // 订单延时队列
    public static final String ORDER_DELAY_QUEUE = "trade.delay.order.queue";
    public static final String ORDER_DELAY_EXCHANGE = "trade.delay.direct";
    public static final String ORDER_DELAY_KEY = "trade.delay.query";
    public static final Integer ORDER_DELAY_TIME = 1_800_000;
}
