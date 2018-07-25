### 热修复-Tinker

----Tinker是微信官方的Android热补丁解决方案，它支持动态下发代码、So库以及资源，让应用能够在不需要重新安装的情况下实现更新。

**使用步骤**

- 步骤1：在项目的build.gradle中添加插件依赖
```
buildscript {

    repositories {
        jcenter()
    }
    
    dependencies {
        classpath 'com.android.tools.build:gradle:2.2.2'

        // TinkerPatch 插件
        classpath "com.tinkerpatch.sdk:tinkerpatch-gradle-plugin:1.2.8"
    }
}
```

- 步骤2：在app/build.gradle中添加依赖

```
dependencies {
    // 若使用annotation需要单独引用,对于tinker的其他库都无需再引用
    provided("com.tencent.tinker:tinker-android-anno:1.7.7")

    compile("com.tinkerpatch.sdk:tinkerpatch-android-sdk:1.2.8")
}
```
> 若使用 annotation 自动生成 Application， 需要单独引入 Tinker 的 tinker-android-anno 库。除此之外，我们无需再单独引入 tinker 的其他库。


- 步骤3：在app目录下，创建tinkerpatch.gradle文件，将 TinkerPatch 相关的配置都放于tinkerpatch.gradle中

**app/build.gradle中还需要引入**
```
apply from: 'tinkerpatch.gradle'
```

完整的app/build.gradle

```
apply plugin: 'com.android.application' 
apply from: 'tinkerpatch.gradle' 

android { 
    ... 
    
} 

dependencies { 
    ..... 
    
    // 若使用annotation需要单独引用,对于tinker的其他库都无需再引用 
    provided("com.tencent.tinker:tinker-android-anno:1.7.7") 
    
    compile("com.tinkerpatch.sdk:tinkerpatch-android-sdk:1.2.8")
}
```

- 步骤4：配置 tinkerpatchSupport 参数
```
apply plugin: 'tinkerpatch-support'

/**
 * 请按自己的需求修改为适应自己工程的参数
 */
def bakPath = file("${buildDir}/bakApk/")
def baseInfo = "app-1.0.0-0725-09-46-59" // 注意！！！  改成对应的路径
def variantName = "release" // debug or release

/**
 * 对于插件各参数的详细解析请参考
 * http://tinkerpatch.com/Docs/SDK
 */
tinkerpatchSupport {
    /** 可以在debug的时候关闭 tinkerPatch **/
    tinkerEnable = true

    /** 是否使用一键接入功能  **/
    reflectApplication = true

    /** 是否开启加固模式，只有在使用加固时才能开启此开关 **/
    protectedApp = false

    /** 补丁是否支持新增 Activity (exported必须为false)**/
    supportComponent = false

    autoBackupApkPath = "${bakPath}"

    /** 在tinkerpatch.com得到的appKey **/
    appKey = "c968f7acc78510d8"
    /** 注意: 若发布新的全量包, appVersion一定要更新 **/
    appVersion = "1.0.1"


    def pathPrefix = "${bakPath}/${baseInfo}/${variantName}/"
    def name = "${project.name}-${variantName}"

    baseApkFile = "${pathPrefix}/${name}.apk"
    baseProguardMappingFile = "${pathPrefix}/${name}-mapping.txt"
    baseResourceRFile = "${pathPrefix}/${name}-R.txt"
}

/**
 * 用于用户在代码中判断tinkerPatch是否被使能
 */
android {
    defaultConfig {
        buildConfigField "boolean", "TINKER_ENABLE", "${tinkerpatchSupport.tinkerEnable}"
    }
}

/**
 * 一般来说,我们无需对下面的参数做任何的修改
 * 对于各参数的详细介绍请参考:
 * https://github.com/Tencent/tinker/wiki/Tinker-%E6%8E%A5%E5%85%A5%E6%8C%87%E5%8D%97
 */
tinkerPatch {
    ignoreWarning = false
    useSign = true
    dex {
        dexMode = "jar"
        pattern = ["classes*.dex"]
        loader = []
    }
    lib {
        pattern = ["lib/*/*.so"]
    }

    res {
        pattern = ["res/*", "r/*", "assets/*", "resources.arsc", "AndroidManifest.xml"]
        ignoreChange = []
        largeModSize = 100
    }
    packageConfig {
    }
    sevenZip {
        zipArtifact = "com.tencent.mm:SevenZip:1.1.10"
        // path = "/usr/local/bin/7za"
    }
    buildConfig {
        keepDexApply = false
    }
}
```

> 一般来说，无需修改引用 android 的编译配置，也不用修改 tinker 插件原来的配置

打补丁包要真正需要修改的参数
```
def baseInfo = "app-1.0.0-0725-09-46-59"
def variantName = "release" 
appVersion = "1.0.1"
```

- 步骤5：初始化 TinkerPatch SDK
```
public class TinkerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 获得Tinker加载过程的信息
        ApplicationLike tinkerApplicationLike = TinkerPatchApplicationLike.getTinkerPatchApplicationLike();

        // 初始化TinkerPatch SDK, 更多配置可参照API章节中的,初始化SDK
        TinkerPatch.init(tinkerApplicationLike)
                .reflectPatchLibrary()
                .setPatchRollbackOnScreenOff(true)
                .setPatchRestartOnSrceenOff(true)
                .setFetchPatchIntervalByHours(3);

        // 每隔3个小时(通过setFetchPatchIntervalByHours设置)去访问后台时候有更新, 通过handler实现轮训的效果
        TinkerPatch.with().fetchPatchUpdateAndPollWithInterval();
    }
}
```

> 只需简单的初始化 TinkerPatch 的 SDK 即可，无需考虑 Tinker 是如何下载/合成/应用补丁包， 也无需引入各种各样 Tinker 的相关类

**注意**

----初始化的代码建议紧跟 super.onCreate(), 并且所有进程都需要初始化，已达到所有进程都可以被 patch 的目的。
如果确定只想在主进程中初始化 tinkerPatch，那也请至少在 :patch 进程中初始化，否则会有严重的 crash 问题。

- 步骤6：在Manifest.xml中配置Application，还要给SD卡读写权限
```
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>

<application
    android:name=".TinkerApp"
    android:allowBackup="true"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:supportsRtl="true"
    android:theme="@style/AppTheme">

    ...

</application>
```

- 步骤7：打包

> 注意：打包前记得配置签名

每次开发完成后，开始打包。打开Android Studio右侧的Gradle，选择assemableRelease打正式包，或者在Terminal执行：**gradlew assembleRelease**

![image](https://github.com/zhaoqingyue/Tinker/blob/master/img/gradle0.png)

完成后可以在文件夹build中找到生成的文件（称为：基准包）

![image](https://github.com/zhaoqingyue/Tinker/blob/master/img/gradle1.png)

> 将app-1.0.0-0725-09-36-29备份，打补丁包的时，需要用到