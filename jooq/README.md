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
 extra{ //optional
    dictionary{
         dialect:SQLite //required
         url:"jdbc:sqlite:data.sqlite" //required
         username: "" //optional as driver needs
         password: "" //optional as driver needs
         poolSize = 10 //optional
         cachePrepStmts = true //optional
         prepStmtCacheSize = 250 //optional
         prepStmtCacheSqlLimit = 2048 //optional
    }
 }
}
```
usage
```ktolin
Jooq.ctx.resultQuery("SELECT 1").fetch() //just pure JOOQ
Jooq.get()?.resultQuery("SELECT 1").fetch() //use default DslContext
Jooq.get("ctx")?.resultQuery("SELECT 1").fetch() //use default DslContext
Jooq.get("dictionary")?.resultQuery("SELECT 1").fetch() //use extra DslContext
Jooq["ctx"]?.resultQuery("SELECT 1").fetch() //use default DslContext
Jooq["dictionary"]?.resultQuery("SELECT 1").fetch() //use extra DslContext
Jooq.get<Dao>("ctx")?.insert() //fast use dao
```
## notes



## dependencies
```groovy
dependencies {
    implementation project(":features:template")
    implementation project(":features:properties")
    compileOnly "io.ktor:ktor-server-core:$ktor_version"
    implementation "org.jooq:jooq:$jooq_version"
    implementation "com.zaxxer:HikariCP:$hikariCP_version"
}


```
