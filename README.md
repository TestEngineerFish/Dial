# Dial
自定义表盘式时间选择范围选择器，可自定义样式、可选择时间区间等,同时也支持编辑模式
* 设置选择范围 0～24 点<br>

![slider0_24](https://gitee.com/tingyusha/images/raw/master/0-24.gif)

<br>

* 设置选择范围 3～18 点<br>

![slider3_18](https://gitee.com/tingyusha/images/raw/master/3-18.gif)


使用步骤
#
由于具体约束都在 DialView.kt 中已设置，使用相当简单，只需要构造一个DialView类即可：<br>

>具体配置修改可参看源代码<br>

>>
```kotlin
DialView(start = 10, end = 3, editType = TimeEditType.EDIT, modifier = Modifier)
```
觉得有用,给个star,小小鼓励,谢谢啦🙏
如果有不明白的，或者有疑问、建议之类，都欢迎拍砖，Issues或者直接联系：<br>
*微信：_ShaTingYu<br>
*QQ: 916878440<br>
