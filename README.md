# ImagePicker

## 介绍
基于Kotlin，AndroidX的仿微信图片选择器

## 使用方法

#### 第一步：项目增加Androidx库和Glide图片加载库、图片缩放库
```
    implementation `androidx.appcompat:appcompat:x.x.x`
    implementation "androidx.recyclerview:recyclerview:x.x.x"
    implementation `com.github.bumptech.glide:glide:x.x.x`
    implementation `androidx.constraintlayout:constraintlayout:2.0.4`
    implementation 'com.github.chrisbanes:PhotoView:2.3.0'
``` 

#### 第二步：在AndroidManifest.xml中添加Activity声明
``` 
<activity android:name="com.mx.imgpicker.app.ImgPickerActivity" />
``` 

#### 第三步：使用前需要添加相册、存储权限 
```
    Manifest.permission.CAMERA
    Manifest.permission.READ_EXTERNAL_STORAGE
    Manifest.permission.WRITE_EXTERNAL_STORAGE
```
注意：`没有权限进入选择页面会报错！`

#### 第四步：启动选择页面
```
val intent = PickerBuilder().setMaxSize(3).createIntent(this)
startActivityForResult(intent,0x22)
```
##### PickerBuilder参数说明
1. `setMaxSize(size: Int)` 设置最大选择文件个数
2. `setType(type: PickerType)` 设置类型 
    * PickerType.Image = 图片
    * PickerType.Video = 视频
3. `setActivityCallback(call: ((activity: AppCompatActivity) -> Unit))` 在图片选择器Activity创建时会回调这个方法，一般会通过这个来改变导航栏、状态栏的Theme,demo中搭配`ImmersionBar`来实现沉浸式效果
```
setActivityCallback { activity ->
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

#### 第五步：获取返回结果
```
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == 0x22) {
            val paths = PickerBuilder.getPickerResult(data) //返回List<String>类型数据
            println(paths)
        }
    }
```
