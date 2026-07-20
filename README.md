# 简单记账 - 本地安卓记账APP

一个纯本地运行的安卓记账应用，无需联网，数据保存在手机本地SQLite数据库中。

## 功能特性

- ✅ **本地存储**：所有数据保存在手机本地SQLite数据库，完全离线使用
- ✅ **收支记录**：支持添加支出和收入记录
- ✅ **分类管理**：内置常用分类（餐饮、交通、购物等），支持自定义分类
- ✅ **统计功能**：按月统计收支情况，分类支出占比展示
- ✅ **备注功能**：每条记录支持添加备注
- ✅ **日期选择**：支持选择记录日期
- ✅ **数据删除**：长按记录可删除
- ✅ **emoji图标**：分类使用emoji图标，简洁直观

## 项目结构

```
SimpleLedger/
├── app/
│   ├── build.gradle              # 模块级Gradle配置
│   ├── proguard-rules.pro        # 混淆规则
│   └── src/main/
│       ├── AndroidManifest.xml   # 应用清单
│       ├── java/com/simpleledger/app/
│       │   ├── MainActivity.java           # 主界面
│       │   ├── AddRecordActivity.java      # 添加记录界面
│       │   ├── StatsActivity.java          # 统计界面
│       │   ├── CategoryManageActivity.java # 分类管理界面
│       │   ├── DatabaseHelper.java         # SQLite数据库帮助类
│       │   ├── Record.java                 # 记录数据模型
│       │   ├── Category.java               # 分类数据模型
│       │   ├── RecordAdapter.java          # 记录列表适配器
│       │   ├── CategoryAdapter.java        # 分类列表适配器
│       │   └── CategoryStatAdapter.java    # 统计列表适配器
│       └── res/                  # 资源文件
│           ├── layout/           # 布局文件
│           ├── values/           # 字符串、颜色、主题
│           ├── drawable/         # 图片资源
│           └── menu/             # 菜单资源
├── build.gradle                  # 项目级Gradle配置
├── settings.gradle               # Gradle设置
└── gradle.properties             # Gradle属性
```

## 构建和安装方法

### 方法一：使用Android Studio（推荐）

1. **下载安装Android Studio**
   - 官网：https://developer.android.com/studio
   - 安装时确保包含Android SDK（API Level 33）

2. **导入项目**
   - 打开Android Studio
   - 选择 "Open an Existing Project"
   - 选择本项目根目录

3. **等待同步**
   - Android Studio会自动下载Gradle和依赖（使用阿里云镜像加速）
   - 等待同步完成（右下角进度条消失）

4. **连接手机**
   - 手机开启"开发者选项"和"USB调试"
   - 用USB线连接电脑
   - 手机上允许USB调试授权

5. **运行安装**
   - 点击Android Studio顶部的绿色运行按钮（▶️）
   - 选择你的手机设备
   - 等待编译安装完成，APP会自动打开

### 方法二：命令行构建

1. **安装JDK 17**
   - 下载：https://adoptium.net/
   - 配置JAVA_HOME环境变量

2. **创建local.properties**
   在项目根目录创建 `local.properties` 文件，内容：
   ```
   sdk.dir=C\:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk
   ```
   （注意：路径根据你的Android SDK实际位置修改）

3. **使用Gradle Wrapper构建**
   ```powershell
   # Windows PowerShell
   .\gradlew.bat assembleDebug
   ```

4. **APK位置**
   构建成功后APK位于：
   ```
   app\build\outputs\apk\debug\app-debug.apk
   ```

5. **安装到手机**
   ```powershell
   # 确保手机已连接并授权
   adb install app\build\outputs\apk\debug\app-debug.apk
   ```

### 方法三：直接传输APK安装

如果你已经有APK文件（app-debug.apk）：
1. 将APK文件复制到手机
2. 在手机文件管理器中找到APK
3. 点击安装（需要允许"未知来源应用安装"）

## 使用说明

### 首页
- 顶部显示本月支出、收入和结余
- 列表显示所有记账记录
- 右下角粉色按钮：添加新记录
- 右上角齿轮按钮：分类管理
- 长按记录：删除该条记录

### 添加记录
- 顶部切换"支出"/"收入"
- 输入金额（必填）
- 选择分类（必选）
- 点击日期可修改日期
- 可添加备注（可选）
- 点击右上角"保存"完成

### 统计页面
- 底部导航切换到"统计"
- 查看本月总支出、总收入
- 查看各分类支出占比和进度条

### 分类管理
- 首页右上角齿轮图标进入
- 切换支出/收入分类标签
- 右下角+号：添加自定义分类
- 长按分类：删除分类（注意：删除后该分类的旧记录会显示异常）

## 技术说明

- **最低支持**：Android 5.0 (API Level 21)
- **目标版本**：Android 13 (API Level 33)
- **开发语言**：Java
- **数据存储**：SQLite本地数据库
- **UI框架**：Material Design + AndroidX
- **网络权限**：无（完全离线）

## 默认分类

**支出分类**：
- 🍔 餐饮
- 🚗 交通
- 🛒 购物
- 🎮 娱乐
- 💊 医疗
- 🏠 住房
- 📚 教育
- 📦 其他

**收入分类**：
- 💰 工资
- 🎁 奖金
- 📈 投资
- 💵 其他

## 注意事项

1. 数据仅保存在手机本地，卸载APP会丢失数据
2. 删除分类时，该分类下的历史记录不会被删除，但分类名可能显示异常
3. 建议定期截图备份重要数据
4. APP不申请任何网络权限，完全离线运行，保护隐私

## 常见问题

**Q: 构建时下载依赖很慢怎么办？**
A: 项目已配置阿里云Maven镜像，国内访问速度很快。如果还是慢，可以检查网络连接。

**Q: 手机连接电脑后没反应？**
A: 确认已开启USB调试，并且手机上点击了"允许此电脑调试"。可以运行 `adb devices` 检查设备是否连接。

**Q: 安装时提示"解析包错误"？**
A: 确保手机系统版本是Android 5.0或以上，并且APK文件完整下载。
