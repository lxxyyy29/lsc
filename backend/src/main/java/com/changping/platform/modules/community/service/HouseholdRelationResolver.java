package com.changping.platform.modules.community.service;

import java.util.Set;

/**
 * 户内「与户主关系」推算器。
 *
 * <p>数据模型只存"相对于户主的称谓"，因此户主变更时必须重算。推算需要三个输入：
 * <ul>
 *   <li>anchorRelation：新户主相对旧户主的关系（由操作员选择，或用新户主当前 relation 推断）</li>
 *   <li>oldHeadGender：旧户主性别</li>
 *   <li>memberGender：待推算成员性别</li>
 * </ul>
 *
 * <p>推算规则覆盖常见直系亲属（配偶 / 子女 / 父母 / 兄弟姐妹 / 祖孙 / 儿媳女婿），
 * 无法确定的关系统一置为「其他」，由操作员在界面上人工修正。
 */
public final class HouseholdRelationResolver {

    public static final String HEAD = "户主";
    public static final String SPOUSE = "配偶";
    public static final String SON = "儿子";
    public static final String DAUGHTER = "女儿";
    public static final String FATHER = "父亲";
    public static final String MOTHER = "母亲";
    public static final String BROTHER = "兄弟";
    public static final String SISTER = "姐妹";
    public static final String GRANDFATHER = "祖父";
    public static final String GRANDMOTHER = "祖母";
    public static final String GRANDSON = "孙子";
    public static final String GRANDDAUGHTER = "孙女";
    public static final String DAUGHTER_IN_LAW = "儿媳";
    public static final String SON_IN_LAW = "女婿";
    public static final String OTHER = "其他";

    private static final Set<String> CHILD = Set.of(SON, DAUGHTER);
    private static final Set<String> PARENT = Set.of(FATHER, MOTHER);
    private static final Set<String> SIBLING = Set.of(BROTHER, SISTER);

    private HouseholdRelationResolver() {
    }

    /**
     * 推算某成员在新户主下的「与户主关系」。
     *
     * <p>注意：新户主自身不经过本方法——调用方应直接将其关系置为「户主」。
     *
     * @param existingRelation 成员变更前的 relation（相对旧户主）
     * @param isOldHead        该成员是否为旧户主
     * @param anchorRelation   新户主相对旧户主的关系
     * @param oldHeadGender    旧户主性别
     * @param memberGender     该成员性别
     */
    public static String resolve(String existingRelation, boolean isOldHead, String anchorRelation,
            String oldHeadGender, String memberGender) {
        String anchor = normalize(anchorRelation);
        if (isOldHead) {
            return invert(anchor, oldHeadGender);
        }
        String existing = normalize(existingRelation);
        if (existing == null || existing.isEmpty()) {
            return OTHER;
        }
        // 同户出现多个"户主"标记的异常数据：按"该成员是（旧）户主"处理
        if (HEAD.equals(existing)) {
            return invert(anchor, memberGender);
        }
        return compose(existing, anchor, memberGender);
    }

    /**
     * 由新户主当前的 relation 推断「新户主相对旧户主的关系」。
     * 新户主原本就是相对旧户主存的称谓，因此可直接复用。
     *
     * @return 推断出的关系；无法推断（为空 / 已是户主）时返回 null，需操作员指定
     */
    public static String inferAnchorRelation(String newHeadCurrentRelation) {
        String r = normalize(newHeadCurrentRelation);
        if (r == null || r.isEmpty() || HEAD.equals(r)) {
            return null;
        }
        return r;
    }

    /** 旧户主（或异常多户主成员）相对新户主的关系 = anchor 的逆关系 */
    private static String invert(String anchor, String gender) {
        if (anchor == null || anchor.isEmpty()) {
            return OTHER;
        }
        switch (anchor) {
            case HEAD:
                return HEAD;
            case SON:
            case DAUGHTER:
                return byGender(gender, FATHER, MOTHER);
            case FATHER:
            case MOTHER:
                return byGender(gender, SON, DAUGHTER);
            case SPOUSE:
                return SPOUSE;
            case BROTHER:
            case SISTER:
                return byGender(gender, BROTHER, SISTER);
            default:
                return OTHER;
        }
    }

    /** 已知 X 相对旧户主为 existing，新户主相对旧户主为 anchor，求 X 相对新户主 */
    private static String compose(String existing, String anchor, String memberGender) {
        if (anchor == null || anchor.isEmpty()) {
            return OTHER;
        }
        // 新户主是旧户主的子女：旧户主 → 父/母；旧户主配偶 → 父/母；其他子女 → 兄弟/姐妹；旧户主父母 → 祖父母
        if (CHILD.contains(anchor)) {
            if (SPOUSE.equals(existing)) {
                return byGender(memberGender, FATHER, MOTHER);
            }
            if (CHILD.contains(existing)) {
                return byGender(memberGender, BROTHER, SISTER);
            }
            if (PARENT.contains(existing)) {
                return byGender(memberGender, GRANDFATHER, GRANDMOTHER);
            }
            return OTHER;
        }
        // 新户主是旧户主的配偶：旧户主子女 → 子女；旧户主父母 → 父母
        if (SPOUSE.equals(anchor)) {
            if (CHILD.contains(existing)) {
                return byGender(memberGender, SON, DAUGHTER);
            }
            if (PARENT.contains(existing)) {
                return byGender(memberGender, FATHER, MOTHER);
            }
            return OTHER;
        }
        // 新户主是旧户主的父/母：旧户主配偶 → 儿媳/女婿；旧户主子女 → 孙辈；旧户主父母 → 父母
        if (PARENT.contains(anchor)) {
            if (SPOUSE.equals(existing)) {
                return byGender(memberGender, SON_IN_LAW, DAUGHTER_IN_LAW);
            }
            if (CHILD.contains(existing)) {
                return byGender(memberGender, GRANDSON, GRANDDAUGHTER);
            }
            if (PARENT.contains(existing)) {
                return byGender(memberGender, FATHER, MOTHER);
            }
            return OTHER;
        }
        // 新户主是旧户主的兄弟姐妹：旧户主父母 → 父母；旧户主子女 → 侄辈
        if (SIBLING.contains(anchor)) {
            if (PARENT.contains(existing)) {
                return byGender(memberGender, FATHER, MOTHER);
            }
            if (CHILD.contains(existing)) {
                return byGender(memberGender, "侄子", "侄女");
            }
            return OTHER;
        }
        return OTHER;
    }

    private static String byGender(String gender, String male, String female) {
        return "男".equals(gender) ? male : female;
    }

    /**
     * 关系归一化：把常见口语/别名映射到标准称谓，无法识别时返回 trimmed 原值（后续 compose 会归入「其他」）。
     */
    public static String normalize(String relation) {
        if (relation == null) {
            return null;
        }
        String r = relation.trim();
        if (r.isEmpty()) {
            return r;
        }
        if (HEAD.equals(r)) {
            return HEAD;
        }
        if (r.contains("配偶") || r.equals("妻") || r.equals("夫") || r.contains("妻子") || r.contains("丈夫")
                || r.equals("老婆") || r.equals("老公")) {
            return SPOUSE;
        }
        if (r.contains("祖父") || r.contains("外祖父") || r.contains("爷爷") || r.contains("姥爷")) {
            return GRANDFATHER;
        }
        if (r.contains("祖母") || r.contains("外祖母") || r.contains("奶奶") || r.contains("姥姥")) {
            return GRANDMOTHER;
        }
        if (r.contains("孙女") || r.contains("外孙女")) {
            return GRANDDAUGHTER;
        }
        if (r.contains("孙子") || r.contains("外孙")) {
            return GRANDSON;
        }
        if (r.contains("儿媳") || r.contains("媳妇") || r.contains("儿媳妇")) {
            return DAUGHTER_IN_LAW;
        }
        if (r.contains("女婿")) {
            return SON_IN_LAW;
        }
        if (r.contains("父亲") || r.equals("爸") || r.contains("爸爸")) {
            return FATHER;
        }
        if (r.contains("母亲") || r.equals("妈") || r.contains("妈妈")) {
            return MOTHER;
        }
        if (r.contains("兄弟") || r.equals("哥") || r.equals("弟") || r.contains("哥哥") || r.contains("弟弟")
                || r.contains("兄长")) {
            return BROTHER;
        }
        if (r.contains("姐妹") || r.equals("姐") || r.equals("妹") || r.contains("姐姐") || r.contains("妹妹")) {
            return SISTER;
        }
        if (r.contains("儿子")) {
            return SON;
        }
        if (r.contains("女儿")) {
            return DAUGHTER;
        }
        return r;
    }
}
