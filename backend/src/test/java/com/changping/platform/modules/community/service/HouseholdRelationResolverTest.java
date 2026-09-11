package com.changping.platform.modules.community.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 户主变更时「与户主关系」推算规则测试
 */
class HouseholdRelationResolverTest {

    /**
     * 用户场景：原户主为张三父亲，张三与户主关系为「儿子」；
     * 变更户主为张三后，张三父亲与户主关系应变为「父亲」，张三自身为「户主」。
     */
    @Test
    void changeHeadToSon() {
        String anchor = "儿子"; // 新户主（张三）相对原户主（张三父亲）的关系
        String oldHeadGender = "男";

        assertEquals("父亲", resolve("户主", true, anchor, oldHeadGender, "男"));
        // 新户主张三本身由 changeHead 直接置为「户主」，不经过本推算方法
        // 张三母亲（原户主配偶）→ 新户主的母亲
        assertEquals("母亲", resolve("配偶", false, anchor, oldHeadGender, "女"));
        // 张三妹妹（原户主女儿）→ 新户主的姐妹
        assertEquals("姐妹", resolve("女儿", false, anchor, oldHeadGender, "女"));
        // 原户主父母 → 新户主的祖父母
        assertEquals("祖父", resolve("父亲", false, anchor, oldHeadGender, "男"));
        assertEquals("祖母", resolve("母亲", false, anchor, oldHeadGender, "女"));
    }

    @Test
    void changeHeadToDaughter() {
        String anchor = "女儿";
        // 原户主（母亲）→ 新户主的母亲
        assertEquals("母亲", resolve("户主", true, anchor, "女", "女"));
        assertEquals("父亲", resolve("配偶", false, anchor, "女", "男"));
        assertEquals("兄弟", resolve("儿子", false, anchor, "女", "男"));
    }

    @Test
    void changeHeadToSpouse() {
        String anchor = "配偶";
        assertEquals("配偶", resolve("户主", true, anchor, "男", "女"));
        assertEquals("儿子", resolve("儿子", false, anchor, "男", "男"));
        assertEquals("女儿", resolve("女儿", false, anchor, "男", "女"));
        assertEquals("父亲", resolve("父亲", false, anchor, "男", "男"));
    }

    @Test
    void changeHeadToParent() {
        String anchor = "父亲"; // 新户主是原户主的父亲
        String oldHeadGender = "男";
        assertEquals("儿子", resolve("户主", true, anchor, oldHeadGender, "男"));
        // 原户主的母亲 → 新户主的配偶（母亲）
        assertEquals("母亲", resolve("母亲", false, anchor, oldHeadGender, "女"));
        // 原户主的配偶 → 新户主的儿媳
        assertEquals("儿媳", resolve("配偶", false, anchor, oldHeadGender, "女"));
        // 原户主的子女 → 新户主的孙辈
        assertEquals("孙子", resolve("儿子", false, anchor, oldHeadGender, "男"));
        assertEquals("孙女", resolve("女儿", false, anchor, oldHeadGender, "女"));
    }

    @Test
    void changeHeadToSibling() {
        String anchor = "兄弟";
        assertEquals("兄弟", resolve("户主", true, anchor, "男", "男"));
        assertEquals("父亲", resolve("父亲", false, anchor, "男", "男"));
        assertEquals("侄子", resolve("儿子", false, anchor, "男", "男"));
    }

    /** 新户主当前 relation 可直接作为锚点关系；已是户主或为空时无法推断 */
    @Test
    void inferAnchorRelation() {
        assertEquals("儿子", HouseholdRelationResolver.inferAnchorRelation("儿子"));
        assertEquals("配偶", HouseholdRelationResolver.inferAnchorRelation("妻子"));
        assertNull(HouseholdRelationResolver.inferAnchorRelation("户主"));
        assertNull(HouseholdRelationResolver.inferAnchorRelation(""));
        assertNull(HouseholdRelationResolver.inferAnchorRelation(null));
    }

    /** 无法识别的关系统一归为「其他」，便于前端提示人工核对 */
    @Test
    void unknownRelationFallsBackToOther() {
        assertEquals("其他", resolve("侄子", false, "儿子", "男", "男"));
        assertEquals("其他", resolve("邻居", false, "配偶", "男", "男"));
    }

    private static String resolve(String existing, boolean isOldHead, String anchor, String oldHeadGender, String gender) {
        return HouseholdRelationResolver.resolve(existing, isOldHead, anchor, oldHeadGender, gender);
    }
}
