package com.hmall.item.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.common.constant.MqConstant;
import com.hmall.common.domain.PageDTO;
import com.hmall.common.domain.PageQuery;
import com.hmall.common.utils.BeanUtils;
import com.hmall.item.domain.dto.ItemDTO;
import com.hmall.item.domain.dto.OrderDetailDTO;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "商品管理相关接口")
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final IItemService itemService;
    private final RabbitTemplate rabbitTemplate;

    @ApiOperation("分页查询商品")
    @GetMapping("/page")
    public PageDTO<ItemDTO> queryItemByPage(PageQuery query) {
        // 1.分页查询
        Page<Item> result = itemService.page(query.toMpPage("update_time", false));
        // 2.封装并返回
        return PageDTO.of(result, ItemDTO.class);
    }

    @ApiOperation("根据id批量查询商品")
    @GetMapping
    public List<ItemDTO> queryItemByIds(@RequestParam("ids") List<Long> ids){
        return itemService.queryItemByIds(ids);
    }

    @ApiOperation("根据id查询商品")
    @GetMapping("{id}")
    public ItemDTO queryItemById(@PathVariable("id") Long id) {
        return BeanUtils.copyBean(itemService.getById(id), ItemDTO.class);
    }

    @ApiOperation("新增商品")
    @PostMapping
    public void saveItem(@RequestBody ItemDTO itemDTO) {
        // 新增
        Item item = BeanUtils.copyBean(itemDTO, Item.class);
        itemService.save(item);
        // 发送消息同步更新es索引
        rabbitTemplate.convertAndSend(
                MqConstant.ES_EXCHANGE,
                MqConstant.ES_INDEX_KEY,
                BeanUtils.copyProperties(item, ItemDoc.class)
        );
    }

    @ApiOperation("更新商品状态")
    @PutMapping("/status/{id}/{status}")
    public void updateItemStatus(@PathVariable("id") Long id, @PathVariable("status") Integer status){
        Item item = new Item();
        item.setId(id);
        item.setStatus(status);
        itemService.updateById(item);

        item = itemService.getById(id);
        // 如果商品状态为1新增，不为1删除
        if (status == 1) {
            rabbitTemplate.convertAndSend(
                    MqConstant.ES_EXCHANGE,
                    MqConstant.ES_INDEX_KEY,
                    BeanUtils.copyProperties(item, ItemDoc.class));
        } else {
            rabbitTemplate.convertAndSend(
                    MqConstant.ES_EXCHANGE,
                    MqConstant.ES_DELETE_KEY,
                    id
            );
        }
    }

    @ApiOperation("更新商品")
    @PutMapping
    @Transactional
    public void updateItem(@RequestBody ItemDTO itemDto) {
        // 不允许修改商品状态，所以强制设置为null，更新时，就会忽略该字段
        itemDto.setStatus(null);
        // 更新
        Item item = BeanUtils.copyBean(itemDto, Item.class);
        itemService.updateById(item);

        // 发送消息同步更新es索引
        rabbitTemplate.convertAndSend(
                MqConstant.ES_EXCHANGE,
                MqConstant.ES_UPDATE_KEY,
                BeanUtils.copyProperties(item, ItemDoc.class)
        );
    }

    @ApiOperation("根据id删除商品")
    @DeleteMapping("{id}")
    @Transactional
    public void deleteItemById(@PathVariable("id") Long id) {
        itemService.removeById(id);

        // 发送消息同步更新es索引
        rabbitTemplate.convertAndSend(
                MqConstant.ES_EXCHANGE,
                MqConstant.ES_DELETE_KEY,
                id
        );
    }

    @ApiOperation("批量扣减库存")
    @PutMapping("/stock/deduct")
    public void deductStock(@RequestBody List<OrderDetailDTO> items){
        itemService.deductStock(items);
    }

    @ApiOperation("批量增加库存")
    @PutMapping("/stock/increase")
    public void increaseStock(@RequestBody List<OrderDetailDTO> items){
        itemService.increaseStock(items);
    }
}
