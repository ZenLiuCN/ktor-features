# ktor-features
[![](https://jitpack.io/v/ZenLiuCN/ktor-features.svg)](https://jitpack.io/#ZenLiuCN/ktor-features)

collection of features for ktor 
# properties feature
1. contains easy template to create ktor features
2. add power to easy fetch configuration Pojo from `application.conf`.

# features
## basic 
+ logback: integration logback configuration by HOCON in `application.conf`
+ redis: integration redis access by lettuce
## higher level
+ ez-hiarki: integration HiarkiCP
+ ez-ebean: integration ebean
+ ez-caffeine: integration cache with caffeine
+ ez-liquibase: integration liquibase 
+ ez-auth: warp on offical ktor-auth for easier extendability

# NOTE

most dependency not as `dependOn`, so you have to import by your own;

but with this design,you donot need to `exclusion` for use newer versions;

