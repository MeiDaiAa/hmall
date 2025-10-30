package com.hmall.item.listener;

import cn.hutool.json.JSONUtil;
import com.hmall.common.constant.MqConstant;
import com.hmall.item.domain.po.ItemDoc;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class EsEventListener {
    private final RestHighLevelClient client = new RestHighLevelClient(
            RestClient.builder(HttpHost.create("http://192.168.100.128:9200"))
    );

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstant.ES_INDEX_OR_UPDATE_QUEUE, durable = "true"),
            exchange = @Exchange(name = MqConstant.ES_EXCHANGE),
            key = {MqConstant.ES_INDEX_KEY, MqConstant.ES_UPDATE_KEY})
    )
    public void listenEsIndex(ItemDoc itemDoc) throws IOException {
        log.info("监听到新增或更新消息：{}", itemDoc);
        UpdateRequest updateRequest = new UpdateRequest("items", itemDoc.getId());
        updateRequest.doc(JSONUtil.toJsonStr(itemDoc), XContentType.JSON);
        // 不存在则插入
        updateRequest.docAsUpsert(true);
        client.update(updateRequest, RequestOptions.DEFAULT);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstant.ES_DELETE_QUEUE, durable = "true"),
            exchange = @Exchange(name = MqConstant.ES_EXCHANGE),
            key = MqConstant.ES_DELETE_KEY
    ))
    public void listenEsDelete(String id) throws IOException {
        log.info("监听到删除消息：{}", id);
        client.delete(new DeleteRequest("items", id), RequestOptions.DEFAULT);
    }
}
