# KidsLearn API 完善与去 Mock 化开发计划

## 背景
当前前端系统在 `USE_MOCK=false` 模式下，核心答题流已与服务端联通。但由于服务端部分外围业务的接口（如目标配置、宠物、成就系统、日历统计）尚未开发完成，前端 `api.ts` 被迫通过短路逻辑继续读取本地 `mock/data`。本计划旨在逐步补齐后端短板，实现真正的前后端全面数据连通。

---

## 阶段一：学习目标动态化（剥离硬编码进度）
**【优先级】**：⭐⭐⭐⭐⭐ (最高)
**【影响范围】**：`Child` 实体、`ChildDTO`、`ChildController`、前端 `appStore.ts` 每日任务初始化
**【难度】**：🔴⚪⚪⚪⚪ (简单)
**【状态】**：✅ 完成

**已完成项**：
- ✅ 数据库 V13 migration 已添加 `daily_target_en` / `daily_target_ma` 字段（默认 5/3）
- ✅ 后端 `Child` 实体包含 `dailyTargetEn` / `dailyTargetMa` 属性
- ✅ 后端 `ChildDTO` record 包含两个字段
- ✅ `appStore.ts` 的 `setChild()` 覆盖逻辑已就位
- ✅ `api.ts` `activateAccountWithCode` real 分支构建 `child` 对象时补充了 `dailyTargetEn` / `dailyTargetMa` 透传字段，使 `setChild` 中的覆盖逻辑能正常触发

---

## 阶段二：宠物系统落地（喂食与情感互动）
**【优先级】**：⭐⭐⭐⭐ (高)
**【影响范围】**：`PetController`、`PetService`、`PetStateRepository`、前端 `PetView.vue` 与 `api.ts`
**【难度】**：🔴🔴⚪⚪⚪ (中等)
**【状态】**：✅ 完成

**已完成项**：
- ✅ 后端 `PetController.java` — 3 个端点完整实现（`getPetState`、`feedPet`、`interactPet`）
- ✅ 后端 `PetService.java` — 喂食、互动、进化逻辑完整（含 `PET_EVOLVED` 日志）
- ✅ 前端 `api.ts` `USE_MOCK=false` 分支 — `getPetState` 和 `feedPet` 均已接入后端
- ✅ 前端 `PetView.vue` — `onMounted` 时调用 `syncPetState(childId)` 同步数据
- ✅ `appStore.feedPet()` 改为 async 函数，调用 `apiFeedPet(childId, item.id)` 并用后端返回值同步状态（星星、饱食度、快乐度）
- ✅ `PetView.vue` `feed()` 函数改为 async，以正确 await `feedPet()` 结果
- ✅ `PetController.java` 的 `feedPet` 端点通过 `childRepository` 查询并返回 `childStars`，`PetStateDTO` 增加 `childStars` 字段
- ✅ `api.ts` `feedPet` 响应类型增加 `childStars`，real 分支不再本地扣除星星

---

## 阶段三：成就与徽章体系研发
**【优先级】**：⭐⭐⭐ (中)
**【影响范围】**：`AchievementController`、`BadgeController`、`quiz_answers` 的事件监听聚合、前端成绩单页
**【难度】**：🔴🔴🔴⚪⚪ (偏难)
**【状态】**：✅ 前端已完成，后端已完成

**前端完成项**（2026-03-25）：
- `api.ts` 新增 `getAchievements(childId)`、`getBadges(childId)`，支持 `USE_MOCK` 切换
- `appStore.ts` 新增 `syncAchievements`、`syncBadges` action
- `PetView.vue` 接入 API，`onMounted` 时通过 store 同步数据
- 接口定义见 `api.interface.ts`：详见 `GetAchievementsResponse`、`GetBadgesResponse`

**后端完成项**（2026-03-25）：
- ✅ `AchievementController.java` — `GET /api/children/{childId}/achievements`
- ✅ `BadgeController.java` — `GET /api/children/{childId}/badges`
- ✅ `BadgeService.java` — 徽章查询与解锁判定完整实现
- ✅ `AchievementService.java` — 完整重写，成就判定逻辑完整（连击、题量、连续学习天数）
- ✅ `AchievementRepository.java` / `BadgeRepository.java` / `AchievementDefinitionRepository.java` / `BadgeDefinitionRepository.java`
- ✅ `AchievementDTO.java` / `BadgeDTO.java`
- ✅ 单元测试：18 个测试全部通过（AchievementServiceTest 7个、BadgeServiceTest 7个、AchievementControllerTest 2个、BadgeControllerTest 2个）

**待验证项**：
- [ ] 需在 IDE 中启动后端并验证 API 实际响应格式

---

## 阶段四：学习日历热力图分析接口
**【优先级】**：⭐⭐ (低，视觉锦上添花)
**【影响范围】**：`LearningSessionRepository`、`AnalyticsController`、前端主页打卡日历
**【难度】**：🔴🔴⚪⚪⚪ (中等，考察 SQL 分组能力)
**【状态】**：⚠️ 部分完成

**已完成项**：
- ✅ 后端 `CalendarController.java` — `GET /api/children/{childId}/calendar/{year}/{month}` 和 `GET /api/children/{childId}/calendar/stats` 均已实现
- ✅ 后端 `CalendarService.java` — SQL 聚合查询完整（含连续天数 `calculateStreak()` 计算）
- ✅ 前端 `CalendarView.vue` — `onMounted` 调用 `syncCalendar`，月份切换时 `watch` 重新获取
- ✅ `api.ts` `getMonthCalendar` 格式转换已修复（解 `ApiResponse` 包装 + 转为 `"2026-3": { days }` 结构）
- ✅ `api.ts` `getCalendarStats` 已删除（前端从未调用，是死代码）

**说明**：`CalendarView.vue` 实际使用的数据全部来自已有接口：
- 连续天数 → `child.streak`（Child 实体）
- 本月星星 → `child.stars`（Child 实体）
- 热力图 → `getMonthCalendar`


---

## 整体进度

| 阶段 | 前端 | 后端 | 备注 |
|------|------|------|------|
| 阶段一（学习目标动态化） | ✅ 完成 | ✅ 完成 | `api.ts` 透传 `dailyTargetEn/Ma` 已修复（2026-03-25） |
| 阶段二（宠物系统） | ✅ 完成 | ✅ 完成 | `appStore.feedPet()` 已改为调用后端 API（2026-03-25） |
| 阶段三（成就与徽章） | ✅ 完成 | ✅ 完成 | AchievementController/BadgeController 已实现，18个单元测试通过（2026-03-25） |
| 阶段四（日历热力图） | ✅ 完成 | ✅ 完成 | `getMonthCalendar` 格式已修复，`getCalendarStats` 为死代码已删除 |

**当前瓶颈**：无
