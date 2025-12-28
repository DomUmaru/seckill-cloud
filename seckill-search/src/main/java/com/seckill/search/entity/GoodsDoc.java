package com.seckill.search.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

// indexName = "goods" 相当于表名
@Data
@Document(indexName = "goods")
public class GoodsDoc {

    @Id
    private Long id;

    // Text: 会被分词 (例如 "华为手机" -> "华为", "手机")
    // analyzer = "ik_max_word" 需要安装 IK 分词器，如果没有安装，暂时用标准分词 standard
    @Field(type = FieldType.Text, analyzer = "standard")
    private String goodsName;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String goodsTitle;

    @Field(type = FieldType.Keyword) // Keyword: 不分词，精确匹配
    private String goodsImg;

    @Field(type = FieldType.Double)
    private BigDecimal goodsPrice;

    @Field(type = FieldType.Integer)
    private Integer stockCount;
}