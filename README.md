# ImagePicker
## 介绍
基于Kotlin，AndroidX的仿微信图片选择器
[![](https://jitpack.io/v/com.gitee.zhangmengxiong/MXImagePicker.svg)](https://jitpack.io/#com.gitee.zhangmengxiong/MXImagePicker)
库引用： 替换x.x.x 为最新版本
```
    implementation 'com.gitee.zhangmengxiong:MXImagePicker:x.x.x'
```

![Image text](https://gitee.com/zhangmengxiong/MXImagePicker/raw/master/imgs/screenshot1.png)
![Image text](https://gitee.com/zhangmengxiong/MXImagePicker/raw/master/imgs/screenshot2.png)

## 使用方法

#### 第一步：项目增加Androidx库和Glide图片加载库、图片缩放库
```
    implementation `androidx.appcompat:appcompat:x.x.x`
    implementation "androidx.recyclerview:recyclerview:x.x.x"
    implementation `com.github.bumptech.glide:glide:x.x.x`
    implementation `androidx.constraintlayout:constraintlayout:2.0.4`
    implementation 'com.github.chrisbanes:PhotoView:2.3.0'
```

#### 第二步：使用前需要添加相册、存储权限
```
    Manifest.permission.CAMERA
    Manifest.permission.READ_EXTERNAL_STORAGE
    Manifest.permission.WRITE_EXTERNAL_STORAGE
```
注意：`没有权限进入选择页面会报错！`

#### 第三步：启动选择页面
```
val intent = MXPickerBuilder().setMaxSize(3).createIntent(this)
startActivityForResult(intent,0x22)
```
##### MXPickerBuilder参数说明
1. `setMaxSize(size: Int)` 设置最大选择文件个数
2. `setType(type: PickerType)` 设置类型 
    * PickerType.Image = 图片
    * PickerType.Video = 视频
3. `setCameraEnable(enable: Boolean)` 设置是否启动拍摄功能，默认=true
4. `setMaxVideoLength(length: Int)` 当类型=Video时，可以选择视频最大时长限制，单位：秒   默认=-1 无限制

```
// 在图片选择器Activity创建时会回调这个方法，一般会通过这个来改变导航栏、状态栏的Theme,demo中搭配`ImmersionBar`来实现沉浸式效果
ImagePickerService.registerActivityCallback { activity ->
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
```
    <!--  页面背景色  -->
    <color name="picker_color_background">#333333</color>
   
    <!--  字体、icon颜色  --> 
    <color name="picker_color_important">#F1F1F1</color>

    <!--  选中状态颜色  -->  
    <color name="picker_color_select">#03CE65</color>
```

##### 多语言设置
将下面字符串定义放入对应的语言目录中，可以修改页面对应的文字提示
```
    <string name="picker_string_select">选择</string>
    <string name="picker_string_all">全部</string>
    <string name="picker_string_image_limit_tip">您最多只能选择 %s 张图片！</string>
    <string name="picker_string_video_limit_tip">您最多只能选择 %s 个视频！</string>
    <string name="picker_string_video_limit_length_tip">只能选择 %s 秒以内的视频</string>
    <string name="picker_string_need_permission_storage_camera">需要写入存储、相机权限</string>
    <string name="picker_string_need_permission_storage">需要读取存储权限</string>
    <string name="picker_string_open_failed">打开失败！</string>
```

dimens.xml 资源
```
    <!--  顶部导航栏高度  -->  
    <dimen name="picker_bar_height">50dp</dimen>
```

##### 自定义图片加载器（默认使用Glide）

通过继承实现接口`IImageLoader` ,并注册到服务`ImagePickerService`即可
```
// 数据对象
data class Item(val path: String, val uri: Uri, val mimeType: String, val time: Long, val name: String, val type: PickerType)

/**
 * 图片显示接口
 */
interface IImageLoader {
    fun displayImage(item: Item, imageView: ImageView)
}

/**
 * 提供默认Glide显示图片
 */
class GlideImageLoader : IImageLoader {
    override fun displayImage(item: Item, imageView: ImageView) {
        if (item.type == PickerType.Image) {
            Glide.with(imageView).load(item.uri).into(imageView)
        } else {
            Glide.with(imageView).load(Uri.fromFile(File(item.path))).into(imageView)
        }
    }
}

// 全局注册加载器，可以卸载Application里面，不影响启动速度
ImagePickerService.registerImageLoader(GlideImageLoader())
```

#### 第四步：获取返回结果
```
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == 0x22) {
            val paths = MXPickerBuilder.getPickerResult(data) //返回List<String>类型数据
            println(paths)
        }
    }
```



### 调取摄像头单独拍摄照片
```
val builder = MXCaptureBuilder().setType(MXPickerType.Image)

startActivityForResult(builder.createIntent(this), 0x11)

// 在onActivityResult获取结果
val file = builder.getCaptureFile()
```
### 调取摄像头单独拍摄视频
```
val builder = MXCaptureBuilder().setType(MXPickerType.Video).setMaxVideoLength(10)
startActivityForResult(builder.createIntent(this), 0x11)

// 在onActivityResult获取结果
val file = builder.getCaptureFile()
```


### 图片查看器
![Image text](https://gitee.com/zhangmengxiong/MXImagePicker/raw/master/imgs/screenshot3.png)
```
ImgShowActivity.open(
    this, arrayListOf(
        "http://videos.jzvd.org/v/饺子主动.jpg",
        "http://videos.jzvd.org/v/饺子运动.jpg"
    ), "图片详情"
)
```