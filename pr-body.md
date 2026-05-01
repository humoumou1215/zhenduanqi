解决 #28。

实现内容：
- 前端安装 jsonpath-plus 库
- 场景步骤 extract_rules 字段支持 JSONPath 变量提取
- 变量缓存到 diagnose store 的 variables Map
- 后续步骤占位符自动填充变量值
- 支持用户修改预填值后再执行
- 开始新诊断清空变量缓存
