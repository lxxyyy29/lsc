package com.changping.platform.common.response;
import java.util.List;
/**
 * @Author lxy
 * @Description //分页查询结果封装体，包含数据列表、总记录数、当前页码和每页大小
 * @Date 2026/04/18 09:15
 */
public record PagedResult<T>(List<T> items, long total, int page, int pageSize) {

    /**
     * @Author lxy
     * @Description //静态工厂方法，构造分页结果对象
     * @Date 2026/04/18 09:15
     * @Param [items 当前页数据列表, total 总记录数, page 当前页码, pageSize 每页大小]
     * @return PagedResult<T> 分页结果对象
     */
    public static <T> PagedResult<T> of(List<T> items, long total, int page, int pageSize) {
        return new PagedResult<>(items, total, page, pageSize);
    }

    /**
     * @Author lxy
     * @Description //对页码进行安全处理，最小值为 1
     * @Date 2026/04/18 09:15
     * @Param [page 原始页码]
     * @return int 安全的页码值
     */
    public static int safePage(int page) { return Math.max(1, page); }

    /**
     * @Author lxy
     * @Description //对每页大小进行安全处理，限制在 1 到 500 之间
     * @Date 2026/04/18 09:15
     * @Param [pageSize 原始每页大小]
     * @return int 安全的每页大小值
     */
    public static int safePageSize(int pageSize) { return Math.max(1, Math.min(pageSize, 2000)); }

    /**
     * @Author lxy
     * @Description //根据页码和每页大小计算 SQL 查询偏移量
     * @Date 2026/04/18 09:15
     * @Param [page 当前页码, pageSize 每页大小]
     * @return int SQL 查询偏移量
     */
    public static int safeOffset(int page, int pageSize) {
        return (safePage(page) - 1) * safePageSize(pageSize);
    }
}
