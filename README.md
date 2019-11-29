[![Platform](https://img.shields.io/badge/平台-%20Android%20-brightgreen.svg)](https://github.com/yinhaide/Rocket-master/wiki)
[![Feature](https://img.shields.io/badge/特性-%20轻量级%20%7C%20稳定%20%20%7C%20强大%20-brightgreen.svg)](https://github.com/yinhaide/Rocket-master/wiki)
[![LICENSE](https://img.shields.io/hexpm/l/plug.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# HDWave
一个非常轻量级别的双波浪控件，内部自动管理动画生命周期，无需开发者干涉，可用于流量波动，电量波动等比较炫酷的波动场景。

<img src="image/wave.gif" width = "360px"/>

## 特性
+ **双正弦曲线设计的波动曲线，内部管理动画声音周期**

+ **支持切换形状（圆形、正方形）**

+ **支持切换边界宽度与边界颜色**

+ **支持切换双波形的颜色**

+ **支持切换波形高度**

+ **支持切换波形振幅**

+ **支持切换波形的频率,波形密度**

+ **支持切换波形平移速度**

## 如何快速集成

### 导入方式
在工程级别的**build.gradle**添加
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
在应用级别的**build.gradle**添加
```
api 'com.github.yinhaide:HDWave:0.0.1'
```

## 控件属性
```
Shape               : 形状 (0: 圆形; 1:方形; 默认圆形)
HeightRatio         : 高度比例 (0 ~ 1, 建议值: 0.5)
AmplitudeRatio      : 振幅比例 (0 ~ 1, 建议值: 0.04)
Frequency           : 横向波浪个数 (建议值: 1)
FrontColor          : 前波形颜色, 十六进制, 如: "#3CFFFFFF"
BehindColor         : 后波形颜色, 十六进制, 如: "#28FFFFFF"
ShiftTime           : 动画时间, 值越大越快(建议值: 4000)
HeightTime          : 动画时间, 值越大越快(建议值: 1000)
BorderWidth         : 边界宽度(建议值: 0)
BorderColor         : 边界颜色, 十六进制, 如: "#44FFFFFF"
```
## 控件可设置的方法
```
// 动态设置形状
public void setShape(ShapeType shapeType): shapeType: CIRCLE, shapeType: SQUARE

// 动态设置高度比例
public void setHeightRatio(float waveLevelRatio): (范围: 0 ~ 1)

// 动态设置振幅
public void setAmplitudeRatio(float amplitudeRatio):  (范围: 0 ~ 1)

// 两波形间距比例
public void setDistanceRatio(float distanceRatio):  (范围: 0 ~ 1)

// 动态设置波浪数
public void setFrequency(int frequency): 建议值1

// 动态设置前波浪色
public void setFrontColor(int frontWaveColor): 十六进制, 如:"#3CFFFFFF"

// 动态设置后波浪色
public void setBehindColor(int behindWaveColor): 十六进制, 如:"#28FFFFFF"

// 动态设置波浪水平滚动时间间隔
public void setShiftTime(int mWaveShiftAniTime): 建议值1000ms, 值越大, 滚动越慢

// 动态设置后边界色
public void setBorderColor(int behindWaveColor): 十六进制, 如:"#44FFFFFF"

// 动态设置后边宽度
public void setBorderWidth(int width): 建议0
```
## 范例
```
[XML]
    <com.de.wave.core.WaveView
        android:id="@+id/wave"
        android:layout_width="200dp"
        android:layout_height="200dp"
        android:layout_alignParentBottom="true"
        android:layout_centerHorizontal="true"
        app:de_amplitudeRatio="0.04"
        app:de_frequency="1"
        app:de_heightRatio="0.5"
        app:de_shiftTime="4000"
        app:de_heightTime="1000"
        app:de_shape="circle"
        app:de_borderWidth="0"
        app:de_borderColor="#44FFFFFF"
        app:de_behindColor="#3CFFFFFF"
        app:de_frontColor="#28FFFFFF" />

[JAVA] 
    WaveView waveView = findViewById(R.id.wave);
    waveView.setBorderWidth(mBorderWidth);
    waveView.setBorderColor(mBorderColor);
    waveView.setShapeType(WaveView.ShapeType.CIRCLE);
    waveView.setShapeType(WaveView.ShapeType.SQUARE);
    waveView.setFrontColor(Color.parseColor("#28f16d7a"));
    waveView.setBehindColor(Color.parseColor("#3cf16d7a"));
    waveView.setHeightRatio(ratio);
    waveView.setFrequencyr(i);
    waveView.setAmplitudeRatio(ratio);
    waveView.setShiftTime(seekBar.getProgress());
```

## 分享设计思路
+ 第一步：绘制两条正弦曲线，振幅为整个控件的一般高度，刚好填满整个控件
+ 第二步：其中的一个正弦可为偏移正弦，保证两条重合
+ 第三部：伸缩变换（正弦曲线在数值方向做压缩变换，改变振幅大小）
+ 第四步: 平移变换（开启平移动画，在波长的倍数下周期横向平移）

## 这个项目会持续更新中... 
> 都看到这里了，如果觉得写的可以或者对你有帮助的话，顺手给个星星点下Star~

这个控件内部采用一个Fragment框架，如果有兴趣的话可以去了解一下
+ [https://github.com/yinhaide/Rocket-master](https://github.com/yinhaide/Rocket-master)

## 关于我
+ **Email:** [123302687@qq.com](123302687@qq.com)
+ **Github:** [https://github.com/yinhaide](https://github.com/yinhaide)
+ **简书:** [https://www.jianshu.com/u/33c3dd2ceaa3](https://www.jianshu.com/u/33c3dd2ceaa3)
+ **CSDN:** [https://blog.csdn.net/yinhaide](https://blog.csdn.net/yinhaide)

## LICENSE
````
Copyright 2019 haide.yin(123302687@qq.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
````