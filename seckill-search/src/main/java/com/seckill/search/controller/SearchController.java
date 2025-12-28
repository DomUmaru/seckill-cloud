package com.seckill.search.controller;

import com.seckill.common.result.Result;
import com.seckill.search.entity.GoodsDoc;
import com.seckill.search.repository.GoodsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private GoodsRepository goodsRepository;

    /**
     * 1. 模拟数据同步 (把数据存入 ES)
     * 正常应该通过 Canal 监听 MySQL Binlog 自动同步，或者用 MQ 异步同步
     */
    @PostMapping("/sync")
    public Result<String> syncData() {
        GoodsDoc doc1 = new GoodsDoc();
        doc1.setId(1L);
        doc1.setGoodsName("iPhone 15 Pro");
        doc1.setGoodsTitle("苹果手机 5G 钛金属");
        doc1.setGoodsPrice(new BigDecimal("9999"));
        doc1.setStockCount(100);

        GoodsDoc doc2 = new GoodsDoc();
        doc2.setId(2L);
        doc2.setGoodsName("Huawei Mate 60");
        doc2.setGoodsTitle("华为手机 遥遥领先");
        doc2.setGoodsPrice(new BigDecimal("6999"));
        doc2.setStockCount(50);

        // save 方法会自动创建索引(如果不存在)并保存文档
        goodsRepository.save(doc1);
        goodsRepository.save(doc2);

        return Result.success("数据同步完成");
    }

    /**
     * 2. 搜索接口
     */
    @GetMapping("/query")
    public Result<List<GoodsDoc>> search(@RequestParam String keyword) {
        // 利用 Repository 的魔法方法进行搜索
        List<GoodsDoc> list = goodsRepository.findByGoodsNameOrGoodsTitle(keyword, keyword);
        return Result.success(list);
    }
}