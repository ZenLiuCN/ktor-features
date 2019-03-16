# ktor-features
# jooq database feature
## feature

easy use datasource driven by `jooq` and `hikariCP`

## example
configration
```hocon
redis{
  url = "redis://TOKEN@IP:PROT/DATABASE?timeout=10s"
  url = ${?REDIS}
}
```
usage
```ktolin
Redis.redis //RedisClient<String,String>
Redis.redisJson //RedisClient<String,JsonNode>
```
## notes



## dependencies
```groovy
dependencies {
    implementation project(":features:template")
    implementation project(":features:properties")
    compileOnly "io.ktor:ktor-server-core:$ktor_version"
    compileOnly "com.fasterxml.jackson.core:jackson-databind:$jackson_version"
    compileOnly  "io.lettuce:lettuce-core:$lettuce_version"
}


```
