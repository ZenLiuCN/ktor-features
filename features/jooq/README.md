# ktor-features
# jooq database feature
## feature

easy use datasource driven by `jooq` and `hikariCP`

## example
configration
```hocon
jooq{
 dialect:SQLite //required
 url:"jdbc:sqlite:data.sqlite" //required
 username: "" //optional as driver needs
 password: "" //optional as driver needs
 poolSize = 10 //optional
 cachePrepStmts = true //optional
 prepStmtCacheSize = 250 //optional
 prepStmtCacheSqlLimit = 2048 //optional
}
```
usage
```ktolin
Jooq.ctx.resultQuery("SELECT 1").excute() //just jooq
```
## notes



## dependencies
```groovy
dependencies {
    implementation project(":features:template")
    implementation project(":features:properties")
    compileOnly "io.ktor:ktor-server-core:$ktor_version"
    compileOnly "org.jooq:jooq:$jooq_version"
    compileOnly "com.zaxxer:HikariCP:$hikariCP_version"
}


```
