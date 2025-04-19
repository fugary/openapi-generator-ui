### 代码生成工具

目前有代码生成工具提供了`OpenAPI`文档，在此基础上封装一下，得到一个简单的代码生成工具。

原始工具：https://openapi-generator.tech/

封装工具：https://github.com/fugary/openapi-generator-ui

### 下载使用

默认端口是`9890`，启动后访问：http://localhost:9890

1. 下载运行
   1. 下载地址：https://github.com/fugary/openapi-generator-ui/releases
   2. 下载解压找到`bin`目录，运行里面的`start.bat`即可
2. Docker运行
   1. `docker run -p 9890:9890 fugary/openapi-generator-ui:latest`

### 在线测试

有需要可以尝试下在线测试版本：https://openapi-generator-ui.fugary.com/

### 支持内容

1. 支持填写OpenAPI URL，需要服务器能访问的地址，不能是本地地址或者有限制IP白名单的地址
   1. 支持Basic认证模式
2. 支持填写JSON/YAML文件内容来生成代码
3. 支持上传JSON/YAML文件来生成代码
4. 支持选择部分API来生成代码
5. 支持生成参数详细配置，并支持自动记住上次的配置

选择不同方式提供API数据

![image-20250419132831883](https://git.mengqingpo.com:8888/fugary/blogpic/-/raw/main/pictures/2025/04/19_13_28_40_image-20250419132831883.png)

选择部分接口

![image-20250419133252954](https://git.mengqingpo.com:8888/fugary/blogpic/-/raw/main/pictures/2025/04/19_13_32_54_image-20250419133252954.png)

可选参数配置

![image-20250419133342733](https://git.mengqingpo.com:8888/fugary/blogpic/-/raw/main/pictures/2025/04/19_13_33_44_image-20250419133342733.png)

生成后会自动弹出新窗口下载文件。
