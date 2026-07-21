# 咖啡记账 (Coffee Ledger)

一个纯本地运行的安卓记账应用，无需联网，数据保存在手机本地 SQLite 数据库中。
采用暖米色 Glassmorphism 简约设计风格，集成记账、统计、日历、热力图、年账单、预算、项目、账户、内置计算器键盘等丰富功能。

---

## 目录

- [功能特性](#功能特性)
- [界面预览](#界面预览)
- [项目结构](#项目结构)
- [构建和安装](#构建和安装)
- [使用说明](#使用说明)
- [技术架构](#技术架构)
- [设计规范](#设计规范)
- [版本历史](#版本历史)
- [注意事项](#注意事项)
- [常见问题](#常见问题)
- [版权声明](#版权声明)

---

## 功能特性

### 核心记账
- **收支记录**：支持支出 / 收入双向切换，每笔记录可关联分类、项目、账户、日期、备注、标签
- **内置计算器键盘**：禁用系统输入法，自带数字 + 运算符键盘，支持表达式实时计算（如 `120*3+50 = 410`），千分位实时格式化
- **金额联想**：输入备注时自动推荐匹配分类（如输入"午餐"自动选中"餐饮"）
- **双击快速选分类**：双击分类区域弹出"最近使用分类"列表
- **备注小字**：所有账单列表（明细、日历等）每笔账单分类名称下方均显示浅灰色备注小字

### 分类管理
- **无限级嵌套**：支持分类无限层级（如 `餐饮 → 早餐 → 路边摊`），面包屑导航
- **低饱和度莫兰迪色系**：分类图标采用饱和度 < 30% 的莫兰迪色 + 白色线条图案
- **外圈圆环选中态**：选中时显示 2.5px `#C9B8A0` 外圈圆环，2px 间距，保留原图标颜色
- **emoji 图标**：分类使用 emoji 图标视觉区分

### 多维度统计
- **明细页（明细 Tab）**：月份切换 + 月度收支结余汇总 + 账单列表
- **统计页（统计 Tab）**：月度总收支 + 各分类支出占比 + 进度条可视化
- **年账单**：暖棕色汇总卡片（年结余/年收入/年支出）+ 12 个月份列表（倒序）+ 月账单/年账单切换
- **年度热力图**：GitHub 风格的年度账单热力图，点击日期展开当日明细
- **日历视图**：月历形式展示每日收支，点击日期查看当日详情

### 项目与账户
- **项目管理**：支持创建多个项目（如"旅游"、"装修"），每笔账单可关联项目，查看项目明细
- **账户管理**：多账户支持（现金、银行卡、支付宝等），实时显示账户余额
- **多标签系统**：每笔账单可打多个标签（如 `#家庭聚餐 #生日`），支持跨维度统计

### 预算与提醒
- **预算管理**：可设置月度预算，实时显示预算使用进度
- **定时提醒**：支持设置每日记账提醒时间，本地通知推送
- **重要标记**：每笔账单可标记为"重要"，列表中高亮显示

### 设置
- **触觉反馈**：所有点击操作触发轻微震动，可在设置中关闭
- **CSV 导出**：一键导出所有账单数据为 CSV 文件
- **年度热力图入口**：从设置页进入年度账单热力图
- **年账单入口**：从设置页进入年账单汇总页

---

## 界面预览

### 整体风格
- **暖米色主题**：主色 `#D6C7B2`，背景 `#F8F3ED`，深灰文字 `#3D352C`
- **Glassmorphism 玻璃拟态**：20px 模糊 + 22% 白色透明 + 1px 白色内描边 + 柔和阴影
- **8 点网格间距**：所有元素间距遵循 8dp 倍数
- **沉浸式状态栏**：`WindowCompat.setDecorFitsSystemWindows(false)` + 各页面自行处理 insets
- **底部导航栏**：三列布局（明细 / 记账 / 统计）+ 凸起圆形加号按钮（圆心位于 Tab 栏上边缘）
- **顶部导航栏**：统一 56dp 高度，矩形玻璃条样式

### 弹窗设计
- **毛玻璃弹窗**：95% 不透明暖白色背景 + `setDimAmount(0.85f)` 增强背景虚化
- **圆角矩形**：20dp 圆角 + 1px 浅灰色边框

---

## 项目结构

```
SimpleLedger/
├── app/
│   ├── build.gradle                    # 模块级 Gradle 配置（含签名配置）
│   ├── proguard-rules.pro              # 混淆规则
│   └── src/main/
│       ├── AndroidManifest.xml         # 应用清单（注册所有 Activity 和权限）
│       ├── java/com/simpleledger/app/
│       │   │
│       │   ├── MainActivity.java                # 主界面（Fragment 切换 + 底部导航）
│       │   │
│       │   ├── RecordsFragment.java             # 明细页 Fragment
│       │   ├── AddRecordFragment.java           # 记账页 Fragment（含内置计算器键盘）
│       │   ├── StatsFragment.java               # 统计页 Fragment
│       │   │
│       │   ├── AddRecordActivity.java           # 添加记录 Activity
│       │   ├── StatsActivity.java               # 统计详情 Activity
│       │   ├── CalendarActivity.java            # 日历视图 Activity
│       │   ├── CategoryManageActivity.java      # 无限级分类管理 Activity
│       │   ├── ProjectsActivity.java            # 项目列表 Activity
│       │   ├── ProjectDetailActivity.java       # 项目详情 Activity
│       │   ├── AccountsActivity.java            # 账户管理 Activity
│       │   ├── BudgetActivity.java              # 预算管理 Activity
│       │   ├── HeatmapActivity.java             # 年度热力图 Activity
│       │   ├── PivotActivity.java               # 年账单汇总 Activity（5.1 重新设计）
│       │   ├── SettingsActivity.java            # 设置 Activity
│       │   │
│       │   ├── DatabaseHelper.java              # SQLite 数据库帮助类（v5）
│       │   ├── Record.java                      # 记录数据模型
│       │   ├── Category.java                    # 分类数据模型（含 parentId 支持无限级）
│       │   ├── Account.java                     # 账户数据模型
│       │   ├── Project.java                     # 项目数据模型
│       │   ├── Budget.java                      # 预算数据模型
│       │   │
│       │   ├── RecordAdapter.java               # 账单列表适配器
│       │   ├── CategoryAdapter.java             # 分类选择适配器
│       │   ├── CategoryStatAdapter.java         # 分类统计适配器
│       │   ├── ProjectAdapter.java              # 项目列表适配器
│       │   │
│       │   ├── CalculatorKeyboard.java          # 内置计算器键盘（禁用系统输入法）
│       │   ├── ExpressionEvaluator.java         # 表达式求值器（递归下降解析器）
│       │   ├── HeatmapView.java                 # 热力图自定义 View
│       │   ├── HapticHelper.java                # 触觉反馈工具类
│       │   └── ReminderReceiver.java            # 定时提醒 BroadcastReceiver
│       │
│       └── res/
│           ├── layout/                          # 布局文件（20+ 个）
│           ├── values/                          # 字符串、颜色、主题
│           ├── drawable/                        # 图片资源（含 Glassmorphism 背景）
│           └── menu/                            # 菜单资源
│
├── build.gradle                                 # 项目级 Gradle 配置
├── settings.gradle                              # Gradle 设置（含 foojay toolchain resolver）
├── gradle.properties                            # Gradle 属性（含 JDK 17 配置）
├── local.properties                             # 本地 SDK 路径
└── README.md                                    # 本文档
```

---

## 构建和安装

### 方法一：直接安装 APK（推荐普通用户）

1. 从 GitHub 仓库下载最新的 `CoffeeLedger_v5.1.apk`（或 `SimpleLedger.apk`）
2. 将 APK 文件传输到手机
3. 在手机文件管理器中找到 APK，点击安装
4. 首次安装需允许"未知来源应用安装"
5. 打开 APP 即可使用

> **APK 已使用开发者签名证书签名**（v1 JAR + v2 APK Signature Scheme），可直接安装。

### 方法二：使用 Android Studio（推荐开发者）

1. **下载安装 Android Studio**
   - 官网：https://developer.android.com/studio
   - 安装时确保包含 Android SDK（API Level 33）

2. **导入项目**
   - 打开 Android Studio
   - 选择 "Open an Existing Project"
   - 选择本项目根目录

3. **等待同步**
   - Android Studio 会自动下载 Gradle 和依赖（已配置阿里云镜像加速）
   - 等待同步完成（右下角进度条消失）

4. **连接手机**
   - 手机开启"开发者选项"和"USB 调试"
   - 用 USB 线连接电脑
   - 手机上允许 USB 调试授权

5. **运行安装**
   - 点击 Android Studio 顶部的绿色运行按钮（▶️）
   - 选择你的手机设备
   - 等待编译安装完成，APP 会自动打开

### 方法三：命令行构建

1. **安装 JDK 17**
   - 下载：https://adoptium.net/
   - 配置 `JAVA_HOME` 环境变量
   - 或修改 `gradle.properties` 中的 `org.gradle.java.home` 指向 JDK 17 路径

2. **创建 `local.properties`**
   ```
   sdk.dir=C\:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk
   ```

3. **使用 Gradle Wrapper 构建**
   ```powershell
   # Windows PowerShell
   .\gradlew.bat assembleRelease
   ```

4. **APK 位置**
   ```
   app\build\outputs\apk\release\app-release.apk
   ```

5. **安装到手机**
   ```powershell
   adb install app\build\outputs\apk\release\app-release.apk
   ```

---

## 使用说明

### 首页（明细 Tab）

- **顶部**：月份切换 + 当月支出/收入/结余汇总
- **列表**：所有账单记录，每条显示分类图标 + 分类名 + 备注（浅灰小字）+ 日期 + 金额
- **右上角**：三点菜单按钮（分类管理、设置等）
- **底部加号**：凸起的圆形记账按钮
- **长按记录**：弹出更多操作菜单（删除、查看详情等）

### 记账页（记账 Tab）

1. **切换类型**：顶部切换"支出" / "收入"
2. **输入金额**：使用内置计算器键盘，支持表达式（如 `120*3+50`），顶部显示实时计算结果
3. **选择分类**：点击分类图标，外圈圆环高亮表示选中；双击分类区域可弹出"最近使用分类"
4. **选择日期**：点击日期选择器
5. **选择项目**：点击项目标签，可选择关联项目（可选）
6. **选择账户**：点击账户标签，选择扣款账户
7. **添加备注**：输入备注文本，输入时会自动联想分类
8. **添加标签**：可输入多个标签（如 `#家庭聚餐 #生日`）
9. **保存**：点击右下角"保存"按钮

### 统计页（统计 Tab）

- **顶部**：本月总支出、总收入、结余
- **分类列表**：各分类支出金额 + 占比百分比 + 进度条
- **切换月份**：左右滑动或点击月份选择器

### 日历视图

- **月历形式**：显示当月每一天
- **日期标记**：有账单的日期下方显示小圆点，颜色深浅表示金额大小
- **点击日期**：展开当日所有账单详情
- **左右切换**：上下月切换

### 年账单

- **顶部切换栏**：左侧年份选择（带下拉箭头）+ 右侧"月账单"/"年账单"切换标签
- **汇总卡片**（暖棕色渐变背景）：
  - 第一行：年结余（30sp 加粗大字）
  - 第二行：年收入 / 年支出（左右分栏）
- **月份列表**（倒序，12→1）：
  - 四列：月份 | 月收入 | 月支出 | 月结余
  - 右侧灰色箭头 `>` 表示可点击查看月详情
  - 表头浅灰，数据深灰，金额右对齐

### 年度热力图

- **GitHub 风格**：365 天的账单热力图
- **颜色深浅**：表示当日支出金额
- **点击日期**：展开当日所有账单详情
- **年份切换**：顶部年份选择器

### 分类管理

- **无限级嵌套**：点击分类进入子分类，面包屑导航显示层级路径
- **添加分类**：右下角 + 号添加子分类
- **长按分类**：删除分类（注意：删除后该分类的历史记录会显示异常）

### 项目管理

- **项目列表**：显示所有项目及总支出
- **添加项目**：右下角 + 号
- **点击项目**：进入项目详情，查看该项目下所有账单
- **记账时关联**：在记账页可选择关联项目

### 账户管理

- **账户列表**：显示所有账户及当前余额
- **添加账户**：支持现金、银行卡、支付宝、微信等
- **记账时选择**：在记账页可选择扣款账户

### 预算管理

- **设置预算**：设置月度总预算
- **进度显示**：实时显示当月预算使用情况
- **超支提醒**：超出预算时颜色变红

### 设置

- **通用**：触觉反馈开关、记账提醒时间设置
- **数据分析**：年度账单热力图入口、年账单入口
- **数据管理**：CSV 导出
- **关于**：版本信息

---

## 技术架构

### 技术栈

- **最低支持**：Android 5.0 (API Level 21)
- **目标版本**：Android 13 (API Level 33)
- **开发语言**：Java
- **数据存储**：SQLite 本地数据库（DatabaseHelper v5）
- **UI 框架**：Material Design 1.9.0 + AndroidX AppCompat 1.6.1
- **图表库**：MPAndroidChart v3.1.0
- **网络权限**：无（完全离线）

### 核心技术实现

#### 沉浸式状态栏
```java
WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
    int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
    v.setPadding(0, statusBarHeight, 0, 0);
    return insets;
});
```

#### 内置计算器键盘
- 禁用系统输入法：`et.setShowSoftInputOnFocus(false)`
- 递归下降解析器实现表达式求值（支持 `+`、`-`、`*`、`/`、`(`、`)`）
- 实时千分位格式化，光标位置自动修正

#### 无限级分类
- 数据库 `categories` 表含 `parent_id` 字段
- `CategoryManageActivity` 递归加载子分类，面包屑导航显示层级路径

#### Glassmorphism 玻璃拟态
- 20px 模糊 + 22% 白色透明 + 1px 白色内描边
- 阴影：`0 6px 24px rgba(139, 125, 107, 0.1)`

#### FAB 凸起按钮定位
```java
int fabMarginBottom = navBarHeight + tabHeightPx - fabRadiusPx;
// 圆心位于 Tab 栏上边缘线，上半圆完全突出在 Tab 栏上方
```

#### 定时提醒
- `AlarmManager` + `NotificationChannel` 实现本地定时通知
- `ReminderReceiver` 接收提醒广播并触发通知

---

## 设计规范

### 色彩系统

| 用途 | 颜色 | 色值 |
|------|------|------|
| 主色 | 暖棕色 | `#D6C7B2` |
| 主色深 | 深暖棕 | `#B5A48A` |
| 背景 | 浅米白 | `#F8F3ED` |
| 背景深 | 米色 | `#EDE4D6` |
| 文字主色 | 深灰棕 | `#3D352C` |
| 文字次色 | 中灰棕 | `#8B7D6B` |
| 文字提示 | 浅灰 | `#B8AC9A` |
| 支出色 | 柔和红 | `#D4756A` |
| 收入色 | 柔和绿 | `#7BA678` |
| 结余色 | 柔和蓝 | `#8AA4B8` |

### 莫兰迪分类色系（饱和度 < 30%）

| 名称 | 色值 |
|------|------|
| 米色 | `#D6C7B2` |
| 粉色 | `#D4B5A8` |
| 蓝色 | `#A8B8C4` |
| 绿色 | `#A8B5A0` |
| 紫色 | `#B5A8B4` |
| 黄色 | `#D4C9A8` |
| 棕色 | `#B8A690` |
| 灰色 | `#B0B0B0` |

### 间距系统

- 遵循 **8 点网格系统**：所有元素间距为 8dp 的倍数（8dp、16dp、24dp、32dp）
- 文字行宽限制：30-38 个中文字符

### 分类选中态规范

- 外圈圆环：2.5px `#C9B8A0` 描边
- 圆环间距：2px（不遮挡图标）
- 选中时图标颜色和图案保持不变
- 文字：主色加粗

---

## 版本历史

### v5.1（当前版本）
- **修复**：5.0 版本 APP 闪退问题（`AddRecordFragment.loadCategories()` 中 `getView()` 空指针）
- **重新设计**：年维度透视表 → 年账单汇总页面
  - 顶部切换栏（年份选择 + 月账单/年账单切换）
  - 暖棕色渐变汇总卡片（年结余/年收入/年支出）
  - 12 个月份列表（倒序，四列 + 右侧箭头）
  - 移除技术说明文字，改为简洁按月汇总列表
- **构建环境**：系统 JRE 21 无 javac，下载 Temurin JDK 17 用于编译

### v5.0
- 设置页辅助说明小字完整显示（不再被截断）
- 弹窗毛玻璃大幅增强（95% 不透明 + dimAmount 0.85）
- 明细列表每笔账单下方添加浅灰色备注小字
- 修复记账页分类只显示 8 个的问题（NestedScrollView + 手动设置 RecyclerView 高度）

### v4.6
- 修复 APK 未签名导致安装失败（使用 debug keystore 签名 release 包）
- 修复顶部三点菜单按钮被状态栏裁切
- 修复底部加号按钮被 Tab 栏遮挡

### v4.5
- 多维度透视表（行=月份，列=分类，值=金额）
- GitHub 风格年度账单热力图
- 内置计算器键盘（支持表达式 `120*3+50`）
- 无限级分类嵌套（如 `餐饮 → 早餐 → 路边摊`）
- 多标签系统（如 `#家庭聚餐 #生日`）
- 智能辅助：金额自动格式化、分类联想
- 定时记账提醒
- 长按菜单、快捷手势

### v4.2
- 分类图标采用低饱和度莫兰迪色系（饱和度 < 30%）+ 白色线条图案
- 分类选中态改为 2.5px `#C9B8A0` 外圈圆环 + 2px 间距
- 修复明细页按钮遮挡问题

### v4.0
- Glassmorphism 玻璃拟态 UI 重设计
- 暖米色主色调（`#D6C7B2`）
- 8 点网格间距系统
- 卡片式、仪表盘、单列布局

### v3.0
- 日历视图（CalendarActivity）
- 账单备注支持
- 明细页月份选择器
- 支出支持项目选择

---

## 注意事项

1. **数据本地存储**：所有数据仅保存在手机本地 SQLite 数据库，卸载 APP 会丢失数据
2. **定期备份**：建议定期使用"设置 → CSV 导出"功能备份重要数据
3. **删除分类**：删除分类时，该分类下的历史记录不会被删除，但分类名可能显示异常
4. **完全离线**：APP 不申请任何网络权限，完全离线运行，保护隐私
5. **系统要求**：Android 5.0 (API 21) 及以上

---

## 常见问题

**Q: 构建时提示"Java compiler is not available"怎么办？**
A: 系统缺少 JDK（仅有 JRE）。下载 Temurin JDK 17（https://adoptium.net/），并在 `gradle.properties` 中配置：
```
org.gradle.java.home=C:/路径/jdk-17
```

**Q: 构建时下载依赖很慢怎么办？**
A: 项目已配置阿里云 Maven 镜像，国内访问速度很快。如仍慢，检查网络连接。

**Q: 手机连接电脑后没反应？**
A: 确认已开启 USB 调试，并且手机上点击了"允许此电脑调试"。运行 `adb devices` 检查设备是否连接。

**Q: 安装时提示"解析包错误"？**
A: 确保手机系统版本是 Android 5.0 或以上，并且 APK 文件完整下载。

**Q: APK 安装失败提示"未签名"？**
A: 从 v4.6 起，所有 release APK 均已使用开发者签名证书签名（v1 + v2 方案），可直接安装。如遇此问题，请确认下载的是最新版本的 APK。

**Q: 闪退怎么办？**
A: v5.0 版本存在已知闪退问题（已在 v5.1 修复）。请升级到 v5.1 或更高版本。

**Q: 分类只显示 8 个？**
A: v5.0 版本存在此问题（已在 v5.1 修复）。请升级到 v5.1 或更高版本。

---

## 版权声明

**本软件由 CofZhang 完成，享有版权。**

© 2026 CofZhang. All rights reserved.

未经授权，禁止复制、传播、修改或用于商业用途。
