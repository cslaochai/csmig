# csmip 医保结算前置系统

HIS（cslk）↔ 医保局平台（csmis模拟 / 生产期真实平台）之间的**转发层**。

## 架构

```
HIS（cslk, 端口8080）
  ↓ HTTP 明文JSON（内网）
csmip前置系统（本项目, 端口8091, context-path /mip）
  ↓ TODO: 生产期加 SM2签名 + SM4加密 + CA证书 + 医保专网
csmis医保局端mock（端口8090, context-path /mss）  ← 开发期上游
```

## 职责

1. 接收HIS的医保请求（/gateway/2201、/gateway/2301、/gateway/2304、/insure/person/query等）
2. 落请求/响应日志（mip_gateway_log表，对账、排查重复扣费、审计用）
3. 开发期：明文透传给csmis
4. 生产期：在此层加国密SM2/SM4加签加密、CA证书管理、医保专网出口

## 技术栈

Spring Boot 3.2.5 / Java 21 / MyBatis-Plus / MySQL（库：mip_gateway）

## 快速启动

```bash
cd source/back_end_mip
mvn spring-boot:run
# 启动后监听 http://localhost:8091/mip
```

## HIS侧配置切换

cslk的 `application.yml` 里把医保上游地址从csmis指到csmip：

```yaml
his:
  insurance:
    channel:
      base-url: http://localhost:8091/mip   # 原来是 http://localhost:8090/mss
```

## TODO（生产期）

- [ ] SM2签名（医院CA私钥对报文摘要签名）
- [ ] SM4加密报文体
- [ ] CA证书加载与轮换
- [ ] 医保专网出口配置
- [ ] 上游超时/重试策略（前置机侧重试，HIS侧重试已在ChargeFacadeServiceImpl）
- [ ] 请求日志保留策略（按月分区/归档）
