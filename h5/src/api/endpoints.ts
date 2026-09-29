/**
 * 小程序端服务器地址：唯一出口。
 *
 * 为什么必须写绝对地址：微信小程序的 `uni.request` / `uni.uploadFile` 只接受完整 URL，
 * 相对路径（`/api/...`）会直接报 invalid url 失败 —— 所以小程序分支必须写全 `协议://主机:端口`。
 *
 * ⚠️ 服务器 IP / 端口变更时，只改这一个文件，并同步处理：
 *    1) 微信开发者工具 → 详情 → 本地设置 → 勾选「不校验合法域名」
 *       （IP + http 无法配到小程序后台的合法域名，正式发布需 HTTPS 已备案域名）
 *    2) 服务器 `docker/.env` 的 `DOMAIN` / `OSS_ACCESS`（决定后端回显的图片/文件地址）
 *
 * 历史教训：此前这个地址被写死在 10 个文件里，服务器换 IP 后小程序所有接口全部失败，
 * 页面表现是"点按钮没反应"，排查成本很高。
 */

/** 平台访问入口（当前服务器 IP:端口，与 docker/.env 的 DOMAIN 保持一致） */
export const MP_ORIGIN = 'http://202.168.190.132:9071'

/** 小程序端通用 API 前缀（对应 `/api`） */
export const MP_API_BASE = `${MP_ORIGIN}/api`

/** 小程序端移动端 API 前缀（对应 `/api/h5`） */
export const MP_H5_API_BASE = `${MP_ORIGIN}/api/h5`
