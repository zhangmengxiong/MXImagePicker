# ImagePicker
## 介绍
基于Kotlin，AndroidX的仿微信图片选择器
[![](https://jitpack.io/v/com.gitee.zhangmengxiong/MXImagePicker.svg)](https://jitpack.io/#com.gitee.zhangmengxiong/MXImagePicker)
库引用： 替换1.4.3 为最新版本
```gradle
    implementation 'com.gitee.zhangmengxiong:MXImagePicker:1.5.1'
```

![Image text](https://gitee.com/zhangmengxiong/MXImagePicker/raw/master/imgs/screenshot1.png)
![Image text](https://gitee.com/zhangmengxiong/MXImagePicker/raw/master/imgs/screenshot2.png)

## 使用方法

#### 第一步：项目增加Androidx库和Glide图片加载库、图片缩放库
```gradle
    implementation "androidx.appcompat:appcompat:x.x.x"
    implementation "androidx.recyclerview:recyclerview:x.x.x"
    implementation "com.github.bumptech.glide:glide:x.x.x"
    implementation "androidx.constraintlayout:constraintlayout:2.0.4"
    implementation "com.github.chrisbanes:PhotoView:2.3.0"
```

#### 第二步：使用前需要修改‘AndroidManifest.xml’配置：添加相册、存储权限
```kotlin
    Manifest.permission.CAMERA
    Manifest.permission.READ_EXTERNAL_STORAGE

    // targetSdkVersion >= 29 的应用需要在application节点添加以下属性
    android:requestLegacyExternalStorage="true"
```
注意：`没有权限进入选择页面会报错！`

#### 第三步：启动选择页面
```kotlin
val intent = MXPickerBuilder().setMaxSize(3).createIntent(this)
startActivityForResult(intent,0x22)
```
##### MXPickerBuilder参数说明
1. `setMaxSize(size: Int)` 设置最大选择文件个数
2. `setType(type: PickerType)` 设置类型 
    * PickerType.Image = 图片
    * PickerType.Video = 视频
    * PickerType.ImageAndVideo = 图片 + 视频  混合选择
3. `setCameraEnable(enable: Boolean)` 设置是否启动拍摄功能，默认=true
4. `setMaxVideoLength(length: Int)` 当类型=Video时，可以选择视频最大时长限制，单位：秒   默认=-1 无限制
5. `setMaxListSize(size: Int)` 最长列表加载长度，防止图片过多时产生OOM  -1=不限制   默认限制长度=1000条

```kotlin
// 在图片选择器Activity创建时会回调这个方法，一般会通过这个来改变导航栏、状态栏的Theme,demo中搭配`ImmersionBar`来实现沉浸式效果
MXImagePicker.registerActivityCallback { activity ->
    ImmersionBar.with(activity)
            .autoDarkModeEnable(true)
            .statusBarColorInt(activity.resources.getColor(R.color.picker_color_background))
            .fitsSystemWindows(true)
            .navigationBarColor(R.color.picker_color_background)
            .init()
}

```

##### 页面颜色设置
将下面颜色值放如主项目的资源xml中，可以修改页面对应的颜色显示
```xml
    <!--  页面背景色  -->
    <color name="mx_picker_color_background">#333333</color>
   
    <!--  字体、icon颜色  --> 
    <color name="mx_picker_color_important">#F1F1F1</color>

    <!--  选中状态颜色  -->  
    <color name="mx_picker_color_select">#03CE65</color>
```

##### 多语言设置
将下面字符串定义放入对应的语言目录中，可以修改页面对应的文字提示
```xml
    <string name="mx_picker_string_select">选择</string>
    <string name="mx_picker_string_all">全部</string>
    <string name="mx_picker_string_image_limit_tip">您最多只能选择 %s 张图片！</string>
    <string name="mx_picker_string_video_limit_tip">您最多只能选择 %s 个视频！</string>
    <string name="mx_picker_string_video_limit_length_tip">只能选择 %s 秒以内的视频</string>
    <string name="mx_picker_string_need_permission_storage_camera">需要写入存储、相机权限</string>
    <string name="mx_picker_string_need_permission_storage">需要读取存储权限</string>
    <string name="mx_picker_string_open_failed">打开失败！</string>
    <string name="mx_picker_string_preview">预览</string>
    <string name="mx_picker_string_not_compress">原图</string>
    <string name="mx_picker_string_take_pic">拍摄图片</string>
    <string name="mx_picker_string_take_video">拍摄视频</string>
    <string name="mx_picker_string_show_list">图片查看</string>
```

dimens.xml 资源
```xml
    <!--  顶部导航栏高度  -->  
    <dimen name="mx_picker_bar_height">50dp</dimen>
```

##### 自定义图片加载器（默认使用Glide）

通过继承实现接口`IImageLoader` ,并注册到服务`MXImagePicker`即可
```kotlin
// 数据对象
data class MXItem(val path: String, val time: Long, val type: MXPickerType, val duration: Int = 0)

// 全局注册加载器，可以卸载Application里面，不影响启动速度
MXImagePicker.registerImageLoader { activity, item, imageView ->
    if (File(item.path).exists()) {
        Glide.with(activity).load(File(item.path))
            .placeholder(R.drawable.mx_icon_picker_image_place_holder).into(imageView)
    } else if (item.path.startsWith("http")) {
        Glide.with(activity).load(item.path)
            .placeholder(R.drawable.mx_icon_picker_image_place_holder).into(imageView)
    } else {
        Glide.with(activity).load(item.uri)
            .placeholder(R.drawable.mx_icon_picker_image_place_holder).into(imageView)
    }
}
```

#### 第四步：获取返回结果
```kotlin
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == RESULT_OK && requestCode == 0x22) {
        val paths = MXPickerBuilder.getPickerResult(data) ?: return //返回List<String>类型数据
        println(paths)
    }
}
```



### 调取摄像头单独拍摄照片
```kotlin
val builder = MXCaptureBuilder().setType(MXPickerType.Image)

startActivityForResult(builder.createIntent(this), 0x11)

// 在onActivityResult获取结果
val file = builder.getCaptureFile()
```
### 调取摄像头单独拍摄视频
```kotlin
val builder = MXCaptureBuilder().setType(MXPickerType.Video).setMaxVideoLength(10)
startActivityForResult(builder.createIntent(this), 0x11)

// 在onActivityResult获取结果
val file = builder.getCaptureFile()
```


### 图片查看器
![Image text](https://gitee.com/zhangmengxiong/MXImagePicker/raw/master/imgs/screenshot3.png)
```kotlin
MXImgShowActivity.open(
    this, arrayListOf(
        "http://videos.jzvd.org/v/饺子主动.jpg",
        "http://videos.jzvd.org/v/饺子运动.jpg"
    ), "图片详情"
)
```

### 单张图片压缩
```kotlin
val file = File(".../xx.png")
val scaleImg = MXImageCompress.from(context)
    .setCacheDir(applicationContext.cacheDir) // 缓存目录
    .setSupportAlpha(true) // 支持透明通道(’.png‘格式) 默认=’.jpg‘格式
    .setIgnoreFileSize(50) // 设置文件低于这个大小时，不进行压缩
    .compress(file)

```