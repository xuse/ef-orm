# 从Eclipse到IDEA IntelJ


## 基本思想
1 键盘优先，鼠标找不到的功能都到热键定义里去找。
2 绝大部分功能都在菜单上，popup+菜单功能都在热键上，热键最全面。
3、Tool Windows，可以控制隐藏显示。Tool Window可以呈现在三面的任意一个位置，可以浮动也可以
停泊或独立。
4、原生功能+原生插件+可安装插件实现各种扩展。

##安装使用基本步骤
1. 更改字体
微软雅黑，放大
1. 更改字符编码
UTF-8
1. CodeStyle 改为TAB模式<br/>
符合Eclipse和Spring的习惯

##安装插件
###必备插件——
* JettyRun 快速启动WEB工程
* MavenHelper：更好的Maven查看分析工具
* Grep Console 控制台渲染
* KeyPrompter:提示快捷键
* BrowseWorkAtCaret 在同单词中跳跃，但CTRL-ALT-DOWN热键默认被用于复制行，热键冲突需要修改

###个人需要插件
* JavaCC   支持jj文件
* SQL Query Plugin  支持数据库操作
* Markdown Support
* Markdown pic paste支持

##热键调节
1. 更改keymap热键为Eclipse风格<br/>
1. 微调keymap
  * Find 改为CTRL+F
  * Replace改为CTRL+H
  * Find in path 改为CTRL+SHIFT+S
  * rename file改为ALT+F2
  * Duplicate entireLine改为ALT+V(和BrowseWorkAtCaret插件错开)
  
###可装可不装（建议装）
* GsonFormat    将json转换为Bean
* Shifter 更换选中部分的写法，切换引号、更换属性等等。需要调教规则，热键有些繁琐需要简化

### 很好但不太适合我的插件——
* Properties to YAML
* Alibaba Java Coding Guidelines 阿里巴巴语法规范检查插件
AceJump不使用鼠标的情况下在当前屏幕内跳转，适合Linux习惯的人
Eclipse CodeFormatter  可以直接读取Eclipse的格式化规范文件，但我觉得现有的formatter已经和Eclipse差不多了

##原生插件高级应用
Language Injection： 在代码中插入另一种语言的编辑上下文，
EditorConfig，支持EditorConfig标准的配置文件来配置编辑器风格。默认开启一般不需关闭

### 可以关掉的原生插件
* Smali support
* JavaFX
#其他设置
开启项目自动编译



# 尚未解决问题
* 右侧的错误提示太不明显。试图找一个在Gutter上显示错误的插件（仿Eclipse，但找到的一个效果不好）
另外的方法是定义代码的错误字体和背景，让编译错误显得更加明显。
* 设置idea自动导入包
https://www.cnblogs.com/hongdada/p/6024574.html
勾选标注 1 选项，IntelliJ IDEA 将在我们书写代码的时候自动帮我们优化导入的包，比如自动去掉一些没有用到的包。 
勾选标注 2 选项，IntelliJ IDEA 将在我们书写代码的时候自动帮我们导入需要用到的包。但是对于那些同名的包，还是需要手动 Alt + Enter 进行导入的
* 实际测试发现，CTRL+ALT+=会触发一个事件，然后按ALT+ENTER会自动导入

* 好像原生就支持定义控制台输出的颜色，grepConsole这样用途是否明确？
* 检查插件 Awesome consloe是否有用。据说可以将Conosle信息加上到文件的链接。(难道现在没有吗)
