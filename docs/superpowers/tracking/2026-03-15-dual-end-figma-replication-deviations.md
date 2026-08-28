# Dual-End Figma Replication Deviations Log

## Allowed deviations
- none yet

## Deferred gaps
- H5 `/verify` 页面本轮已完成 Figma 风格重构，但继续保留当前代码中的路由权限行为：路由守卫与工作台快捷入口仍使用 `menu:h5:workorder:list`，提交按钮继续使用 `button:h5:workorder:verify`；矩阵中的 `menu:h5:verify` 作为设计权限标记暂不下沉到实现，避免引入新的鉴权模型。
- 当前工作目录 `/Users/tangxinglin/code/javacode/dgcp-oa` 不是 git 仓库，无法执行 plan 中要求的 worktree 创建、分步 commit 与基于 git 的开发分支收口；本轮按“非功能性流程偏差”记录，继续执行实现与验证。
- H5 `/verify` 页面在矩阵中记录的权限点为 `menu:h5:verify`，但当前代码路由守卫实际使用的是 `menu:h5:workorder:list`；在未完成页面重构前先保留现状，并作为待收口的文档/实现一致性差异。
- H5 Mine 页面已按 Figma 增加页面内“退出登录”操作，但壳层头部仍保留全局“退出”按钮；两者都会执行同一 logout 行为，本轮保留该双入口以避免影响现有壳层语义，后续如统一壳层设计可再收口。
- Web 工单中心列表本轮完成 Figma 模板化重构，但仍沿用本地静态数据源，未额外实现设计稿中的加载态/空态专属视觉分支；现阶段以保持当前数据语义与路由连续性为优先。
