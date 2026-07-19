# Maven 安装和配置指南（Windows）

## 问题诊断
IDEA 报错 "No valid Maven installation found" 是因为：
1. 系统没有安装 Maven
2. 或者 Maven 安装了但没有配置环境变量
3. 或者 IDEA 没有正确配置 Maven 路径

---

## 解决方案 1：使用 IDEA 内置 Maven（推荐，快速）

### 步骤 1：打开 IDEA 设置
- Windows/Linux: `File` → `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Maven`
- Mac: `IntelliJ IDEA` → `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Maven`

### 步骤 2：配置 Maven home directory
- **Maven home directory**: 点击 `...` 按钮，选择 IDEA 自带的 Maven
  - 通常路径：`C:\Users\你的用户名\.gradle\caches\modules-2\files-2.1\org.apache.maven\apache-maven\版本号`
  - 或者选择 `Bundled (Maven 3)` 选项

### 步骤 3：配置用户设置文件
- **User settings file**: `C:\Users\你的用户名\.m2\settings.xml`（如果有的话）
- 如果没有，保持默认即可

### 步骤 4：应用设置
- 点击 `Apply` 和 `OK`

### 步骤 5：刷新 Maven 项目
- 在 IDEA 右侧的 `Maven` 面板中，点击刷新按钮（🔄）

---

## 解决方案 2：手动安装 Maven（推荐，稳定）

### 步骤 1：下载 Maven

访问 Maven 官网下载：
- 官网：https://maven.apache.org/download.cgi
- 下载：`apache-maven-3.9.9-bin.zip`（或最新版本）
- 也可以直接下载：https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip

### 步骤 2：解压 Maven

1. 创建安装目录，例如：`D:\develop\maven`
2. 将下载的 zip 文件解压到该目录
3. 解压后路径：`D:\develop\maven\apache-maven-3.9.9`

### 步骤 3：配置环境变量

#### 方法 A：通过系统设置配置（推荐）

1. 右键点击"此电脑" → "属性"
2. 点击"高级系统设置"
3. 点击"环境变量"

4. 在"系统变量"区域，点击"新建"：
   - **变量名**: `MAVEN_HOME`
   - **变量值**: `D:\develop\maven\apache-maven-3.9.9`（你的 Maven 安装路径）

5. 找到 `Path` 变量，点击"编辑"：
   - 点击"新建"
   - 添加：`%MAVEN_HOME%\bin`

6. 点击"确定"保存所有设置

#### 方法 B：通过 PowerShell 配置（临时）

```powershell
# 设置 MAVEN_HOME
[System.Environment]::SetEnvironmentVariable('MAVEN_HOME', 'D:\develop\maven\apache-maven-3.9.9', 'Machine')

# 添加到 PATH
$env:Path += ';D:\develop\maven\apache-maven-3.9.9\bin'

# 永久添加到 PATH
$oldPath = [System.Environment]::GetEnvironmentVariable('Path', 'Machine')
$newPath = $oldPath + ';D:\develop\maven\apache-maven-3.9.9\bin'
[System.Environment]::SetEnvironmentVariable('Path', $newPath, 'Machine')
```

### 步骤 4：验证安装

**重要：配置环境变量后，需要重启 IDEA 和新的终端窗口！**

打开新的 PowerShell 或 CMD，运行：
```bash
mvn -version
```

预期输出：
```
Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)
Maven home: D:\develop\maven\apache-maven-3.9.9
Java version: 17.0.x, vendor: Eclipse Adoptium
Default locale: zh_CN, platform encoding: GBK
OS name: Windows 10, version 10.0, arch: amd64, family: windows
```

如果看到类似输出，说明 Maven 安装成功！

### 步骤 5：在 IDEA 中配置 Maven

1. 打开 IDEA 设置
   - `File` → `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Maven`

2. 配置 Maven home directory：
   - 选择 `D:\develop\maven\apache-maven-3.9.9`

3. 配置 User settings file（可选）：
   - 默认：`C:\Users\你的用户名\.m2\settings.xml`
   - 如果没有，保持默认即可

4. 点击 `Apply` 和 `OK`

5. 在 IDEA 右侧的 `Maven` 面板中，点击刷新按钮（🔄）

---

## 解决方案 3：配置 Maven 镜像源（加速下载）

### 创建 settings.xml

在 `C:\Users\你的用户名\.m2\` 目录下创建 `settings.xml` 文件：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">

    <!-- 本地仓库路径，默认是 C:\Users\你的用户名\.m2\repository -->
    <localRepository>D:\develop\maven\repository</localRepository>

    <!-- 镜像配置 -->
    <mirrors>
        <!-- 阿里云镜像 -->
        <mirror>
            <id>aliyunmaven</id>
            <mirrorOf>*</mirrorOf>
            <name>阿里云公共仓库</name>
            <url>https://maven.aliyun.com/repository/public</url>
        </mirror>
    </mirrors>

    <!-- 仓库配置 -->
    <profiles>
        <profile>
            <id>jdk-17</id>
            <activation>
                <activeByDefault>true</activeByDefault>
                <jdk>17</jdk>
            </activation>
            <properties>
                <maven.compiler.source>17</maven.compiler.source>
                <maven.compiler.target>17</maven.compiler.target>
                <maven.compiler.compilerVersion>17</maven.compiler.compilerVersion>
            </properties>
        </profile>
    </profiles>

</settings>
```

### 在 IDEA 中应用 settings.xml

1. 打开 IDEA 设置
   - `File` → `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Maven`

2. 配置 User settings file：
   - 勾选 `Override`
   - 选择：`C:\Users\你的用户名\.m2\settings.xml`

3. 点击 `Apply` 和 `OK`

---

## 常见问题

### Q1: 配置环境变量后仍然无法识别 mvn 命令？
**A:**
- 确保重启了新的终端窗口
- 检查 `MAVEN_HOME` 路径是否正确
- 检查 `Path` 中是否包含 `%MAVEN_HOME%\bin`
- 尝试重启电脑

### Q2: IDEA 中 Maven 版本不正确？
**A:**
- 在 IDEA 设置中，明确指定 Maven home directory
- 不要选择 "Bundled (Maven 3)"，选择你安装的版本

### Q3: Maven 下载依赖很慢？
**A:**
- 配置阿里云镜像（见上方）
- 或者使用 IDEA 内置的 Maven，通常会有缓存

### Q4: 项目无法识别为 Maven 项目？
**A:**
- 确保项目根目录有 `pom.xml` 文件
- 在 IDEA 中：右键点击 `pom.xml` → `Add as Maven Project`

---

## 快速检查清单

- [ ] 已下载并解压 Maven
- [ ] 已配置 `MAVEN_HOME` 环境变量
- [ ] 已将 `%MAVEN_HOME%\bin` 添加到 `Path`
- [ ] 重启了终端，`mvn -version` 能正常显示版本
- [ ] 在 IDEA 中配置了 Maven home directory
- [ ] 在 IDEA 中点击了 Maven 刷新按钮
- [ ] 项目能正常下载依赖

---

## 推荐方案

**快速方案：** 使用 IDEA 内置 Maven
- 优点：快速，无需额外安装
- 缺点：可能版本不是最新的

**稳定方案：** 手动安装 Maven
- 优点：版本可控，可以在多个项目中使用
- 缺点：需要额外安装和配置

**建议：** 先使用 IDEA 内置 Maven 快速开始，后续有需要再手动安装。

---

## 验证项目是否正常

配置完成后，在 IDEA 中：

1. 打开右侧的 `Maven` 面板
2. 展开 `rag-qa-system` → `Lifecycle`
3. 双击 `clean` 清理项目
4. 双击 `compile` 编译项目
5. 查看底部是否有错误信息

如果一切正常，你就可以开始学习 RAG 项目了！🚀
