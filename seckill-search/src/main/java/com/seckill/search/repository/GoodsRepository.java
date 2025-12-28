package com.seckill.search.repository;

import com.seckill.search.entity.GoodsDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoodsRepository extends ElasticsearchRepository<GoodsDoc, Long> {

    // 魔法方法：Spring 会自动把这个方法名翻译成 ES 的查询语句
    // 相当于: SELECT * FROM goods WHERE goodsName LIKE %name% OR goodsTitle LIKE %name%
    List<GoodsDoc> findByGoodsNameOrGoodsTitle(String name, String title);
}