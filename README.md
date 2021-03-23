# ImagePicker

## 介绍
基于Kotlin，AndroidX的仿微信图片选择器

## 使用方法

#### 第一步：项目增加Androidx库和Glide图片加载库
```
    implementation 'androidx.appcompat:appcompat:x.x.x'
    implementation "androidx.recyclerview:recyclerview:x.x.x"
    implementation 'com.github.bumptech.glide:glide:x.x.x'
    implementation 'androidx.constraintlayout:constraintlayout:2.0.4'
``` 

#### 第二步：在AndroidManifest.xml中添加Activity声明
``` 
<activity android:name="com.mx.imgpicker.app.ImgPickerActivity" />
``` 

#### 第三步：使用前需要添加相册、存储权限 
```
    Manifest.permission.CAMERA
    Manifest.permission.READ_EXTERNAL_STORAGE
    Manifest.permission.READ_EXTERNAL_STORAGE
```
注意：'没有权限进入选择页面会报错！'

#### 第四步：启动选择页面
```
val intent = PickerBuilder().setMaxSize(3).createIntent(this)
startActivityForResult(intent,0x22)
```
##### PickerBuilder参数说明
1. ’setMaxSize(size: Int)‘ 设置最大选择文件个数
2. ’setType(type: PickerType)‘ 设置类型 
    * PickerType.Image = 图片
    * PickerType.Video = 视频


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
